package com.compliance.audit;

import com.compliance.model.AuditLog;
import com.compliance.repository.AuditLogRepository;
import com.compliance.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String action, String entityType, UUID entityId, Map<String, Object> oldValues, Map<String, Object> newValues) {
        AuditLog entry = AuditLog.builder()
                .tenantId(TenantContext.getTenantId())
                .userId(TenantContext.getUserId())
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValues(oldValues)
                .newValues(newValues)
                .build();
        auditLogRepository.save(entry);
        log.info("audit: action={} entity={}:{} user={}", action, entityType, entityId, TenantContext.getUserId());
    }

    public void log(String action, String entityType, UUID entityId) {
        log(action, entityType, entityId, null, null);
    }
}
