from __future__ import annotations

import asyncio
import json
import os
import sys
from pathlib import Path
from unittest.mock import AsyncMock, patch

import pytest
from fastapi.testclient import TestClient

# Ensure backend package resolves cleanly
sys.path.insert(0, str(Path("backend").resolve()))
os.environ["DATABASE_URL"] = "sqlite:///./backend/test_guardian.db"

from app.ai.provider import LLMResult  # noqa: E402
from app.core.database import Base, SessionLocal, engine  # noqa: E402
from app.core.rate_limit import ai_rate_limiter, auth_rate_limiter  # noqa: E402
from app.main import app  # noqa: E402
from app.rag.pipeline import HybridRAGPipeline  # noqa: E402
from app.services.seed import seed_initial_data  # noqa: E402


@pytest.fixture(scope="module", autouse=True)
def setup_test_db():
    db_path = Path("backend/test_guardian.db")
    if db_path.exists():
        db_path.unlink()
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    try:
        seed_initial_data(db)
        HybridRAGPipeline().ingest_default_if_empty(db)
    finally:
        db.close()
    yield
    if db_path.exists():
        db_path.unlink()


@pytest.fixture(autouse=True)
def reset_rate_limits():
    auth_rate_limiter.reset()
    ai_rate_limiter.reset()


@pytest.fixture()
def client():
    with TestClient(app) as c:
        yield c


