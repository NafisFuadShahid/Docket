package com.compliance.repository;

import com.compliance.model.Circular;
import com.compliance.model.enums.CircularStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CircularRepository extends JpaRepository<Circular, UUID>, JpaSpecificationExecutor<Circular> {
    Optional<Circular> findBySourceIdAndCircularNumber(UUID sourceId, String circularNumber);
    List<Circular> findBySourceId(UUID sourceId);

    Page<Circular> findByStatus(CircularStatus status, Pageable pageable);

    @Query("SELECT c FROM Circular c WHERE LOWER(c.title) LIKE :search OR LOWER(c.circularNumber) LIKE :search")
    Page<Circular> findBySearch(String search, Pageable pageable);

    @Query("SELECT c FROM Circular c WHERE c.status = :status AND (LOWER(c.title) LIKE :search OR LOWER(c.circularNumber) LIKE :search)")
    Page<Circular> findByStatusAndSearch(CircularStatus status, String search, Pageable pageable);

    @Query("SELECT c FROM Circular c WHERE c.sourceId = :sourceId AND c.title = :title")
    Optional<Circular> findBySourceIdAndTitle(UUID sourceId, String title);

    @Query("SELECT COUNT(c) FROM Circular c WHERE c.status = :status")
    long countByStatus(CircularStatus status);
}
