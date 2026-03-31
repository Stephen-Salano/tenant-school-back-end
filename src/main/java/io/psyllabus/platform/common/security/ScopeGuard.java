package io.psyllabus.platform.common.security;

import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.auth.enums.AccessScope;
import io.psyllabus.platform.branch.repository.BranchRepository;
import io.psyllabus.platform.common.exception.InsufficientPermissionException;
import io.psyllabus.platform.common.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ScopeGuard — Centralizes access control logic for multi-tenant and multi-branch data isolation.
 * Use in @PreAuthorize expressions or manually in services.
 */
@Component("scopeGuard")
@RequiredArgsConstructor
@Slf4j
public class ScopeGuard {

    private final BranchRepository branchRepository;

    /**
     * Checks if the current user has permission to access or modify a specific branch.
     * Use this in @PreAuthorize("@scopeGuard.canAccessBranch(#branchId)").
     *
     * @param branchId The ID of the branch to check.
     * @return true if access is allowed, throws InsufficientPermissionException otherwise.
     */
    public boolean canAccessBranch(UUID branchId) {
        if (branchId == null) return false;

        User currentUser = getCurrentUser();
        UUID tenantId = TenantContext.getCurrentTenantId();

        // 1. Platform Admins skip all local scope checks
        if (currentUser.getAccessScope() == AccessScope.PLATFORM) {
            return true;
        }

        // 2. Ensure the branch actually belongs to the user's tenant (Fast existence check)
        if (!branchRepository.existsByIdAndTenantId(branchId, tenantId)) {
            log.warn("Access Denied: Branch {} does not belong to Tenant {}", branchId, tenantId);
            throw new InsufficientPermissionException("Branch not found in your school context");
        }

        // 3. If user is SCHOOL scope, they can access ANY branch within their tenant
        if (currentUser.getAccessScope() == AccessScope.SCHOOL || currentUser.getAccessScope() == AccessScope.SYSTEM) {
            return true;
        }

        // 4. If user is BRANCH scope, they can ONLY access their assigned branch
        if (currentUser.getAccessScope() == AccessScope.BRANCH) {
            if (branchId.equals(currentUser.getBranchId())) {
                return true;
            }
            log.warn("Access Denied: User {} (BRANCH scope) attempted to access Branch {}", currentUser.getId(), branchId);
            throw new InsufficientPermissionException("You do not have permission to access this branch");
        }

        return false;
    }

    /**
     * Ensures the current user has SCHOOL-level administrative privileges.
     * Used for operations like creating new branches or updating school profiles.
     */
    public boolean isSchoolAdmin() {
        User user = getCurrentUser();
        return user.getAccessScope() == AccessScope.SCHOOL 
                || user.getAccessScope() == AccessScope.SYSTEM
                || user.getAccessScope() == AccessScope.PLATFORM;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User)) {
            throw new InsufficientPermissionException("User not authenticated");
        }
        return (User) auth.getPrincipal();
    }
}
