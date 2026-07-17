from pydantic import BaseModel, Field
from typing import Optional


class ParsedCircular(BaseModel):
    circular_number: str | None = None
    title: str
    title_bn: str | None = None
    department: str | None = None
    issued_date: str | None = None
    source_url: str | None = None
    language: str = "EN"
    pdf_url: str | None = None
    pdf_url_bn: str | None = None
    metadata: dict = Field(default_factory=dict)


class CrawlResult(BaseModel):
    source_id: str
    circulars: list[ParsedCircular]


class ExtractionResult(BaseModel):
    document_version_id: str
    full_text: str | None = None
    extraction_method: str = "PDF_TEXT"
    page_count: int = 0
    chunks: list[dict] = Field(default_factory=list)
    status: str = "COMPLETED"
    error_message: str | None = None


class ExtractedObligation(BaseModel):
    obligation_title: str
    obligation_detail: str
    source_quote: str | None = None
    source_page: int | None = None
    regulator: str = "Bangladesh Bank"
    circular_number: str | None = None
    source_department: str | None = None
    affected_institution_types: list[str] = Field(default_factory=list)
    affected_business_lines: list[str] = Field(default_factory=list)
    impacted_departments: list[str] = Field(default_factory=list)
    deadline: str | None = None
    effective_date: str | None = None
    required_actions: list[str] = Field(default_factory=list)
    required_evidence: list[str] = Field(default_factory=list)
    severity: str = "MEDIUM"
    confidence: float = 0.0
    rationale: str | None = None


class ObligationExtractionResult(BaseModel):
    circular_id: str
    tenant_id: str
    obligations: list[ExtractedObligation]
    model_used: str


class AssistantRequest(BaseModel):
    message: str
    conversation_id: str | None = None
    tenant_id: str


class AssistantResponse(BaseModel):
    content: str
    citations: list[dict] = Field(default_factory=list)
    model_used: str | None = None


class CrawlRequest(BaseModel):
    source_url: str
    source_type: str


class ExtractTextRequest(BaseModel):
    document_version_id: str
    file_path: str


class ExtractObligationsRequest(BaseModel):
    circular_id: str
    tenant_id: str
    text: str
    circular_number: str | None = None
    regulator: str = "Bangladesh Bank"
    department: str | None = None
