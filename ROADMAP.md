# SnoozeAI – Execution Roadmap (Weeks 1–10)

> Status Legend: ✅ done · 🟡 in progress · ⏳ not started

---

## Week 1 — Repo & Scaffolding ✅

**Goals**

- Single-repo layout: `backend/`, `ios/`, `infra/`, `shared/`.
- Basic README, `.env` templates, and run instructions.
- Python venv + uvicorn dev loop.
- iOS SwiftUI project bootstrapped (Swift Package Manager for Firebase if needed).

**Artifacts/Refs**

- `backend/app/main.py`, `backend/app/routes.py`
- `ios/` project created and builds on Simulator

**Acceptance**

- [x] `source .venv/bin/activate && python -m uvicorn backend.app.main:app --reload --port 8000` → server starts
- [x] Xcode builds & runs a blank app on Simulator

---

## Week 2 — Backend API (stubbed) ✅

**Goals**

- FastAPI endpoints: `/health`, `/summarize`, `/classify` (mock logic; no OpenAI).
- Simple contracts stable enough for iOS & the extension.
- Curlable smoke tests.

**Acceptance**

- [x] `curl localhost:8000/health` → `{"ok":true}`
- [x] `curl -X POST /summarize …` returns mock summary
- [x] `curl -X POST /classify …` returns mock urgency + label

---

## Week 3 — iOS App Skeleton ✅

**Goals**

- SwiftUI app shell + **HomeView** (request notification permission).
- **SnoozedDashboardView** list with **SnoozedCardView** cells.
- **LocalSnoozedCache** persisted to file; live countdown.
- **SnoozeService** network client + `withRetry` helper hitting local backend.

**Acceptance**

- [x] App runs; tapping “Request Notification Permission” prompts & grants
- [x] Dashboard renders demo items; countdown ticks; relaunch restores from cache
- [x] Network calls to backend succeed when server is running

---

## Week 4 — Notification Service Extension (demo) ✅

**Goals**

- Notification Service Extension intercepts remote-style pushes (`"mutable-content": 1`).
- Calls backend `/summarize`; rewrites notification `subtitle/body`.
- ATS exceptions for localhost in **app** and **extension** (dev-only).
- Simulator push flow tested.

**Acceptance**

- [x] Backend running
- [x] `xcrun simctl push booted "dev.brianjames.AINotificationAgent" payload.apns`
- [x] Banner arrives; subtitle “AI Summary”; backend logs POST `/summarize`

---

## Week 5 — Real AI Toggle ✅

**Goals**

- `backend/app/ai.py` with `summarize()` and `classify()` using `USE_OPENAI=true|false`.
- OpenAI path (gpt-4o-mini) when enabled; token/length control.
- `/summarize` & `/classify` delegate to `ai.py`.
- Update `.env.example` with `USE_OPENAI`, `OPENAI_API_KEY`, `OPENAI_MODEL`.

**Acceptance**

- [x] `USE_OPENAI=false` → mock summary, no external calls
- [x] `USE_OPENAI=true` + key → real model output from `/summarize`

---

## Week 6 — Firestore Persistence ✅

**Goals**

- `POST /store` → Firestore `snoozes/{id}` with `serverTimestamp`.
- `GET /items?limit=50` → latest items (e.g., by `snooze_until` desc).
- iOS dashboard loads from backend with local-cache fallback.

**Acceptance**

- [x] `export GOOGLE_APPLICATION_CREDENTIALS=/abs/path/service-account.json`
- [x] POST then GET returns stored item(s)
- [x] iOS shows server items on appear/refresh; cache reconciles

---

## Week 7 — Create/Snooze UX ✅

**Goals**

- `NewSnoozeSheet.swift` (Title, Body, Duration: 15m/1h/Today PM/Custom).
- `SnoozeService.createSnooze(...)` → backend → Firestore.
- Optimistic UI insert; error banner on failure.

**Acceptance**

- [x] Creating a snooze adds a card immediately; survives relaunch
- [x] Failure path shows retry prompt/banner

---

## Week 8 — Scheduling & Background ✅

**Goals**

- Schedule local notification for each new snooze at `snoozeUntil`.
- Foreground passive refresh + manual “Refresh”.
- BGTask skeleton registered (future ready).

**Acceptance**

- [x] Local notif fires on time in Simulator
- [x] Manual refresh updates list without relaunch

---

## Week 9 — Preferences & Triage ✅

**Goals**

- `SettingsView.swift`: quiet hours, default snooze, prioritized sources.
- Persist via `@AppStorage` (UserDefaults).
- Backend accepts optional “hints” for `/classify` (simple rules for now).

**Acceptance**

- [x] Settings persist across relaunch
- [x] New Snooze sheet preselects default duration

---

## Week 10 — Polish & Delivery ⏳

**Goals**

- Clean empty/loading/error states.
- Minimal logging/telemetry (backend INFO per route; iOS `print`/`os_log`).
- README update + 2–3 min demo script.
- Reminder: tighten ATS exceptions before TestFlight/release.

**Acceptance**

- [ ] Fresh clone → follow README → run backend + iOS + push demo successfully
- [ ] Demo script covers: request permission → create snooze → dashboard → local notif → push summary via `simctl`

---

## Dev Commands

```bash
# backend (from repo root)
make backend

# unit tests
make test

# toggle AI
make openai-on   # or openai-off

# Firestore (set once when ready)
export GOOGLE_APPLICATION_CREDENTIALS=/abs/path/service-account.json

# push a test APNs payload to the booted simulator (runs the extension)
make push
```
