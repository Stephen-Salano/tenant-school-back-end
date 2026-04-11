package io.psyllabus.platform.onboarding.service.impl;

import io.psyllabus.platform.academic.repository.SchoolClassRepository;
import io.psyllabus.platform.academic.repository.SubjectGroupRepository;
import io.psyllabus.platform.auth.repository.UserRepository;
import io.psyllabus.platform.branch.repository.BranchRepository;
import io.psyllabus.platform.onboarding.dto.response.RowError;
import io.psyllabus.platform.onboarding.dto.response.ValidationReport;
import io.psyllabus.platform.onboarding.enums.ImportJobType;
import io.psyllabus.platform.onboarding.service.CsvValidationService;
import io.psyllabus.platform.staff.repository.StaffRepository;
import io.psyllabus.platform.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvValidationServiceImpl implements CsvValidationService {

    private final BranchRepository branchRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectGroupRepository subjectGroupRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final UserRepository userRepository;

    @Value("${onboarding.max-rows:500}")
    private int maxRows;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Accepts: 0712345678 / 0112345678 / +254712345678 / 254712345678 */
    private static final Pattern KENYAN_PHONE = Pattern.compile("^(\\+?254|0)(7|1)\\d{8}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Set<String> VALID_GENDERS = Set.of("MALE", "FEMALE", "OTHER");
    private static final Set<String> VALID_RELATIONSHIPS = Set.of("FATHER", "MOTHER", "GUARDIAN", "AUNT", "UNCLE", "GRANDPARENT", "SIBLING", "OTHER");

    // Required columns per job type
    private static final List<String> STUDENT_REQUIRED = List.of(
            "first_name", "last_name", "date_of_birth", "gender", "grade_level",
            "guardian1_first_name", "guardian1_last_name", "guardian1_phone", "guardian1_relationship"
    );

    private static final List<String> STAFF_REQUIRED = List.of(
            "first_name", "last_name", "email", "gender", "hire_date"
    );

    @Override
    public ValidationReport phaseOne(List<Map<String, String>> rows, ImportJobType jobType) {
        List<RowError> errors = new ArrayList<>();

        if (rows.size() > maxRows) {
            errors.add(new RowError(0, null, "File exceeds maximum of " + maxRows + " rows. Found: " + rows.size(), null));
            return ValidationReport.failed(rows.size(), errors);
        }

        if (rows.isEmpty()) {
            errors.add(new RowError(0, null, "CSV file has no data rows.", null));
            return ValidationReport.failed(0, errors);
        }

        Set<String> headers = rows.get(0).keySet();
        List<String> required = jobType == ImportJobType.STUDENT_BULK_IMPORT ? STUDENT_REQUIRED : STAFF_REQUIRED;

        for (String col : required) {
            if (!headers.contains(col)) {
                errors.add(new RowError(0, col, "Required column missing from CSV header.", col));
            }
        }

        if (!errors.isEmpty()) return ValidationReport.failed(rows.size(), errors);

        for (int i = 0; i < rows.size(); i++) {
            int rowNum = i + 1;
            Map<String, String> row = rows.get(i);
            if (jobType == ImportJobType.STUDENT_BULK_IMPORT) {
                validateStudentRow(row, rowNum, errors);
            } else {
                validateStaffRow(row, rowNum, errors);
            }
        }

        return errors.isEmpty() ? ValidationReport.ok(rows.size()) : ValidationReport.failed(rows.size(), errors);
    }

    @Override
    @Transactional(readOnly = true)
    public ValidationReport phaseTwo(List<Map<String, String>> rows, ImportJobType jobType, UUID tenantId) {
        List<RowError> errors = new ArrayList<>();

        Set<String> validBranchCodes = branchRepository.findAllCodesByTenantId(tenantId);
        Set<String> validClassKeys = schoolClassRepository.findAllActiveClassKeysByTenantId(tenantId);
        Set<String> validSubjectGroups = subjectGroupRepository.findAllNamesByTenantId(tenantId);
        Set<String> existingAdmissions = studentRepository.findAllAdmissionNumbersByTenantId(tenantId);
        Set<String> existingEmployeeNums = staffRepository.findAllEmployeeNumbersByTenantId(tenantId);
        Set<String> existingEmails = userRepository.findAllEmailsByTenantId(tenantId);

        Set<String> seenInFile = new HashSet<>();

        for (int i = 0; i < rows.size(); i++) {
            int rowNum = i + 1;
            Map<String, String> row = rows.get(i);

            String branchCode = trim(row.get("branch_code"));
            if (!branchCode.isEmpty() && !validBranchCodes.contains(branchCode.toUpperCase())) {
                errors.add(new RowError(rowNum, "branch_code", "Branch code not found in this school.", branchCode));
            }

            if (jobType == ImportJobType.STUDENT_BULK_IMPORT) {
                validateStudentReferences(row, rowNum, validClassKeys, existingAdmissions, existingEmails, seenInFile, errors);
            } else {
                validateStaffReferences(row, rowNum, validClassKeys, validSubjectGroups, existingEmployeeNums, existingEmails, seenInFile, errors);
            }
        }

        return errors.isEmpty() ? ValidationReport.ok(rows.size()) : ValidationReport.failed(rows.size(), errors);
    }

    private void validateStudentRow(Map<String, String> row, int rowNum, List<RowError> errors) {
        requireNonBlank(row, rowNum, "first_name", errors);
        requireNonBlank(row, rowNum, "last_name", errors);
        requireNonBlank(row, rowNum, "grade_level", errors);
        validateDate(row, rowNum, "date_of_birth", errors);
        validateEnum(row, rowNum, "gender", VALID_GENDERS, errors);
        validateOptionalEmail(row, rowNum, "email", errors);
        validateOptionalPhone(row, rowNum, "phone", errors);

        requireNonBlank(row, rowNum, "guardian1_first_name", errors);
        requireNonBlank(row, rowNum, "guardian1_last_name", errors);
        validatePhone(row, rowNum, "guardian1_phone", errors);
        validateEnum(row, rowNum, "guardian1_relationship", VALID_RELATIONSHIPS, errors);
        validateOptionalEmail(row, rowNum, "guardian1_email", errors);

        if (hasAny(row, "guardian2_first_name", "guardian2_last_name", "guardian2_phone", "guardian2_relationship")) {
            requireNonBlank(row, rowNum, "guardian2_first_name", errors);
            requireNonBlank(row, rowNum, "guardian2_last_name", errors);
            validatePhone(row, rowNum, "guardian2_phone", errors);
            validateEnum(row, rowNum, "guardian2_relationship", VALID_RELATIONSHIPS, errors);
            validateOptionalEmail(row, rowNum, "guardian2_email", errors);
        }
    }

    private void validateStaffRow(Map<String, String> row, int rowNum, List<RowError> errors) {
        requireNonBlank(row, rowNum, "first_name", errors);
        requireNonBlank(row, rowNum, "last_name", errors);
        requireNonBlank(row, rowNum, "email", errors);
        validateEmail(row, rowNum, "email", errors);
        validateEnum(row, rowNum, "gender", VALID_GENDERS, errors);
        validateDate(row, rowNum, "hire_date", errors);
        validateOptionalPhone(row, rowNum, "phone", errors);

        String isClassTeacher = trim(row.get("is_class_teacher")).toUpperCase();
        if ("YES".equals(isClassTeacher)) {
            requireNonBlank(row, rowNum, "class_teacher_grade", errors);
            // Stream is optional in some schools, but if specified in CSV, we validate it in phase 2
        }
    }

    private void validateStudentReferences(
            Map<String, String> row, int rowNum, Set<String> validClassKeys,
            Set<String> existingAdmissions, Set<String> existingEmails, Set<String> seenInFile, List<RowError> errors) {

        String grade = trim(row.get("grade_level"));
        String stream = trim(row.get("stream_name"));
        String classKey = grade + "::" + stream;

        if (!grade.isEmpty() && !validClassKeys.contains(classKey)) {
            errors.add(new RowError(rowNum, "grade_level", "No active class found for this grade/stream combination.", grade + " / " + stream));
        }

        String admNum = trim(row.get("admission_number"));
        if (!admNum.isEmpty()) {
            if (existingAdmissions.contains(admNum) || !seenInFile.add("adm:" + admNum)) {
                errors.add(new RowError(rowNum, "admission_number", "Admission number already exists.", admNum));
            }
        }

        String email = trim(row.get("email")).toLowerCase();
        if (!email.isEmpty() && (existingEmails.contains(email) || !seenInFile.add("email:" + email))) {
            errors.add(new RowError(rowNum, "email", "Email already registered in this school.", email));
        }
    }

    private void validateStaffReferences(
            Map<String, String> row, int rowNum, Set<String> validClassKeys, Set<String> validSubjectGroups,
            Set<String> existingEmployeeNums, Set<String> existingEmails, Set<String> seenInFile, List<RowError> errors) {

        String email = trim(row.get("email")).toLowerCase();
        if (!email.isEmpty() && (existingEmails.contains(email) || !seenInFile.add("email:" + email))) {
            errors.add(new RowError(rowNum, "email", "Email already registered in this school.", email));
        }

        String empNum = trim(row.get("employee_number"));
        if (!empNum.isEmpty()) {
            if (existingEmployeeNums.contains(empNum) || !seenInFile.add("emp:" + empNum)) {
                errors.add(new RowError(rowNum, "employee_number", "Employee number already exists.", empNum));
            }
        }

        String groupName = trim(row.get("subject_group_name"));
        if (!groupName.isEmpty() && !validSubjectGroups.contains(groupName)) {
            errors.add(new RowError(rowNum, "subject_group_name", "Subject group not found in this school.", groupName));
        }

        String isClassTeacher = trim(row.get("is_class_teacher")).toUpperCase();
        if ("YES".equals(isClassTeacher)) {
            String grade = trim(row.get("class_teacher_grade"));
            String stream = trim(row.get("class_teacher_stream"));
            String key = grade + "::" + stream;
            if (!validClassKeys.contains(key)) {
                errors.add(new RowError(rowNum, "class_teacher_grade", "No active class found for this class teacher assignment.", grade + " / " + stream));
            }
        }
    }

    private void requireNonBlank(Map<String, String> row, int rowNum, String col, List<RowError> errors) {
        if (trim(row.get(col)).isEmpty()) {
            errors.add(new RowError(rowNum, col, "This field is required.", null));
        }
    }

    private void validateDate(Map<String, String> row, int rowNum, String col, List<RowError> errors) {
        String val = trim(row.get(col));
        if (val.isEmpty()) return;
        try {
            LocalDate.parse(val, DATE_FMT);
        } catch (DateTimeParseException e) {
            errors.add(new RowError(rowNum, col, "Invalid date format. Expected DD/MM/YYYY.", val));
        }
    }

    private void validateEnum(Map<String, String> row, int rowNum, String col, Set<String> validValues, List<RowError> errors) {
        String val = trim(row.get(col)).toUpperCase();
        if (!val.isEmpty() && !validValues.contains(val)) {
            errors.add(new RowError(rowNum, col, "Invalid value. Accepted: " + validValues, val));
        }
    }

    private void validateEmail(Map<String, String> row, int rowNum, String col, List<RowError> errors) {
        String val = trim(row.get(col));
        if (!val.isEmpty() && !EMAIL_PATTERN.matcher(val).matches()) {
            errors.add(new RowError(rowNum, col, "Invalid email format.", val));
        }
    }

    private void validateOptionalEmail(Map<String, String> row, int rowNum, String col, List<RowError> errors) {
        String val = trim(row.get(col));
        if (!val.isEmpty()) validateEmail(row, rowNum, col, errors);
    }

    private void validatePhone(Map<String, String> row, int rowNum, String col, List<RowError> errors) {
        String val = trim(row.get(col)).replaceAll("\\s", "");
        if (val.isEmpty()) {
            errors.add(new RowError(rowNum, col, "Phone number is required.", null));
            return;
        }
        if (!KENYAN_PHONE.matcher(val).matches()) {
            errors.add(new RowError(rowNum, col, "Invalid phone format. Use 0712345678 or +254712345678.", val));
        }
    }

    private void validateOptionalPhone(Map<String, String> row, int rowNum, String col, List<RowError> errors) {
        String val = trim(row.get(col)).replaceAll("\\s", "");
        if (!val.isEmpty() && !KENYAN_PHONE.matcher(val).matches()) {
            errors.add(new RowError(rowNum, col, "Invalid phone format. Use 0712345678 or +254712345678.", val));
        }
    }

    private boolean hasAny(Map<String, String> row, String... cols) {
        for (String col : cols) {
            if (!trim(row.get(col)).isEmpty()) return true;
        }
        return false;
    }

    private String trim(String val) {
        return val == null ? "" : val.strip();
    }
}
