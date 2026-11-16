from __future__ import annotations

from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.database import get_db
from app.core.security import require_roles
from app.ml.inference import MLInferenceService
from app.models.entities import (
    AIInteraction,
    AuditLog,
    Incident,
    IncidentEvent,
    KnowledgeDocument,
    ModelPrediction,
    ReportReview,
    SyncEvent,
    User,
)
from app.rag.pipeline import HybridRAGPipeline
from app.schemas.dto import AdminReviewRequest, KnowledgeDocCreate, RoleUpdateRequest
from app.services.audit import record_audit

router = APIRouter(prefix="/admin", tags=["Administration & Reviewer"])
rag_pipeline = HybridRAGPipeline()
ml_service = MLInferenceService()


@router.get("/dashboard")
def get_admin_dashboard(
    admin_user: User = Depends(require_roles("reviewer", "admin")),
    db: Session = Depends(get_db),
):
    total_incidents = db.query(Incident).filter(Incident.deleted_at.is_(None)).count()
    active_incidents = (
        db.query(Incident)
        .filter(Incident.deleted_at.is_(None), Incident.status == "active")
        .count()
    )
    awaiting_review = (
        db.query(Incident)
        .filter(
            Incident.deleted_at.is_(None),
            Incident.status.in_(["submitted_for_review", "under_review", "additional_info_requested"]),
        )
        .count()
    )
    closed_incidents = (
        db.query(Incident)
        .filter(Incident.deleted_at.is_(None), Incident.status.in_(["closed", "approved"]))
        .count()
    )

    by_status = dict(
        db.query(Incident.status, func.count(Incident.id))
        .filter(Incident.deleted_at.is_(None))
        .group_by(Incident.status)
        .all()
    )
    by_category = dict(
        db.query(Incident.category, func.count(Incident.id))
        .filter(Incident.deleted_at.is_(None))
        .group_by(Incident.category)
        .all()
    )
    recent_events = (
        db.query(IncidentEvent)
        .order_by(IncidentEvent.created_at.desc())
        .limit(10)
        .all()
    )
    total_syncs = db.query(SyncEvent).count()

    return {
        "metrics": {
            "total_incidents": total_incidents,
            "active_incidents": active_incidents,
            "awaiting_review": awaiting_review,
            "closed_incidents": closed_incidents,
            "total_synced_outbox_events": total_syncs,
        },
        "status_breakdown": by_status,
        "category_breakdown": by_category,
        "recent_activity": [
            {
                "id": e.id,
                "incident_id": e.incident_id,
                "event_type": e.event_type,
                "note": e.note,
                "actor_role": e.actor_role,
                "timestamp": e.event_timestamp.isoformat(),
            }
            for e in recent_events
        ],
        "system_health": {
            "database": "connected",
            "ml_models_loaded": ml_service.is_ready,
            "ml_model_version": ml_service.metadata.get("model_version", "unavailable"),
        },
    }


@router.get("/incidents")
def list_admin_incidents(
    status_filter: str | None = Query(default=None, alias="status"),
    reviewer: User = Depends(require_roles("reviewer", "admin")),
    db: Session = Depends(get_db),
):
    q = db.query(Incident).filter(Incident.deleted_at.is_(None))
    if status_filter:
        q = q.filter(Incident.status == status_filter)
    rows = q.order_by(Incident.updated_at.desc()).limit(100).all()
    return [
        {
            "id": inc.id,
            "user_id": inc.user_id,
            "title": inc.title,
            "category": inc.category,
            "status": inc.status,
            "review_status": inc.review_status,
            "device_brand": inc.device_brand,
            "device_model": inc.device_model,
            "imei_masked": f"***********{inc.imei_last4}" if inc.imei_last4 else None,
            "ai_summary": inc.ai_summary,
            "missing_fields": inc.missing_fields or [],
            "reviewer_notes": inc.reviewer_notes,
            "occurred_at": inc.occurred_at.isoformat(),
            "updated_at": inc.updated_at.isoformat(),
        }
        for inc in rows
    ]


