package io.psyllabus.platform.branch.services;

import io.psyllabus.platform.audit.enums.AuditAction;
import io.psyllabus.platform.audit.service.AuditService;
import io.psyllabus.platform.auth.dto.request.RegisterRequest;
import io.psyllabus.platform.auth.dto.responses.UserResponse;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.auth.enums.AccessScope;
import io.psyllabus.platform.auth.enums.UserType;
import io.psyllabus.platform.auth.repository.UserRepository;
import io.psyllabus.platform.auth.services.AuthService;
import io.psyllabus.platform.branch.dto.request.BranchCreateRequest;
import io.psyllabus.platform.branch.dto.responses.BranchDetailResponse;
import io.psyllabus.platform.branch.dto.responses.BranchResponse;
import io.psyllabus.platform.branch.entities.Branch;
import io.psyllabus.platform.branch.repository.BranchRepository;
import io.psyllabus.platform.common.constants.FeatureConstants;
import io.psyllabus.platform.common.exception.*;
import io.psyllabus.platform.common.security.ScopeGuard;
import io.psyllabus.platform.common.util.PlanUtils;
import io.psyllabus.platform.common.util.RandomUtils;
import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.subscription.entity.Subscription;
import io.psyllabus.platform.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of BranchService for campus-level management.
 * Enforces data isolation via TenantContext and ScopeGuard.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final SubscriptionService subscriptionService;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ScopeGuard scopeGuard;

    @Value("${platform.dev.skip-subscription-check:false}")
    private boolean skipSubscriptionCheck;

    @Override
    public BranchResponse createBranch(BranchCreateRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        log.info("Initiating branch creation for tenant={}: code={}", tenantId, request.code());

        // 1. Subscription & Limit Gate (Bypassed in dev if configured)
        if (!skipSubscriptionCheck) {
            Subscription sub = subscriptionService.getActiveSubscription(tenantId);
            if (sub == null || !sub.getFeatures().contains(FeatureConstants.MULTI_CAMPUS)) {
                throw new SubscriptionException("Upgrade required: MULTI_CAMPUS feature is not available on your plan.");
            }

            long currentCount = branchRepository.countByTenantId(tenantId);
            if (currentCount >= PlanUtils.getBranchLimit(sub.getPlanType())) {
                throw new SubscriptionException(String.format("Limit reached: Your %s plan allows only %d branches.", 
                        sub.getPlanType(), PlanUtils.getBranchLimit(sub.getPlanType())));
            }
        } else {
            log.debug("Bypassing subscription and branch limit checks for tenant={}", tenantId);
        }

        // 2. Pre-emptive Validation
        if (branchRepository.existsByCodeAndTenantId(request.code(), tenantId)) {
            throw new DuplicateResourceException("Branch code '" + request.code() + "' is already in use.");
        }
        if (userRepository.existsByEmailAndTenantId(request.adminEmail(), tenantId)) {
            throw new DuplicateResourceException("An account with email " + request.adminEmail() + " already exists.");
        }

        // 3. Provision Infrastructure
        Branch branch = Branch.builder()
                .tenantId(tenantId)
                .name(request.name())
                .code(request.code().toUpperCase())
                .address(request.address())
                .phone(request.phone())
                .isActive(true)
                .isMainBranch(false)
                .build();
        branch = branchRepository.save(branch);

        // 4. Provision Delegation (Branch Admin)
        String tempPassword = RandomUtils.generateRandomAlphanumeric(12);
        RegisterRequest regRequest = new RegisterRequest(
                request.adminEmail(),
                request.adminEmail(),
                tempPassword,
                request.adminFirstName(),
                request.adminLastName(),
                request.adminPhone(),
                UserType.ADMIN,
                branch.getId(),
                AccessScope.BRANCH
        );

        UserResponse adminResponse = authService.register(regRequest);
        
        // 5. Linkage: Persist Admin Link
        branch.setBranchAdminId(adminResponse.id());
        branchRepository.save(branch);

        // 6. Audit Traceability
        auditService.log(AuditAction.CREATE, "BRANCH", branch.getId(), 
                String.format("Created branch %s and assigned admin %s", branch.getCode(), request.adminEmail()));

        log.info("Branch {} successfully provisioned for tenant {}", branch.getId(), tenantId);
        return mapToResponse(branch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getAllBranches() {
        User user = getCurrentUser();
        UUID tenantId = TenantContext.getCurrentTenantId();

        // Admin scopes see all branches
        if (scopeGuard.isSchoolAdmin()) {
            return branchRepository.findAllByTenantId(tenantId).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        // Branch scope sees only their own
        if (user.getAccessScope() == AccessScope.BRANCH) {
            Branch b = branchRepository.findByIdAndTenantId(user.getBranchId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned branch not found"));
            return List.of(mapToResponse(b));
        }

        throw new InsufficientPermissionException("You do not have permission to view branch listings.");
    }

    @Override
    @Transactional(readOnly = true)
    public BranchDetailResponse getBranchById(UUID branchId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        scopeGuard.canAccessBranch(branchId);

        Branch branch = branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));

        return mapToDetailResponse(branch);
    }

    @Override
    public void setBranchStatus(UUID branchId, boolean active) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        if (!scopeGuard.isSchoolAdmin()) {
            throw new InsufficientPermissionException("Only school administrators can deactivate branches.");
        }

        Branch branch = branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));

        if (branch.isMainBranch() && !active) {
            throw new ValidationException("The Main Campus cannot be deactivated.");
        }

        branch.setActive(active);
        branchRepository.save(branch);
        
        auditService.log(AuditAction.UPDATE, "BRANCH", branchId, "Status updated to active=" + active);
        log.info("Branch {} status updated to {}", branchId, active);
    }

    @Override
    public void deleteBranch(UUID branchId) {
        throw new UnsupportedOperationException("Hard deletion is restricted for audit safety. Use setBranchStatus to deactivate.");
    }

    private BranchResponse mapToResponse(Branch b) {
        String adminName = fetchAdminName(b.getBranchAdminId());
        return new BranchResponse(b.getId(), b.getName(), b.getCode(), b.getAddress(), 
                b.getPhone(), b.getBranchAdminId(), adminName, b.isMainBranch(), 
                b.isActive(), b.getCreatedAt());
    }

    private BranchDetailResponse mapToDetailResponse(Branch b) {
        String adminName = fetchAdminName(b.getBranchAdminId());
        return new BranchDetailResponse(b.getId(), b.getName(), b.getCode(), b.getAddress(),
                b.getPhone(), b.getEmail(), b.getBranchAdminId(), adminName, b.isMainBranch(),
                b.isActive(), b.getCreatedAt(), b.getCreatedBy() != null ? b.getCreatedBy().toString() : "System");
    }

    private String fetchAdminName(UUID adminId) {
        if (adminId == null) return "Unassigned";
        return userRepository.findById(adminId)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Unknown Admin");
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
