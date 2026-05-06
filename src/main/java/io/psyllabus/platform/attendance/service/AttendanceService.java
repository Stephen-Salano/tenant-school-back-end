package io.psyllabus.platform.attendance.service;

import io.psyllabus.platform.attendance.dto.request.AttendanceBulkMarkRequest;
import io.psyllabus.platform.attendance.dto.responses.AttendanceChildrenResponse;
import io.psyllabus.platform.attendance.dto.responses.AttendanceClassStatusResponse;
import io.psyllabus.platform.attendance.dto.responses.AttendanceMarkResultResponse;
import io.psyllabus.platform.attendance.dto.responses.ClassAttendanceResponse;
import io.psyllabus.platform.attendance.dto.responses.StudentAttendanceResponse;
import io.psyllabus.platform.auth.entities.User;

import java.time.LocalDate;
import java.util.UUID;

public interface AttendanceService {

    AttendanceMarkResultResponse markAttendance(AttendanceBulkMarkRequest request, User currentUser);

    ClassAttendanceResponse getClassAttendance(UUID classId, LocalDate date, User currentUser);

    StudentAttendanceResponse getStudentAttendance(UUID studentId, LocalDate startDate, LocalDate endDate, User currentUser);

    AttendanceChildrenResponse getChildrenAttendance(LocalDate startDate, LocalDate endDate, User currentUser);

    AttendanceClassStatusResponse getClassAttendanceStatus(UUID classId, LocalDate date, User currentUser);
}
