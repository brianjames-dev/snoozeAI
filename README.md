# SnoozeAI (AI Notification Agent)

Monorepo for **iOS (SwiftUI)** client + **FastAPI** backend. This README shows how to run both and how to test notifications in the iOS Simulator.

---

## Prerequisites

- Python 3.11+
- Xcode 15+ with iOS Simulator
- A Firebase Firestore service-account JSON (local path; do **not** commit it)

---

## Repo Layout

/backend  
 app/ (FastAPI: main.py, routes/)  
 requirements.txt  
 .env.example  
/ios (SwiftUI app +, later, Notification Service Extension)

---

## Backend — Setup & Run (FastAPI)

1. Create & activate a virtual environment, install deps:
   make setup

2. Configure environment (copy and edit):
   cp backend/.env.example backend/.env

   in backend/.env, set:

   USE_OPENAI=false            # flip true with a valid key
   OPENAI_API_KEY=sk-...
   OPENAI_MODEL=gpt-4o-mini
   GOOGLE_APPLICATION_CREDENTIALS=/ABS/PATH/service-account.json
   GCP_PROJECT_ID=your-gcp-project

3. Start the API:
   make backend

4. Quick smoke tests (in another terminal):
   curl -s http://127.0.0.1:8000/health
   curl -s -X POST http://127.0.0.1:8000/summarize -H "Content-Type: application/json" -d '{"text":"Long original body"}'
   curl -s -X POST http://127.0.0.1:8000/classify -H "Content-Type: application/json" -d '{"text":"Might be urgent"}'

---

## Dev Shortcuts (Makefile)

- `make backend` — run FastAPI (auto-creates venv if missing).
- `make test` — run backend unit tests (`pytest`).
- `make openai-on` / `make openai-off` — toggle `USE_OPENAI` in `backend/.env`.
- `make push` — push `payload.apns` to the currently booted iOS simulator via Notification Service Extension.
- `make bundle-id` — echoes the app bundle identifier (`dev.brianjames.AINotificationAgent`).

---

## Backend API Contracts

- `POST /summarize` `{text,max_tokens}` → `{summary}`. Honors `USE_OPENAI`; fallback mock is prefixed with `"Summary:"`.
- `POST /classify` `{text,hints?}` → `{urgency,label}`. `hints` is an array of keywords (from Settings) that nudge urgency.
- `POST /store` `{id,title,body,summary,urgency?,snoozeUntil}` → `{ok,id}`. Persists into Firestore when configured, otherwise a dev-only in-memory cache.
- `GET /items?limit=50` → `{items:[{id,title,summary,urgency,snoozeUntil}]}` ordered by `snoozeUntil` desc.

---

## iOS App Flow

- **Home tab** — Request notification permission + simple local notification test.
- **Snoozed tab** — Loads cached snoozes instantly, refreshes from backend, supports pull-to-refresh, manual refresh button, optimistic inserts, and error banners.
- **New Snooze Sheet** — Title + body + preset/custom duration. Calls backend summarize/classify/store, schedules a local notification, updates cache.
- **Settings tab** — Quiet hours, default snooze duration, prioritized sources (hints sent to classifier). Values persist via `@AppStorage`.
- **Background Refresh Skeleton** — Registers a `BGAppRefreshTask` identifier and re-schedules when app backgrounds (placeholder for future fetch logic).
