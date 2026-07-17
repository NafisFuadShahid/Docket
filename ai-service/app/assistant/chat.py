import json

from app.core.config import settings
from app.core.logging import logger
from app.schemas.models import AssistantRequest, AssistantResponse


SYSTEM_PROMPT = """You are a Bangladesh banking compliance assistant. You help compliance officers understand regulatory requirements.

Rules:
1. Only answer based on the provided context (circulars, obligations, tasks, evidence).
2. Every factual claim must include a citation: [Source: circular number, page X] or [Source: obligation title].
3. If you don't have enough information, say so clearly. Never guess or fabricate regulatory guidance.
4. You may help draft internal memos, summarize circulars, or explain obligations.
5. Never provide legal advice. Always recommend consulting legal counsel for legal interpretations.

Context will be provided in the user message."""


class ComplianceAssistant:

    def chat(self, request: AssistantRequest, context: str = "") -> AssistantResponse:
        if not settings.has_ai_key:
            return self._mock_response(request)

        from openai import OpenAI
        client = OpenAI(api_key=settings.AI_API_KEY, base_url=settings.AI_API_BASE)

        try:
            user_message = f"Context:\n{context}\n\nQuestion: {request.message}" if context else request.message
            response = client.chat.completions.create(
                model=settings.AI_MODEL,
                messages=[
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {"role": "user", "content": user_message},
                ],
                temperature=0.2,
            )
            content = response.choices[0].message.content
            citations = self._extract_citations(content)

            return AssistantResponse(
                content=content,
                citations=citations,
                model_used=settings.AI_MODEL,
            )
        except Exception as e:
            logger.error("assistant_error", error=str(e))
            return self._mock_response(request)

    def _mock_response(self, request: AssistantRequest) -> AssistantResponse:
        msg = request.message.lower()
        if "overdue" in msg:
            content = "Based on the current task data, there are overdue tasks that need attention. [Source: Task Dashboard]. Please check the Tasks page for the full list of overdue items with their deadlines and assigned departments."
        elif "circular" in msg or "latest" in msg:
            content = "The most recent circulars can be found in the Regulatory Inbox. [Source: Regulatory Inbox]. Each circular includes extracted obligations, applicability status, and linked tasks."
        elif "memo" in msg or "draft" in msg:
            content = "I can help draft an internal memo. However, in demo mode, I cannot generate full text. When an AI API key is configured, I can draft memos based on specific circulars and obligations. [Source: System Configuration]."
        else:
            content = "I'm the compliance assistant running in demo mode. With a configured AI API key, I can answer questions about circulars, obligations, tasks, and evidence with full citations. [Source: System Configuration]. Please configure AI_API_KEY to enable full functionality."

        return AssistantResponse(
            content=content,
            citations=[{"source": "demo_mode", "note": "Mock response — configure AI_API_KEY for real answers"}],
            model_used="mock-provider",
        )

    def _extract_citations(self, text: str) -> list[dict]:
        import re
        citations = []
        for match in re.finditer(r'\[Source:\s*([^\]]+)\]', text):
            citations.append({"source": match.group(1).strip()})
        return citations


assistant = ComplianceAssistant()
