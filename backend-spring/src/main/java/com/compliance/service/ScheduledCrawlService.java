package com.compliance.service;

import com.compliance.model.RegulatorySource;
import com.compliance.repository.RegulatorySourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class ScheduledCrawlService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledCrawlService.class);

    private final RegulatorySourceRepository sourceRepository;
    private final RestTemplate restTemplate;

    @Value("${app.ai-service.base-url}")
    private String aiServiceUrl;

    public ScheduledCrawlService(RegulatorySourceRepository sourceRepository, RestTemplate restTemplate) {
        this.sourceRepository = sourceRepository;
        this.restTemplate = restTemplate;
    }

    @Scheduled(fixedDelayString = "${app.crawl.check-interval:300000}")
    public void checkAndCrawl() {
        for (RegulatorySource source : sourceRepository.findByIsActiveTrue()) {
            if (shouldCrawl(source)) {
                try {
                    restTemplate.postForEntity(
                        aiServiceUrl + "/crawl/" + source.getId(),
                        Map.of("source_url", source.getBaseUrl(), "source_type", source.getSourceType().name()),
                        String.class);
                    log.info("scheduled_crawl_triggered source={}", source.getSlug());
                } catch (Exception e) {
                    log.warn("scheduled_crawl_failed source={} error={}", source.getSlug(), e.getMessage());
                }
            }
        }
    }

    private boolean shouldCrawl(RegulatorySource source) {
        if (source.getLastCrawledAt() == null) return true;
        long minutesSinceLastCrawl = ChronoUnit.MINUTES.between(source.getLastCrawledAt(), Instant.now());
        return minutesSinceLastCrawl >= source.getCrawlIntervalMinutes();
    }
}
