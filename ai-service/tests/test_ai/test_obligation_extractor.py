from app.ai.obligation_extractor import MockObligationExtractor
from app.schemas.models import ExtractObligationsRequest


def test_mock_extractor_returns_valid_obligations():
    extractor = MockObligationExtractor()
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
    assert result.model_used == "mock-provider"
    assert len(result.obligations) == 2

    ob = result.obligations[0]
    assert ob.obligation_title
    assert ob.obligation_detail
    assert ob.regulator == "Bangladesh Bank"
    assert ob.circular_number == "BRPD Circular No. 15"
    assert ob.severity in ["LOW", "MEDIUM", "HIGH", "CRITICAL"]
    assert 0.0 <= ob.confidence <= 1.0
    assert len(ob.required_actions) > 0
    assert len(ob.required_evidence) > 0
    assert len(ob.affected_institution_types) > 0


def test_mock_extractor_schema_valid():
    extractor = MockObligationExtractor()
    request = ExtractObligationsRequest(
        circular_id="test-id",
        tenant_id="test-tenant",
        text="Test circular content",
    )
    result = extractor.extract(request)

    for ob in result.obligations:
        assert ob.model_dump()
        assert isinstance(ob.impacted_departments, list)
        assert isinstance(ob.required_actions, list)
