import numpy as np
import httpx

from app.core.config import settings
from app.core.logging import logger

JINA_URL = "https://api.jina.ai/v1/embeddings"
JINA_MODEL = "jina-embeddings-v3"


async def jina_embed(texts: list[str]) -> np.ndarray:
    api_key = settings.JINA_API_KEY
    if not api_key:
        raise ValueError("JINA_API_KEY not set")

    async with httpx.AsyncClient() as client:
        resp = await client.post(
            JINA_URL,
            headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
            json={"model": JINA_MODEL, "input": texts, "task": "retrieval.passage"},
            timeout=30,
        )
        resp.raise_for_status()
        data = resp.json()
        embeddings = [item["embedding"] for item in sorted(data["data"], key=lambda x: x["index"])]
        return np.array(embeddings, dtype=np.float32)
