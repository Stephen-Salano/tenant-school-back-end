package io.psyllabus.platform.auth.dto.responses;

import io.psyllabus.platform.academic.dto.response.TeacherAssignmentResponse;
import io.psyllabus.platform.auth.enums.AccessScope;
import io.psyllabus.platform.auth.enums.UserType;
import io.psyllabus.platform.subscription.enums.PlanType;

import java.util.List;
import java.util.UUID;

public record ClaimsResponse(
  // User details
  UUID userId,
  String firstName,
  String lastName,
  UserType userType,
  
  // Tenant details
  UUID tenantId,
  String tenantName,
  UUID branchId,
  String branchName,
  AccessScope accessScope,
  
  // Subscription details
  PlanType subscriptionPlan,
  List<String> features,
  boolean subscriptionActive,
  
  // UI-specific hints
  String dashboardUrl,
  String defaultLandingPage,

  // Role-specific domain context
  UUID staffId,
  UUID subjectGroupId,
  List<TeacherAssignmentResponse> teacherAssignments,
  UUID studentId,
  String admissionNumber,
  UUID currentClassId,
  UUID guardianId,
  List<LinkedStudentClaim> linkedStudents
) {}
