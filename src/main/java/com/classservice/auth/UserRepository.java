package com.classservice.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByTenantIdAndEmail(UUID tenantId, String email);

    /**
     * Return all users belonging to the given tenant.
     *
     * @param tenantId the tenant scope
     * @return list of users in that tenant
     */
    List<AppUser> findAllByTenantId(UUID tenantId);
}
