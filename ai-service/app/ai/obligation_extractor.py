import json
from typing import Protocol

from app.core.config import settings
from app.core.logging import logger
from app.schemas.models import ExtractedObligation, ObligationExtractionResult, ExtractObligationsRequest


class ObligationExtractorProvider(Protocol):
    def extract(self, request: ExtractObligationsRequest) -> ObligationExtractionResult: ...


class MockObligationExtractor:
    """Deterministic mock extractor for demo/testing when no AI API key is configured."""

    def extract(self, request: ExtractObligationsRequest) -> ObligationExtractionResult:
        text = request.text[:500]
        obligations = [
            ExtractedObligation(
                obligation_title=f"Comply with {request.circular_number or 'regulatory directive'}",
                obligation_detail=f"Financial institutions must comply with the requirements outlined in this circular. {text[:200]}...",
                source_quote=text[:150],
                source_page=1,
                regulator=request.regulator,
                circular_number=request.circular_number,
                source_department=request.department,
                affected_institution_types=["scheduled_bank"],
                affected_business_lines=["general"],
                impacted_departments=["compliance", "operations"],
                required_actions=["Review circular", "Update internal policy", "Train relevant staff", "Submit compliance report"],
                required_evidence=["Updated policy document", "Training records", "Compliance report"],
                severity="MEDIUM",
                confidence=0.75,
                rationale="Mock extraction — review required. Actual obligations should be verified by compliance officer.",
            ),
            ExtractedObligation(
                obligation_title=f"Report compliance status for {request.circular_number or 'this circular'}",
                obligation_detail="Submit compliance status report to the regulator within the prescribed timeline.",
                source_quote="Report to be submitted within the stipulated timeframe...",
                source_page=2,
                regulator=request.regulator,
                circular_number=request.circular_number,
                source_department=request.department,
                affected_institution_types=["scheduled_bank"],
                impacted_departments=["compliance"],
                required_actions=["Prepare compliance report", "Submit to regulator"],
                required_evidence=["Compliance report", "Submission acknowledgement"],
                severity="HIGH",
                confidence=0.70,
                rationale="Mock extraction — standard reporting obligation inferred from circular text.",
            ),
        ]
        logger.info("mock_extraction", circular_id=request.circular_id, obligations=len(obligations))
        return ObligationExtractionResult(
            circular_id=request.circular_id,
            tenant_id=request.tenant_id,
            obligations=obligations,
            model_used="mock-provider",
        )


class OpenAIObligationExtractor:
    """Real LLM-based obligation extraction using OpenAI-compatible API."""

    SYSTEM_PROMPT = """You are a Bangladesh banking compliance expert. Extract structured regulatory obligations from the circular text.

For each obligation found, provide:
- obligation_title: concise title
- obligation_detail: full description
- source_quote: exact quote from the text
- source_page: page number if known
- regulator: "Bangladesh Bank" or "BFIU"
- circular_number: if mentioned
- source_department: issuing department
- affected_institution_types: list from [scheduled_bank, nbfi, mfs, psp, ad_branch, islamic_banking, agent_banking, digital_lending]
- affected_business_lines: list from [foreign_exchange, trade_finance, treasury, card_payment, sme_agri_loans, branch_network]
- impacted_departments: list from [credit_risk, trade_finance, aml_cft, ict_security, treasury, operations, legal, branch_banking, compliance]
- deadline: ISO date if mentioned
- effective_date: ISO date if mentioned
- required_actions: list of actions needed
- required_evidence: list of evidence to collect
- severity: LOW, MEDIUM, HIGH, or CRITICAL
- confidence: 0.0 to 1.0
- rationale: why this is an obligation

Return a JSON array of obligations. Be precise and cite the source text."""

    def extract(self, request: ExtractObligationsRequest) -> ObligationExtractionResult:
        from openai import OpenAI

        client = OpenAI(api_key=settings.AI_API_KEY, base_url=settings.AI_API_BASE)

        try:
            response = client.chat.completions.create(
                model=settings.AI_MODEL,
                messages=[
                    {"role": "system", "content": self.SYSTEM_PROMPT},
                    {"role": "user", "content": f"Circular: {request.circular_number or 'Unknown'}\nDepartment: {request.department or 'Unknown'}\n\nText:\n{request.text[:8000]}"},
                ],
                response_format={"type": "json_object"},
                temperature=0.1,
            )

            content = response.choices[0].message.content
            data = json.loads(content)
            obligations_data = data.get("obligations", data) if isinstance(data, dict) else data
            if not isinstance(obligations_data, list):
                obligations_data = [obligations_data]

            obligations = []
            for item in obligations_data:
                try:
                    obligations.append(ExtractedObligation(**item))
                except Exception as e:
                    logger.warning("obligation_parse_error", error=str(e), item=str(item)[:200])

            logger.info("ai_extraction", circular_id=request.circular_id, model=settings.AI_MODEL, obligations=len(obligations))
            return ObligationExtractionResult(
                circular_id=request.circular_id,
                tenant_id=request.tenant_id,
                obligations=obligations,
                model_used=settings.AI_MODEL,
            )
        except Exception as e:
            logger.error("ai_extraction_failed", error=str(e))
            return MockObligationExtractor().extract(request)


def get_extractor() -> ObligationExtractorProvider:
    if settings.has_ai_key:
        return OpenAIObligationExtractor()
    return MockObligationExtractor()
