# Guardian AI — Deployment, Environment & Demo Guide

## 1. Environment Variables (`/backend/.env`)
Create `/backend/.env` from `/backend/.env.example`:
- `DATABASE_URL`: Defaults to SQLite in local/test dev or PostgreSQL + pgvector (`postgresql+psycopg://guardian:guardian@postgres:5432/guardian_ai`) in Docker Compose.
- `REDIS_URL`: Redis connection URL (`redis://redis:6379/0`).
- `JWT_SECRET`: Random 64-char secret for signing HS256 JWT tokens.
- `FIELD_ENCRYPTION_KEY`: Base64 URL-safe 32-byte Fernet key for server-side IMEI & trusted-contact phone encryption.
- `GROQ_API_KEY` *(optional)*: Primary LLM provider (`llama-3.3-70b-versatile`) for synthesis over verified RAG snippets.
- `GEMINI_API_KEY` *(optional)*: Secondary fallback LLM provider (`gemini-2.5-flash`).
  - If neither API key is configured, the backend automatically uses the deterministic grounded synthesis fallback (`deterministic_fallback`) so the app remains 100% functional.
- `ALLOWED_ORIGINS`: Comma-separated allowed origins (defaults to `http://localhost:3000,http://127.0.0.1:3000`).

> **Security Note:** Never commit `/backend/.env` or embed server LLM keys into Android `BuildConfig` or APK resources.

---

## 2. Database Migrations (Alembic)
```bash
alembic -c backend/alembic.ini upgrade head
```
The initial migration `001_initial_schema.py` creates all relational tables (`users`, `devices`, `incidents`, `incident_events`, `incident_evidence`, `checklist_items`, `trusted_contacts`, `contact_share_logs`, `emergency_resources`, `knowledge_documents`, `knowledge_chunks`, `sync_outbox_events`, `audit_logs`, `ai_interaction_logs`, `consent_records`, `ml_inference_logs`).

On FastAPI startup, `seed_initial_data()` idempotently loads `/knowledge_base/processed/verified_documents.json`, verified Bangladesh emergency resources, and a default administrator account (`admin@guardianai.app`). Change the default admin password immediately in any shared deployment.

---

## 3. Docker Compose Stack
```bash
docker compose up --build -d
```
Services started:
- `postgres` (`pgvector/pgvector:pg16`, port `5432`)
- `redis` (`redis:7-alpine`, port `6379`)
- `backend` (`http://localhost:8000`) via `/infrastructure/docker/backend.Dockerfile`
- `dashboard` (`http://localhost:3000`, `NEXT_PUBLIC_API_URL=http://localhost:8000/api/v1`) via `/infrastructure/docker/dashboard.Dockerfile`

---

## 4. End-to-End Demo Flow
1. **Android Onboarding & Offline-First Entry:** Launch the app, choose `EN` or `বাংলা`, complete the 3-slide onboarding, and tap **Continue in Offline-First Mode** (or sign in against `http://10.0.2.2:8000`).
2. **Device Vault:** Open **Profile -> Device & Encrypted IMEI Vault**, save a Luhn-valid 15-digit IMEI (`356938035643809`), verifying it displays masked (`•••• •••• •••• 3809`).
3. **Emergency Workflow:** Open the highlighted **Emergency** tab, log a suspected theft or snatching, optionally capture consent-based GPS coordinates, and tap **Save Incident & Start Response**.
4. **XAI & Draft GD Export:** View the created incident's **GD Readiness & Explainable AI** score breakdown, attach an image via Android's zero-permission **Photo Picker**, preview `[DRAFT ONLY]` GD text, and export/share PDF.
5. **Bilingual Grounded AI:** Open **Guardian AI Assistant**, ask in Bangla (`"সিম কীভাবে ব্লক করব?"`) or English (`"What do I need for Online GD?"`) to see source-cited responses, plus verify out-of-scope refusal.
6. **Dashboard Review:** Sign into the Next.js Admin Dashboard (`http://localhost:3000`) as `admin@guardianai.app` to inspect analytics, review submitted incidents, audit logs, and manage knowledge base entries.
