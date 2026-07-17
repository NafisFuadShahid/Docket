from app.extraction.pdf_extractor import extract_text_from_pdf


def test_extract_nonexistent_file():
    result = extract_text_from_pdf("/nonexistent/file.pdf", "test-doc-version-id")
    assert result.status == "FAILED"
    assert result.document_version_id == "test-doc-version-id"
    assert "not found" in result.error_message.lower()


def test_extraction_result_schema():
    result = extract_text_from_pdf("/nonexistent/file.pdf", "test-id")
    data = result.model_dump()
    assert "document_version_id" in data
    assert "status" in data
    assert "extraction_method" in data
