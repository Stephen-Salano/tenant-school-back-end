package io.psyllabus.platform.school.repository;

import io.psyllabus.platform.school.entities.School;
import io.psyllabus.platform.school.enums.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
/**
 * Repository for School (tenant) entity.
 * Schools are top-level tenants in the multi-tenant architecture.
 */
@Repository
public interface SchoolRepository extends JpaRepository<School, UUID> {

    /**
     * Find school by subdomain prefix
     * Used for tenant resolution from subdomain (e.g., school-1.yourplatform.com)
     * @param subdomainPrefix the subdomain prefix (e.g., "sc")
     * @return
     */
    Optional<School> findBySubdomainPrefix(String subdomainPrefix);

    /**
     * Check if the subdomain prefix already exists
     * Used for validation during school registration
     * @param subdomainPrefix the subdomain prefix to check
     * @return true if exists, false otherwise
     */
    boolean existsBySubdomainPrefix(String subdomainPrefix);

    /**
     * Check if email already exists
     * Used for validation during school registration
     * @param email the email to check
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Find school by name (case-insensitive).
     * Used for search and admin purposes
     * @param name the school name
     * @return Optional containing the school if found
     */
    Optional<School> findByNameIgnoreCase(String name);

    /**
     * Count schools by subscription plan
     * used for business analytics
     * @param plan the subscription plan
     * @return count of schools
     */
    long countByPlan(SubscriptionPlan plan);

    /**
     * Count active schools
     * Used for business metrics
     * @return count of active schools
     */
    long countByIsActiveTrue();
}
