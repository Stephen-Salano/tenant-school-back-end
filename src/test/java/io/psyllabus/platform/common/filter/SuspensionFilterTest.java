package io.psyllabus.platform.common.filter;

import io.psyllabus.platform.common.util.TenantContext;
import io.psyllabus.platform.school.entities.School;
import io.psyllabus.platform.school.enums.SchoolStatus;
import io.psyllabus.platform.school.repository.SchoolRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
class SuspensionFilterTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void doFilterInternal_allowsActiveSchool() throws Exception {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
        AtomicInteger lookups = new AtomicInteger();
        SchoolRepository schoolRepository = repositoryReturning(Optional.of(schoolWithStatus(tenantId, SchoolStatus.ACTIVE)), lookups);

        SuspensionFilter filter = new SuspensionFilter(schoolRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        assertEquals(200, response.getStatus());
        assertEquals(1, lookups.get());
    }

    @Test
    void doFilterInternal_blocksSuspendedSchool() throws Exception {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
        SchoolRepository schoolRepository = repositoryReturning(Optional.of(schoolWithStatus(tenantId, SchoolStatus.SUSPENDED)), new AtomicInteger());

        SuspensionFilter filter = new SuspensionFilter(schoolRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertEquals("School access is currently unavailable. Please contact support.", response.getContentAsString());
        assertNull(chain.getRequest());
    }

    @Test
    void doFilterInternal_blocksDeactivatedSchool() throws Exception {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
        SchoolRepository schoolRepository = repositoryReturning(Optional.of(schoolWithStatus(tenantId, SchoolStatus.DEACTIVATED)), new AtomicInteger());

        SuspensionFilter filter = new SuspensionFilter(schoolRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertEquals("School access is currently unavailable. Please contact support.", response.getContentAsString());
        assertNull(chain.getRequest());
    }

    @Test
    void doFilterInternal_skipsSchoolLookupWhenTenantMissing() throws Exception {
        AtomicInteger lookups = new AtomicInteger();
        SchoolRepository schoolRepository = repositoryReturning(Optional.empty(), lookups);
        SuspensionFilter filter = new SuspensionFilter(schoolRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        assertEquals(0, lookups.get());
    }

    private School schoolWithStatus(UUID tenantId, SchoolStatus status) {
        return School.builder()
                .id(tenantId)
                .name("Tenant")
                .status(status)
                .build();
    }

    private SchoolRepository repositoryReturning(Optional<School> school, AtomicInteger lookups) {
        return (SchoolRepository) Proxy.newProxyInstance(
                SchoolRepository.class.getClassLoader(),
                new Class[]{SchoolRepository.class},
                (proxy, method, args) -> {
                    if ("findByIdCrossTenant".equals(method.getName())) {
                        lookups.incrementAndGet();
                        return school;
                    }
                    if ("toString".equals(method.getName())) {
                        return "SchoolRepositoryProxy";
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    throw new UnsupportedOperationException("Unexpected method: " + method.getName());
                }
        );
    }
}
