# Guardian AI — Project Submission & Provenance Details

This document contains the exact answers to the hackathon / platform submission questions for **Guardian AI**.

---

## 1. Data & AI Provenance (Required — Judge + Admin)

### Data Sources
*List data sources used (APIs, datasets, scraped data...)*

1. **UCI Human Activity Recognition Using Smartphones (HAR) Dataset**
   - **Publisher / Source:** University of California Irvine ML Repository (Anguita et al., 2013).
   - **Licence:** CC BY 4.0 / Academic Open Access.
   - **Volume & Features:** 10,299 windowed samples from 30 participants wearing waist-mounted smartphones; 561 time- and frequency-domain tri-axial accelerometer and gyroscope features (50 Hz sampling).
   - **Use Case:** Trained Task A (Device Activity/Context Classification: walking, stairs, sitting, standing, laying) and Task B (Motion Anomaly Detection: stationary baseline vs. dynamic movement) using a strict participant-aware split (21 train / 9 disjoint test subjects).
   - **Ethical Limitation Notice:** Does NOT contain phone-theft labels; models are strictly scoped to device motion context.

2. **Official Bangladesh Regulatory & Emergency Knowledge Base (Verified Nov 16, 2026)**
   - **Bangladesh Police Online GD Portal:** `https://gd.police.gov.bd/` — Official requirements, NID verification, and General Diary filing rules for lost/stolen mobile phones.
   - **National Emergency Service 999:** `https://nhd.gov.bd/999` — Operational emergency response and dispatch guidance.
   - **Bangladesh Telecommunication Regulatory Commission (BTRC) & NEIR:** `https://www.btrc.gov.bd/` — National Equipment Identity Register (NEIR) procedures, IMEI registration, and verification guidelines.
   - **Bangladesh Mobile Network Operators (GP, Robi, Banglalink, Teletalk):** Official SIM deactivation, PUK retrieval, and replacement customer service hotlines.
   - **Bangladesh Mobile Financial Services (bKash 16247, Nagad 16167, DBBL Rocket 16216):** Emergency wallet freeze and account lock procedures.
   - **Global Device Platforms:** Google Android Find Hub (`google.com/android/find`) and Apple Find My (`support.apple.com/en-us/101593`) remote lock, wipe, and Lost Mode documentation.

3. **Curated Bilingual NLP Evaluation Dataset (`ml/datasets/nlp_eval_bn_en.json`)**
   - Curated evaluation set of authentic incident narratives in Bangla script, English, and Banglish (phonetic Bangla) to benchmark intent extraction, 15-digit Luhn-validated IMEI detection, Dhaka location extraction, and brand identification.

---

### AI Models Used
*AI models used (GPT-4, Gemini, custom models...)*

1. **Google Gemini (`gemini-2.5-flash`)**
   - Used via the Gemini REST API for bilingual police General Diary (GD) draft preparation, contextual incident extraction, and structured JSON generation.

2. **Groq (`llama-3.3-70b-versatile`)**
   - High-throughput, sub-second inference engine used as the primary LLM provider for grounded conversational RAG responses and timeline summarisation.

3. **Custom Trained Machine Learning Models (Trained with scikit-learn in `/ml`):**
   - **Task A — Device Activity/Context Classifier (`LinearSVC`):** Trained on 561 HAR features across 21 subjects; achieved **96.03% accuracy** and **0.9608 Macro F1** on 9 completely held-out test subjects. Outperformed Logistic Regression, Random Forest, and Decision Tree baselines.
   - **Task B — Motion Anomaly Detector (`OneClassSVM`):** Unsupervised novelty detection trained on stationary sensor data to detect sudden non-stationary motion spikes (**ROC-AUC = 1.0000, F1 = 0.9655**).
   - **Explainable AI (XAI):** Permutation importance module providing real-time attribution scores for top sensor signals (`tBodyGyroJerk-entropy()-X`, `fBodyGyro-entropy()-X`, etc.).

