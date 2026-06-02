package io.psyllabus.platform.school.repository;

import io.psyllabus.platform.school.entities.School;
import io.psyllabus.platform.subscription.enums.PlanType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
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
     * Find school by custom domain
     * Used for tenant resolution from a vanity URL (e.g., portal.school.com)
     * @param customDomain the custom domain
     * @return
     */
    Optional<School> findByCustomDomain(String customDomain);

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
    long countByPlan(PlanType plan);

    /**
     * Count schools by status
     * Used for business metrics
     * @return count of schools with given status
     */
    /**
     * Count schools grouped by status.
     * Used for consolidated business metrics.
     * @return List of arrays where [0] is status and [1] is count
     */
    @Query("SELECT s.status, COUNT(s) FROM School s GROUP BY s.status")
    List<Object[]> countGroupByStatus();

    Page<School> findAllByOrderByCreatedAtDesc(Pageable pageable);

    default Optional<School> findByIdCrossTenant(UUID schoolId){
        return findById(schoolId);
    }
}
