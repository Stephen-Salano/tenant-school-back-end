package io.psyllabus.platform.common.filter;

import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.school.enums.SchoolStatus;
import io.psyllabus.platform.school.repository.SchoolRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SuspensionFilter extends OncePerRequestFilter {

    private final SchoolRepository schoolRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        UUID tenantId = TenantContext.getCurrentTenantId();
        if (tenantId != null) {
            schoolRepository.findById(tenantId).ifPresent(school -> {
                if (school.getStatus() == SchoolStatus.SUSPENDED) {
                    log.warn("Access denied: School {} is suspended", tenantId);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    try {
                        response.getWriter().write("School is suspended. Please contact support.");
                    } catch (IOException e) {
                        log.error("Failed to write suspension response", e);
                    }
                }
            });
        }
        
        if (!response.isCommitted()) {
            filterChain.doFilter(request, response);
        }
    }
}
