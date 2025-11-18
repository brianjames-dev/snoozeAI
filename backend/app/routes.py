from fastapi import APIRouter, HTTPException, Query
from .schemas import TextIn, SummaryOut, ClassifyOut, StoreIn, StoreOut, ItemsOut
from .ai import summarize as ai_summarize, classify as ai_classify
from .db import store_snoozed, fetch_items

router = APIRouter()

@router.get("/health")
def health():
    return {"ok": True}

@router.post("/summarize", response_model=SummaryOut)
async def summarize(payload: TextIn):
    summary = await ai_summarize(payload.text, payload.max_tokens or 80)
    return {"summary": summary}

@router.post("/classify", response_model=ClassifyOut)
async def classify(payload: TextIn):
    result = await ai_classify(payload.text, hints=payload.hints)
    return result

@router.post("/store", response_model=StoreOut)
def store(payload: StoreIn):
    try:
        doc_id = store_snoozed(payload)
    except Exception as exc:
        raise HTTPException(status_code=500, detail="Failed to persist snooze") from exc
    return {"ok": True, "id": doc_id}


@router.get("/items", response_model=ItemsOut)
def items(limit: int = Query(50, ge=1, le=100)):
    try:
        records = fetch_items(limit=limit)
    except Exception as exc:
        raise HTTPException(status_code=500, detail="Failed to load items") from exc

    normalized = []
    for record in records:
        normalized.append(
            {
                "id": record.get("id"),
                "title": record.get("title", ""),
                "summary": record.get("summary", ""),
                "urgency": record.get("urgency", 0.0),
                "snooze_until": record.get("snoozeUntil"),
            }
        )
    return {"items": normalized}