@router.post("/incidents/{incident_id}/review")
def review_incident(
    incident_id: str,
    body: AdminReviewRequest,
    reviewer: User = Depends(require_roles("reviewer", "admin")),
    db: Session = Depends(get_db),
):
    inc = db.get(Incident, incident_id)
    if not inc or inc.deleted_at is not None:
        raise HTTPException(status_code=404, detail="Incident not found")

    prev = inc.status
    inc.status = body.decision
    inc.review_status = body.decision
    inc.reviewer_notes = body.notes

    review = ReportReview(
        incident_id=inc.id,
        reviewer_id=reviewer.id,
        decision=body.decision,
        notes=body.notes,
    )
    db.add(review)
    db.add(
        IncidentEvent(
            incident_id=inc.id,
            event_type="reviewer_decision",
            note=f"Reviewer set status {prev} -> {body.decision}: {body.notes}",
            actor_role=reviewer.role,
        )
    )
    record_audit(
        db,
        actor_user_id=reviewer.id,
        actor_role=reviewer.role,
        action="admin.incident_reviewed",
        resource_type="incident",
        resource_id=inc.id,
        details={"from": prev, "to": body.decision},
    )
    db.commit()
    return {
        "incident_id": inc.id,
        "status": inc.status,
        "review_status": inc.review_status,
        "reviewer_notes": inc.reviewer_notes,
        "disclaimer": "Internal review completed. Reviewer approval does NOT submit a police report.",
    }


@router.get("/users")
def list_users(
    admin: User = Depends(require_roles("admin")),
    db: Session = Depends(get_db),
):
    users = db.query(User).filter(User.deleted_at.is_(None)).order_by(User.created_at.desc()).all()
    return [
        {
            "id": u.id,
            "email": u.email,
            "role": u.role,
            "is_active": u.is_active,
            "full_name": u.profile.full_name if u.profile else u.email,
            "preferred_language": u.profile.preferred_language if u.profile else "en",
            "created_at": u.created_at.isoformat(),
        }
        for u in users
    ]


@router.patch("/users/{user_id}/role")
def update_user_role(
    user_id: str,
    body: RoleUpdateRequest,
    admin: User = Depends(require_roles("admin")),
    db: Session = Depends(get_db),
):
    target = db.get(User, user_id)
    if not target:
        raise HTTPException(status_code=404, detail="User not found")
    prev_role = target.role
    target.role = body.role
    if body.is_active is not None:
        target.is_active = body.is_active
    record_audit(
        db,
        actor_user_id=admin.id,
        actor_role=admin.role,
        action="admin.user_role_updated",
        resource_type="user",
        resource_id=target.id,
        details={"from_role": prev_role, "to_role": body.role, "is_active": target.is_active},
    )
    db.commit()
    return {"user_id": target.id, "role": target.role, "is_active": target.is_active}


@router.get("/knowledge")
def list_knowledge_docs(
    admin: User = Depends(require_roles("reviewer", "admin")),
    db: Session = Depends(get_db),
):
    rag_pipeline.ingest_default_if_empty(db)
    docs = db.query(KnowledgeDocument).order_by(KnowledgeDocument.id.asc()).all()
    return [
        {
            "id": d.id,
            "title": d.title,
            "publisher": d.publisher,
            "source_url": d.source_url,
            "last_verified": d.last_verified,
            "language": d.language,
            "jurisdiction": d.jurisdiction,
            "trust_level": d.trust_level,
            "is_active": d.is_active,
        }
        for d in docs
    ]


@router.post("/knowledge", status_code=201)
def add_knowledge_doc(
    body: KnowledgeDocCreate,
    admin: User = Depends(require_roles("admin")),
    db: Session = Depends(get_db),
):
    if db.get(KnowledgeDocument, body.id):
        raise HTTPException(status_code=409, detail="Document ID already exists")
    doc = KnowledgeDocument(
        id=body.id,
        title=body.title,
        publisher=body.publisher,
        source_url=body.source_url,
        date_retrieved=datetime.now(timezone.utc).strftime("%Y-%m-%d"),
        last_verified=body.last_verified,
        language=body.language,
        jurisdiction=body.jurisdiction,
        content=body.content,
        is_active=True,
    )
    db.add(doc)
    db.flush()
    chunks_count = rag_pipeline.reindex_document(db, doc)
    record_audit(
        db,
        actor_user_id=admin.id,
        actor_role=admin.role,
        action="admin.knowledge_added",
        resource_type="knowledge_document",
        resource_id=doc.id,
        details={"chunks": chunks_count},
    )
    db.commit()
    return {"id": doc.id, "chunks_indexed": chunks_count}


