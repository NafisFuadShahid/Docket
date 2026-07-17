from pathlib import Path

from app.crawler.parsers import BBCircularParser, BFIUCircularParser

FIXTURES = Path(__file__).parent.parent / "fixtures"


def test_bb_circular_parser():
    html = (FIXTURES / "bb_circular_sample.html").read_text()
    parser = BBCircularParser()
    results = parser.parse(html)

    assert len(results) == 3
    assert results[0].title == "BRPD Circular No. 15, Revised Loan Classification Policy"
    assert results[0].circular_number is not None
    assert "BRPD" in results[0].circular_number
    assert results[0].department == "Banking Regulation & Policy Department"
    assert results[0].pdf_url is not None
    assert results[0].pdf_url.endswith(".pdf")


def test_bb_circular_parser_extracts_department_prefix():
    html = (FIXTURES / "bb_circular_sample.html").read_text()
    parser = BBCircularParser()
    results = parser.parse(html)

    prefixes = [r.circular_number for r in results if r.circular_number]
    assert any("FEPD" in p for p in prefixes)
    assert any("DOS" in p for p in prefixes)


def test_bfiu_circular_parser():
    html = (FIXTURES / "bfiu_circular_sample.html").read_text()
    parser = BFIUCircularParser()
    results = parser.parse(html)

    assert len(results) >= 2
    assert any("AML" in r.title for r in results)
    assert all(r.department == "BFIU" for r in results)


def test_parser_handles_empty_html():
    parser = BBCircularParser()
    results = parser.parse("<html><body></body></html>")
    assert results == []


def test_parser_handles_malformed_html():
    parser = BBCircularParser()
    results = parser.parse("<html><table><tr><td>only one cell</td></tr></table></html>")
    assert results == []
