package io.psyllabus.platform.academic.controller;


import io.psyllabus.platform.academic.service.EnrollmentService;
import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.common.util.PaginationUtils;
import io.psyllabus.platform.student.dto.responses.StudentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/classes/{classId}/enrollments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Academic - Enrollments", description = "Manage student class memberships")
public class EnrollmentController extends BaseController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enroll a student in a class")
    public ResponseEntity<ApiResponse<Void>> enrollStudent(
            @PathVariable UUID classId,
            @PathVariable UUID studentId) {
        log.info("Enrolling student {} in class {} [tenant={}]", studentId, classId, getCurrentTenantId());
        enrollmentService.enrollStudent(classId, studentId);
        return okMessage("Student enrolled successfully");
    }

    @DeleteMapping("/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unenroll a student from a class")
    public ResponseEntity<ApiResponse<Void>> unenrollStudent(
            @PathVariable UUID classId,
            @PathVariable UUID studentId) {
        log.info("Unenrolling student {} from class {} [tenant={}]", studentId, classId, getCurrentTenantId());
        enrollmentService.unenrollStudent(classId, studentId);
        return okMessage("Student unenrolled successfully");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "List all students enrolled in a class")
    public ResponseEntity<ApiResponse<PageResponse<StudentResponse>>> listEnrolledStudents(
            @PathVariable UUID classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PaginationUtils.of(page, size, "createdAt");
        return paged(enrollmentService.listEnrolledStudents(classId, pageable), "Enrolled students retrieved");
    }
}
