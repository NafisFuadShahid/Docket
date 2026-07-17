package com.compliance.repository;

import com.compliance.model.AssistantConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssistantConversationRepository extends JpaRepository<AssistantConversation, UUID> {
    List<AssistantConversation> findByTenantIdAndUserIdOrderByUpdatedAtDesc(UUID tenantId, UUID userId);
    Optional<AssistantConversation> findByIdAndTenantId(UUID id, UUID tenantId);
}
