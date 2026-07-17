package com.compliance.service;

import com.compliance.model.Alert;
import com.compliance.model.enums.AlertSeverity;
import com.compliance.model.enums.AlertType;
import com.compliance.repository.AlertRepository;
import com.compliance.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);
    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public Page<Alert> listAlerts(Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        return alertRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    public Page<Alert> listUnread(Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        return alertRepository.findByTenantIdAndIsReadOrderByCreatedAtDesc(tenantId, false, pageable);
    }

    public long unreadCount() {
        return alertRepository.countByTenantIdAndIsReadFalse(TenantContext.getTenantId());
    }

    @Transactional
    public void markRead(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        Alert a = alertRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        a.setIsRead(true);
        a.setReadAt(Instant.now());
        alertRepository.save(a);
    }

    @Transactional
    public void markAllRead() {
        alertRepository.markAllRead(TenantContext.getTenantId());
    }

    public void createAlert(UUID tenantId, UUID userId, AlertType type, String title,
                            String message, AlertSeverity severity, String entityType, UUID entityId) {
        Alert a = Alert.builder()
                .tenantId(tenantId)
                .userId(userId)
                .alertType(type)
                .title(title)
                .message(message)
                .severity(severity)
                .entityType(entityType)
                .entityId(entityId)
                .build();
        alertRepository.save(a);
        log.info("alert_created type={} severity={} entity={}:{}", type, severity, entityType, entityId);
    }
}
