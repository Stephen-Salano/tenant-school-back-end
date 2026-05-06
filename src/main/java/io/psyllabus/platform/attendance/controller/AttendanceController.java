package io.psyllabus.platform.attendance.controller;

import io.psyllabus.platform.attendance.dto.request.AttendanceBulkMarkRequest;
import io.psyllabus.platform.attendance.dto.responses.AttendanceChildrenResponse;
import io.psyllabus.platform.attendance.dto.responses.AttendanceClassStatusResponse;
import io.psyllabus.platform.attendance.dto.responses.AttendanceMarkResultResponse;
import io.psyllabus.platform.attendance.dto.responses.ClassAttendanceResponse;
import io.psyllabus.platform.attendance.dto.responses.StudentAttendanceResponse;
import io.psyllabus.platform.attendance.service.AttendanceService;
import io.psyllabus.platform.common.annotation.RequireFeature;
import io.psyllabus.platform.common.constants.FeatureConstants;
import io.psyllabus.platform.common.controller.BaseController;
import io.psyllabus.platform.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Attendance", description = "Attendance marking and retrieval")
public class AttendanceController extends BaseController {

    private final AttendanceService attendanceService;

    @PostMapping("/mark")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @RequireFeature(FeatureConstants.ATTENDANCE)
    @Operation(summary = "Mark attendance for a class")
    public ResponseEntity<ApiResponse<AttendanceMarkResultResponse>> markAttendance(
            @Valid @RequestBody AttendanceBulkMarkRequest request,
            Authentication authentication
    ) {
        log.info("POST /api/attendance/mark [tenant={}, classId={}, date={}]", getCurrentTenantId(), request.classId(), request.date());
        AttendanceMarkResultResponse response = attendanceService.markAttendance(request, getCurrentUser(authentication));
        return response.idempotent()
                ? ok(response, "Attendance request already processed")
                : created(response, "Attendance marked successfully");
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @RequireFeature(FeatureConstants.ATTENDANCE)
    @Operation(summary = "Get attendance for a class on a date")
    public ResponseEntity<ApiResponse<ClassAttendanceResponse>> getClassAttendance(
            @PathVariable UUID classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication
    ) {
        return ok(attendanceService.getClassAttendance(classId, date, getCurrentUser(authentication)), "Class attendance retrieved");
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'PARENT', 'STUDENT')")
    @RequireFeature(FeatureConstants.ATTENDANCE)
    @Operation(summary = "Get attendance history for a student")
    public ResponseEntity<ApiResponse<StudentAttendanceResponse>> getStudentAttendance(
            @PathVariable UUID studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication
    ) {
        return ok(
                attendanceService.getStudentAttendance(studentId, startDate, endDate, getCurrentUser(authentication)),
                "Student attendance retrieved"
        );
    }

    @GetMapping("/children")
    @PreAuthorize("hasRole('PARENT')")
    @RequireFeature(FeatureConstants.ATTENDANCE)
    @Operation(summary = "Get attendance history for linked children")
    public ResponseEntity<ApiResponse<AttendanceChildrenResponse>> getChildrenAttendance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication
    ) {
        return ok(
                attendanceService.getChildrenAttendance(startDate, endDate, getCurrentUser(authentication)),
                "Children attendance retrieved"
        );
    }

    @GetMapping("/class/{classId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @RequireFeature(FeatureConstants.ATTENDANCE)
    @Operation(summary = "Check whether attendance has already been marked for a class")
    public ResponseEntity<ApiResponse<AttendanceClassStatusResponse>> getClassAttendanceStatus(
            @PathVariable UUID classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication
    ) {
        return ok(
                attendanceService.getClassAttendanceStatus(classId, date, getCurrentUser(authentication)),
                "Attendance status retrieved"
        );
    }
}