4. **Hybrid BM25 + Dense Hashed Semantic RAG Pipeline (`rank-bm25` + 128-d Blake2b Embeddings):**
   - Fast, deterministic retrieval pipeline with bilingual Bangla/English query expansions and stopword filtering.

5. **In-House Deterministic Safe Fallback Model (`rule-grounded-v1`):**
   - Guarantees 100% platform availability even when cloud LLM API keys are unconfigured or offline.

---

### Responsible AI
*Responsible AI practices (bias mitigation, transparency...)*

- **Strict Anti-Hallucination & Uncertainty Refusal:** The RAG system checks document retrieval confidence before prompting LLMs. If verified evidence is absent, the system issues an explicit refusal to answer rather than hallucinating procedures.
- **Zero False Accusations & Prohibited Theft Claims:** Guardian AI explicitly enforces that sensor scores and AI outputs never declare a phone stolen, never identify suspects, and never submit automated complaints.
- **Data Privacy & PII Pre-Flight Redaction:** All 15-digit IMEIs and exact GPS decimal coordinates are automatically masked (`redact_sensitive_fields`) before sending prompts to external cloud LLM providers.
- **Watermarking & Transparency:** Every generated police report is prominently watermarked with `[DRAFT ONLY — NOT AN OFFICIALLY SUBMITTED GENERAL DIARY]` and displays a list of missing required fields (e.g., NID, invoice).
- **Zero Unconfirmed Actions:** The app will never initiate phone calls (to 999 or telecom operators), share emergency contacts, or open external links without explicit affirmative user confirmation dialogs.
- **Comprehensive Model Cards & Provenance Tracking:** Complete model documentation (`ml/reports/model_card.md`) openly discloses sensor dataset limitations and prohibits misuse.

---

## 2. Tooling & IDE (Team Only)

### IDE / Editor
- **Android Studio Ladybug / Meerkat** (Android Kotlin, Jetpack Compose, Robolectric testing)
- **Visual Studio Code / Cursor** (Python FastAPI backend, Next.js frontend, Docker, ML scripts)
- **Google AI Studio** (Prompt engineering, system instruction testing, Gemini API tuning)

### Deployment Method
- **Docker & Docker Compose** (`docker-compose.yml`) containerized multi-service deployment.
- **Backend API:** FastAPI running on Uvicorn with Gunicorn process management, Dockerized via `infrastructure/docker/backend.Dockerfile`. Deployable to Google Cloud Run, AWS ECS, or DigitalOcean VPS.
- **Admin Dashboard:** Next.js 15 / React 19 deployed via Node.js container (`infrastructure/docker/dashboard.Dockerfile`) or Vercel.
- **Database & Cache:** PostgreSQL 16 with `pgvector` extension and Redis 7 Alpine.
- **Android Distribution:** Signed release APK with ProGuard/R8 code shrinking and Android Keystore AES-256-GCM encryption.

### Frameworks & Libraries
- **Mobile (Android):** Kotlin, Jetpack Compose, Material 3, Android Architecture Components (ViewModel, StateFlow), Room Database, WorkManager, DataStore Preferences, Retrofit 2, OkHttp 3, Android Keystore Crypto.
- **Backend & AI:** Python 3.11, FastAPI, Pydantic v2, SQLAlchemy 2.0, Alembic, `httpx`, `rank-bm25`, `python-jose` (JWT), `passlib` (bcrypt), `cryptography` (Fernet symmetric encryption).
- **Machine Learning & Data:** `scikit-learn`, `numpy`, `pandas`, `scipy`, `joblib`.
- **Admin Web Dashboard:** Next.js 15 (App Router), React 19, TypeScript, Tailwind CSS, Lucide React.
- **Testing:** JUnit 4, Robolectric, Pytest, Alembic migrations test suite.

