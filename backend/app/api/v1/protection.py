from __future__ import annotations

from datetime import datetime, timezone
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.security import encrypt_sensitive, get_current_user
from app.models.entities import (
    ChecklistItem,
    ContactShare,
    EmergencyResource,
    Incident,
    TrustedContact,
    User,
)
from app.schemas.dto import (
    ChecklistItemResponse,
    ChecklistItemUpdate,
    ContactShareCreate,
    TrustedContactCreate,
    TrustedContactResponse,
)
from app.services.audit import record_audit
from app.services.seed import ensure_default_checklist_for_user

router = APIRouter(tags=["Protection, Contacts & Resources"])


@router.get("/protection/checklist", response_model=list[ChecklistItemResponse])
def get_checklist(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    checklist = ensure_default_checklist_for_user(db, user.id)
    db.commit()
    items = (
        db.query(ChecklistItem)
        .filter(ChecklistItem.checklist_id == checklist.id)
        .order_by(ChecklistItem.sort_order.asc())
        .all()
    )
    return [
        ChecklistItemResponse(
            id=i.id,
            category=i.category,
            sort_order=i.sort_order,
            title=i.title,
            what_to_do=i.what_to_do,
            why_it_matters=i.why_it_matters,
            official_url=i.official_url,
            is_completed=i.is_completed,
            completed_at=i.completed_at,
            notes=i.notes,
        )
        for i in items
    ]


@router.patch("/protection/checklist/{item_id}", response_model=ChecklistItemResponse)
def update_checklist_item(
    item_id: str,
    body: ChecklistItemUpdate,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    item = db.get(ChecklistItem, item_id)
    if not item or item.checklist.user_id != user.id:
        raise HTTPException(status_code=404, detail="Checklist item not found")

    if body.is_completed is not None:
        item.is_completed = body.is_completed
        item.completed_at = datetime.now(timezone.utc) if body.is_completed else None
    if body.notes is not None:
        item.notes = body.notes

    record_audit(
        db,
        actor_user_id=user.id,
        actor_role=user.role,
        action="checklist.item_updated",
        resource_type="checklist_item",
        resource_id=item.id,
        details={"category": item.category, "is_completed": item.is_completed},
    )
    db.commit()
    db.refresh(item)
    return ChecklistItemResponse(
        id=item.id,
        category=item.category,
        sort_order=item.sort_order,
        title=item.title,
        what_to_do=item.what_to_do,
        why_it_matters=item.why_it_matters,
        official_url=item.official_url,
        is_completed=item.is_completed,
        completed_at=item.completed_at,
        notes=item.notes,
    )


@router.get("/contacts", response_model=list[TrustedContactResponse])
def list_contacts(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    rows = (
        db.query(TrustedContact)
        .filter(TrustedContact.user_id == user.id)
        .order_by(TrustedContact.created_at.desc())
        .all()
    )
    return [
        TrustedContactResponse(
            id=c.id,
            name=c.name,
            relationship_label=c.relationship_label,
            phone_masked=c.phone_masked,
            is_verified=c.is_verified,
            notify_sms_consent=c.notify_sms_consent,
            created_at=c.created_at,
        )
        for c in rows
    ]


@router.post("/contacts", response_model=TrustedContactResponse, status_code=status.HTTP_201_CREATED)
def add_contact(
    body: TrustedContactCreate,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    clean = body.phone.strip()
    masked = f"{'*' * max(0, len(clean) - 4)}{clean[-4:]}"
    c = TrustedContact(
        user_id=user.id,
        name=body.name.strip(),
        relationship_label=body.relationship_label.strip(),
        phone_masked=masked,
        phone_encrypted=encrypt_sensitive(clean) or "",
        is_verified=False,
        notify_sms_consent=body.notify_sms_consent,
    )
    db.add(c)
    record_audit(
        db,
        actor_user_id=user.id,
        actor_role=user.role,
        action="contact.added",
        resource_type="trusted_contact",
        resource_id=c.id,
    )
    db.commit()
    db.refresh(c)
    return TrustedContactResponse(
        id=c.id,
        name=c.name,
        relationship_label=c.relationship_label,
        phone_masked=c.phone_masked,
        is_verified=c.is_verified,
        notify_sms_consent=c.notify_sms_consent,
        created_at=c.created_at,
    )


@router.delete("/contacts/{contact_id}")
def delete_contact(
    contact_id: str,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    c = db.get(TrustedContact, contact_id)
    if not c or c.user_id != user.id:
        raise HTTPException(status_code=404, detail="Trusted contact not found")
    db.delete(c)
    record_audit(
        db,
        actor_user_id=user.id,
        actor_role=user.role,
        action="contact.removed",
        resource_type="trusted_contact",
        resource_id=contact_id,
    )
    db.commit()
    return {"deleted": True}


@router.post("/contacts/share", status_code=status.HTTP_201_CREATED)
def share_incident_with_contact(
    body: ContactShareCreate,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if not body.consent_confirmed:
        raise HTTPException(
            status_code=400,
            detail="Explicit user consent is required before sharing incident information.",
        )
    inc = db.get(Incident, body.incident_id)
    contact = db.get(TrustedContact, body.contact_id)
    if not inc or inc.user_id != user.id or not contact or contact.user_id != user.id:
        raise HTTPException(status_code=404, detail="Incident or trusted contact not found")

    share = ContactShare(
        incident_id=inc.id,
        contact_id=contact.id,
        consent_confirmed=True,
        include_location=body.include_location,
    )
    db.add(share)
    record_audit(
        db,
        actor_user_id=user.id,
        actor_role=user.role,
        action="contact.incident_shared",
        resource_type="contact_share",
        resource_id=share.id,
        details={"include_location": body.include_location},
    )
    db.commit()
    return {"share_id": share.id, "shared_at": share.shared_at.isoformat()}


@router.post("/contacts/share/{share_id}/revoke")
def revoke_contact_share(
    share_id: str,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    share = db.get(ContactShare, share_id)
    if not share:
        raise HTTPException(status_code=404, detail="Share record not found")
    inc = db.get(Incident, share.incident_id)
    if not inc or inc.user_id != user.id:
        raise HTTPException(status_code=403, detail="Forbidden")
    share.revoked_at = datetime.now(timezone.utc)
    record_audit(
        db,
        actor_user_id=user.id,
        actor_role=user.role,
        action="contact.share_revoked",
        resource_type="contact_share",
        resource_id=share_id,
    )
    db.commit()
    return {"revoked": True, "revoked_at": share.revoked_at.isoformat()}


@router.get("/resources")
def list_emergency_resources(
    category: Optional[str] = Query(default=None),
    db: Session = Depends(get_db),
):
    q = db.query(EmergencyResource)
    if category:
        q = q.filter(EmergencyResource.category == category)
    rows = q.order_by(EmergencyResource.category.asc(), EmergencyResource.name.asc()).all()
    return [
        {
            "id": r.id,
            "name": r.name,
            "category": r.category,
            "address": r.address,
            "contact_number": r.contact_number,
            "source_url": r.source_url,
            "verification_date": r.verification_date,
            "is_verified": r.is_verified,
            "latitude": r.latitude,
            "longitude": r.longitude,
        }
        for r in rows
    ]


@router.get("/resources/{resource_id}")
def get_emergency_resource(resource_id: str, db: Session = Depends(get_db)):
    r = db.get(EmergencyResource, resource_id)
    if not r:
        raise HTTPException(status_code=404, detail="Resource not found")
    return {
        "id": r.id,
        "name": r.name,
        "category": r.category,
        "address": r.address,
        "contact_number": r.contact_number,
        "source_url": r.source_url,
        "verification_date": r.verification_date,
        "is_verified": r.is_verified,
    }
