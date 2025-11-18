from __future__ import annotations

import json
import os
from dataclasses import dataclass
from functools import lru_cache
from typing import List, Optional

import httpx

DEFAULT_MODEL = "gpt-4o-mini"


@dataclass(frozen=True)
class AIConfig:
    use_openai: bool = False
    openai_api_key: str = ""
    openai_model: str = DEFAULT_MODEL


def _env_truthy(value: Optional[str]) -> bool:
    return str(value or "").strip().lower() in {"1", "true", "yes", "on"}


@lru_cache(maxsize=1)
def load_config() -> AIConfig:
    return AIConfig(
        use_openai=_env_truthy(os.getenv("USE_OPENAI", "false")),
        openai_api_key=os.getenv("OPENAI_API_KEY", "").strip(),
        openai_model=os.getenv("OPENAI_MODEL", DEFAULT_MODEL).strip() or DEFAULT_MODEL,
    )


def _resolve_config(config: Optional[AIConfig]) -> AIConfig:
    return config or load_config()


def _can_use_openai(config: AIConfig) -> bool:
    return config.use_openai and bool(config.openai_api_key)


def summarize_offline(text: str, max_tokens: int = 60) -> str:
    prefix = "Summary: "
    snippet = " ".join(text.strip().split())
    max_chars = max_tokens * 4
    trimmed = snippet[:max_chars]
    if len(snippet) > max_chars:
        trimmed = trimmed.rstrip() + "…"
    return prefix + trimmed


def classify_offline(text: str, hints: Optional[List[str]] = None) -> dict:
    lowered = text.lower()
    urgency = 0.25
    urgent_terms = ["urgent", "asap", "now", "minutes", "soon", "today", "immediately"]
    if any(term in lowered for term in urgent_terms):
        urgency = 0.8
    if len(text) < 80:
        urgency = max(urgency, 0.55)
    if hints:
        for hint in hints:
            if hint and hint.strip().lower() in lowered:
                urgency = max(urgency, 0.9)
                break
    label = "urgent" if urgency >= 0.6 else "normal"
    return {"urgency": round(urgency, 2), "label": label}


async def summarize(text: str, max_tokens: int = 60, config: Optional[AIConfig] = None) -> str:
    cfg = _resolve_config(config)
    bounded_tokens = max(16, min(max_tokens or 60, 200))
    if not _can_use_openai(cfg):
        return summarize_offline(text, bounded_tokens)

    messages = [
        {
            "role": "system",
            "content": "You are a concise assistant that summarizes notification text in <= 1 short sentence.",
        },
        {
            "role": "user",
            "content": f"{text}\n\nReturn at most {bounded_tokens} tokens.",
        },
    ]
    payload = {
        "model": cfg.openai_model,
        "messages": messages,
        "temperature": 0.2,
        "max_tokens": bounded_tokens,
    }
    headers = {"Authorization": f"Bearer {cfg.openai_api_key}"}

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post("https://api.openai.com/v1/chat/completions", json=payload, headers=headers)
        resp.raise_for_status()
        data = resp.json()
        choices = data.get("choices") or []
        if choices:
            message = choices[0].get("message", {})
            content = (message.get("content") or "").strip()
            if content:
                return content
    except Exception:
        pass
    return summarize_offline(text, bounded_tokens)


async def classify(text: str, hints: Optional[List[str]] = None, config: Optional[AIConfig] = None) -> dict:
    cfg = _resolve_config(config)
    if not _can_use_openai(cfg):
        return classify_offline(text, hints=hints)

    messages = [
        {
            "role": "system",
            "content": (
                "Classify notification urgency. Respond ONLY with JSON like "
                '{"urgency": 0.75, "label": "urgent"} where urgency in [0,1].'
            ),
        },
        {
            "role": "user",
            "content": text
            + (
                f"\nPriority hints: {', '.join(hints)}"
                if hints
                else ""
            ),
        },
    ]
    payload = {
        "model": cfg.openai_model,
        "messages": messages,
        "temperature": 0.1,
        "max_tokens": 30,
    }
    headers = {"Authorization": f"Bearer {cfg.openai_api_key}"}

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post("https://api.openai.com/v1/chat/completions", json=payload, headers=headers)
        resp.raise_for_status()
        data = resp.json()
        choices = data.get("choices") or []
        if choices:
            content = (choices[0].get("message", {}).get("content") or "").strip()
            parsed = json.loads(content)
            urgency = float(parsed.get("urgency", 0.5))
            label = parsed.get("label", "urgent" if urgency >= 0.6 else "normal")
            label = label if label in {"urgent", "normal"} else ("urgent" if urgency >= 0.6 else "normal")
            return {"urgency": round(max(0.0, min(1.0, urgency)), 2), "label": label}
    except Exception:
        pass
    return classify_offline(text, hints=hints)
