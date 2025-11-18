import logging
from fastapi import FastAPI
from .routes import router

logging.basicConfig(level=logging.INFO)

app = FastAPI(title="AI Notification Agent API")
app.include_router(router)
