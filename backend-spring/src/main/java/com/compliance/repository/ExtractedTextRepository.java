package com.compliance.repository;

import com.compliance.model.ExtractedText;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExtractedTextRepository extends JpaRepository<ExtractedText, UUID> {
    Optional<ExtractedText> findByDocumentVersionId(UUID documentVersionId);
}
