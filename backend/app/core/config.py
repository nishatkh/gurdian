from __future__ import annotations

from functools import lru_cache
from pathlib import Path
from typing import List

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=("backend/.env", ".env"),
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_name: str = "Guardian AI API"
    app_env: str = "development"
    database_url: str = "sqlite:///./backend/guardian_ai.db"
    redis_url: str = "redis://localhost:6379/0"

    jwt_secret: str = "dev-only-change-in-production-guardian-ai-secret-64-chars-minimum"
    jwt_algorithm: str = "HS256"
    access_token_expire_minutes: int = 60
    refresh_token_expire_days: int = 14

    # Optional Fernet base64 key; deterministic derived dev key used if omitted in dev
    field_encryption_key: str = ""

    # LLM Provider Configuration
    groq_api_key: str = ""
    gemini_api_key: str = ""
    groq_model: str = "llama-3.3-70b-versatile"
    gemini_model: str = "gemini-2.5-flash"
    llm_primary_provider: str = "groq"
    llm_fallback_provider: str = "gemini"
    llm_allow_sensitive_fallback: bool = False
    llm_timeout_seconds: float = 20.0

    allowed_origins: str = "http://localhost:3000,http://127.0.0.1:3000"
    knowledge_base_path: str = "knowledge_base/processed/verified_documents.json"
    ml_models_dir: str = "ml/models"

    @property
    def cors_origins(self) -> List[str]:
        return [o.strip() for o in self.allowed_origins.split(",") if o.strip()]

    @property
    def kb_file(self) -> Path:
        return Path(self.knowledge_base_path)


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
