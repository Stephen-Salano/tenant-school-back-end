package io.psyllabus.platform.guardian.controller;


import io.psyllabus.platform.common.annotation.RequireFeature;
import io.psyllabus.platform.common.constants.FeatureConstants;
import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.guardian.dto.request.GuardianCreateRequest;
import io.psyllabus.platform.guardian.dto.responses.GuardianResponse;
import io.psyllabus.platform.guardian.services.GuardianService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * GuardianController — Facade over GuardianService.
 *
 * Workflow:
 *   1. POST /api/guardians               → create guardian (User + Guardian)
 *   2. POST /api/guardians/{id}/link/{studentId} → link to a student (first = primary)
 *   3. GET  /api/guardians/student/{studentId}   → list all guardians for a student
 *
 * RBAC:
 *   Create / Link → ADMIN only
 *   View by student → ADMIN or STAFF
 */
@RestController
@RequestMapping("/api/guardians")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Guardians", description = "Parent/guardian management and student linking")
public class GuardianController extends BaseController {

    private final GuardianService guardianService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @RequireFeature(FeatureConstants.STUDENT_MANAGEMENT)
    @Operation(summary = "Create a new guardian",
            description = "Creates User (PARENT) + Guardian record. Default password = phone number.")
    public ResponseEntity<ApiResponse<GuardianResponse>> createGuardian(
            @Valid @RequestBody GuardianCreateRequest request
    ) {
        log.info("Creating guardian: email={}", request.email());
        return created(guardianService.createGuardian(request), "Guardian created successfully");
    }

    @PostMapping("/{guardianId}/link/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @RequireFeature(FeatureConstants.STUDENT_MANAGEMENT)
    @Operation(summary = "Link guardian to a student",
            description = "Creates a StudentGuardian relationship. First guardian linked automatically becomes primary and emergency contact.")
    public ResponseEntity<ApiResponse<Void>> linkToStudent(
            @PathVariable UUID guardianId,
            @PathVariable UUID studentId
    ) {
        log.info("Linking guardian {} to student {}", guardianId, studentId);
        guardianService.linkToStudent(guardianId, studentId);
        return okMessage("Guardian linked to student");
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @RequireFeature(FeatureConstants.STUDENT_MANAGEMENT)
    @Operation(summary = "Get all guardians for a student")
    public ResponseEntity<ApiResponse<List<GuardianResponse>>> getGuardiansByStudent(
            @PathVariable UUID studentId
    ) {
        return ok(guardianService.findByStudentId(studentId), "Guardians retrieved");
    }
}
