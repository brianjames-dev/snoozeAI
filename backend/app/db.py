from google.cloud import firestore

def get_db():
    # Requires GOOGLE_APPLICATION_CREDENTIALS env var to be set
    return firestore.Client()

def store_snoozed(item: dict) -> str:
    db = get_db()
    ref = db.collection("snoozed_notifications").document()
    ref.set(item)
    return ref.id
