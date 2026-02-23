package io.psyllabus.platform.common.controller;

import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.common.util.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.UUID;

/**
 * Template Method pattern — the invariant skeleton every controller needs.
 *
 * Without this base, these 3-4 lines copy-paste into every single controller method:
 *
 *   request arrives
 *        ↓
 *   [1] who sent this?  → getCurrentUser(auth)       ← invariant (here)
 *        ↓
 *   [2] which school?   → getCurrentTenantId()        ← invariant (here)
 *        ↓
 *   [3] call service    →                             ← varies (concrete controller)
 *        ↓
 *   [4] wrap response   → ok() / created()            ← invariant (here)
 *
 * Concrete controllers extend this and only implement step 3.
 *
 * NOT annotated with @RestController — Spring scans for that on concrete subclasses.
 */
public abstract class BaseController {

    /**
     * Extract the authenticated user from SecurityContext
     * JwtAuthFilter always sets a User Principal - cast is safe
     */
    protected User getCurrentUser(Authentication authentication){
        return (User) authentication.getPrincipal();
    }

    /**
     * Current tenantId from the ThreadLocal set by TenantFilter
     * Use when we need a TenantId without the full user object
     * @return the TenantId
     */
    protected UUID getCurrentTenantId(){
        return TenantContext.getCurrentTenantId();
    }

    // 200 Ok with data
    protected <T> ResponseEntity<ApiResponse<T>> ok(T data, String message){
        return ResponseEntity.ok(ApiResponse.success(data, message));
    }

    // 201 created - for POST endpoints that create a new resource
    protected  <T> ResponseEntity<ApiResponse<T>> created(T data, String message){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, message));
    }

    // 200 OK with no data — for delete/action endpoints. Returns a toast message.
    protected ResponseEntity<ApiResponse<Void>> okMessage(String message){
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    /**
     * 200 OK with paginated data.
     * Use for all list endpoints: GET /api/staff, GET /api/students, etc.
     *
     * Usage:
     *   Page<Staff> page = staffRepository.findByTenantId(tenantId, pageable);
     *   return paged(PageResponse.from(page.map(this::mapToResponse)), "Staff retrieved");
     */
    protected <T> ResponseEntity<ApiResponse<PageResponse<T>>> paged(PageResponse<T> data, String message) {
        return ResponseEntity.ok(ApiResponse.success(data, message));
    }


}
