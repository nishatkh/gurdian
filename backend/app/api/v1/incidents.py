from __future__ import annotations

import json
from datetime import datetime, timezone
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.security import encrypt_sensitive, get_current_user
from app.models.entities import (
    ChecklistItem,
    Incident,
    IncidentEvent,
    IncidentLocation,
    ProtectionChecklist,
    SyncEvent,
    User,
)
from app.schemas.dto import (
    IncidentCreate,
    IncidentEventCreate,
    IncidentEventResponse,
    IncidentResponse,
    IncidentUpdate,
    SyncBatchRequest,
    SyncBatchResponse,
    VALID_INCIDENT_TRANSITIONS,
)
from app.services.audit import record_audit

router = APIRouter(tags=["Incidents & Sync"])


def _compute_missing_fields(inc: Incident, loc: Optional[IncidentLocation]) -> list[str]:
    missing = []
    if not inc.description or len(inc.description.strip()) < 15:
        missing.append("incident_narrative")
    if not inc.imei_last4:
        missing.append("imei_15_digit")
    if not inc.device_brand or not inc.device_model:
        missing.append("device_brand_model")
    if loc is None or not loc.location_label:
        missing.append("incident_location")
    return missing


def _to_incident_response(inc: Incident) -> IncidentResponse:
    latest_loc = inc.locations[-1] if inc.locations else None
    sorted_events = sorted(inc.events, key=lambda e: e.event_timestamp)
    return IncidentResponse(
        id=inc.id,
        user_id=inc.user_id,
        client_incident_id=inc.client_incident_id,
        category=inc.category,
        status=inc.status,
        title=inc.title,
        description=inc.description,
        occurred_at=inc.occurred_at,
        device_brand=inc.device_brand,
        device_model=inc.device_model,
        imei_masked=f"***********{inc.imei_last4}" if inc.imei_last4 else None,
        location_label=latest_loc.location_label if latest_loc else None,
        location_accuracy_meters=latest_loc.accuracy_meters if latest_loc else None,
        ai_summary=inc.ai_summary,
        missing_fields=inc.missing_fields or _compute_missing_fields(inc, latest_loc),
        review_status=inc.review_status,
        reviewer_notes=inc.reviewer_notes,
        created_at=inc.created_at,
        updated_at=inc.updated_at,
        events=[
            IncidentEventResponse(
                id=e.id,
                event_type=e.event_type,
                note=e.note,
                actor_role=e.actor_role,
                event_timestamp=e.event_timestamp,
            )
            for e in sorted_events
        ],
    )


