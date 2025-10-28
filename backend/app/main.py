from fastapi import FastAPI
from .routes import router

app = FastAPI(title="AI Notification Agent API")
app.include_router(router)
