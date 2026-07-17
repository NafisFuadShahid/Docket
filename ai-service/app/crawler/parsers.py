from typing import Protocol
from bs4 import BeautifulSoup

from app.schemas.models import ParsedCircular


class SourceParser(Protocol):
    def parse(self, html: str) -> list[ParsedCircular]: ...


class BBCircularParser:
    """Parse Bangladesh Bank circular listing page."""

    def parse(self, html: str) -> list[ParsedCircular]:
        soup = BeautifulSoup(html, "lxml")
        results = []

        for row in soup.select("table tr"):
            cells = row.find_all("td")
            if len(cells) < 3:
                continue
            try:
                links = row.find_all("a")
                pdf_url = None
                source_url = None
                for link in links:
                    href = link.get("href", "")
                    if href.endswith(".pdf"):
                        pdf_url = href if href.startswith("http") else f"https://www.bb.org.bd{href}"
                    elif href:
                        source_url = href if href.startswith("http") else f"https://www.bb.org.bd{href}"

                title = cells[1].get_text(strip=True) if len(cells) > 1 else ""
                date_text = cells[0].get_text(strip=True) if cells else ""
                dept = cells[2].get_text(strip=True) if len(cells) > 2 else None

                if not title:
                    continue

                circular_number = None
                for prefix in ["BRPD", "FEPD", "DSTI", "GBCSRD", "SFD", "DOS", "SMESPD", "ACD", "PSD"]:
                    if prefix in title:
                        parts = title.split(",")
                        for part in parts:
                            if prefix in part:
                                circular_number = part.strip()
                                break
                        break

                results.append(ParsedCircular(
                    circular_number=circular_number,
                    title=title,
                    department=dept,
                    issued_date=date_text,
                    source_url=source_url,
                    pdf_url=pdf_url,
                    language="EN",
                ))
            except Exception:
                continue
        return results


class BFIUCircularParser:
    """Parse BFIU circular listing page."""

    def parse(self, html: str) -> list[ParsedCircular]:
        soup = BeautifulSoup(html, "lxml")
        results = []

        for row in soup.select("table tr, .circular-item, .list-group-item"):
            try:
                links = row.find_all("a")
                title_el = row.find(["td", "h5", "h4", "span"])
                if not title_el:
                    continue
                title = title_el.get_text(strip=True)
                if not title or len(title) < 5:
                    continue

                pdf_url = None
                source_url = None
                for link in links:
                    href = link.get("href", "")
                    if href.endswith(".pdf"):
                        pdf_url = href if href.startswith("http") else f"https://www.bfiu.org.bd{href}"
                    elif href:
                        source_url = href if href.startswith("http") else f"https://www.bfiu.org.bd{href}"

                results.append(ParsedCircular(
                    title=title,
                    department="BFIU",
                    source_url=source_url,
                    pdf_url=pdf_url,
                    language="BOTH",
                    metadata={"regulator": "BFIU"},
                ))
            except Exception:
                continue
        return results


class BBGuidelineParser:
    """Parse Bangladesh Bank guideline listing page."""

    def parse(self, html: str) -> list[ParsedCircular]:
        soup = BeautifulSoup(html, "lxml")
        results = []

        for row in soup.select("table tr"):
            cells = row.find_all("td")
            if len(cells) < 2:
                continue
            try:
                title = cells[0].get_text(strip=True) if cells else ""
                if not title or len(title) < 5:
                    continue
                links = row.find_all("a")
                pdf_url = None
                for link in links:
                    href = link.get("href", "")
                    if href.endswith(".pdf"):
                        pdf_url = href if href.startswith("http") else f"https://www.bb.org.bd{href}"

                results.append(ParsedCircular(
                    title=title,
                    source_url=pdf_url,
                    pdf_url=pdf_url,
                    language="EN",
                    metadata={"type": "guideline"},
                ))
            except Exception:
                continue
        return results


class BBNoticeParser:
    """Parse Bangladesh Bank noticeboard page."""

    def parse(self, html: str) -> list[ParsedCircular]:
        soup = BeautifulSoup(html, "lxml")
        results = []

        for row in soup.select("table tr, .notice-item"):
            try:
                cells = row.find_all("td")
                if len(cells) < 2:
                    continue
                title = cells[1].get_text(strip=True) if len(cells) > 1 else cells[0].get_text(strip=True)
                date_text = cells[0].get_text(strip=True) if cells else ""
                if not title or len(title) < 5:
                    continue

                links = row.find_all("a")
                pdf_url = None
                for link in links:
                    href = link.get("href", "")
                    if href.endswith(".pdf"):
                        pdf_url = href if href.startswith("http") else f"https://www.bb.org.bd{href}"

                results.append(ParsedCircular(
                    title=title,
                    issued_date=date_text,
                    source_url=pdf_url,
                    pdf_url=pdf_url,
                    language="EN",
                    metadata={"type": "notice"},
                ))
            except Exception:
                continue
        return results


PARSERS: dict[str, SourceParser] = {
    "BB_CIRCULAR": BBCircularParser(),
    "BFIU_CIRCULAR": BFIUCircularParser(),
    "BB_GUIDELINE": BBGuidelineParser(),
    "BB_NOTICE": BBNoticeParser(),
}
