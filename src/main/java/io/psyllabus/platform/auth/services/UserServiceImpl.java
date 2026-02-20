package io.psyllabus.platform.auth.services;

import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.auth.repository.UserRepository;
import io.psyllabus.platform.common.exception.ResourceNotFoundException;
import io.psyllabus.platform.common.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Basic user management service.
 * Profile updates, password changes, and user lookup live here.
 * Deliberately thin — business logic stays in domain services (StaffService, StudentService, etc.)
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User getById(UUID userId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return userRepository.findById(userId)
                .filter(u -> u.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
