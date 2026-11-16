from __future__ import annotations

import json
import re
import time
from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Any, AsyncGenerator, Optional

import httpx

from app.core.config import Settings, get_settings

SYSTEM_SAFETY_PROMPT = """You are Guardian AI, a privacy-aware mobile theft response and digital account protection companion for users in Bangladesh.
STRICT RULES:
1. Never invent incident facts, dates, locations, IMEIs, phone numbers, URLs, or official procedures.
2. Only state official numbers or URLs present in the provided verified knowledge context.
3. Never declare that a phone is stolen based solely on AI or sensor scores, and never identify or accuse suspects.
4. Never claim Guardian AI directly files an Online GD, blocks a SIM, or contacts 999 automatically.
5. Clearly distinguish user-reported facts from AI suggestions.
6. Reply in the requested language (English or Bangla / বাংলা) using clear, calm, actionable steps."""


def redact_sensitive_fields(text: str) -> str:
    """Mask 15-digit IMEIs and exact lat/lng decimals before sending text to external LLMs."""
    masked = re.sub(r"\b(\d{11})(\d{4})\b", r"***********\2", text)
    masked = re.sub(r"(-?\d{1,2}\.\d{3})\d+", r"\1***", masked)
    return masked


@dataclass
class LLMResult:
    text: str
    provider: str
    model: str
    used_fallback: bool = False
    latency_ms: float = 0.0
    prompt_tokens: int = 0
    completion_tokens: int = 0
    estimated_cost_usd: float = 0.0
    parsed_json: Optional[dict[str, Any]] = None
    error: Optional[str] = None


class LLMProvider(ABC):
    name: str

    @abstractmethod
    async def generate_text(
        self,
        *,
        system_prompt: str,
        user_prompt: str,
        temperature: float = 0.2,
        max_tokens: int = 900,
        json_mode: bool = False,
    ) -> LLMResult: ...

    @abstractmethod
    async def health_check(self) -> dict[str, Any]: ...


class GroqProvider(LLMProvider):
    name = "groq"
    ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"

    def __init__(self, api_key: str, model: str, timeout: float = 20.0) -> None:
        self.api_key = api_key
        self.model = model
        self.timeout = timeout

    async def generate_text(
        self,
        *,
        system_prompt: str,
        user_prompt: str,
        temperature: float = 0.2,
        max_tokens: int = 900,
        json_mode: bool = False,
    ) -> LLMResult:
        if not self.api_key:
            raise RuntimeError("GROQ_API_KEY is not configured")

        payload: dict[str, Any] = {
            "model": self.model,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            "temperature": temperature,
            "max_tokens": max_tokens,
        }
        if json_mode:
            payload["response_format"] = {"type": "json_object"}

        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
        }

        t0 = time.perf_counter()
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            resp = await client.post(self.ENDPOINT, headers=headers, json=payload)
            resp.raise_for_status()
            data = resp.json()
        latency_ms = (time.perf_counter() - t0) * 1000.0

        content = data["choices"][0]["message"]["content"]
        usage = data.get("usage", {})
        pt = int(usage.get("prompt_tokens", 0))
        ct = int(usage.get("completion_tokens", 0))
        parsed = json.loads(content) if json_mode else None

        return LLMResult(
            text=content,
            provider=self.name,
            model=self.model,
            latency_ms=round(latency_ms, 2),
            prompt_tokens=pt,
            completion_tokens=ct,
            estimated_cost_usd=round((pt * 0.59 + ct * 0.79) / 1_000_000, 6),
            parsed_json=parsed,
        )

    async def health_check(self) -> dict[str, Any]:
        return {
            "provider": self.name,
            "model": self.model,
            "configured": bool(self.api_key),
        }


