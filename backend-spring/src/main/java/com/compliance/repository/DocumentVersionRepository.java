package com.compliance.repository;

import com.compliance.model.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {
    List<DocumentVersion> findByCircularIdOrderByVersionNumberDesc(UUID circularId);
    Optional<DocumentVersion> findByCircularIdAndSha256Hash(UUID circularId, String hash);
    Optional<DocumentVersion> findFirstByCircularIdOrderByVersionNumberDesc(UUID circularId);
}
