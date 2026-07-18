from app.ai.obligation_extractor import NoOpObligationExtractor
from app.schemas.models import ExtractObligationsRequest


def test_noop_extractor_returns_empty():
    extractor = NoOpObligationExtractor()
    request = ExtractObligationsRequest(
        circular_id="test-circular-id",
        tenant_id="test-tenant-id",
        text="Banks shall classify loans overdue by 3 months as Sub-Standard from the quarter ending...",
        circular_number="BRPD Circular No. 15",
        regulator="Bangladesh Bank",
        department="Banking Regulation & Policy Department",
    )
    result = extractor.extract(request)

    assert result.circular_id == "test-circular-id"
    assert result.tenant_id == "test-tenant-id"
    assert result.model_used == "none"
    assert len(result.obligations) == 0


def test_noop_extractor_schema_valid():
    extractor = NoOpObligationExtractor()
    request = ExtractObligationsRequest(
        circular_id="test-id",
        tenant_id="test-tenant",
        text="Test circular content",
    )
    result = extractor.extract(request)
    assert result.model_dump()
    assert isinstance(result.obligations, list)