@router.post("/knowledge/{doc_id}/toggle")
def toggle_knowledge_doc(
    doc_id: str,
    admin: User = Depends(require_roles("admin")),
    db: Session = Depends(get_db),
):
    doc = db.get(KnowledgeDocument, doc_id)
    if not doc:
        raise HTTPException(status_code=404, detail="Document not found")
    doc.is_active = not doc.is_active
    record_audit(
        db,
        actor_user_id=admin.id,
        actor_role=admin.role,
        action="admin.knowledge_toggled",
        resource_type="knowledge_document",
        resource_id=doc.id,
        details={"is_active": doc.is_active},
    )
    db.commit()
    return {"id": doc.id, "is_active": doc.is_active}


@router.post("/knowledge/{doc_id}/reindex")
def reindex_knowledge_doc(
    doc_id: str,
    admin: User = Depends(require_roles("admin")),
    db: Session = Depends(get_db),
):
    doc = db.get(KnowledgeDocument, doc_id)
    if not doc:
        raise HTTPException(status_code=404, detail="Document not found")
    count = rag_pipeline.reindex_document(db, doc)
    doc.last_verified = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    record_audit(
        db,
        actor_user_id=admin.id,
        actor_role=admin.role,
        action="admin.knowledge_reindexed",
        resource_type="knowledge_document",
        resource_id=doc.id,
        details={"chunks": count},
    )
    db.commit()
    return {"id": doc.id, "chunks_indexed": count, "last_verified": doc.last_verified}


@router.get("/ai-monitoring")
def get_ai_monitoring(
    admin: User = Depends(require_roles("reviewer", "admin")),
    db: Session = Depends(get_db),
):
    settings = get_settings()
    interactions = db.query(AIInteraction).all()
    total = len(interactions)
    fallback_count = sum(1 for i in interactions if i.used_fallback)
    avg_latency = round(sum(i.latency_ms for i in interactions) / total, 2) if total else 0.0
    total_tokens = sum(i.prompt_tokens + i.completion_tokens for i in interactions)
    ml_count = db.query(ModelPrediction).count()

    return {
        "llm_config": {
            "primary_provider": settings.llm_primary_provider,
            "fallback_provider": settings.llm_fallback_provider,
            "groq_model": settings.groq_model,
            "gemini_model": settings.gemini_model,
            "groq_configured": bool(settings.groq_api_key),
            "gemini_configured": bool(settings.gemini_api_key),
        },
        "ai_stats": {
            "total_requests": total,
            "fallback_requests": fallback_count,
            "avg_latency_ms": avg_latency,
            "total_tokens": total_tokens,
        },
        "ml_stats": {
            "models_loaded": ml_service.is_ready,
            "model_version": ml_service.metadata.get("model_version", "unavailable"),
            "best_activity_classifier": ml_service.metadata.get("best_activity_classifier", "unavailable"),
            "best_anomaly_detector": ml_service.metadata.get("best_anomaly_detector", "unavailable"),
            "total_inferences": ml_count,
        },
    }


@router.get("/audit-logs")
def list_audit_logs(
    limit: int = Query(default=100, ge=1, le=500),
    admin: User = Depends(require_roles("reviewer", "admin")),
    db: Session = Depends(get_db),
):
    logs = db.query(AuditLog).order_by(AuditLog.created_at.desc()).limit(limit).all()
    return [
        {
            "id": l.id,
            "actor_user_id": l.actor_user_id,
            "actor_role": l.actor_role,
            "action": l.action,
            "resource_type": l.resource_type,
            "resource_id": l.resource_id,
            "details": l.details,
            "created_at": l.created_at.isoformat(),
        }
        for l in logs
    ]
