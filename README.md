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
 .env.sample  
/ios (SwiftUI app +, later, Notification Service Extension)

---

## Backend — Setup & Run (FastAPI)

1. Create & activate a virtual environment, install deps:
   cd backend
   python3 -m venv .venv
   source .venv/bin/activate
   pip install -r requirements.txt

2. Configure environment (copy and edit):
   cp .env.sample .env

   in .env, set:

   OPENAI_API_KEY=sk-...

   GOOGLE_APPLICATION_CREDENTIALS=/ABS/PATH/service-account.json

   FIRESTORE_PROJECT_ID=your-gcp-project

   ENV=dev

   FEATURE_USE_AI=false # set true once real prompts are wired

3. Start the API:
   uvicorn app.main:app --reload --port 8000

4. Quick smoke tests (in another terminal):
   curl -s http://127.0.0.1:8000/health
   curl -s -X POST http://127.0.0.1:8000/summarize -H "Content-Type: application/json" -d '{"text":"Long original body"}'
   curl -s -X POST http://127.0.0.1:8000/classify -H "Content-Type: application/json" -d '{"text":"Might be urgent"}'
