# SnoozeAI Android (Notification Assistant)

This module brings the existing FastAPI backend to Android via a notification-listener service. It mirrors the iOS flows: capture incoming notifications, call `/summarize` and `/classify`, store/snooze locally, and re-post a summarized notification.

## Quick Start

1. Open `android/` in **Android Studio Hedgehog/Flamingo+**.
2. Set the backend URL (defaults to emulator localhost) in `android/local.properties`:
   ```
   SNOOZE_BASE_URL=http://10.0.2.2:8000
   ```
3. Run on a device/emulator. Grant:
   - Notification permission (Android 13+)
   - Notification Listener access (system settings prompt will appear)
   - Battery optimizations exemption (optional but reduces kills on OEM skins)
4. With the FastAPI backend running (`make backend`), post any notification on the device; the listener will:
   - Extract title/body → call `/summarize`, `/classify`
   - Persist to Room + `/store`
   - Re-post a summarized notification with snooze actions

## Permissions

- Android 13+ notifications permission (prompted in-app)
- Notification Listener access (system settings screen)
- Battery optimization exemption (recommended to reduce Doze/OEM kills)

## Privacy Copy (Play)

"SnoozeAI reads your notification title/body locally, sends it to your configured SnoozeAI backend to summarize/classify, and stores snoozes on-device with optional Firestore sync. No other data is collected. You can revoke notification access anytime in system settings."

## What’s Included

- `app/src/main/AndroidManifest.xml` — permissions + service declarations.
- `NotificationListenerService` (`SnoozeNotificationListenerService.kt`) — ingests notifications.
- `backend/ApiService.kt` — Retrofit client for existing endpoints.
- `data/SnoozeRepository.kt` — Room cache + backend sync.
- `notifications/NotificationPublisher.kt` — posts replacement/snoozed notifications.
- `notifications/SnoozeScheduler.kt` — schedules local re-post via `WorkManager`.
- `ui/MainActivity.kt` — Jetpack Compose tabs:
  - Home: permissions + sample notification trigger
  - Snoozed: list with refresh
  - Settings: quiet hours, default snooze, classifier hints

## Notes / Parity with iOS

- Uses the same contracts: `/summarize`, `/classify`, `/store`, `/items`.
- Local cache is Room (instead of iOS file cache). Snoozes re-post via `WorkManager`.
- The listener **cannot mutate** another app’s notification; it dismisses/ignores and posts its own.
- Add API key handling if you gate `/summarize`/`/classify` behind auth in the future.

## Next Steps

- Wire deep snooze actions (e.g., “Snooze 1h/Today PM”) into UI and `SnoozeScheduler`.
- Add crash/ANR safe-guards and tighter battery policies per OEM.
- Add unit tests for `SnoozeRepository` (Room in-memory) and `NotificationListenerService` (Robolectric).
