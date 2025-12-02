# SnoozeAI (Android + FastAPI backend)

Lightweight repo for the Android notification-listener client and the FastAPI backend it calls.

---

## Repo layout

- `backend/` — FastAPI app exposing `/summarize`, `/classify`, `/store`, `/items`, `/health`.
- `android/` — Android Studio project (notification listener, Compose UI).

---

## Backend — setup & run

1) Create a virtual environment and install deps:
```bash
python3 -m venv .venv
. .venv/bin/activate
pip install -r backend/requirements.txt
```

2) Configure environment (copy and edit):
```bash
cp backend/.env.example backend/.env
# set USE_OPENAI, OPENAI_API_KEY, Firestore creds if needed
```

3) Run the API (bind to all interfaces so the emulator can reach `http://10.0.2.2:8000`):
```bash
source .venv/bin/activate
set -a && source backend/.env && set +a
PYTHONPATH=. uvicorn backend.app.main:app --host 0.0.0.0 --port 8000
```

4) Optional: seed sample snoozes for testing:
```bash
python android/scripts/seed_snoozes.py --all   # defaults to http://127.0.0.1:8000
curl http://127.0.0.1:8000/items              # verify
```

---

## Android app

1) Open `android/` in Android Studio (Hedgehog/Flamingo+).
2) Set the backend URL in `android/local.properties` (default is emulator host alias):
```
SNOOZE_BASE_URL=http://10.0.2.2:8000
```
3) Run on a device/emulator. Grant:
- Notification permission (Android 13+)
- Notification Listener access (system settings prompt will appear)
- Battery optimization exemption (recommended)

Flow: the listener captures notifications from other apps, calls the backend (`/summarize`, `/classify`, `/store`), caches via Room, and re-posts a summarized notification with snooze actions.

---

## API quick reference

- `POST /summarize` `{text,max_tokens}` → `{summary}`
- `POST /classify` `{text,hints?}` → `{urgency,label}`
- `POST /store` `{id,title,body,summary,urgency?,snoozeUntil}` → `{ok,id}`
- `GET /items?limit=50` → `{items:[...]}` (ordered by `snoozeUntil` desc)
- `GET /health` → status map
