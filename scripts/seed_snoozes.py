#!/usr/bin/env python3
"""
Seed Firestore via the FastAPI backend with realistic notification examples.
Usage:

    python scripts/seed_snoozes.py --sample pagerduty
    python scripts/seed_snoozes.py --all
"""

from __future__ import annotations

import argparse
import datetime as dt
import sys
import uuid
from typing import Dict

import httpx

DEFAULT_BASE = "http://127.0.0.1:8000"

# Matching push payloads live under `payloads/<key>.apns`.
# Example Notification Service test command(s):
#   make push PAYLOAD=payloads/payload_pagerduty.apns
#   make push PAYLOAD=payloads/payload_flight-checkin.apns
#   make push PAYLOAD=payloads/payload_rent.apns
#   make push PAYLOAD=payloads/payload_school.apns
#   make push PAYLOAD=payloads/payload_calendar.apns
#   make push PAYLOAD=payloads/payload_news.apns
SAMPLES: Dict[str, Dict[str, str]] = {
    "flight-checkin": {
        "title": "Flight Check-In",
        "body": (
            "JetBlue reminder: online check-in for BOS → SFO opens in 15 minutes. "
            "Please confirm TSA Pre and make sure your passport + negative test upload are complete. "
            "Seats are filling quickly so confirm now if you want to keep the exit-row upgrade."
        ),
    },
    "pagerduty": {
        "title": "PagerDuty",
        "body": (
            "Incident #14322 (api-prod-1 CPU) triggered. Error rate up 320% in last 5 minutes, "
            "latency > 1.8s, and customer traffic is being throttled. Ack within 2 minutes and join the Zoom bridge."
        ),
    },
    "rent": {
        "title": "Rent Payment",
        "body": (
            "Your rent for 25 Larkin St is due tomorrow. Submit payment in the Resident Portal to avoid late fees. "
            "Please include the parking add-on and note that ACH transfers take 24h, so pay with card if you're running behind."
        ),
    },
    "school": {
        "title": "School Pickup",
        "body": (
            "Reminder from Lincoln Elementary: Ava's parent-teacher conference was moved to 3:30 PM today and the teacher "
            "needs you to bring last week's reading log. Parking lot construction will add 10 minutes to pickup."
        ),
    },
    "calendar": {
        "title": "Calendar Invite",
        "body": (
            "Weekly sync with Product starts in 20 minutes on Zoom. Agenda: launch blockers, KPI review, and final sign-off on the "
            "marketing one-pager. Bring the dashboard link from Growth so we can annotate the slides live."
        ),
    },
    "news": {
        "title": "News Alert",
        "body": (
            "NYTimes: Fed hints at rate cuts later this year as inflation cools for the third month. "
            "Markets reacting; futures up 1.3% and analysts expect tech to rally. Read the recap before tomorrow's investor update."
        ),
    },
}


def iso_in(minutes: int) -> str:
    return (dt.datetime.utcnow() + dt.timedelta(minutes=minutes)).isoformat() + "Z"


def seed_sample(client: httpx.Client, base: str, key: str, minutes: int) -> None:
    sample = SAMPLES[key]
    text = sample["body"]
    summary = client.post(f"{base}/summarize", json={"text": text}).json()["summary"]

    classify_payload = {"text": text}
    urgency_resp = client.post(f"{base}/classify", json=classify_payload).json()
    payload = {
        "id": uuid.uuid4().hex,
        "title": sample["title"],
        "body": text,
        "summary": summary,
        "urgency": urgency_resp["urgency"],
        "snoozeUntil": iso_in(minutes),
    }
    resp = client.post(f"{base}/store", json=payload)
    resp.raise_for_status()
    print(f"Seeded {key} → {resp.json()}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Seed the SnoozeAI backend with sample notifications.")
    parser.add_argument(
        "--base-url",
        default=DEFAULT_BASE,
        help="Backend base URL (default: http://127.0.0.1:8000)",
    )
    parser.add_argument(
        "--sample",
        action="append",
        choices=sorted(SAMPLES.keys()),
        help="Sample key to seed. Repeat to add multiple.",
    )
    parser.add_argument(
        "--all",
        action="store_true",
        help="Seed all sample notifications.",
    )
    parser.add_argument(
        "--minutes",
        type=int,
        default=45,
        help="Minutes from now to snooze each sample (default: 45).",
    )
    args = parser.parse_args()

    keys = args.sample if args.sample else []
    if args.all:
        keys = list(SAMPLES.keys())

    if not keys:
        parser.error("Provide at least one --sample or use --all.")

    with httpx.Client(timeout=10.0) as client:
        for key in keys:
            seed_sample(client, args.base_url.rstrip("/"), key, args.minutes)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
