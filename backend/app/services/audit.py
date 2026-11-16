from __future__ import annotations

from typing import Any, Optional

from sqlalchemy.orm import Session

from app.models.entities import AuditLog


def record_audit(
    db: Session,
    *,
    actor_user_id: Optional[str],
    actor_role: str,
    action: str,
    resource_type: str,
    resource_id: Optional[str] = None,
    details: Optional[dict[str, Any]] = None,
) -> AuditLog:
    entry = AuditLog(
        actor_user_id=actor_user_id,
        actor_role=actor_role,
        action=action,
        resource_type=resource_type,
        resource_id=resource_id,
        details=details or {},
    )
    db.add(entry)
    return entry
