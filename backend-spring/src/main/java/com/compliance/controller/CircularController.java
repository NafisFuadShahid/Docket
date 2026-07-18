package com.compliance.controller;

import com.compliance.dto.CircularDtos.*;
import com.compliance.model.RegulatorySource;
import com.compliance.service.CircularService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CircularController {

    private final CircularService circularService;

    public CircularController(CircularService circularService) {
        this.circularService = circularService;
    }

    @GetMapping("/sources")
    public ResponseEntity<List<RegulatorySource>> listSources() {
        return ResponseEntity.ok(circularService.listSources());
    }

    @PostMapping("/sources/{id}/crawl")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_ADMIN')")
    public ResponseEntity<Void> triggerCrawl(@PathVariable UUID id) {
        circularService.triggerCrawl(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/sources/crawl-all")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_ADMIN')")
    public ResponseEntity<Void> triggerCrawlAll() {
        circularService.triggerCrawlAll();
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/circulars")
    public ResponseEntity<Page<CircularResponse>> listCirculars(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(circularService.listCirculars(pageable, status, search));
    }

    @GetMapping("/circulars/{id}")
    public ResponseEntity<CircularDetailResponse> getCircular(@PathVariable UUID id) {
        return ResponseEntity.ok(circularService.getCircularDetail(id));
    }

    @GetMapping("/circulars/{id}/versions")
    public ResponseEntity<List<DocumentVersionResponse>> getVersions(@PathVariable UUID id) {
        return ResponseEntity.ok(circularService.getVersions(id));
    }

    @GetMapping("/circulars/{id}/text")
    public ResponseEntity<String> getText(@PathVariable UUID id) {
        String text = circularService.getExtractedText(id);
        return text != null ? ResponseEntity.ok(text) : ResponseEntity.noContent().build();
    }

    @PostMapping("/circulars/{id}/extract")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_ADMIN')")
    public ResponseEntity<Void> triggerExtraction(@PathVariable UUID id) {
        circularService.triggerExtraction(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/internal/pdf-downloaded")
    public ResponseEntity<Void> handlePdfDownloaded(@RequestBody PdfDownloadCallback callback) {
        circularService.handlePdfDownloaded(callback);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/internal/crawl-results")
    public ResponseEntity<Void> handleCrawlResults(@RequestBody CrawlResultCallback callback) {
        circularService.handleCrawlResults(callback);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/internal/extraction-results")
    public ResponseEntity<Void> handleExtractionResults(@RequestBody ExtractionResultCallback callback) {
        circularService.handleExtractionResult(callback);
        return ResponseEntity.ok().build();
    }
}
