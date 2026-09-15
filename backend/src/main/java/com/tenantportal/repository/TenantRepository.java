package com.tenantportal.repository;

import com.tenantportal.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByEmail(String email);

    boolean existsByEmail(String email);
}
