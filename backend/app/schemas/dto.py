from __future__ import annotations

from datetime import datetime
from typing import Any, Literal, Optional

from pydantic import BaseModel, EmailStr, Field

IncidentStatus = Literal[
    "draft",
    "active",
    "submitted_for_review",
    "under_review",
    "additional_info_requested",
    "approved",
    "closed",
    "cancelled",
]

IncidentCategory = Literal[
    "lost_phone",
    "suspected_theft",
    "snatching",
    "sim_compromise",
    "account_compromise",
]

VALID_INCIDENT_TRANSITIONS: dict[str, set[str]] = {
    "draft": {"active", "submitted_for_review", "cancelled"},
    "active": {"submitted_for_review", "closed", "cancelled"},
    "submitted_for_review": {"under_review", "additional_info_requested", "cancelled"},
    "under_review": {"additional_info_requested", "approved", "closed"},
    "additional_info_requested": {"submitted_for_review", "under_review", "cancelled"},
    "approved": {"closed"},
    "closed": set(),
    "cancelled": set(),
}


class RegisterRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)
    full_name: str = Field(min_length=2, max_length=160)
    phone: Optional[str] = Field(default=None, max_length=32)
    preferred_language: Literal["en", "bn"] = "en"


class LoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=1, max_length=128)


class RefreshRequest(BaseModel):
    refresh_token: str


class ForgotPasswordRequest(BaseModel):
    email: EmailStr


class AuthResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    user_id: str
    email: str
    full_name: str
    role: str
    preferred_language: str


class UserProfileResponse(BaseModel):
    user_id: str
    email: str
    phone: Optional[str]
    role: str
    full_name: str
    preferred_language: str
    district: Optional[str]
    consent_location_capture: bool
    consent_ai_processing: bool
    notify_push: bool
    notify_sync_alerts: bool


class UserProfileUpdate(BaseModel):
    full_name: Optional[str] = Field(default=None, min_length=2, max_length=160)
    phone: Optional[str] = Field(default=None, max_length=32)
    preferred_language: Optional[Literal["en", "bn"]] = None
    district: Optional[str] = Field(default=None, max_length=100)
    consent_location_capture: Optional[bool] = None
    consent_ai_processing: Optional[bool] = None
    notify_push: Optional[bool] = None
    notify_sync_alerts: Optional[bool] = None


class DeviceCreate(BaseModel):
    brand: str = Field(min_length=1, max_length=80)
    model: str = Field(min_length=1, max_length=120)
    color: Optional[str] = Field(default=None, max_length=60)
    os_type: Literal["android", "ios", "other"] = "android"
    imei: Optional[str] = Field(default=None, max_length=24)
    sim_operator: Optional[str] = Field(default=None, max_length=64)
    is_primary: bool = True


class DeviceResponse(BaseModel):
    id: str
    brand: str
    model: str
    color: Optional[str]
    os_type: str
    imei_masked: Optional[str]
    sim_operator: Optional[str]
    is_primary: bool
    created_at: datetime


class IncidentLocationInput(BaseModel):
    location_label: Optional[str] = Field(default=None, max_length=255)
    latitude: Optional[float] = Field(default=None, ge=-90.0, le=90.0)
    longitude: Optional[float] = Field(default=None, ge=-180.0, le=180.0)
    accuracy_meters: Optional[float] = Field(default=None, ge=0.0)
    consent_granted: bool = False


class IncidentCreate(BaseModel):
    client_incident_id: Optional[str] = Field(default=None, max_length=64)
    category: IncidentCategory
    status: Literal["draft", "active"] = "active"
    title: str = Field(min_length=3, max_length=200)
    description: str = Field(default="", max_length=5000)
    occurred_at: Optional[datetime] = None
    device_id: Optional[str] = None
    device_brand: Optional[str] = Field(default=None, max_length=80)
    device_model: Optional[str] = Field(default=None, max_length=120)
    imei: Optional[str] = Field(default=None, max_length=24)
    location: Optional[IncidentLocationInput] = None


class IncidentUpdate(BaseModel):
    title: Optional[str] = Field(default=None, min_length=3, max_length=200)
    description: Optional[str] = Field(default=None, max_length=5000)
    status: Optional[IncidentStatus] = None
    device_brand: Optional[str] = Field(default=None, max_length=80)
    device_model: Optional[str] = Field(default=None, max_length=120)
    imei: Optional[str] = Field(default=None, max_length=24)
    location_label: Optional[str] = Field(default=None, max_length=255)