class GeminiProvider(LLMProvider):
    name = "gemini"

    def __init__(self, api_key: str, model: str, timeout: float = 20.0) -> None:
        self.api_key = api_key
        self.model = model
        self.timeout = timeout

    async def generate_text(
        self,
        *,
        system_prompt: str,
        user_prompt: str,
        temperature: float = 0.2,
        max_tokens: int = 900,
        json_mode: bool = False,
    ) -> LLMResult:
        if not self.api_key:
            raise RuntimeError("GEMINI_API_KEY is not configured")

        url = (
            f"https://generativelanguage.googleapis.com/v1beta/models/"
            f"{self.model}:generateContent?key={self.api_key}"
        )
        gen_cfg: dict[str, Any] = {
            "temperature": temperature,
            "maxOutputTokens": max_tokens,
        }
        if json_mode:
            gen_cfg["responseMimeType"] = "application/json"

        payload = {
            "systemInstruction": {"parts": [{"text": system_prompt}]},
            "contents": [{"role": "user", "parts": [{"text": user_prompt}]}],
            "generationConfig": gen_cfg,
        }

        t0 = time.perf_counter()
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            resp = await client.post(url, json=payload)
            resp.raise_for_status()
            data = resp.json()
        latency_ms = (time.perf_counter() - t0) * 1000.0

        candidates = data.get("candidates", [])
        if not candidates:
            raise RuntimeError("Gemini returned empty candidates")
        content = "".join(
            part.get("text", "") for part in candidates[0].get("content", {}).get("parts", [])
        )
        usage = data.get("usageMetadata", {})
        pt = int(usage.get("promptTokenCount", 0))
        ct = int(usage.get("candidatesTokenCount", 0))
        parsed = json.loads(content) if json_mode else None

        return LLMResult(
            text=content,
            provider=self.name,
            model=self.model,
            latency_ms=round(latency_ms, 2),
            prompt_tokens=pt,
            completion_tokens=ct,
            estimated_cost_usd=round((pt * 0.15 + ct * 0.60) / 1_000_000, 6),
            parsed_json=parsed,
        )

    async def health_check(self) -> dict[str, Any]:
        return {
            "provider": self.name,
            "model": self.model,
            "configured": bool(self.api_key),
        }


class LLMRouter:
    """Provider-independent LLM router with transient retry, controlled fallback, and deterministic safe fallback."""

    def __init__(self, settings: Optional[Settings] = None) -> None:
        self.settings = settings or get_settings()
        self.providers: dict[str, LLMProvider] = {
            "groq": GroqProvider(
                api_key=self.settings.groq_api_key,
                model=self.settings.groq_model,
                timeout=self.settings.llm_timeout_seconds,
            ),
            "gemini": GeminiProvider(
                api_key=self.settings.gemini_api_key,
                model=self.settings.gemini_model,
                timeout=self.settings.llm_timeout_seconds,
            ),
        }

    def _ordered_providers(self, task_prefer: Optional[str] = None) -> list[LLMProvider]:
        primary_key = task_prefer or self.settings.llm_primary_provider
        fallback_key = self.settings.llm_fallback_provider
        order: list[LLMProvider] = []
        if primary_key in self.providers:
            order.append(self.providers[primary_key])
        if fallback_key in self.providers and fallback_key != primary_key:
            order.append(self.providers[fallback_key])
        return order

    async def generate(
        self,
        *,
        user_prompt: str,
        system_prompt: str = SYSTEM_SAFETY_PROMPT,
        deterministic_fallback_text: str,
        deterministic_fallback_json: Optional[dict[str, Any]] = None,
        json_mode: bool = False,
        contains_sensitive_incident_data: bool = False,
        preferred_provider: Optional[str] = None,
    ) -> LLMResult:
        safe_prompt = redact_sensitive_fields(user_prompt)
        ordered = self._ordered_providers(preferred_provider)

        last_err: Optional[str] = None
        for idx, provider in enumerate(ordered):
            if idx > 0 and contains_sensitive_incident_data and not self.settings.llm_allow_sensitive_fallback:
                # Privacy guard: do not route sensitive incident payload to secondary provider unless explicitly permitted
                continue
            for attempt in range(2):  # 1 retry for transient failures
                try:
                    res = await provider.generate_text(
                        system_prompt=system_prompt,
                        user_prompt=safe_prompt,
                        json_mode=json_mode,
                    )
                    res.used_fallback = idx > 0
                    return res
                except (httpx.TimeoutException, httpx.HTTPStatusError, httpx.RequestError, ValueError, RuntimeError) as exc:
                    last_err = f"{provider.name}(attempt={attempt + 1}): {exc}"
                    if isinstance(exc, RuntimeError) and "not configured" in str(exc):
                        break  # Do not retry missing API key

        return LLMResult(
            text=deterministic_fallback_text,
            provider="deterministic_fallback",
            model="rule-grounded-v1",
            used_fallback=True,
            latency_ms=1.0,
            parsed_json=deterministic_fallback_json,
            error=last_err,
        )

    async def stream_chunks(self, text: str) -> AsyncGenerator[str, None]:
        words = text.split(" ")
        buf = []
        for w in words:
            buf.append(w)
            if len(buf) >= 6:
                yield " ".join(buf) + " "
                buf.clear()
        if buf:
            yield " ".join(buf)
