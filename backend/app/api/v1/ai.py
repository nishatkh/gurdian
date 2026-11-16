from __future__ import annotations

import json
from datetime import datetime, timezone
from typing import Any, Optional

from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.ai.provider import LLMRouter, redact_sensitive_fields
from app.core.database import get_db
from app.core.rate_limit import ai_rate_limiter, rate_limit_by_ip
from app.core.security import get_current_user
from app.ml.inference import MLInferenceService
from app.models.entities import (
    AIInteraction,
    ChecklistItem,
    Incident,
    ModelPrediction,
    ProtectionChecklist,
    ReportDraft,
    User,
)
from app.nlp.extractor import extract_incident_fields
from app.rag.pipeline import HybridRAGPipeline, detect_language
from app.schemas.dto import (
    AIChatRequest,
    AIChatResponse,
    AIExtractRequest,
    CitationItem,
    ReportDraftRequest,
    ReportDraftResponse,
)
from app.services.audit import record_audit

router = APIRouter(prefix="/ai", tags=["AI, RAG, NLP & ML Inference"])
llm_router = LLMRouter()
rag_pipeline = HybridRAGPipeline()
ml_service = MLInferenceService()


class RetrieveRequest(BaseModel):
    query: str
    top_k: int = 4
    mode: str = "hybrid"


class SensorContextRequest(BaseModel):
    incident_id: Optional[str] = None
    features: list[float]


def _log_ai(
    db: Session,
    *,
    user_id: Optional[str],
    incident_id: Optional[str],
    feature: str,
    provider: str,
    model_name: str,
    used_fallback: bool,
    latency_ms: float,
    pt: int = 0,
    ct: int = 0,
    status: str = "success",
) -> None:
    db.add(
        AIInteraction(
            user_id=user_id,
            incident_id=incident_id,
            feature=feature,
            provider=provider,
            model_name=model_name,
            used_fallback=used_fallback,
            latency_ms=latency_ms,
            prompt_tokens=pt,
            completion_tokens=ct,
            status=status,
        )
    )


