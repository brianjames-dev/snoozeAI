from __future__ import annotations

import os
from datetime import datetime
from typing import Dict, List, Optional

from google.cloud import firestore

_client: Optional[firestore.Client] = None
_local_cache: Dict[str, Dict] = {}
_collection_name = "snoozes"


def _get_project_kwargs() -> dict:
    project_id = os.getenv("GCP_PROJECT_ID")
    return {"project": project_id} if project_id else {}


def get_db() -> Optional[firestore.Client]:
    global _client
    if _client is not None:
        return _client
    try:
        _client = firestore.Client(**_get_project_kwargs())
        return _client
    except Exception as exc:
        # Firestore isn't configured locally; fall back to in-memory store to keep demos working.
        print("[DB] Firestore unavailable, using in-memory cache:", exc)
        return None


def _serialize_record(payload: Dict) -> Dict:
    snooze_until = payload["snooze_until"]
    if isinstance(snooze_until, str):
        snooze_until = datetime.fromisoformat(snooze_until)
    return {
        "title": payload["title"],
        "body": payload["body"],
        "summary": payload["summary"],
        "urgency": payload.get("urgency", 0.0),
        "snoozeUntil": snooze_until,
    }


def store_snoozed(payload) -> str:
    doc_id = payload.id
    record = _serialize_record(payload.model_dump())
    client = get_db()
    if client is None:
        _local_cache[doc_id] = record
        return doc_id

    ref = client.collection(_collection_name).document(doc_id)
    record_to_save = {
        **record,
        "createdAt": firestore.SERVER_TIMESTAMP,
        "updatedAt": firestore.SERVER_TIMESTAMP,
    }
    ref.set(record_to_save)
    return doc_id


def fetch_items(limit: int = 50) -> List[Dict]:
    client = get_db()
    if client is None:
        items = [
            {"id": k, **v}
            for k, v in sorted(
                _local_cache.items(),
                key=lambda entry: entry[1]["snoozeUntil"],
                reverse=True,
            )
        ]
        return items[:limit]

    docs = (
        client.collection(_collection_name)
        .order_by("snoozeUntil", direction=firestore.Query.DESCENDING)
        .limit(limit)
        .stream()
    )
    items: List[Dict] = []
    for snap in docs:
        data = snap.to_dict()
        if not data:
            continue
        items.append(
            {
                "id": snap.id,
                "title": data.get("title", ""),
                "body": data.get("body", ""),
                "summary": data.get("summary", ""),
                "urgency": data.get("urgency", 0.0),
                "snoozeUntil": data.get("snoozeUntil"),
            }
        )
    return items


def update_snoozed(doc_id: str, payload) -> str:
    updates = {}
    data = payload.model_dump(exclude_unset=True)
    if "title" in data and data["title"] is not None:
        updates["title"] = data["title"]
    if "body" in data and data["body"] is not None:
        updates["body"] = data["body"]
    if "summary" in data and data["summary"] is not None:
        updates["summary"] = data["summary"]
    if "urgency" in data and data["urgency"] is not None:
        updates["urgency"] = data["urgency"]
    if "snooze_until" in data and data["snooze_until"] is not None:
        value = data["snooze_until"]
        if isinstance(value, str):
            value = datetime.fromisoformat(value)
        updates["snoozeUntil"] = value

    if not updates:
        return doc_id

    client = get_db()
    if client is None:
        record = _local_cache.get(doc_id)
        if not record:
            raise ValueError("Unknown snooze id")
        record.update(updates)
        return doc_id

    ref = client.collection(_collection_name).document(doc_id)
    updates["updatedAt"] = firestore.SERVER_TIMESTAMP
    ref.set(updates, merge=True)
    return doc_id


def delete_snoozed(doc_id: str) -> str:
    client = get_db()
    if client is None:
        if doc_id in _local_cache:
            del _local_cache[doc_id]
        return doc_id

    client.collection(_collection_name).document(doc_id).delete()
    return doc_id
