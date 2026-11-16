from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.rate_limit import auth_rate_limiter, rate_limit_by_ip
from app.core.security import (
    create_access_token,
    create_refresh_token,
    decode_token,
    get_current_user,
    hash_password,
    verify_password,
)
from app.models.entities import User, UserProfile
from app.schemas.dto import (
    AuthResponse,
    ForgotPasswordRequest,
    LoginRequest,
    RefreshRequest,
    RegisterRequest,
)
from app.services.audit import record_audit
from app.services.seed import ensure_default_checklist_for_user

router = APIRouter(prefix="/auth", tags=["Authentication"])


@router.post("/register", response_model=AuthResponse, status_code=status.HTTP_201_CREATED)
def register(
    body: RegisterRequest,
    db: Session = Depends(get_db),
    _: None = Depends(rate_limit_by_ip(auth_rate_limiter)),
):
    existing = db.query(User).filter(User.email == body.email.lower()).first()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="An account with this email already exists",
        )
    user = User(
        email=body.email.lower(),
        phone=body.phone,
        password_hash=hash_password(body.password),
        role="user",
    )
    db.add(user)
    db.flush()

    profile = UserProfile(
        user_id=user.id,
        full_name=body.full_name,
        preferred_language=body.preferred_language,
    )
    db.add(profile)
    ensure_default_checklist_for_user(db, user.id)
    record_audit(
        db,
        actor_user_id=user.id,
        actor_role=user.role,
        action="auth.register",
        resource_type="user",
        resource_id=user.id,
    )
    db.commit()

    return AuthResponse(
        access_token=create_access_token(user.id, user.role),
        refresh_token=create_refresh_token(user.id, user.role),
        user_id=user.id,
        email=user.email,
        full_name=profile.full_name,
        role=user.role,
        preferred_language=profile.preferred_language,
    )


@router.post("/login", response_model=AuthResponse)
def login(
    body: LoginRequest,
    db: Session = Depends(get_db),
    _: None = Depends(rate_limit_by_ip(auth_rate_limiter)),
):
    user = db.query(User).filter(User.email == body.email.lower()).first()
    if not user or not verify_password(body.password, user.password_hash) or not user.is_active:
        record_audit(
            db,
            actor_user_id=user.id if user else None,
            actor_role="anonymous",
            action="auth.login_failed",
            resource_type="user",
            details={"email": body.email.lower()},
        )
        db.commit()
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password",
        )

    profile = user.profile
    record_audit(
        db,
        actor_user_id=user.id,
        actor_role=user.role,
        action="auth.login",
        resource_type="user",
        resource_id=user.id,
    )
    db.commit()

    return AuthResponse(
        access_token=create_access_token(user.id, user.role),
        refresh_token=create_refresh_token(user.id, user.role),
        user_id=user.id,
        email=user.email,
        full_name=profile.full_name if profile else user.email,
        role=user.role,
        preferred_language=profile.preferred_language if profile else "en",
    )


@router.post("/refresh", response_model=AuthResponse)
def refresh_token(body: RefreshRequest, db: Session = Depends(get_db)):
    payload = decode_token(body.refresh_token, expected_type="refresh")
    user = db.get(User, payload["sub"])
    if not user or not user.is_active or user.deleted_at is not None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Account unavailable")
    profile = user.profile
    return AuthResponse(
        access_token=create_access_token(user.id, user.role),
        refresh_token=create_refresh_token(user.id, user.role),
        user_id=user.id,
        email=user.email,
        full_name=profile.full_name if profile else user.email,
        role=user.role,
        preferred_language=profile.preferred_language if profile else "en",
    )


@router.post("/forgot-password")
def forgot_password(body: ForgotPasswordRequest, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.email == body.email.lower()).first()
    if user:
        record_audit(
            db,
            actor_user_id=user.id,
            actor_role=user.role,
            action="auth.password_reset_requested",
            resource_type="user",
            resource_id=user.id,
        )
        db.commit()
    return {
        "message": "If an account exists for that email, password recovery instructions have been queued.",
    }
