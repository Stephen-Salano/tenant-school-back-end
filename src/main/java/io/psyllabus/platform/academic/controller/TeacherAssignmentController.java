package io.psyllabus.platform.academic.controller;

import io.psyllabus.platform.academic.dto.request.TeacherAssignmentCreateRequest;
import io.psyllabus.platform.academic.dto.response.TeacherAssignmentResponse;
import io.psyllabus.platform.academic.service.TeacherAssignmentService;
import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
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
 * REST facade for teacher assignments.
 */
@RestController
@RequestMapping("/api/teacher-assignments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Academic - Teacher Assignments", description = "Manage who teaches what, where and when")
public class TeacherAssignmentController extends BaseController {

    private final TeacherAssignmentService teacherAssignmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign a teacher to a class, subject and term")
    public ResponseEntity<ApiResponse<TeacherAssignmentResponse>> assignTeacher(
            @Valid @RequestBody TeacherAssignmentCreateRequest request) {
        log.info("POST /api/teacher-assignments — staffId={} classId={} subject={} tenant={}", 
                request.staffId(), request.classId(), request.subjectId(), getCurrentTenantId());
        return created(teacherAssignmentService.assignTeacher(request), "Teacher assigned successfully");
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get all teacher assignments for a specific class")
    public ResponseEntity<ApiResponse<List<TeacherAssignmentResponse>>> getAssignmentsByClass(
            @PathVariable UUID classId) {
        return ok(teacherAssignmentService.getAssignmentsByClass(classId), "Class assignments retrieved");
    }

    @GetMapping("/staff/{staffId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get all teacher assignments for a specific staff member")
    public ResponseEntity<ApiResponse<List<TeacherAssignmentResponse>>> getAssignmentsByStaff(
            @PathVariable UUID staffId) {
        return ok(teacherAssignmentService.getAssignmentsByStaff(staffId), "Staff assignments retrieved");
    }
}
