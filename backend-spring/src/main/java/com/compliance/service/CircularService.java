package com.compliance.service;

import com.compliance.audit.AuditService;
import com.compliance.dto.CircularDtos.*;
import com.compliance.model.Circular;
import com.compliance.model.DocumentVersion;
import com.compliance.model.ExtractedText;
import com.compliance.model.RegulatorySource;
import com.compliance.model.enums.CircularStatus;
import com.compliance.model.enums.ExtractionStatus;
import com.compliance.model.enums.Language;
import com.compliance.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CircularService {

    private static final Logger log = LoggerFactory.getLogger(CircularService.class);

    private final CircularRepository circularRepository;
    private final RegulatorySourceRepository sourceRepository;
    private final DocumentVersionRepository docVersionRepository;
    private final ExtractedTextRepository extractedTextRepository;
    private final TenantRepository tenantRepository;
    private final AuditService auditService;
    private final RestTemplate restTemplate;

    @Value("${app.ai-service.base-url}")
    private String aiServiceUrl;

    public CircularService(CircularRepository circularRepository,
                           RegulatorySourceRepository sourceRepository,
                           DocumentVersionRepository docVersionRepository,
                           ExtractedTextRepository extractedTextRepository,
                           TenantRepository tenantRepository,
                           AuditService auditService, RestTemplate restTemplate) {
        this.circularRepository = circularRepository;
        this.sourceRepository = sourceRepository;
        this.docVersionRepository = docVersionRepository;
        this.extractedTextRepository = extractedTextRepository;
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
        this.restTemplate = restTemplate;
    }

    public Page<CircularResponse> listCirculars(Pageable pageable) {
        return circularRepository.findAll(pageable).map(this::toResponse);
    }

    public CircularDetailResponse getCircularDetail(UUID id) {
        Circular c = circularRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        CircularDetailResponse detail = new CircularDetailResponse();
        detail.setCircular(toResponse(c));

        List<DocumentVersionResponse> versions = docVersionRepository.findByCircularIdOrderByVersionNumberDesc(id)
                .stream().map(this::toVersionResponse).toList();
        detail.setVersions(versions);

        if (!versions.isEmpty()) {
            var latestDoc = docVersionRepository.findFirstByCircularIdOrderByVersionNumberDesc(id);
            latestDoc.flatMap(dv -> extractedTextRepository.findByDocumentVersionId(dv.getId()))
                    .ifPresent(et -> detail.setExtractedText(et.getFullText()));
        }
        return detail;
    }

    public List<DocumentVersionResponse> getVersions(UUID circularId) {
        return docVersionRepository.findByCircularIdOrderByVersionNumberDesc(circularId)
                .stream().map(this::toVersionResponse).toList();
    }

    public String getExtractedText(UUID circularId) {
        var latestDoc = docVersionRepository.findFirstByCircularIdOrderByVersionNumberDesc(circularId);
        if (latestDoc.isEmpty()) return null;
        return extractedTextRepository.findByDocumentVersionId(latestDoc.get().getId())
                .map(ExtractedText::getFullText).orElse(null);
    }

    public List<RegulatorySource> listSources() {
        return sourceRepository.findAll();
    }

    public void triggerCrawl(UUID sourceId) {
        RegulatorySource source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            restTemplate.postForEntity(aiServiceUrl + "/crawl/" + sourceId, Map.of("source_url", source.getBaseUrl(), "source_type", source.getSourceType().name()), String.class);
            log.info("crawl_triggered source={} type={}", source.getSlug(), source.getSourceType());
        } catch (Exception e) {
            log.error("crawl_trigger_failed source={} error={}", source.getSlug(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI service unavailable");
        }
    }

    public void triggerExtraction(UUID circularId) {
        Circular circular = circularRepository.findById(circularId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var docVersion = docVersionRepository.findFirstByCircularIdOrderByVersionNumberDesc(circularId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No document version found"));

        try {
            restTemplate.postForEntity(aiServiceUrl + "/extract-text",
                Map.of("document_version_id", docVersion.getId().toString(),
                       "file_path", docVersion.getFilePath()),
                String.class);

            circular.setStatus(CircularStatus.DOWNLOADED);
            circularRepository.save(circular);
            log.info("extraction_triggered circular={}", circularId);
        } catch (Exception e) {
            log.error("extraction_trigger_failed circular={} error={}", circularId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI service unavailable");
        }
    }

    @Transactional
    public void handleCrawlResults(CrawlResultCallback callback) {
        RegulatorySource source = sourceRepository.findById(callback.getSourceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        source.setLastCrawledAt(Instant.now());
        sourceRepository.save(source);

        for (ParsedCircular parsed : callback.getCirculars()) {
            var existing = circularRepository.findBySourceIdAndTitle(source.getId(), parsed.getTitle());
            if (existing.isEmpty()) {
                Circular c = Circular.builder()
                        .sourceId(source.getId())
                        .circularNumber(parsed.getCircularNumber())
                        .title(parsed.getTitle())
                        .titleBn(parsed.getTitleBn())
                        .department(parsed.getDepartment())
                        .sourceUrl(parsed.getSourceUrl())
                        .language(parsed.getLanguage() != null ? Language.valueOf(parsed.getLanguage().toUpperCase()) : Language.EN)
                        .status(CircularStatus.DETECTED)
                        .rawMetadata(parsed.getMetadata() != null ? parsed.getMetadata() : Map.of())
                        .build();
                circularRepository.save(c);
                log.info("new_circular_detected title={} source={}", c.getTitle(), source.getSlug());

                if (parsed.getPdfUrl() != null) {
                    try {
                        restTemplate.postForEntity(aiServiceUrl + "/download-pdf",
                            Map.of("circular_id", c.getId().toString(),
                                   "pdf_url", parsed.getPdfUrl(),
                                   "language", c.getLanguage().name()),
                            String.class);
                        log.info("pdf_download_triggered circular={} url={}", c.getId(), parsed.getPdfUrl());
                    } catch (Exception e) {
                        log.warn("pdf_download_trigger_failed circular={} error={}", c.getId(), e.getMessage());
                    }
                }
            } else {
                Circular c = existing.get();
                c.setLastSeenAt(Instant.now());
                circularRepository.save(c);
            }
        }
    }

    @Transactional
    public void handlePdfDownloaded(PdfDownloadCallback callback) {
        Circular circular = circularRepository.findById(callback.getCircularId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        int nextVersion = docVersionRepository.findByCircularIdOrderByVersionNumberDesc(callback.getCircularId())
                .stream().mapToInt(DocumentVersion::getVersionNumber).max().orElse(0) + 1;

        DocumentVersion dv = DocumentVersion.builder()
                .circularId(callback.getCircularId())
                .versionNumber(nextVersion)
                .filePath(callback.getFilePath())
                .fileName(callback.getFileName())
                .sha256Hash(callback.getSha256Hash())
                .fileSize(callback.getFileSize())
                .language(Language.valueOf(callback.getLanguage() != null ? callback.getLanguage().toUpperCase() : "EN"))
                .contentType("application/pdf")
                .build();
        docVersionRepository.save(dv);

        circular.setStatus(CircularStatus.DOWNLOADED);
        circularRepository.save(circular);
        log.info("pdf_downloaded circular={} version={}", callback.getCircularId(), nextVersion);

        // Auto-trigger text extraction
        try {
            restTemplate.postForEntity(aiServiceUrl + "/extract-text",
                Map.of("document_version_id", dv.getId().toString(), "file_path", dv.getFilePath()),
                String.class);
            log.info("auto_extraction_triggered circular={}", callback.getCircularId());
        } catch (Exception e) {
            log.warn("auto_extraction_trigger_failed circular={}", callback.getCircularId());
        }
    }

    @Transactional
    public void handleExtractionResult(ExtractionResultCallback callback) {
        var docVersion = docVersionRepository.findById(callback.getDocumentVersionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        ExtractedText et = extractedTextRepository.findByDocumentVersionId(docVersion.getId())
                .orElse(ExtractedText.builder().documentVersionId(docVersion.getId()).build());

        et.setFullText(callback.getFullText());
        et.setExtractionMethod(com.compliance.model.enums.ExtractionMethod.valueOf(callback.getExtractionMethod()));
        et.setPageCount(callback.getPageCount());
        et.setChunks(callback.getChunks() != null ? callback.getChunks() : List.of());
        et.setExtractionStatus(ExtractionStatus.valueOf(callback.getStatus()));
        et.setErrorMessage(callback.getErrorMessage());
        extractedTextRepository.save(et);

        Circular circular = circularRepository.findById(docVersion.getCircularId()).orElse(null);
        if (circular != null && circular.getStatus() == CircularStatus.DOWNLOADED) {
            circular.setStatus(CircularStatus.TEXT_EXTRACTED);
            circularRepository.save(circular);
        }
        log.info("text_extracted circular={} status={}", docVersion.getCircularId(), callback.getStatus());

        if ("COMPLETED".equals(callback.getStatus()) && callback.getFullText() != null && !callback.getFullText().isBlank()) {
            try {
                var tenants = tenantRepository.findAll();
                if (!tenants.isEmpty()) {
                    UUID tenantId = tenants.get(0).getId();
                    Circular circular2 = circularRepository.findById(docVersion.getCircularId()).orElse(null);
                    restTemplate.postForEntity(aiServiceUrl + "/extract-obligations",
                        Map.of("circular_id", docVersion.getCircularId().toString(),
                               "tenant_id", tenantId.toString(),
                               "text", callback.getFullText().substring(0, Math.min(8000, callback.getFullText().length())),
                               "circular_number", circular2 != null ? String.valueOf(circular2.getCircularNumber()) : "",
                               "department", circular2 != null ? String.valueOf(circular2.getDepartment()) : ""),
                        String.class);
                    log.info("obligation_extraction_triggered circular={}", docVersion.getCircularId());
                }
            } catch (Exception e) {
                log.warn("obligation_extraction_trigger_failed circular={} error={}", docVersion.getCircularId(), e.getMessage());
            }
        }
    }

    public CircularResponse toResponse(Circular c) {
        CircularResponse r = new CircularResponse();
        r.setId(c.getId());
        r.setSourceId(c.getSourceId());
        r.setCircularNumber(c.getCircularNumber());
        r.setTitle(c.getTitle());
        r.setTitleBn(c.getTitleBn());
        r.setDepartment(c.getDepartment());
        r.setIssuedDate(c.getIssuedDate());
        r.setEffectiveDate(c.getEffectiveDate());
        r.setSourceUrl(c.getSourceUrl());
        r.setLanguage(c.getLanguage().name());
        r.setStatus(c.getStatus().name());
        r.setRawMetadata(c.getRawMetadata());
        r.setFirstSeenAt(c.getFirstSeenAt());
        r.setLastSeenAt(c.getLastSeenAt());
        r.setCreatedAt(c.getCreatedAt());
        r.setVersionCount(docVersionRepository.findByCircularIdOrderByVersionNumberDesc(c.getId()).size());

        sourceRepository.findById(c.getSourceId()).ifPresent(s -> {
            r.setSourceName(s.getName());
            r.setSourceType(s.getSourceType().name());
        });
        return r;
    }

    private DocumentVersionResponse toVersionResponse(DocumentVersion dv) {
        DocumentVersionResponse r = new DocumentVersionResponse();
        r.setId(dv.getId());
        r.setVersionNumber(dv.getVersionNumber());
        r.setFileName(dv.getFileName());
        r.setContentType(dv.getContentType());
        r.setFileSize(dv.getFileSize());
        r.setSha256Hash(dv.getSha256Hash());
        r.setLanguage(dv.getLanguage().name());
        r.setDownloadedAt(dv.getDownloadedAt());
        return r;
    }
}
