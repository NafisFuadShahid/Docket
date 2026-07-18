import httpx
from fastapi import APIRouter, BackgroundTasks, HTTPException

from app.core.config import settings
from app.core.logging import logger
from app.crawler.crawler import crawler
from app.extraction.pdf_extractor import extract_text_from_pdf
from app.ai.obligation_extractor import get_extractor
from app.assistant.chat import assistant
from app.embeddings.rag import index_document as rag_index, query as rag_query
from app.schemas.models import (
    CrawlRequest, DownloadPdfRequest, ExtractTextRequest,
    ExtractObligationsRequest, AssistantRequest, AssistantResponse,
)

router = APIRouter()

INTERNAL_HEADERS = {"X-Internal-Api-Key": settings.INTERNAL_API_KEY}


@router.get("/health")
async def health():
    return {"status": "ok", "ai_key_configured": settings.has_ai_key}


@router.post("/crawl/{source_id}")
async def crawl_source(source_id: str, request: CrawlRequest, background_tasks: BackgroundTasks):
    """Trigger a crawl for the given source. Results are sent back to Spring Boot."""

    async def _crawl_and_callback():
        result = await crawler.crawl_source(source_id, request.source_url, request.source_type)
        try:
            async with httpx.AsyncClient() as client:
                await client.post(
                    f"{settings.SPRING_BACKEND_URL}/api/v1/internal/crawl-results",
                    json=result.model_dump(),
                    headers=INTERNAL_HEADERS,
                    timeout=30,
                )
            logger.info("crawl_callback_sent", source_id=source_id, count=len(result.circulars))
        except Exception as e:
            logger.error("crawl_callback_failed", source_id=source_id, error=str(e))

    background_tasks.add_task(_crawl_and_callback)
    return {"status": "crawl_started", "source_id": source_id}


@router.post("/download-pdf")
async def download_pdf(request: DownloadPdfRequest, background_tasks: BackgroundTasks):
    """Download a PDF from regulator site and notify Spring Boot."""

    async def _download_and_callback():
        try:
            file_path, sha256, file_size = await crawler.download_pdf(
                request.pdf_url, f"{settings.STORAGE_PATH}/circulars/{request.circular_id}")

            result = {
                "circular_id": request.circular_id,
                "file_path": file_path,
                "file_name": request.pdf_url.split("/")[-1] or "document.pdf",
                "sha256_hash": sha256,
                "file_size": file_size,
                "language": request.language or "EN",
            }

            async with httpx.AsyncClient() as client:
                await client.post(
                    f"{settings.SPRING_BACKEND_URL}/api/v1/internal/pdf-downloaded",
                    json=result,
                    headers=INTERNAL_HEADERS,
                    timeout=30,
                )
            logger.info("pdf_download_callback_sent", circular_id=request.circular_id)
        except Exception as e:
            logger.error("pdf_download_failed", circular_id=request.circular_id, error=str(e))

    background_tasks.add_task(_download_and_callback)
    return {"status": "download_started", "circular_id": request.circular_id}


@router.post("/extract-text")
async def extract_text(request: ExtractTextRequest, background_tasks: BackgroundTasks):
    """Extract text from a PDF file. Results are sent back to Spring Boot."""

    async def _extract_and_callback():
        result = extract_text_from_pdf(request.file_path, request.document_version_id)
        try:
            async with httpx.AsyncClient() as client:
                await client.post(
                    f"{settings.SPRING_BACKEND_URL}/api/v1/internal/extraction-results",
                    json=result.model_dump(),
                    headers=INTERNAL_HEADERS,
                    timeout=30,
                )
            logger.info("extraction_callback_sent", doc_version_id=request.document_version_id, status=result.status)
        except Exception as e:
            logger.error("extraction_callback_failed", error=str(e))

        if result.status == "COMPLETED" and result.full_text:
            await rag_index(request.document_version_id, result.full_text)

    background_tasks.add_task(_extract_and_callback)
    return {"status": "extraction_started", "document_version_id": request.document_version_id}


@router.post("/extract-obligations")
async def extract_obligations(request: ExtractObligationsRequest, background_tasks: BackgroundTasks):
    """Extract obligations from circular text using AI. Results sent to Spring Boot."""

    async def _extract_and_callback():
        extractor = get_extractor()
        result = extractor.extract(request)
        try:
            async with httpx.AsyncClient() as client:
                await client.post(
                    f"{settings.SPRING_BACKEND_URL}/api/v1/obligations/internal/extraction-callback",
                    json=result.model_dump(),
                    headers=INTERNAL_HEADERS,
                    timeout=30,
                )
            logger.info("obligation_callback_sent", circular_id=request.circular_id, count=len(result.obligations))
        except Exception as e:
            logger.error("obligation_callback_failed", error=str(e))

    background_tasks.add_task(_extract_and_callback)
    return {"status": "extraction_started", "circular_id": request.circular_id}


@router.post("/assistant/chat")
async def assistant_chat(request: AssistantRequest) -> AssistantResponse:
    """Chat with the compliance assistant. Enriches context with RAG search."""
    rag_context = await rag_query(request.message)
    combined = request.context or ""
    if rag_context:
        combined = f"{combined}\n\n=== Relevant Document Excerpts (RAG) ===\n{rag_context}"
    return assistant.chat(request, context=combined)