@router.post("/retrieve")
def retrieve_knowledge(
    body: RetrieveRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    out = rag_pipeline.retrieve(db, body.query, top_k=body.top_k, mode=body.mode)
    return {
        "grounded": out["grounded"],
        "method": out["method"],
        "citations": [
            {
                "doc_id": r.doc_id,
                "title": r.title,
                "publisher": r.publisher,
                "source_url": r.source_url,
                "last_verified": r.last_verified,
                "score": r.hybrid_score,
                "bm25_score": r.bm25_score,
                "dense_score": r.dense_score,
                "excerpt": r.content[:280],
            }
            for r in out["results"]
        ],
    }


@router.post("/chat", response_model=AIChatResponse)
async def ai_chat(
    body: AIChatRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    _: None = Depends(rate_limit_by_ip(ai_rate_limiter)),
):
    lang = body.language
    if lang == "auto":
        detected = detect_language(body.query)
        lang = "bn" if detected == "bn" else "en"

    rag_out = rag_pipeline.retrieve(db, body.query, top_k=3, mode="hybrid")
    citations = [
        CitationItem(
            doc_id=r.doc_id,
            title=r.title,
            publisher=r.publisher,
            source_url=r.source_url,
            last_verified=r.last_verified,
            score=r.hybrid_score,
            excerpt=r.content[:260],
        )
        for r in rag_out["results"]
    ]

    incident_ctx_text = ""
    contains_sensitive = False
    if body.include_incident_context and body.incident_id:
        inc = db.get(Incident, body.incident_id)
        if not inc or inc.user_id != user.id:
            raise HTTPException(status_code=403, detail="Unauthorized incident context")
        contains_sensitive = True
        incident_ctx_text = (
            f"\nConfirmed Incident Context (Masked):\n"
            f"- Category: {inc.category}, Status: {inc.status}\n"
            f"- Device: {inc.device_brand or 'Unknown'} {inc.device_model or ''}\n"
            f"- IMEI (masked): {'****' + inc.imei_last4 if inc.imei_last4 else 'Not provided'}\n"
            f"- Missing Fields: {', '.join(inc.missing_fields or [])}\n"
        )

    if not rag_out["grounded"] and not incident_ctx_text:
        no_evidence_msg = (
            "আমাদের যাচাইকৃত জ্ঞানভাণ্ডারে এই প্রশ্নের জন্য নির্ভরযোগ্য অফিসিয়াল তথ্য পাওয়া যায়নি। অনুগ্রহ করে সরাসরি বাংলাদেশ পুলিশ (৯৯৯ / gd.police.gov.bd) বা সংশ্লিষ্ট অফিসিয়াল হেল্পলাইনে যোগাযোগ করুন।"
            if lang == "bn"
            else "I could not find verified official documentation in our knowledge base to reliably answer this question. Please verify directly with Bangladesh Police (999 / gd.police.gov.bd) or your official provider."
        )
        _log_ai(
            db,
            user_id=user.id,
            incident_id=body.incident_id,
            feature="chat_insufficient_evidence",
            provider="rag_guardrail",
            model_name="strict-grounding-v1",
            used_fallback=True,
            latency_ms=1.0,
        )
        db.commit()
        return AIChatResponse(
            answer=no_evidence_msg,
            language=lang,
            provider_used="rag_guardrail",
            model_used="strict-grounding-v1",
            used_fallback=True,
            grounded=False,
            citations=[],
            uncertainty_notice="Insufficient verified evidence in knowledge base.",
        )

    evidence_block = "\n\n".join(
        f"[{c.doc_id} | Verified {c.last_verified} | {c.source_url}]\n{c.excerpt}"
        for c in citations
    )
    fallback_text = (
        f"যাচাইকৃত অফিসিয়াল তথ্য অনুযায়ী:\n" + "\n• ".join([c.excerpt for c in citations])
        if lang == "bn"
        else "Based on verified official Guardian AI guidance:\n• " + "\n• ".join([c.excerpt for c in citations])
    )
    if incident_ctx_text:
        fallback_text = f"{fallback_text}\n{incident_ctx_text}"

    prompt = (
        f"Reply in {'Bangla (বাংলা)' if lang == 'bn' else 'English'}.\n"
        f"User Question: {body.query}\n"
        f"{incident_ctx_text}\n"
        f"Verified Knowledge Context:\n{evidence_block}\n"
        "Cite source titles and verification dates. Do not invent numbers or URLs."
    )

    res = await llm_router.generate(
        user_prompt=prompt,
        deterministic_fallback_text=fallback_text,
        contains_sensitive_incident_data=contains_sensitive,
        preferred_provider="groq",
    )
    _log_ai(
        db,
        user_id=user.id,
        incident_id=body.incident_id,
        feature="chat",
        provider=res.provider,
        model_name=res.model,
        used_fallback=res.used_fallback,
        latency_ms=res.latency_ms,
        pt=res.prompt_tokens,
        ct=res.completion_tokens,
    )
    record_audit(
        db,
        actor_user_id=user.id,
        actor_role=user.role,
        action="ai.chat",
        resource_type="ai_interaction",
        resource_id=body.incident_id,
        details={"provider": res.provider, "grounded": rag_out["grounded"]},
    )
    db.commit()

    return AIChatResponse(
        answer=res.text,
        language=lang,
        provider_used=res.provider,
        model_used=res.model,
        used_fallback=res.used_fallback,
        grounded=rag_out["grounded"],
        citations=citations,
        uncertainty_notice=(
            "Generated using deterministic verified-source fallback because live LLM API key was unavailable."
            if res.provider == "deterministic_fallback"
            else None
        ),
    )


@router.post("/chat/stream")
async def ai_chat_stream(
    body: AIChatRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    resp = await ai_chat(body=body, user=user, db=db)

    async def event_stream():
        async for chunk in llm_router.stream_chunks(resp.answer):
            yield f"data: {json.dumps({'delta': chunk})}\n\n"
        yield f"data: {json.dumps({'done': True, 'provider': resp.provider_used, 'citations': [c.model_dump() for c in resp.citations]})}\n\n"

    return StreamingResponse(event_stream(), media_type="text/event-stream")


@router.post("/extract")
def nlp_extract(
    body: AIExtractRequest,
    user: User = Depends(get_current_user),
):
    return extract_incident_fields(body.narrative)


@router.post("/summarise")
async def summarise_incident(
    incident_id: str,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    inc = db.get(Incident, incident_id)
    if not inc or (inc.user_id != user.id and user.role not in {"reviewer", "admin"}):
        raise HTTPException(status_code=404, detail="Incident not found")

    timeline_txt = "\n".join(
        f"- {e.event_timestamp.isoformat()} [{e.event_type}]: {e.note}"
        for e in sorted(inc.events, key=lambda x: x.event_timestamp)
    )
    deterministic = (
        f"Incident '{inc.title}' ({inc.category}, status={inc.status}). "
        f"Device: {inc.device_brand or 'Unknown'} {inc.device_model or ''}. "
        f"Timeline events: {len(inc.events)}. Missing fields: {', '.join(inc.missing_fields or []) or 'None'}."
    )
    prompt = (
        f"Summarise ONLY the confirmed facts below without inventing anything:\n"
        f"Title: {inc.title}\nCategory: {inc.category}\nStatus: {inc.status}\n"
        f"Description: {redact_sensitive_fields(inc.description)}\n"
        f"Timeline:\n{timeline_txt}"
    )
    res = await llm_router.generate(
        user_prompt=prompt,
        deterministic_fallback_text=deterministic,
        contains_sensitive_incident_data=True,
        preferred_provider="groq",
    )
    inc.ai_summary = res.text
    _log_ai(
        db,
        user_id=user.id,
        incident_id=inc.id,
        feature="summarise",
        provider=res.provider,
        model_name=res.model,
        used_fallback=res.used_fallback,
        latency_ms=res.latency_ms,
    )
    db.commit()
    return {
        "incident_id": inc.id,
        "summary": res.text,
        "provider_used": res.provider,
        "used_fallback": res.used_fallback,
    }


@router.post("/report-draft", response_model=ReportDraftResponse)
async def generate_report_draft(
    body: ReportDraftRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    inc = db.get(Incident, body.incident_id)
    if not inc or inc.user_id != user.id:
        raise HTTPException(status_code=404, detail="Incident not found")

    checklist = (
        db.query(ProtectionChecklist)
        .filter(ProtectionChecklist.user_id == user.id)
        .first()
    )
    completed_steps = []
    if checklist:
        items = db.query(ChecklistItem).filter(ChecklistItem.checklist_id == checklist.id).all()
        completed_steps = [i.title for i in items if i.is_completed]

    latest_loc = inc.locations[-1] if inc.locations else None
    location_str = latest_loc.location_label if latest_loc and latest_loc.location_label else "Not provided"
    device_desc = f"{inc.device_brand or 'Not provided'} {inc.device_model or ''}".strip()
    imei_masked = f"***********{inc.imei_last4}" if inc.imei_last4 else "Not provided"
    missing = inc.missing_fields or []

    structured_payload = {
        "incident_id": inc.id,
        "is_official_gd": False,
        "category": inc.category,
        "occurred_at": inc.occurred_at.isoformat(),
        "location": location_str,
        "device_description": device_desc,
        "imei_masked": imei_masked,
        "narrative": inc.description or "Not provided",
        "actions_completed": completed_steps,
        "missing_information": missing,
        "timeline_count": len(inc.events),
    }

    deterministic_draft = (
        f"[DRAFT ONLY — NOT AN OFFICIALLY SUBMITTED GENERAL DIARY]\n\n"
        f"Subject: Draft Report for {inc.category.replace('_', ' ').title()}\n"
        f"Date & Time: {inc.occurred_at.strftime('%Y-%m-%d %H:%M UTC')}\n"
        f"Location: {location_str}\n"
        f"Device: {device_desc} (IMEI: {imei_masked})\n\n"
        f"Incident Narrative:\n{inc.description or 'Pending user description.'}\n\n"
        f"Protection Actions Already Taken:\n"
        + ("\n".join(f"- {s}" for s in completed_steps) if completed_steps else "- None recorded yet")
        + "\n\nMissing Details to Verify Before Filing Online GD (https://gd.police.gov.bd/):\n"
        + ("\n".join(f"- {m}" for m in missing) if missing else "- Core fields present; verify NID & invoice.")
    )

    prompt = (
        f"Draft a polite, factual police General Diary (GD) preparation note in "
        f"{'Bangla' if body.language == 'bn' else 'English'} using ONLY these confirmed fields. "
        f"Prefix with 'DRAFT ONLY — NOT AN OFFICIALLY SUBMITTED GD'. Do not invent any facts.\n"
        f"{json.dumps(structured_payload, ensure_ascii=False)}"
    )
    res = await llm_router.generate(
        user_prompt=prompt,
        deterministic_fallback_text=deterministic_draft,
        contains_sensitive_incident_data=True,
        preferred_provider="gemini",
    )

    draft = ReportDraft(
        incident_id=inc.id,
        is_official_gd=False,
        structured_payload=structured_payload,
        draft_text=res.text,
        user_verified=False,
    )
    db.add(draft)
    _log_ai(
        db,
        user_id=user.id,
        incident_id=inc.id,
        feature="report_draft",
        provider=res.provider,
        model_name=res.model,
        used_fallback=res.used_fallback,
        latency_ms=res.latency_ms,
    )
    record_audit(
        db,
        actor_user_id=user.id,
        actor_role=user.role,
        action="ai.report_draft_generated",
        resource_type="report_draft",
        resource_id=draft.id,
    )
    db.commit()
    db.refresh(draft)

    return ReportDraftResponse(
        id=draft.id,
        incident_id=inc.id,
        is_official_gd=False,
        disclaimer="DRAFT ONLY: Guardian AI does not submit reports to Bangladesh Police or Online GD.",
        structured_payload=structured_payload,
        draft_text=draft.draft_text,
        missing_information=missing,
        provider_used=res.provider,
        created_at=draft.created_at,
    )


@router.get("/assess/{incident_id}")
def assess_incident(
    incident_id: str,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    inc = db.get(Incident, incident_id)
    if not inc or (inc.user_id != user.id and user.role not in {"reviewer", "admin"}):
        raise HTTPException(status_code=404, detail="Incident not found")

    checklist = (
        db.query(ProtectionChecklist)
        .filter(ProtectionChecklist.user_id == inc.user_id)
        .first()
    )
    completed = 0
    total = 9
    if checklist:
        items = db.query(ChecklistItem).filter(ChecklistItem.checklist_id == checklist.id).all()
        total = len(items)
        completed = sum(1 for i in items if i.is_completed)

    latest_loc = inc.locations[-1] if inc.locations else None
    return ml_service.assess_incident_rules(
        category=inc.category,
        description=inc.description,
        has_imei=bool(inc.imei_last4),
        has_device_info=bool(inc.device_brand and inc.device_model),
        has_location=bool(latest_loc and latest_loc.location_label),
        completed_checklist_count=completed,
        total_checklist_count=total,
        timeline_count=len(inc.events),
    )


@router.post("/ml/predict-context")
def predict_ml_context(
    body: SensorContextRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    try:
        result = ml_service.predict_sensor_context(body.features)
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc

    db.add(
        ModelPrediction(
            incident_id=body.incident_id,
            task_name=result["task"],
            predicted_label=result["predicted_activity"],
            confidence=result["confidence"],
            anomaly_score=result["motion_anomaly_score"],
            explanation_json={"top_signals": result["xai_top_signals"]},
        )
    )
    db.commit()
    return result
