package io.psyllabus.platform.auth.repository;

import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.auth.enums.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {


    /**
     * Find user by username within a specific tenant
     * CRITICAL: We will always include tenantId to maintain multi-tenant isolation
     * @param username of the user
     * @param tenantId of the user
     * @return Optional user object
     */
    Optional<User> findByUsernameAndTenantId(String username, UUID tenantId);

    /**
     * Find user by email within a specific tenant
     * Used for login when user provides email instead of username
     * @param email of the user
     * @param tenantId that the user belongs to
     * @return optional user object
     */
    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);

    /**
     * Check if username exists within a tenant
     * Used for registration and validation
     * @param username of the user
     * @param tenantId that the user belongs to
     * @return optional user object
     */
    boolean existsByUsernameAndTenantId(String username, UUID tenantId);

    /**
     * Check if email exists within a tenant
     * @param email of the user
     * @param tenantId the user belongs to
     * @return optional user
     */
    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    /**
     * Find user by username, email or admission number (flexible login)
     * Supports multiple login methods in a single query
     * @param identifier username, email or admission number
     * @param tenantId the tenant (school) schoolId
     * @return Optional containing the user if found
     */
    @Query("""
        SELECT u from User u
        WHERE u.tenantId = :tenantId
        AND (u.username = :identifier OR u.email = :identifier)
    """)
    Optional<User> findByIdentifierAndTenantId(
            @Param("identifier") String identifier,
            @Param("tenantId") UUID tenantId
    );

    /**
     * Count users by type within a tenant
     * Used for schools statistics
     * @param tenantId the tenant (school) schoolId
     * @param userType the user type to count
     * @return count of users
     */
    long countByTenantIdAndUserType(UUID tenantId, UserType userType);

}
