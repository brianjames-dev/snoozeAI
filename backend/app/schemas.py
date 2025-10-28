
## `backend/app/schemas.py`
from pydantic import BaseModel
from typing import Optional
from datetime import datetime

class TextIn(BaseModel):
    text: str
    max_tokens: Optional[int] = 80

class SummaryOut(BaseModel):
    summary: str

class ClassifyOut(BaseModel):
    urgency: float
    label: str

class StoreIn(BaseModel):
    userId: str
    title: str
    body: str
    summary: str
    urgency: float
    snoozeUntil: datetime

class StoreOut(BaseModel):
    id: str
