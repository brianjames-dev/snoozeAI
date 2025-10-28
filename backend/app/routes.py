from fastapi import APIRouter
from .schemas import TextIn, SummaryOut, ClassifyOut, StoreIn, StoreOut
from .services import ai_summarize, ai_classify
from .db import store_snoozed

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
    urgency = await ai_classify(payload.text)
    return {"urgency": urgency, "label": "urgent" if urgency > 0.6 else "normal"}

@router.post("/store", response_model=StoreOut)
def store(payload: StoreIn):
    # DEMO MODE: pretend we saved it; log to console
    print("[DEMO] /store payload:", payload.model_dump())
    return {"id": "demo_" + payload.userId}