class IncidentEventCreate(BaseModel):
    client_event_id: Optional[str] = Field(default=None, max_length=64)
    event_type: str = Field(min_length=2, max_length=48)
    note: str = Field(min_length=1, max_length=2000)
    event_timestamp: Optional[datetime] = None


class IncidentEventResponse(BaseModel):
    id: str
    event_type: str
    note: str
    actor_role: str
    event_timestamp: datetime


class IncidentResponse(BaseModel):
    id: str
    user_id: str
    client_incident_id: Optional[str]
    category: str
    status: str
    title: str
    description: str
    occurred_at: datetime
    device_brand: Optional[str]
    device_model: Optional[str]
    imei_masked: Optional[str]
    location_label: Optional[str]
    location_accuracy_meters: Optional[float]
    ai_summary: Optional[str]
    missing_fields: list[str]
    review_status: str
    reviewer_notes: Optional[str]
    created_at: datetime
    updated_at: datetime
    events: list[IncidentEventResponse] = []


class SyncOutboxItem(BaseModel):
    client_event_id: str = Field(min_length=4, max_length=64)
    entity_type: Literal["incident_upsert", "incident_event", "checklist_toggle"]
    payload: dict[str, Any]
    original_timestamp: datetime


class SyncBatchRequest(BaseModel):
    items: list[SyncOutboxItem]


class SyncBatchResponse(BaseModel):
    processed_ids: list[str]
    deduplicated_ids: list[str]
    failed: list[dict[str, str]]


class ChecklistItemResponse(BaseModel):
    id: str
    category: str
    sort_order: int
    title: str
    what_to_do: str
    why_it_matters: str
    official_url: Optional[str]
    is_completed: bool
    completed_at: Optional[datetime]
    notes: Optional[str]


class ChecklistItemUpdate(BaseModel):
    is_completed: Optional[bool] = None
    notes: Optional[str] = Field(default=None, max_length=1000)


class TrustedContactCreate(BaseModel):
    name: str = Field(min_length=2, max_length=120)
    relationship_label: str = Field(default="Family", max_length=60)
    phone: str = Field(min_length=6, max_length=32)
    notify_sms_consent: bool = False


class TrustedContactResponse(BaseModel):
    id: str
    name: str
    relationship_label: str
    phone_masked: str
    is_verified: bool
    notify_sms_consent: bool
    created_at: datetime


class ContactShareCreate(BaseModel):
    incident_id: str
    contact_id: str
    consent_confirmed: bool
    include_location: bool = False


class AIChatRequest(BaseModel):
    query: str = Field(min_length=1, max_length=2000)
    language: Literal["en", "bn", "auto"] = "auto"
    incident_id: Optional[str] = None
    include_incident_context: bool = False


class CitationItem(BaseModel):
    doc_id: str
    title: str
    publisher: str
    source_url: str
    last_verified: str
    score: float
    excerpt: str


class AIChatResponse(BaseModel):
    answer: str
    language: str
    provider_used: str
    model_used: str
    used_fallback: bool
    grounded: bool
    citations: list[CitationItem]
    uncertainty_notice: Optional[str] = None


class AIExtractRequest(BaseModel):
    narrative: str = Field(min_length=3, max_length=5000)
    language: Literal["en", "bn", "auto"] = "auto"


class ReportDraftRequest(BaseModel):
    incident_id: str
    language: Literal["en", "bn"] = "en"
    user_confirmed_fields: Optional[dict[str, Any]] = None


class ReportDraftResponse(BaseModel):
    id: str
    incident_id: str
    is_official_gd: bool = False
    disclaimer: str
    structured_payload: dict[str, Any]
    draft_text: str
    missing_information: list[str]
    provider_used: str
    created_at: datetime


class AdminReviewRequest(BaseModel):
    decision: Literal[
        "under_review",
        "additional_info_requested",
        "approved",
        "closed",
    ]
    notes: str = Field(min_length=3, max_length=2000)


class RoleUpdateRequest(BaseModel):
    role: Literal["user", "reviewer", "admin"]
    is_active: Optional[bool] = None


class KnowledgeDocCreate(BaseModel):
    id: str = Field(min_length=3, max_length=64)
    title: str = Field(min_length=3, max_length=255)
    publisher: str = Field(min_length=2, max_length=160)
    source_url: str = Field(min_length=8, max_length=512)
    last_verified: str = Field(min_length=8, max_length=32)
    language: Literal["en", "bn", "bilingual"] = "bilingual"
    jurisdiction: str = "BD"
    content: str = Field(min_length=20)
