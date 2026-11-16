from __future__ import annotations

from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.security import encrypt_sensitive, get_current_user
from app.models.entities import Device, Incident, User, UserProfile
from app.schemas.dto import (
    DeviceCreate,
    DeviceResponse,
    UserProfileResponse,
    UserProfileUpdate,
)
from app.services.audit import record_audit

router = APIRouter(tags=["Users & Devices"])


@router.get("/users/me", response_model=UserProfileResponse)
def get_me(user: User = Depends(get_current_user)):
    p = user.profile
    return UserProfileResponse(
        user_id=user.id,
        email=user.email,
        phone=user.phone,
        role=user.role,
        full_name=p.full_name if p else user.email,
        preferred_language=p.preferred_language if p else "en",
        district=p.district if p else None,
        consent_location_capture=p.consent_location_capture if p else False,
        consent_ai_processing=p.consent_ai_processing if p else True,
        notify_push=p.notify_push if p else True,
        notify_sync_alerts=p.notify_sync_alerts if p else True,
    )


@router.patch("/users/me", response_model=UserProfileResponse)
def update_me(
    body: UserProfileUpdate,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    p = user.profile or UserProfile(user_id=user.id, full_name=user.email)
    if p.id is None:
        db.add(p)
    if body.full_name is not None:
        p.full_name = body.full_name
    if body.phone is not None:
        user.phone = body.phone
    if body.preferred_language is not None:
        p.preferred_language = body.preferred_language
    if body.district is not None:
        p.district = body.district
    if body.consent_location_capture is not None:
        p.consent_location_capture = body.consent_location_capture
    if body.consent_ai_processing is not None:
        p.consent_ai_processing = body.consent_ai_processing
    if body.notify_push is not None:
        p.notify_push = body.notify_push
    if body.notify_sync_alerts is not None:
        p.notify_sync_alerts = body.notify_sync_alerts

    record_audit(
        db,
        actor_user_id=user.id,
        actor_role=user.role,
        action="user.profile_updated",
        resource_type="user",
        resource_id=user.id,
    )
    db.commit()
    db.refresh(user)
    return get_me(user)


@router.get("/users/me/export")
def export_my_data(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    incidents = db.query(Incident).filter(Incident.user_id == user.id, Incident.deleted_at.is_(None)).all()
    devices = db.query(Device).filter(Device.user_id == user.id, Device.deleted_at.is_(None)).all()
    record_audit(
        db,
        actor_user_id=user.id,
        actor_role=user.role,
        action="user.data_exported",
        resource_type="user",
        resource_id=user.id,
    )
    db.commit()
    return {
        "exported_at": datetime.now(timezone.utc).isoformat(),
        "user": {"id": user.id, "email": user.email, "role": user.role},
        "devices": [
            {"id": d.id, "brand": d.brand, "model": d.model, "imei_last4": d.imei_last4}
            for d in devices
        ],
        "incidents": [
            {"id": inc.id, "title": inc.title, "category": inc.category, "status": inc.status}
            for inc in incidents
        ],
    }


@router.delete("/users/me", status_code=status.HTTP_200_OK)
def delete_my_account(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    user.is_active = False
    user.deleted_at = datetime.now(timezone.utc)
    record_audit(
        db,
        actor_user_id=user.id,
        actor_role=user.role,
        action="user.soft_deleted",
        resource_type="user",
        resource_id=user.id,
    )
    db.commit()
    return {"status": "deleted", "user_id": user.id}


@router.get("/devices", response_model=list[DeviceResponse])
def list_devices(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    rows = (
        db.query(Device)
        .filter(Device.user_id == user.id, Device.deleted_at.is_(None))
        .order_by(Device.created_at.desc())
        .all()
    )
    return [
        DeviceResponse(
            id=d.id,
            brand=d.brand,
            model=d.model,
            color=d.color,
            os_type=d.os_type,
            imei_masked=f"***********{d.imei_last4}" if d.imei_last4 else None,
            sim_operator=d.sim_operator,
            is_primary=d.is_primary,
            created_at=d.created_at,
        )
        for d in rows
    ]


@router.post("/devices", response_model=DeviceResponse, status_code=status.HTTP_201_CREATED)
def create_device(
    body: DeviceCreate,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    clean_imei = "".join(c for c in (body.imei or "") if c.isdigit())
    device = Device(
        user_id=user.id,
        brand=body.brand,
        model=body.model,
        color=body.color,
        os_type=body.os_type,
        imei_encrypted=encrypt_sensitive(clean_imei) if clean_imei else None,
        imei_last4=clean_imei[-4:] if len(clean_imei) >= 4 else None,
        sim_operator=body.sim_operator,
        is_primary=body.is_primary,
    )
    db.add(device)
    record_audit(
        db,
        actor_user_id=user.id,
        actor_role=user.role,
        action="device.created",
        resource_type="device",
        resource_id=device.id,
    )
    db.commit()
    db.refresh(device)
    return DeviceResponse(
        id=device.id,
        brand=device.brand,
        model=device.model,
        color=device.color,
        os_type=device.os_type,
        imei_masked=f"***********{device.imei_last4}" if device.imei_last4 else None,
        sim_operator=device.sim_operator,
        is_primary=device.is_primary,
        created_at=device.created_at,
    )