### Context / Memory Files
- `metadata.json` — Application manifest and Gemini API capability declarations.
- `ml/datasets/provenance.json` — Dataset provenance, licensing, and limitations log.
- `ml/configs/training_config.json` — Model hyperparameters, random seeds, and participant split configuration.
- `knowledge_base/metadata/sources.json` — Verified official data sources registry with verification dates and trust levels.
- `docs/API.md` — REST and streaming SSE endpoint contracts.
- `docs/DEPLOYMENT.md` — Production deployment, environment variable configuration, and migration guides.
- `docs/PROMPT_LIBRARY.md` — Complete catalog of development and runtime system prompts.

---

## 3. MCP Usage (Model Context Protocol — Team Only)

### MCP Servers Used (Name, Purpose)
1. **StitchMCP (UI Design & Screen Generation Server):**
   - **Purpose:** Accelerated rapid prototyping and validation of Android Jetpack Compose and Next.js responsive layouts, ensuring adherence to the obsidian/lime design system.
2. **Chrome DevTools MCP Server:**
   - **Purpose:** Automated browser testing, auditing accessibility (WCAG 2.1 contrast and touch target standards), and diagnosing Core Web Vitals (LCP, CLS) on the Next.js admin dashboard.
3. **Filesystem & Codebase Tooling MCP Server:**
   - **Purpose:** Structured repository inspection, automated test suite execution (Robolectric and pytest), and deterministic migration orchestration.

### Tools Exposed via MCP (Guardian AI Platform MCP Capabilities)
If exposing Guardian AI services as an MCP server for assistant integration:
- `assess_incident(incident_id: str)`: Computes GD readiness score, missing information checklist, and explainable AI risk indicators.
- `retrieve_knowledge(query: str, top_k: int, mode: str)`: Performs bilingual hybrid BM25 + dense semantic retrieval over official Bangladesh emergency documentation.
- `predict_sensor_context(features: list[float])`: Evaluates 561 smartphone sensor inputs to return activity classification (`LinearSVC`), motion anomaly scores (`OneClassSVM`), and top permutation XAI signals.
- `generate_report_draft(incident_id: str, language: str)`: Generates structured, unsubmitted police General Diary draft notes.

### Permissions Granted to MCP Tools
- **Filesystem Access:** Read and write permissions restricted strictly to the workspace directory (`/Users/nishatkhan/Downloads/gurdian`).
- **Network Permissions:** Local loopback HTTP communication (`localhost:8000` for FastAPI and `localhost:3000` for Next.js). External outbound network calls strictly restricted to official LLM endpoints (Google Generative Language API and Groq Cloud API).
- **Sensitive Operations:** No access to host system credentials, private keys, or external databases without explicit user environment configuration.

---

## 4. Prompt Library (Team Only)

*(Full text and technical breakdown documented in [`docs/PROMPT_LIBRARY.md`](file:///Users/nishatkhan/Downloads/gurdian/docs/PROMPT_LIBRARY.md))*

- **Prompt 1: Guardian AI Core System Safety Prompt (`SYSTEM_SAFETY_PROMPT`)** — Enforces grounded responses, no automated accusations, PII protection, and bilingual output.
- **Prompt 2: Grounded RAG Emergency Chat Prompt** — Synthesizes verified emergency guidance from retrieved official excerpts with verification dates.
- **Prompt 3: Police General Diary (GD) Draft Prompt** — Generates watermarked `[DRAFT ONLY]` legal notes using structured incident JSON without inventing details.
- **Prompt 4: Timeline Incident Summarisation Prompt** — Distills chronological incident event logs into concise factual summaries.
- **Prompt 5: Master Architectural Development Prompt** — The comprehensive development brief that guided full-stack architecture, offline-first security rules, and ML pipelines.
- **Prompt 6: Organic Wellness & Obsidian-Lime Design System Prompt** — Guided the UI design refactor to high-contrast accessible typography and neumorphic styling.
