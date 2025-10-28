# Infra Notes

- Place your Firestore service account as `infra/service-account.json` (keep out of git).
- Export credentials for the backend:
  ```bash
  export GOOGLE_APPLICATION_CREDENTIALS="/ABSOLUTE/PATH/ai-notif-agent/infra/service-account.json"
  ```
