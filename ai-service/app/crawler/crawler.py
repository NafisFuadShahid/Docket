import asyncio
import hashlib
import time
from pathlib import Path
from urllib.parse import urlparse

import httpx

from app.core.config import settings
from app.core.logging import logger
from app.crawler.parsers import PARSERS
from app.schemas.models import CrawlResult, ParsedCircular


class RegulatoryCrawler:

    def __init__(self):
        self.rate_limit = settings.CRAWLER_RATE_LIMIT
        self.timeout = settings.CRAWLER_TIMEOUT
        self.allowed_domains = settings.allowed_domains
        self._last_request_time = 0.0

    def _is_allowed(self, url: str) -> bool:
        parsed = urlparse(url)
        host = parsed.hostname
        if not host:
            return False
        return any(host == d or host.endswith("." + d) for d in self.allowed_domains)

    async def _rate_limited_get(self, client: httpx.AsyncClient, url: str) -> httpx.Response:
        if not self._is_allowed(url):
            raise ValueError(f"Domain not allowlisted: {url}")

        elapsed = time.monotonic() - self._last_request_time
        wait = max(0, (1.0 / self.rate_limit) - elapsed)
        if wait > 0:
            await asyncio.sleep(wait)

        self._last_request_time = time.monotonic()
        return await client.get(url, timeout=self.timeout, follow_redirects=True)

    async def crawl_source(self, source_id: str, source_url: str, source_type: str) -> CrawlResult:
        parser = PARSERS.get(source_type)
        if not parser:
            logger.error("no_parser_found", source_type=source_type)
            return CrawlResult(source_id=source_id, circulars=[])

        async with httpx.AsyncClient(
            headers={"User-Agent": "BDComplianceCrawler/1.0 (compliance monitoring)"},
            follow_redirects=True,
        ) as client:
            try:
                response = await self._rate_limited_get(client, source_url)
                response.raise_for_status()
            except Exception as e:
                logger.error("crawl_failed", source_url=source_url, error=str(e))
                return CrawlResult(source_id=source_id, circulars=[])

            circulars = parser.parse(response.text)
            logger.info("crawl_completed", source_url=source_url, count=len(circulars))
            return CrawlResult(source_id=source_id, circulars=circulars)

    async def download_pdf(self, url: str, dest_dir: str) -> tuple[str, str, int]:
        """Download PDF and return (file_path, sha256_hash, file_size)."""
        if not self._is_allowed(url):
            raise ValueError(f"Domain not allowlisted: {url}")

        async with httpx.AsyncClient(follow_redirects=True) as client:
            response = await self._rate_limited_get(client, url)
            response.raise_for_status()

            content = response.content
            sha256 = hashlib.sha256(content).hexdigest()
            filename = urlparse(url).path.split("/")[-1] or "document.pdf"

            Path(dest_dir).mkdir(parents=True, exist_ok=True)
            file_path = f"{dest_dir}/{sha256}_{filename}"
            Path(file_path).write_bytes(content)

            logger.info("pdf_downloaded", url=url, hash=sha256[:16], size=len(content))
            return file_path, sha256, len(content)


crawler = RegulatoryCrawler()
