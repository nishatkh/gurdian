# Guardian AI — Mobile Security, Theft Response & Digital Account Protection Platform (Bangladesh)

**Guardian AI** is an offline-first Android mobile companion, FastAPI security backend, Next.js administration dashboard, and machine learning context/NLP pipeline built to guide smartphone users in Bangladesh through immediate post-loss or post-theft containment in **English and Bangla**.

> **Important Safety & Scope Notice:** Guardian AI is an independent response companion and evidence-preparation tool. It **does NOT** replace Bangladesh Police, National Emergency Service **999**, Online GD (`gd.police.gov.bd`), or BTRC/NEIR, and it **does NOT** submit official General Diaries automatically.

---

## Architecture Overview

| Component | Path | Stack |
|---|---|---|
| **Android App** | `/app/` | Kotlin, Jetpack Compose, Material 3 (`#050505` / `#B6FF55`), Room + WorkManager outbox, Android Keystore AES-GCM, DataStore, Retrofit/OkHttp, Photo Picker, PDF `PdfDocument`, bilingual EN/BN UI |
| **Backend API** | `/backend/` | Python 3.11, FastAPI, SQLAlchemy 2.x, Alembic, Pydantic v2, JWT/bcrypt RBAC, Fernet encryption, Groq + Gemini LLM router, Hybrid BM25+Dense RAG, Bangla/English/Banglish NLP extractor, ML inference service |
| **Admin Dashboard** | `/dashboard/` | Next.js 15, React 19, TypeScript, Tailwind CSS dark obsidian/lime UI |
| **ML Pipeline** | `/ml/` | scikit-learn, NumPy, pandas, joblib — UCI HAR participant-aware Activity Classifier (`LinearSVC`, 96.03% held-out accuracy) & Motion Anomaly Detector (`OneClassSVM`, 1.0000 ROC-AUC) + XAI permutation importance |
| **Verified Knowledge Base** | `/knowledge_base/` | Curated bilingual official documentation (999, Online GD, BTRC/NEIR, Google Find Hub, Apple Find My, GP/Robi/Banglalink, bKash/Nagad/Rocket) verified 2026-11-16 |
| **Containers** | `/docker-compose.yml`, `/infrastructure/docker/` | Dockerized FastAPI backend & Next.js dashboard |

---

## Quick Start

### 1. Android Application (`/app`)
- Build/compile using Android Gradle (`gradle assembleDebug`) or open in Android Studio.
- Configure `BACKEND_BASE_URL` in `/app/build.gradle.kts` (defaults to `http://10.0.2.2:8000` for Android Emulator host loopback).
- Run Robolectric & Compose JVM unit tests:
  ```bash
  gradle :app:testDebugUnitTest
  ```

### 2. Backend API (`/backend`)
```bash
cp backend/.env.example backend/.env
# Add GROQ_API_KEY and/or GEMINI_API_KEY in backend/.env (optional — deterministic grounded fallback works without keys)
python3 -m pip install -r backend/requirements.txt
alembic -c backend/alembic.ini upgrade head
PYTHONPATH=backend uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```
Run backend tests:
```bash
PYTHONPATH=backend pytest -v backend/app/tests/test_guardian_backend.py
```

### 3. Admin Web Dashboard (`/dashboard`)
```bash
npm --prefix dashboard install
NEXT_PUBLIC_API_BASE_URL=http://localhost:8000 npm --prefix dashboard run dev
# Or production build:
NODE_ENV=production npm --prefix dashboard run build
```

### 4. ML Training & Evaluation (`/ml`)
```bash
python3 ml/src/train_and_evaluate.py
python3 ml/src/evaluate_nlp.py
```
Reports and model cards are saved to `/ml/reports/`.

---

## Security & Privacy Highlights
- **Zero-trust client storage:** Raw IMEIs and trusted contact numbers are encrypted with Android Keystore AES-256-GCM (`KeystoreCryptoManager`) and masked (`•••• •••• •••• 3809`) across UI, drafts, API responses, and logs.
- **Permission minimization:** Storage (`READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE`) is never requested; evidence attachment uses Android's zero-permission Photo Picker (`PickVisualMedia`). Location (`ACCESS_COARSE_LOCATION`) is requested *only* when the user explicitly taps Capture Location in Emergency Mode.
- **Strict Grounded AI & Guardrails:** Unrecognized / out-of-scope queries trigger an explicit uncertainty refusal rather than hallucinated instructions. Every AI assessment includes explainable rule traces and a disclosure that AI never declares a phone stolen.
- **No Unconfirmed External Actions:** Dialing 999, operator codes, external portals, and trusted-contact sharing always require explicit user confirmation dialogs.
