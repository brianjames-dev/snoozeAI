import os
import httpx

USE_OPENAI = os.getenv("USE_OPENAI", "false").lower() == "true"
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")
OPENAI_MODEL = os.getenv("OPENAI_MODEL", "gpt-4o-mini")

def _fallback_summary(text: str, max_tokens: int = 80) -> str:
    # very simple fallback/mocked summary
    s = text.strip().replace("\n", " ")
    return (s[:120] + "…") if len(s) > 120 else s

async def ai_summarize(text: str, max_tokens: int = 80) -> str:
    if not USE_OPENAI or not OPENAI_API_KEY:
        return _fallback_summary(text, max_tokens)
    try:
        headers = {"Authorization": f"Bearer {OPENAI_API_KEY}"}
        # Using /v1/responses style; adjust if using a different endpoint
        payload = {
            "model": OPENAI_MODEL,
            "input": f"Summarize in one concise sentence (<= {max_tokens} tokens): {text}"
        }
        async with httpx.AsyncClient(timeout=8.0) as client:
            r = await client.post("https://api.openai.com/v1/responses",
                                  headers=headers, json=payload)
            r.raise_for_status()
            data = r.json()
            # defensive extraction (depends on exact API response shape)
            # try some common shapes:
            if isinstance(data, dict):
                # new Responses API (example extraction):
                out = data.get("output") or data.get("choices") or []
                if isinstance(out, list) and out:
                    # attempt to walk into content/text
                    first = out[0]
                    if isinstance(first, dict):
                        content = first.get("content") or first.get("message") or {}
                        if isinstance(content, list) and content:
                            maybe_text = content[0].get("text")
                            if maybe_text:
                                return maybe_text.strip()
                        if isinstance(content, dict):
                            maybe_text = content.get("text")
                            if maybe_text:
                                return maybe_text.strip()
            return _fallback_summary(text, max_tokens)
    except Exception:
        return _fallback_summary(text, max_tokens)

async def ai_classify(text: str) -> float:
    # Simple LLM-aided or heuristic urgency
    if not USE_OPENAI or not OPENAI_API_KEY:
        # heuristic: shorter + time words => higher urgency
        t = text.lower()
        score = 0.3
        if any(w in t for w in ["minutes", "min", "urgent", "asap", "now", "today", "soon"]):
            score = 0.8
        return score
    # If using OpenAI, you could add a rubric prompt; for now reuse summarize to save a call
    summary = await ai_summarize(text)
    t = (text + " " + summary).lower()
    return 0.8 if any(w in t for w in ["minutes", "urgent", "asap", "now", "today", "soon"]) else 0.3
