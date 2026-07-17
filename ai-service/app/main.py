from fastapi import FastAPI

from app.api.routes import router
from app.core.logging import setup_logging

setup_logging()

app = FastAPI(title="BD Compliance AI Service", version="1.0.0")
app.include_router(router)
