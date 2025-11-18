"""
Legacy helpers kept for backwards compatibility.

New callers should import from `backend.app.ai` directly.
"""

from .ai import classify, summarize


async def ai_summarize(text: str, max_tokens: int = 80) -> str:
    return await summarize(text, max_tokens=max_tokens)


async def ai_classify(text: str) -> float:
    result = await classify(text)
    return result.get("urgency", 0.0)