def _auth_header(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def test_health_and_resources(client: TestClient):
    r = client.get("/health")
    assert r.status_code == 200
    assert r.json()["status"] == "ok"

    res = client.get("/api/v1/resources")
    assert res.status_code == 200
    items = res.json()
    assert any(x["contact_number"] == "999" and x["is_verified"] for x in items)


def test_full_13_step_workflow_and_security(client: TestClient):
    # 1. User registers
    reg = client.post(
        "/api/v1/auth/register",
        json={
            "email": "nusrat@example.com",
            "password": "StrongPassword!2026",
            "full_name": "Nusrat Jahan",
            "phone": "+8801711000111",
            "preferred_language": "bn",
        },
    )
    assert reg.status_code == 201
    user_token = reg.json()["access_token"]

    # Register a second user to test ownership isolation
    reg2 = client.post(
        "/api/v1/auth/register",
        json={
            "email": "other@example.com",
            "password": "StrongPassword!2026",
            "full_name": "Other User",
        },
    )
    assert reg2.status_code == 201
    other_token = reg2.json()["access_token"]

    # 2 & 3. User creates an incident (also simulating offline sync)
    inc_resp = client.post(
        "/api/v1/incidents",
        headers=_auth_header(user_token),
        json={
            "client_incident_id": "offline-inc-001",
            "category": "snatching",
            "status": "active",
            "title": "Phone snatched near Farmgate",
            "description": "My Samsung Galaxy A54 was snatched at Farmgate Bus Stand around 5:30 PM.",
            "device_brand": "Samsung",
            "device_model": "Galaxy A54",
            "imei": "356938035643809",
            "location": {
                "location_label": "Farmgate Bus Stand, Dhaka",
                "latitude": 23.7561,
                "longitude": 90.3872,
                "accuracy_meters": 18.5,
                "consent_granted": True,
            },
        },
    )
    assert inc_resp.status_code == 201
    inc_data = inc_resp.json()
    inc_id = inc_data["id"]
    # Verify IMEI is masked and never returned raw
    assert inc_data["imei_masked"] == "***********3809"
    assert "356938035643809" not in json.dumps(inc_data)

    # Verify other user cannot access Nusrat's incident
    forbidden = client.get(f"/api/v1/incidents/{inc_id}", headers=_auth_header(other_token))
    assert forbidden.status_code == 403

    # Verify invalid state transition is blocked
    bad_state = client.patch(
        f"/api/v1/incidents/{inc_id}",
        headers=_auth_header(user_token),
        json={"status": "approved"},
    )
    assert bad_state.status_code == 400

    # 4. Offline outbox sync idempotency test
    sync_payload = {
        "items": [
            {
                "client_event_id": "outbox-ev-001",
                "entity_type": "incident_event",
                "payload": {
                    "client_incident_id": "offline-inc-001",
                    "event_type": "observation",
                    "note": "Checked nearby police box at Farmgate",
                },
                "original_timestamp": "2026-11-16T11:45:00Z",
            }
        ]
    }
    s1 = client.post("/api/v1/sync/outbox", headers=_auth_header(user_token), json=sync_payload)
    assert s1.status_code == 200
    assert s1.json()["processed_ids"] == ["outbox-ev-001"]
    # Re-sending same client_event_id deduplicates safely
    s2 = client.post("/api/v1/sync/outbox", headers=_auth_header(user_token), json=sync_payload)
    assert s2.status_code == 200
    assert s2.json()["deduplicated_ids"] == ["outbox-ev-001"]

    # 5. User completes protection checklist steps
    cl = client.get("/api/v1/protection/checklist", headers=_auth_header(user_token))
    assert cl.status_code == 200
    items = cl.json()
    assert len(items) >= 9
    first_item_id = items[0]["id"]
    upd_cl = client.patch(
        f"/api/v1/protection/checklist/{first_item_id}",
        headers=_auth_header(user_token),
        json={"is_completed": True, "notes": "Blocked GP SIM via 121"},
    )
    assert upd_cl.status_code == 200
    assert upd_cl.json()["is_completed"] is True

    # 6. AI summarises confirmed incident (deterministic fallback when keys unset)
    summ = client.post(
        f"/api/v1/ai/summarise?incident_id={inc_id}",
        headers=_auth_header(user_token),
    )
    assert summ.status_code == 200
    assert "Farmgate" in summ.json()["summary"]

    # 7. RAG retrieves relevant Bangladesh Online GD guidance in Bangla & English
    rag_en = client.post(
        "/api/v1/ai/retrieve",
        headers=_auth_header(user_token),
        json={"query": "How do I file an Online GD in Bangladesh?", "mode": "hybrid"},
    )
    assert rag_en.status_code == 200
    assert rag_en.json()["grounded"] is True
    assert any("gd.police.gov.bd" in c["source_url"] for c in rag_en.json()["citations"])

    rag_bn = client.post(
        "/api/v1/ai/chat",
        headers=_auth_header(user_token),
        json={"query": "আমার ফোন চুরি হয়েছে, সিম ব্লক ও জিডি করার নিয়ম কী?", "language": "bn"},
    )
    assert rag_bn.status_code == 200
    assert rag_bn.json()["grounded"] is True
    assert len(rag_bn.json()["citations"]) >= 1

    # Out-of-scope query returns insufficient evidence notice
    oos = client.post(
        "/api/v1/ai/chat",
        headers=_auth_header(user_token),
        json={"query": "How do I bake a chocolate croissant recipe?", "language": "en"},
    )
    assert oos.status_code == 200
    assert oos.json()["grounded"] is False

    # 8. Report draft generation & deterministic rule assessment
    draft = client.post(
        "/api/v1/ai/report-draft",
        headers=_auth_header(user_token),
        json={"incident_id": inc_id, "language": "en"},
    )
    assert draft.status_code == 200
    assert draft.json()["is_official_gd"] is False
    assert "DRAFT ONLY" in draft.json()["draft_text"]

    assess = client.get(f"/api/v1/ai/assess/{inc_id}", headers=_auth_header(user_token))
    assert assess.status_code == 200
    assert assess.json()["gd_readiness_score"] >= 70

    # Submit incident for review
    sub = client.patch(
        f"/api/v1/incidents/{inc_id}",
        headers=_auth_header(user_token),
        json={"status": "submitted_for_review"},
    )
    assert sub.status_code == 200

    # Regular user cannot access admin endpoints
    no_admin = client.get("/api/v1/admin/dashboard", headers=_auth_header(user_token))
    assert no_admin.status_code == 403

    # 9-12. Admin/Reviewer logs in, inspects, requests info, user updates, reviewer closes
    admin_login = client.post(
        "/api/v1/auth/login",
        json={"email": "admin@guardianai.app", "password": "GuardianAdmin!2026"},
    )
    assert admin_login.status_code == 200
    admin_token = admin_login.json()["access_token"]

    dash = client.get("/api/v1/admin/dashboard", headers=_auth_header(admin_token))
    assert dash.status_code == 200
    assert dash.json()["metrics"]["total_incidents"] >= 1

    rev_req = client.post(
        f"/api/v1/admin/incidents/{inc_id}/review",
        headers=_auth_header(admin_token),
        json={"decision": "additional_info_requested", "notes": "Please add SIM operator name."},
    )
    assert rev_req.status_code == 200
    assert rev_req.json()["status"] == "additional_info_requested"

    user_upd = client.patch(
        f"/api/v1/incidents/{inc_id}",
        headers=_auth_header(user_token),
        json={
            "status": "submitted_for_review",
            "description": "My Samsung Galaxy A54 with Grameenphone SIM was snatched at Farmgate Bus Stand at 5:30 PM.",
        },
    )
    assert user_upd.status_code == 200

    rev_close = client.post(
        f"/api/v1/admin/incidents/{inc_id}/review",
        headers=_auth_header(admin_token),
        json={"decision": "closed", "notes": "Internal guidance and draft review completed."},
    )
    assert rev_close.status_code == 200
    assert rev_close.json()["status"] == "closed"

    # 13. Audit logs recorded actions
    logs = client.get("/api/v1/admin/audit-logs", headers=_auth_header(admin_token))
    assert logs.status_code == 200
    actions = {entry["action"] for entry in logs.json()}
    assert "auth.register" in actions
    assert "incident.created" in actions
    assert "sync.outbox_batch" in actions
    assert "admin.incident_reviewed" in actions


def test_ml_inference_and_nlp_extract(client: TestClient):
    reg = client.post(
        "/api/v1/auth/login",
        json={"email": "admin@guardianai.app", "password": "GuardianAdmin!2026"},
    )
    assert reg.status_code == 200, reg.text
    token = reg.json()["access_token"]

    ext = client.post(
        "/api/v1/ai/extract",
        headers=_auth_header(token),
        json={"narrative": "গতকাল মিরপুর ১০ এলাকায় আমার Samsung ফোন চুরি হয়েছে।"},
    )
    assert ext.status_code == 200
    assert ext.json()["device_brand"] == "Samsung"
    assert ext.json()["intent"] == "report_theft"

    # Test real ML model loading & inference if training completed
    model_meta = Path("ml/models/model_metadata.json")
    if model_meta.exists():
        bad_vec = client.post(
            "/api/v1/ai/ml/predict-context",
            headers=_auth_header(token),
            json={"features": [0.1, 0.2]},
        )
        assert bad_vec.status_code == 422

        valid_vec = client.post(
            "/api/v1/ai/ml/predict-context",
            headers=_auth_header(token),
            json={"features": [0.0] * 561},
        )
        assert valid_vec.status_code == 200
        data = valid_vec.json()
        assert "predicted_activity" in data
        assert "xai_top_signals" in data


def test_llm_provider_fallback_behavior():
    from app.ai.provider import LLMRouter
    from app.core.config import Settings

    s = Settings(
        groq_api_key="test-groq-key",
        gemini_api_key="test-gemini-key",
        llm_primary_provider="groq",
        llm_fallback_provider="gemini",
        llm_allow_sensitive_fallback=True,
    )
    router = LLMRouter(settings=s)
    with (
        patch.object(router.providers["groq"], "generate_text", new=AsyncMock(side_effect=RuntimeError("Groq 503"))),
        patch.object(
            router.providers["gemini"],
            "generate_text",
            new=AsyncMock(
                return_value=LLMResult(
                    text="Recovered via Gemini",
                    provider="gemini",
                    model="gemini-2.5-flash",
                )
            ),
        ),
    ):
        res = asyncio.run(
            router.generate(
                user_prompt="How to block SIM?",
                deterministic_fallback_text="Fallback text",
                contains_sensitive_incident_data=False,
            )
        )
        assert res.provider == "gemini"
        assert res.used_fallback is True
        assert res.text == "Recovered via Gemini"