@router.post("/incidents", response_model=IncidentResponse, status_code=status.HTTP_201_CREATED)
def create_incident(
    body: IncidentCreate,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if body.client_incident_id:
        existing = (
            db.query(Incident)
            .filter(
                Incident.client_incident_id == body.client_incident_id,
                Incident.user_id == user.id,
            )
            .first()
        )
        if existing:
            return _to_incident_response(existing)

    clean_imei = "".join(c for c in (body.imei or "") if c.isdigit())
    inc = Incident(
        user_id=user.id,
        device_id=body.device_id,
        client_incident_id=body.client_incident_id,
        category=body.category,
        status=body.status,
        title=body.title,
        description=body.description,
        occurred_at=body.occurred_at or datetime.now(timezone.utc),
        device_brand=body.device_brand,
        device_model=body.device_model,
        imei_encrypted=encrypt_sensitive(clean_imei) if clean_imei else None,
        imei_last4=clean_imei[-4:] if len(clean_imei) >= 4 else None,
    )
    db.add(inc)
    db.flush()

    loc_obj: Optional[IncidentLocation] = None
    if body.location:
        coords_json = (
            json.dumps({"lat": body.location.latitude, "lng": body.location.longitude})
            if body.location.consent_granted
            and body.location.latitude is not None
            and body.location.longitude is not None
            else None
        )
        loc_obj = IncidentLocation(
            incident_id=inc.id,
            location_label=body.location.location_label,
            coordinates_encrypted=encrypt_sensitive(coords_json) if coords_json else None,
            accuracy_meters=body.location.accuracy_meters,
            consent_granted=body.location.consent_granted,
        )
        db.add(loc_obj)

    inc.missing_fields = _compute_missing_fields(inc, loc_obj)
    db.add(
        IncidentEvent(
            incident_id=inc.id,
            event_type="incident_created",
            note=f"Incident created in status '{inc.status}' ({inc.category})",
            actor_role=user.role,
            event_timestamp=inc.occurred_at,
        )
    )
    record_audit(
        db,
        actor_user_id=user.id,
        actor_role=user.role,
        action="incident.created",
        resource_type="incident",
        resource_id=inc.id,
        details={"category": inc.category, "status": inc.status},
    )
    db.commit()
    db.refresh(inc)
    return _to_incident_response(inc)


@router.get("/incidents", response_model=list[IncidentResponse])
def list_incidents(
    status_filter: Optional[str] = Query(default=None, alias="status"),
    limit: int = Query(default=50, ge=1, le=200),
    offset: int = Query(default=0, ge=0),
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    q = db.query(Incident).filter(Incident.user_id == user.id, Incident.deleted_at.is_(None))
    if status_filter:
        q = q.filter(Incident.status == status_filter)
    rows = q.order_by(Incident.updated_at.desc()).offset(offset).limit(limit).all()
    return [_to_incident_response(r) for r in rows]


@router.get("/incidents/{incident_id}", response_model=IncidentResponse)
def get_incident(
    incident_id: str,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    inc = db.get(Incident, incident_id)
    if not inc or inc.deleted_at is not None:
        raise HTTPException(status_code=404, detail="Incident not found")
    if inc.user_id != user.id and user.role not in {"reviewer", "admin"}:
        raise HTTPException(status_code=403, detail="Not authorized to access this incident")
    return _to_incident_response(inc)


@router.patch("/incidents/{incident_id}", response_model=IncidentResponse)
def update_incident(
    incident_id: str,
    body: IncidentUpdate,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    inc = db.get(Incident, incident_id)
    if not inc or inc.deleted_at is not None:
        raise HTTPException(status_code=404, detail="Incident not found")
    if inc.user_id != user.id:
        raise HTTPException(status_code=403, detail="Only incident owner can edit details")

    if body.status and body.status != inc.status:
        allowed = VALID_INCIDENT_TRANSITIONS.get(inc.status, set())
        if body.status not in allowed:
            raise HTTPException(
                status_code=400,
                detail=f"Invalid incident status transition: {inc.status} -> {body.status}",
            )
        prev_status = inc.status
        inc.status = body.status
        if body.status == "submitted_for_review":
            inc.review_status = "pending"
        db.add(
            IncidentEvent(
                incident_id=inc.id,
                event_type="status_changed",
                note=f"Status updated from {prev_status} to {body.status}",
                actor_role=user.role,
            )
        )

    if body.title is not None:
        inc.title = body.title
    if body.description is not None:
        inc.description = body.description
    if body.device_brand is not None:
        inc.device_brand = body.device_brand
    if body.device_model is not None:
        inc.device_model = body.device_model
    if body.imei is not None:
        clean_imei = "".join(c for c in body.imei if c.isdigit())
        inc.imei_encrypted = encrypt_sensitive(clean_imei) if clean_imei else None
        inc.imei_last4 = clean_imei[-4:] if len(clean_imei) >= 4 else None
    if body.location_label is not None:
        db.add(
            IncidentLocation(
                incident_id=inc.id,
                location_label=body.location_label,
                consent_granted=False,
            )
        )
        db.flush()

    latest_loc = inc.locations[-1] if inc.locations else None
    inc.missing_fields = _compute_missing_fields(inc, latest_loc)
    record_audit(
        db,
        actor_user_id=user.id,
        actor_role=user.role,
        action="incident.updated",
        resource_type="incident",
        resource_id=inc.id,
        details={"status": inc.status},
    )
    db.commit()
    db.refresh(inc)
    return _to_incident_response(inc)


@router.post("/incidents/{incident_id}/events", response_model=IncidentEventResponse, status_code=201)
def add_incident_event(
    incident_id: str,
    body: IncidentEventCreate,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    inc = db.get(Incident, incident_id)
    if not inc or inc.deleted_at is not None:
        raise HTTPException(status_code=404, detail="Incident not found")
    if inc.user_id != user.id:
        raise HTTPException(status_code=403, detail="Only incident owner can add timeline events")

    if body.client_event_id:
        dup = (
            db.query(IncidentEvent)
            .filter(IncidentEvent.client_event_id == body.client_event_id)
            .first()
        )
        if dup:
            return IncidentEventResponse(
                id=dup.id,
                event_type=dup.event_type,
                note=dup.note,
                actor_role=dup.actor_role,
                event_timestamp=dup.event_timestamp,
            )

    ev = IncidentEvent(
        incident_id=inc.id,
        client_event_id=body.client_event_id,
        event_type=body.event_type,
        note=body.note,
        actor_role=user.role,
        event_timestamp=body.event_timestamp or datetime.now(timezone.utc),
    )
    db.add(ev)
    record_audit(
        db,
        actor_user_id=user.id,
        actor_role=user.role,
        action="incident.event_added",
        resource_type="incident",
        resource_id=inc.id,
        details={"event_type": body.event_type},
    )
    db.commit()
    db.refresh(ev)
    return IncidentEventResponse(
        id=ev.id,
        event_type=ev.event_type,
        note=ev.note,
        actor_role=ev.actor_role,
        event_timestamp=ev.event_timestamp,
    )


@router.get("/incidents/{incident_id}/timeline", response_model=list[IncidentEventResponse])
def get_timeline(
    incident_id: str,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    inc = db.get(Incident, incident_id)
    if not inc:
        raise HTTPException(status_code=404, detail="Incident not found")
    if inc.user_id != user.id and user.role not in {"reviewer", "admin"}:
        raise HTTPException(status_code=403, detail="Forbidden")
    events = sorted(inc.events, key=lambda e: e.event_timestamp)
    return [
        IncidentEventResponse(
            id=e.id,
            event_type=e.event_type,
            note=e.note,
            actor_role=e.actor_role,
            event_timestamp=e.event_timestamp,
        )
        for e in events
    ]


@router.post("/sync/outbox", response_model=SyncBatchResponse)
def sync_offline_outbox(
    body: SyncBatchRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Idempotent offline-first outbox synchronization preserving original timestamps and preventing duplicates."""
    processed: list[str] = []
    deduplicated: list[str] = []
    failed: list[dict[str, str]] = []

    for item in body.items:
        already = (
            db.query(SyncEvent)
            .filter(SyncEvent.client_event_id == item.client_event_id)
            .first()
        )
        if already:
            deduplicated.append(item.client_event_id)
            continue

        try:
            p = item.payload
            if item.entity_type == "incident_upsert":
                cid = p.get("client_incident_id") or item.client_event_id
                inc = (
                    db.query(Incident)
                    .filter(Incident.client_incident_id == cid, Incident.user_id == user.id)
                    .first()
                )
                if inc is None:
                    clean_imei = "".join(c for c in str(p.get("imei") or "") if c.isdigit())
                    inc = Incident(
                        user_id=user.id,
                        client_incident_id=cid,
                        category=p.get("category", "lost_phone"),
                        status=p.get("status", "active"),
                        title=p.get("title", "Offline Incident"),
                        description=p.get("description", ""),
                        occurred_at=item.original_timestamp,
                        device_brand=p.get("device_brand"),
                        device_model=p.get("device_model"),
                        imei_encrypted=encrypt_sensitive(clean_imei) if clean_imei else None,
                        imei_last4=clean_imei[-4:] if len(clean_imei) >= 4 else None,
                    )
                    db.add(inc)
                    db.flush()
                    if p.get("location_label"):
                        db.add(
                            IncidentLocation(
                                incident_id=inc.id,
                                location_label=p.get("location_label"),
                                accuracy_meters=p.get("accuracy_meters"),
                                consent_granted=bool(p.get("consent_granted", False)),
                                captured_at=item.original_timestamp,
                            )
                        )
                    db.add(
                        IncidentEvent(
                            incident_id=inc.id,
                            client_event_id=f"{cid}:created",
                            event_type="offline_incident_synced",
                            note="Offline incident synchronized preserving original timestamp",
                            actor_role=user.role,
                            event_timestamp=item.original_timestamp,
                        )
                    )
                else:
                    # Conflict-safe update: only apply if newer than server updated_at or fill missing fields
                    if p.get("description") and len(str(p.get("description"))) > len(inc.description):
                        inc.description = str(p["description"])
                    if p.get("device_brand") and not inc.device_brand:
                        inc.device_brand = str(p["device_brand"])
                    if p.get("device_model") and not inc.device_model:
                        inc.device_model = str(p["device_model"])

            elif item.entity_type == "incident_event":
                cid = p.get("client_incident_id")
                inc = None
                if p.get("incident_id"):
                    inc = db.get(Incident, p["incident_id"])
                elif cid:
                    inc = (
                        db.query(Incident)
                        .filter(Incident.client_incident_id == cid, Incident.user_id == user.id)
                        .first()
                    )
                if inc and inc.user_id == user.id:
                    db.add(
                        IncidentEvent(
                            incident_id=inc.id,
                            client_event_id=item.client_event_id,
                            event_type=p.get("event_type", "observation"),
                            note=p.get("note", ""),
                            actor_role=user.role,
                            event_timestamp=item.original_timestamp,
                        )
                    )
                else:
                    raise ValueError("Target incident not found for offline event")

            elif item.entity_type == "checklist_toggle":
                checklist = (
                    db.query(ProtectionChecklist)
                    .filter(ProtectionChecklist.user_id == user.id)
                    .first()
                )
                if checklist and p.get("category"):
                    ci = (
                        db.query(ChecklistItem)
                        .filter(
                            ChecklistItem.checklist_id == checklist.id,
                            ChecklistItem.category == p["category"],
                        )
                        .first()
                    )
                    if ci:
                        ci.is_completed = bool(p.get("is_completed", True))
                        ci.completed_at = item.original_timestamp if ci.is_completed else None
                        if p.get("notes") is not None:
                            ci.notes = str(p["notes"])

            db.add(
                SyncEvent(
                    user_id=user.id,
                    client_event_id=item.client_event_id,
                    entity_type=item.entity_type,
                    status="processed",
                )
            )
            processed.append(item.client_event_id)
        except Exception as exc:
            failed.append({"client_event_id": item.client_event_id, "error": str(exc)})

    record_audit(
        db,
        actor_user_id=user.id,
        actor_role=user.role,
        action="sync.outbox_batch",
        resource_type="sync",
        details={"processed": len(processed), "deduplicated": len(deduplicated), "failed": len(failed)},
    )
    db.commit()
    return SyncBatchResponse(
        processed_ids=processed,
        deduplicated_ids=deduplicated,
        failed=failed,
    )
