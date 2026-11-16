# Guardian AI — REST API Reference

Base URL: `http://localhost:8000` (`http://10.0.2.2:8000` from Android Emulator)  
Authentication: `Authorization: Bearer <access_token>` (JWT HS256)

---

## 1. Health
- `GET /health` — Service health status (`status`, `service`).

## 2. Authentication (`/api/v1/auth`)
- `POST /api/v1/auth/register` — Register user (`email`, `password` >= 8 chars, `full_name`, `phone`, `preferred_language`). Returns `access_token`, `refresh_token`, `role`.
- `POST /api/v1/auth/login` — Authenticate user and receive JWT tokens.
- `POST /api/v1/auth/refresh` — Exchange valid `refresh_token` for a fresh `access_token`.

## 3. Users, Devices & Privacy (`/api/v1/users`)
- `GET /api/v1/users/me` / `PATCH /api/v1/users/me` — Retrieve/update profile, language (`en`/`bn`), `consent_location_capture`, `consent_ai_processing`, and sync preferences.
- `GET /api/v1/users/me/export` — Export user devices (`imei_last4` only) and incidents JSON.
- `DELETE /api/v1/users/me` — Soft-delete authenticated user account.
- `GET /api/v1/devices` / `POST /api/v1/devices` — Manage registered devices (`imei` encrypted server-side via Fernet; responses mask raw IMEI).

## 4. Incidents & Offline Sync (`/api/v1`)
- `GET /api/v1/incidents` — List authenticated user's incidents (`status`, `category` optional filters).
- `POST /api/v1/incidents` — Create incident (supports idempotent `client_incident_id`, masks IMEI, writes initial `incident_events` audit entry).
- `GET /api/v1/incidents/{id}` — Full incident detail + timeline events + masked IMEI + review notes.
- `PATCH /api/v1/incidents/{id}` — Update description, location label, device metadata, or status (enforces `VALID_INCIDENT_TRANSITIONS`).
- `POST /api/v1/incidents/{id}/events` — Append chronological timeline observation.
- `POST /api/v1/sync/outbox` — Idempotent offline-first WorkManager outbox batch sync (`client_event_id` deduplication).

## 5. Protection Checklist, Trusted Contacts & Resources (`/api/v1`)
- `GET /api/v1/protection/checklist` — Retrieve 9 ordered Bangladesh containment steps (auto-seeded).
- `PATCH /api/v1/protection/checklist/{item_id}` — Toggle completion, timestamp, and reference notes.
- `GET /api/v1/contacts` / `POST /api/v1/contacts` / `DELETE /api/v1/contacts/{id}` — Manage encrypted trusted contacts (`phone_masked` returned).
- `POST /api/v1/contacts/share` — Log consent-confirmed alert share with a trusted contact.
- `POST /api/v1/contacts/share/{share_id}/revoke` — Revoke contact sharing access.
- `GET /api/v1/resources` — Verified Bangladesh helplines & portals directory with `verification_date`.

## 6. AI, RAG, NLP & ML Inference (`/api/v1/ai`)
- `POST /api/v1/ai/retrieve` — Hybrid BM25 + Dense Vector RRF retrieval with strict grounding threshold.
- `POST /api/v1/ai/chat` — Bilingual RAG + LLM security assistant (`query`, `language`, optional `incident_id`). Returns `answer`, `grounded`, `citations[]`, `provider_used`, `uncertainty_notice`. Refuses out-of-scope queries.
- `POST /api/v1/ai/summarise` — Summarise confirmed incident timeline and containment status.
- `POST /api/v1/ai/extract` — Multilingual EN/BN/Banglish structured field extractor (`intent`, `device_brand`, `location_mention`, `time_mentions`, `imei_masked`, `imei_luhn_valid`, `uncertain_fields`).
- `POST /api/v1/ai/report-draft` — Generate watermarked `[DRAFT ONLY]` bilingual Online GD preparation draft + checklist summary.
- `GET /api/v1/ai/assess/{incident_id}` — Deterministic explainable GD readiness score (`gd_readiness_score`, rule trace, missing fields).
- `POST /api/v1/ai/ml/predict-context` — Executes `LinearSVC` activity classifier (`561` features) + `OneClassSVM` anomaly detection + top permutation importance XAI signals.

## 7. Admin & Reviewer Console (`/api/v1/admin`)
Requires `role` in `{"reviewer", "admin"}` (`admin` required for user role updates & knowledge management):
- `GET /api/v1/admin/dashboard` — Incident totals, active incidents, review queue depth, ML readiness metadata.
- `GET /api/v1/admin/incidents` — Cross-user incident review queue (`status` filter, `imei_masked` only).
- `POST /api/v1/admin/incidents/{id}/review` — Set review decision (`under_review`, `additional_info_requested`, `approved`, `closed`).
- `GET /api/v1/admin/users` / `PATCH /api/v1/admin/users/{id}/role` — Manage user roles & account status (`admin` only).
- `GET /api/v1/admin/knowledge` / `POST /api/v1/admin/knowledge` / `POST /api/v1/admin/knowledge/{doc_id}/toggle` / `POST /api/v1/admin/knowledge/{doc_id}/reindex` — Manage & re-index verified knowledge base documents.
- `GET /api/v1/admin/ai-monitoring` — LLM provider health/latency/fallback telemetry & ML stats.
- `GET /api/v1/admin/audit-logs` — Chronological security & administrative audit stream.
