package io.psyllabus.platform.student.controller;

import io.psyllabus.platform.common.annotation.RequireFeature;
import io.psyllabus.platform.common.constants.FeatureConstants;
import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.psyllabus.platform.common.dto.PageResponse;
import io.psyllabus.platform.common.exception.InsufficientPermissionException;
import io.psyllabus.platform.common.util.PaginationUtils;
import io.psyllabus.platform.student.dto.request.StudentCreateRequest;
import io.psyllabus.platform.student.dto.request.StudentUpdateRequest;
import io.psyllabus.platform.student.dto.responses.StudentDetailResponse;
import io.psyllabus.platform.student.dto.responses.StudentResponse;
import io.psyllabus.platform.student.dto.responses.StudentSubjectResponse;
import io.psyllabus.platform.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * StudentController — Facade over StudentService.
 *
 * K-DPA enforcement for GET /{id}:
 *   ADMIN → full StudentDetailResponse (including medicalConditions)
 *   STAFF → full StudentDetailResponse
 *   PARENT → blocked (InsufficientPermissionException) until guardian-link verification sprint
 *   STUDENT → not implemented yet
 *
 * List endpoint returns StudentResponse (no medical data) — safe for all authorized roles.
 */
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Students", description = "Student enrollment and management")
public class StudentController extends BaseController {

    private final StudentService studentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @RequireFeature(FeatureConstants.STAFF_MANAGEMENT)
    @Operation(summary = "Enroll a new student",
    description = "Creates user account + student record. Default password = admission number")
    public ResponseEntity<ApiResponse<StudentResponse>> enrollStudent(
            @Valid @RequestBody StudentCreateRequest request
            ){
        log.info("Enrolling student: admissionNumber = {}", request.admissionNumber());
        return created(studentService.enrollStudent(request), "Student enrolled successfully");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @RequireFeature(FeatureConstants.STUDENT_MANAGEMENT)
    @Operation(summary = "List all students (paginated)",
            description = "Returns StudentResponse — no medical data. Safe for all authorized roles. Optionally filter by classId.")
    public ResponseEntity<ApiResponse<PageResponse<StudentResponse>>> listStudents(
            @RequestParam(required = false) UUID classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        Pageable pageable = PaginationUtils.of(page, size, sortBy);
        return paged(studentService.findAll(classId, pageable), "Students retrieved");
    }

    @GetMapping("/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @RequireFeature(FeatureConstants.STUDENT_MANAGEMENT)
    @Operation(summary = "Get student detail",
            description = "Returns full detail including medicalConditions. PARENT role is blocked — guardian-link verification pending.")
    public ResponseEntity<ApiResponse<StudentDetailResponse>> getStudent(
            @PathVariable UUID studentId,
            Authentication authentication
    ) {
        // K-DPA guard: PARENT cannot see another student's medical data
        // Even though @PreAuthorize blocks PARENT here, this explicit check is belt-and-suspenders
        // and documents the business rule clearly. Remove the @PreAuthorize restriction
        // TODO: only when guardian-link verification is implemented.
        var user = getCurrentUser(authentication);
        if (user.getUserType().name().equals("PARENT")) {
            throw new InsufficientPermissionException(
                    "Parent access to student detail is pending guardian-link verification"
            );
        }

        return ok(studentService.findById(studentId), "Student retrieved");
    }

    @GetMapping("/admission/{admissionNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @RequireFeature(FeatureConstants.STUDENT_MANAGEMENT)
    @Operation(summary = "Find student by admission number")
    public ResponseEntity<ApiResponse<StudentDetailResponse>> getByAdmissionNumber(
            @PathVariable String admissionNumber,
            Authentication authentication
    ) {
        var user = getCurrentUser(authentication);
        if (user.getUserType().name().equals("PARENT")) {
            throw new InsufficientPermissionException(
                    "Parent access pending guardian-link verification"
            );
        }
        return ok(studentService.findByAdmissionNumber(admissionNumber), "Student retrieved");
    }

    @PatchMapping("/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @RequireFeature(FeatureConstants.STUDENT_MANAGEMENT)
    @Operation(summary = "Update student profile (partial)",
            description = "Updates ONLY the fields provided in the request body.")
    public ResponseEntity<ApiResponse<StudentResponse>> updateStudent(
            @PathVariable UUID studentId,
            @Valid @RequestBody StudentUpdateRequest request
    ) {
        log.info("Updating student: {}", studentId);
        return ok(studentService.updateStudent(studentId, request), "Student updated successfully");
    }

    @DeleteMapping("/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @RequireFeature(FeatureConstants.STUDENT_MANAGEMENT)
    @Operation(summary = "Soft delete student",
            description = "Sets status to INACTIVE and nullifies currentClassId.")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable UUID studentId) {
        log.info("Deleting student: {}", studentId);
        studentService.deleteStudent(studentId);
        return okMessage("Student deleted successfully");
    }

    @GetMapping("/{studentId}/courses")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @RequireFeature(FeatureConstants.STUDENT_MANAGEMENT)
    @Operation(summary = "Get student subjects/courses",
            description = "Returns subjects the student is enrolled in for the current academic term.")
    public ResponseEntity<ApiResponse<List<StudentSubjectResponse>>> getStudentSubjects(@PathVariable UUID studentId) {
        return ok(studentService.getStudentSubjects(studentId), "Student subjects retrieved");
    }
}
