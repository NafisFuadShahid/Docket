package com.compliance.repository;

import com.compliance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);
    List<User> findByTenantId(UUID tenantId);
    List<User> findByTenantIdAndDepartment(UUID tenantId, String department);
}
