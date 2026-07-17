from pathlib import Path

from app.core.logging import logger
from app.schemas.models import ExtractionResult


def extract_text_from_pdf(file_path: str, document_version_id: str) -> ExtractionResult:
    """Extract text from PDF using pdfplumber. Falls back to NEEDS_MANUAL_REVIEW if no text found."""
    try:
        import pdfplumber
    except ImportError:
        logger.warning("pdfplumber_not_available", path=file_path)
        return ExtractionResult(
            document_version_id=document_version_id,
            status="NEEDS_MANUAL_REVIEW",
            error_message="pdfplumber not installed",
        )

    path = Path(file_path)
    if not path.exists():
        return ExtractionResult(
            document_version_id=document_version_id,
            status="FAILED",
            error_message=f"File not found: {file_path}",
        )

    try:
        with pdfplumber.open(path) as pdf:
            pages = []
            chunks = []
            for i, page in enumerate(pdf.pages):
                text = page.extract_text() or ""
                pages.append(text)
                if text.strip():
                    chunks.append({
                        "page": i + 1,
                        "text": text,
                        "char_count": len(text),
                    })

            full_text = "\n\n".join(pages)

            if not full_text.strip():
                logger.warning("no_text_extracted", path=file_path, pages=len(pdf.pages))
                return ExtractionResult(
                    document_version_id=document_version_id,
                    page_count=len(pdf.pages),
                    extraction_method="PDF_TEXT",
                    status="NEEDS_MANUAL_REVIEW",
                    error_message="No text content found — may be a scanned document requiring OCR",
                )

            logger.info("text_extracted", path=file_path, pages=len(pdf.pages), chars=len(full_text))
            return ExtractionResult(
                document_version_id=document_version_id,
                full_text=full_text,
                extraction_method="PDF_TEXT",
                page_count=len(pdf.pages),
                chunks=chunks,
                status="COMPLETED",
            )
    except Exception as e:
        logger.error("extraction_failed", path=file_path, error=str(e))
        return ExtractionResult(
            document_version_id=document_version_id,
            status="FAILED",
            error_message=str(e),
        )
