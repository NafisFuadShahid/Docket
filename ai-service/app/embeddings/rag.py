from pathlib import Path

from lightrag import LightRAG, QueryParam
from lightrag.utils import EmbeddingFunc

from app.core.config import settings
from app.core.logging import logger
from app.embeddings.jina import jina_embed

RAG_DIR = Path(settings.STORAGE_PATH) / "lightrag"
JINA_EMBEDDING_DIM = 1024
_rag_instance: LightRAG | None = None
_initialized = False


async def _groq_llm_func(prompt, system_prompt=None, history_messages=None, keyword_extraction=False, **kwargs) -> str:
    from openai import AsyncOpenAI
    client = AsyncOpenAI(api_key=settings.AI_API_KEY, base_url=settings.AI_API_BASE)

    messages = []
    if system_prompt:
        messages.append({"role": "system", "content": system_prompt})
    if history_messages:
        messages.extend(history_messages)
    messages.append({"role": "user", "content": prompt})

    response = await client.chat.completions.create(
        model=settings.AI_MODEL,
        messages=messages,
        temperature=0.1,
    )
    return response.choices[0].message.content


async def _jina_embedding_func(texts: list[str]) -> list[list[float]]:
    return await jina_embed(texts)


async def get_rag() -> LightRAG | None:
    global _rag_instance, _initialized
    if _rag_instance is not None and _initialized:
        return _rag_instance

    if not settings.has_ai_key or not settings.JINA_API_KEY:
        return None

    RAG_DIR.mkdir(parents=True, exist_ok=True)

    if _rag_instance is None:
        _rag_instance = LightRAG(
            working_dir=str(RAG_DIR),
            llm_model_func=_groq_llm_func,
            llm_model_name=settings.AI_MODEL,
            embedding_func=EmbeddingFunc(
                embedding_dim=JINA_EMBEDDING_DIM,
                max_token_size=8192,
                func=_jina_embedding_func,
            ),
            chunk_token_size=500,
            chunk_overlap_token_size=100,
            max_parallel_insert=2,
            llm_model_max_async=2,
            embedding_func_max_async=4,
        )

    if not _initialized:
        await _rag_instance.initialize_storages()
        _initialized = True
        logger.info("rag_initialized", working_dir=str(RAG_DIR))

    return _rag_instance


async def index_document(doc_id: str, text: str, metadata: dict | None = None):
    rag = await get_rag()
    if rag is None:
        return
    try:
        await rag.ainsert(text, ids=[doc_id])
        logger.info("rag_document_indexed", doc_id=doc_id, chars=len(text))
    except Exception as e:
        logger.error("rag_index_failed", doc_id=doc_id, error=str(e))


async def query(question: str, mode: str = "hybrid") -> str:
    rag = await get_rag()
    if rag is None:
        return ""
    try:
        result = await rag.aquery(question, param=QueryParam(mode=mode))
        return result or ""
    except Exception as e:
        logger.error("rag_query_failed", error=str(e))
        return ""
