package com.compliance.repository;

import com.compliance.model.InstitutionProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InstitutionProfileRepository extends JpaRepository<InstitutionProfile, UUID> {
    Optional<InstitutionProfile> findByTenantId(UUID tenantId);
}
