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

   # in .env, set:

   # OPENAI_API_KEY=sk-...

   # GOOGLE_APPLICATION_CREDENTIALS=/ABS/PATH/service-account.json

   # FIRESTORE_PROJECT_ID=your-gcp-project

   # ENV=dev

   # FEATURE_USE_AI=false # set true once real prompts are wired

3. Start the API:
   uvicorn app.main:app --reload --port 8000

4. Quick smoke tests (in another terminal):
   curl -s http://127.0.0.1:8000/health
   curl -s -X POST http://127.0.0.1:8000/summarize -H "Content-Type: application/json" -d '{"text":"Long original body"}'
   curl -s -X POST http://127.0.0.1:8000/classify -H "Content-Type: application/json" -d '{"text":"Might be urgent"}'

---

## iOS (SwiftUI) — Run & Test Notifications

1. Open the Xcode project under /ios and run on any Simulator.
2. Create a file named payload.apns (anywhere on your machine) with the following JSON:
   {
   "Simulator Target Bundle": "dev.brianjames.AINotificationAgent",
   "aps": {
   "alert": {
   "title": "Test Alert",
   "body": "This is a long original notification body that we will summarize."
   },
   "category": "SNOOZE_CATEGORY"
   },
   "metadata": { "sourceApp": "ExampleCo" }
   }

   • If your app bundle identifier differs, replace dev.brianjames.AINotificationAgent above.  
   • Ensure your iOS app wires "SNOOZE_CATEGORY" to show the Snooze action and call backend endpoints.

3. With the Simulator booted and the app installed, send the push:
   xcrun simctl push booted dev.brianjames.AINotificationAgent payload.apns

   Tips:
   • Background the app to see the system banner.  
   • Foreground to test in-app handling.  
   • Wire your Snooze action to POST to /summarize, /classify, and your store endpoint.

---

## Firestore (Dev Hygiene)

- Store snoozed items in a collection such as snoozed_notifications with fields like userID, title, body, summary, urgency, timestamp.
- During development, add env="dev" or isTest=true so test docs are easy to purge with a one-off script.

---

## Common Issues

- Firestore auth errors: verify GOOGLE_APPLICATION_CREDENTIALS path and FIRESTORE_PROJECT_ID in .env.
- OpenAI errors or timeouts: set FEATURE_USE_AI=false to use mock responses while wiring prompts.
- Simulator push not arriving: confirm the bundle id in payload.apns matches the installed app’s bundle id and that the Simulator is “booted”.
