
## `backend/app/schemas.py`
from pydantic import AliasChoices, BaseModel, ConfigDict, Field
from typing import Optional, List
from datetime import datetime

class TextIn(BaseModel):
    text: str
    max_tokens: Optional[int] = 80
    hints: Optional[List[str]] = None

class SummaryOut(BaseModel):
    summary: str

class ClassifyOut(BaseModel):
    urgency: float
    label: str

class StoreIn(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    id: str
    title: str
    body: str
    summary: str
    urgency: Optional[float] = None
    snooze_until: datetime = Field(
        ...,
        validation_alias=AliasChoices("snoozeUntil", "snooze_until"),
        serialization_alias="snoozeUntil",
    )

class StoreOut(BaseModel):
    ok: bool
    id: str

class SnoozedItemOut(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    id: str
    title: str
    summary: str
    urgency: Optional[float] = None
    snooze_until: datetime = Field(..., serialization_alias="snoozeUntil")

class ItemsOut(BaseModel):
    items: List[SnoozedItemOut]
