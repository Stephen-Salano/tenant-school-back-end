# Contributing to School Management System

Thank you for your interest in contributing! This document provides guidelines for contributing to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Process](#development-process)
- [Coding Standards](#coding-standards)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [Testing Requirements](#testing-requirements)
- [Documentation](#documentation)
- [Community](#community)

---

## Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inclusive environment for all contributors regardless of experience level, gender, gender identity and expression, sexual orientation, disability, personal appearance, body size, race, ethnicity, age, religion, or nationality.

### Expected Behavior

- **Be respectful and considerate** in all interactions
- **Accept constructive criticism gracefully** and learn from it
- **Focus on what's best for the project** and the community
- **Show empathy** towards other contributors and users
- **Use welcoming and inclusive language**
- **Be patient** with new contributors learning the codebase

### Unacceptable Behavior

- **Harassment or discriminatory language** of any kind
- **Personal attacks, trolling, or insulting comments**
- **Publishing others' private information** without permission
- **Sexual language or imagery** and unwelcome sexual attention
- **Any other unprofessional conduct** that creates an unwelcoming environment

### Enforcement
Project maintainers have the right to remove, edit, or reject comments, commits, code, issues, and other contributions that violate this Code of Conduct, or to ban temporarily or permanently any contributor for behaviors they deem inappropriate, threatening, offensive, or harmful.

---

## Getting Started

### Prerequisites

Before contributing, ensure you have:

1. **Read the [README.md](README.MD)** thoroughly
2. **Set up your local development environment** (Java 21, Docker, PostgreSQL, etc.)
3. **Familiarized yourself with the codebase structure** (21 packages, feature-based organization)
4. **Reviewed open issues and pull requests** to avoid duplicate work

### Finding Issues to Work On

#### For First-Time Contributors

- Look for issues labeled **`good first issue`** - These are beginner-friendly tasks
- Issues labeled **`help wanted`** - These explicitly need contributors
- Issues labeled **`documentation`** - Great for getting familiar with the project

#### For Experienced Contributors

- Issues labeled **`enhancement`** - New features or improvements
- Issues labeled **`bug`** - Bug fixes needed
- Issues labeled **`performance`** - Optimization tasks
- Issues labeled **`refactor`** - Code quality improvements

#### Before Starting Work

1. **Comment on the issue** to indicate you're working on it
2. **Ask questions** if requirements are unclear
3. **Wait for confirmation** from maintainers before starting (to avoid conflicts)
4. **Check if the issue is already assigned** to someone else

---

## Development Process

### 1. Fork and Clone
```bash
# Fork the repository on GitHub (click "Fork" button)

# Clone your fork
git clone https://github.com/YOUR-USERNAME/school-management-system.git
cd school-management-system

# Add upstream remote to keep your fork in sync
git remote add upstream https://github.com/original-org/school-management-system.git

# Verify remotes
git remote -v
```

### 2. Set Up Development Environment
```bash
# Copy environment template
cp .env.example .env

# Edit .env with your local settings
nano .env  # or use your preferred editor

# Start Docker services
docker compose up -d

# Install dependencies
./mvnw clean install

# Run tests to verify setup
./mvnw test
```

### 3. Create a Branch

**Always start from the latest `develop` branch:**
```bash
# Switch to develop and update
git checkout develop
git pull upstream develop

# Create your feature branch
git checkout -b feature/your-feature-name

# Or for bug fixes
git checkout -b bugfix/issue-number-description

# Or for documentation
git checkout -b docs/description
```

**Branch Naming Convention:**
- `feature/` - New features (e.g., `feature/cbc-grading`)
- `bugfix/` - Bug fixes (e.g., `bugfix/123-fix-attendance-calculation`)
- `hotfix/` - Critical production fixes (e.g., `hotfix/security-patch`)
- `docs/` - Documentation updates (e.g., `docs/update-readme`)
- `refactor/` - Code refactoring (e.g., `refactor/simplify-grading-logic`)
- `test/` - Test additions/improvements (e.g., `test/add-student-service-tests`)

### 4. Make Changes

#### General Guidelines

- **Write clean, readable code** that follows our style guide
- **Keep changes focused** - One feature/fix per branch
- **Add tests** for new features and bug fixes
- **Update documentation** if you change functionality
- **Run tests frequently** to catch issues early

#### Package-Specific Guidelines

When working on a specific package (e.g., `student`, `grading`, `payments`):

1. **Follow the package structure:**
```
   package/
   ├── entity/          # JPA entities only
   ├── repository/      # Spring Data repositories
   ├── service/         # Business logic
   ├── controller/      # REST controllers
   ├── dto/
   │   ├── request/     # Request DTOs
   │   └── response/    # Response DTOs
   ├── enums/           # Feature-specific enums
   └── exception/       # Feature-specific exceptions (optional)
```

2. **Maintain multi-tenancy:**
    - Always include `tenant_id` in queries
    - Use `TenantContext` to get current tenant
    - Never expose data across tenants

3. **Respect dependencies:**
    - Check the implementation roadmap for package dependencies
    - Don't create circular dependencies
    - If unsure, ask maintainers

### 5. Test Your Changes
```bash
# Run all tests
./mvnw test

# Run tests for specific package
./mvnw test -Dtest=io.psyllabus.platform.student.*

# Run with coverage report
./mvnw test jacoco:report

# View coverage
open target/site/jacoco/index.html

# Run integration tests
./mvnw verify -P integration-tests
```

### 6. Commit Your Changes
```bash
# Stage changes
git add .

# Commit with conventional commit message
git commit -m "feat(student): add bulk enrollment feature"

# Push to your fork
git push origin feature/your-feature-name
```

### 7. Create Pull Request

1. Go to your fork on GitHub
2. Click **"New Pull Request"**
3. Select **`develop`** as the base branch (not `main`)
4. Fill in the PR template (see below)
5. Request review from maintainers
6. Link the related issue (e.g., "Closes #42")

---

## Coding Standards

### Java Code Style

We follow the **Google Java Style Guide** with minor modifications for Spring Boot.

#### Naming Conventions
```java
// Classes: PascalCase
public class StudentService { }
public class CbcGradingStrategy implements GradingStrategy { }

// Interfaces: PascalCase
public interface GradingStrategy { }
public interface PaymentGateway { }

// Methods: camelCase (verb-first)
public void enrollStudent() { }
public StudentResponse findById(UUID id) { }
public boolean isEligible() { }

// Variables: camelCase
private String firstName;
private UUID tenantId;
private LocalDate enrollmentDate;

// Constants: UPPER_SNAKE_CASE
private static final int MAX_STUDENTS_PER_CLASS = 500;
private static final String DEFAULT_TIMEZONE = "Africa/Nairobi";

// Packages: lowercase (no underscores)
package io.psyllabus.platform.student;
package io.psyllabus.platform.grading.strategy;

// DTOs: Descriptive suffixes
StudentCreateRequest
StudentResponse
StudentDetailResponse
PageResponse<T>

// Enums: PascalCase for type, UPPER_SNAKE_CASE for values
public enum UserType {
    ADMIN, TEACHER, PARENT, STUDENT
}

public enum AttendanceStatus {
    PRESENT, ABSENT, LATE, EXCUSED
}
```

#### Code Formatting
```java
// Line length: 120 characters maximum
// Indentation: 4 spaces (no tabs)
// Braces: Always use, even for single-line blocks

// ✅ GOOD
if (condition) {
    doSomething();
}

// ❌ BAD
if (condition) doSomething();

// Method spacing: One blank line between methods
public void method1() {
    // implementation
}

public void method2() {
    // implementation
}


// Import organization
import java.util.*;           // Java standard library
import javax.*;                // Java extensions
import org.springframework.*;  // Third-party libraries
import io.psyllabus.*;        // Project imports

// No wildcard imports except for static imports
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
```

#### Documentation (Javadoc)

**All public methods, classes, and interfaces must have Javadoc:**
```java
/**
 * Calculates the grade level based on score and grading scheme.
 * 
 * <p>This method uses the Strategy pattern to apply the appropriate
 * grading algorithm (CBC, Traditional, etc.) based on the scheme type.
 * 
 * <p><strong>Example:</strong>
 * <pre>
 * GradingLevel level = calculateGrade(85.5, cbcSchemeId);
 * // Returns: EE2 (Exceeds Expectations - Very Good)
 * </pre>
 *
 * @param score the student's score (must be between 0 and 100)
 * @param schemeId the UUID of the grading scheme to use
 * @return the calculated grading level with points and descriptor
 * @throws IllegalArgumentException if score is outside valid range
 * @throws GradingSchemeNotFoundException if scheme doesn't exist
 * @see GradingStrategy
 * @see CbcGradingStrategy
 * @since 1.0.0
 */
public GradingLevel calculateGrade(BigDecimal score, UUID schemeId) {
    // implementation
}
```

### Entity Guidelines
```java
@Entity
@Table(name = "students", indexes = {
    @Index(name = "idx_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_admission_number", columnList = "admission_number"),
    @Index(name = "idx_tenant_class", columnList = "tenant_id,current_class_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student extends Auditable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "admission_number", nullable = false, unique = true, length = 50)
    private String admissionNumber;
    
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;
    
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;
    
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;
    
    // IMPORTANT RULES:
    // 1. Always include tenant_id with nullable = false
    // 2. Add indexes on tenant_id, foreign keys, and frequently queried fields
    // 3. Use @Column(name = "...") for all fields to match database naming
    // 4. Specify length for varchar fields
    // 5. Extend Auditable for created_at, updated_at, created_by, updated_by
    // 6. Use Lombok to reduce boilerplate
}
```

### Service Guidelines
- Always work with interfaces that are implemented, this allows polymorphism

```java
import io.psyllabus.platform.student.service.StudentService;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final TenantContext tenantContext;
    private final StudentMapper studentMapper;

    /**
     * Enrolls a new student in the school.
     *
     * @param request the student enrollment details
     * @return the enrolled student information
     * @throws DuplicateAdmissionNumberException if admission number exists
     */
    public StudentResponse enrollStudent(StudentCreateRequest request) {
        UUID tenantId = tenantContext.getCurrentTenantId();

        log.info("Enrolling student with admission number: {} for tenant: {}",
                request.getAdmissionNumber(), tenantId);

        // Validation
        validateAdmissionNumber(request.getAdmissionNumber(), tenantId);

        // Business logic
        Student student = studentMapper.toEntity(request);
        student.setTenantId(tenantId);
        student.setStatus(StudentStatus.ACTIVE);

        // Save
        Student saved = studentRepository.save(student);

        log.info("Successfully enrolled student: {} (ID: {})",
                saved.getAdmissionNumber(), saved.getId());

        // Return DTO
        return studentMapper.toResponse(saved);
    }

    // IMPORTANT RULES:
    // 1. Always use @Transactional at class level
    // 2. Get tenant_id from TenantContext, never from request
    // 3. Validate inputs before processing
    // 4. Log important operations (INFO level)
    // 5. Return DTOs, never entities
    // 6. Use mappers to convert between entities and DTOs
    // 7. Keep public methods focused (single responsibility)
    // 8. Extract complex logic to private methods

    private void validateAdmissionNumber(String admissionNumber, UUID tenantId) {
        if (studentRepository.existsByAdmissionNumberAndTenantId(admissionNumber, tenantId)) {
            throw new DuplicateAdmissionNumberException(
                    "Admission number already exists: " + admissionNumber);
        }
    }
}
```

### Controller Guidelines
```java
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Tag(name = "Student Management", description = "APIs for managing student enrollment and records")
@Validated
public class StudentController {
    
    private final StudentService studentService;
    
    @PostMapping
    @Operation(summary = "Enroll a new student", 
               description = "Creates a new student record with unique admission number")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Student enrolled successfully",
                     content = @Content(schema = @Schema(implementation = StudentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Admission number already exists",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<StudentResponse>> enrollStudent(
            @Valid @RequestBody StudentCreateRequest request) {
        
        StudentResponse response = studentService.enrollStudent(request);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Student enrolled successfully"));
    }
    
    @GetMapping
    @Operation(summary = "Get all students", 
               description = "Retrieves paginated list of students for current school")
    public ResponseEntity<PageResponse<StudentResponse>> getAllStudents(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) StudentStatus status) {
        
        Page<StudentResponse> students = studentService.getAllStudents(page, size, search, status);
        
        return ResponseEntity.ok(PageResponse.from(students));
    }
    
    // IMPORTANT RULES:
    // 1. Use @RestController (not @Controller)
    // 2. Always return ResponseEntity with proper status codes
    // 3. Wrap responses in ApiResponse or PageResponse
    // 4. Use @Valid for request body validation
    // 5. Document with @Operation and @ApiResponses (for Swagger)
    // 6. Keep controllers thin - delegate to services
    // 7. Use pagination for list endpoints
    // 8. Never access repositories directly
}
```

### DTO Guidelines
```java
// ============ REQUEST DTOs ============
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public record StudentCreateRequest (
    
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "First name contains invalid characters")
    private String firstName,
    
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName,
    
    @NotBlank(message = "Admission number is required")
    @Pattern(regexp = "^[A-Z0-9]{6,10}$", 
             message = "Admission number must be 6-10 uppercase letters/digits")
    private String admissionNumber,
    
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth,
    
    @NotNull(message = "Gender is required")
    private Gender gender,
    
    @Email(message = "Guardian email must be valid")
    private String guardianEmail
        ){
    
    // IMPORTANT RULES:
    // 1. Use validation annotations (@NotNull, @NotBlank, @Size, @Pattern, etc.)
    // 2. Provide clear, user-friendly validation messages
    // 3. Use @Builder for easier object creation in tests
    // 4. Keep DTOs flat (avoid nested objects in requests)
}

// ============ RESPONSE DTOs ============
@Data
@Builder
public record StudentResponse (
    
    private UUID id,
    private String admissionNumber,
    private String firstName,
    private String lastName,
    private LocalDate dateOfBirth,
    private Gender gender,
    private String currentClass,
    private StudentStatus status,
    private LocalDateTime createdAt,
    private LocalDateTime updatedAt
    ){}
    
    // IMPORTANT RULES:
    // 1. Include only what frontend needs (don't expose everything)
    // 2. Use descriptive names (currentClass instead of classId)
    // 3. Convert enums to strings if needed
    // 4. Include audit fields (createdAt, updatedAt)
    // 5. Use @Builder for easier object creation


// ============ DETAILED RESPONSE DTOs ============
@Data
@Builder
public class StudentDetailResponse {
    
    private UUID id;
    private String admissionNumber;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private Gender gender;
    
    // Relationships
    private ClassResponse currentClass;
    private List<GuardianResponse> guardians;
    
    // Metrics
    private BigDecimal currentTermAverage;
    private BigDecimal attendanceRate;
    private Integer daysAbsent;
    
    // Metadata
    private LocalDateTime createdAt;
    private String createdBy;
    
    // IMPORTANT RULES:
    // 1. Use for single-item GET endpoints
    // 2. Include relationships and calculated fields
    // 3. Don't use for list endpoints (too heavy)
}
```

### Repository Guidelines
```java
@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {
    
    // Always include tenant_id in queries for multi-tenancy
    
    boolean existsByAdmissionNumberAndTenantId(String admissionNumber, UUID tenantId);
    
    Optional<Student> findByIdAndTenantId(UUID id, UUID tenantId);
    
    Page<Student> findByTenantId(UUID tenantId, Pageable pageable);
    
    @Query("""
        SELECT s FROM Student s 
        WHERE s.tenantId = :tenantId 
        AND s.currentClassId = :classId
        AND s.status = 'ACTIVE'
        """)
    List<Student> findActiveStudentsByClass(
        @Param("tenantId") UUID tenantId,
        @Param("classId") UUID classId
    );
    
    // Use @EntityGraph to prevent N+1 queries
    @EntityGraph(attributePaths = {"currentClass", "guardians"})
    Optional<Student> findWithDetailsById(UUID id);
    
    // IMPORTANT RULES:
    // 1. Always filter by tenant_id
    // 2. Use method naming conventions (findBy, existsBy, countBy)
    // 3. Use @Query for complex queries
    // 4. Use @EntityGraph to fetch relationships efficiently
    // 5. Return Optional for single results, List/Page for multiple
}
```

---

## Commit Guidelines

### Conventional Commits

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification strictly.

**Format:**
```
<type>(<scope>): <subject>

<body>

<footer>
```

#### Types

| Type | Description | Example |
|------|-------------|---------|
| `feat` | New feature | `feat(grading): add CBC grading strategy` |
| `fix` | Bug fix | `fix(auth): resolve token expiration issue` |
| `docs` | Documentation only | `docs(readme): update setup instructions` |
| `style` | Code style (formatting, semicolons) | `style(student): format StudentService` |
| `refactor` | Code refactoring | `refactor(payment): simplify fee calculation` |
| `perf` | Performance improvement | `perf(analytics): optimize dashboard queries` |
| `test` | Adding/updating tests | `test(student): add enrollment validation tests` |
| `build` | Build system changes | `build(maven): update Spring Boot to 3.2.1` |
| `ci` | CI configuration changes | `ci(github): add code quality workflow` |
| `chore` | Other changes (dependencies, etc.) | `chore(deps): update PostgreSQL driver` |

#### Scopes

Use the package name as the scope:

- `auth`, `student`, `staff`, `guardian`
- `academic`, `grading`, `assessment`, `attendance`
- `payments`, `analytics`, `ai`, `messaging`
- `activity`, `specialclass`, `branch`
- `tenant`, `subscription`, `common`

#### Examples

**Feature:**
```
feat(grading): implement CBC grading strategy

Added CbcGradingStrategy with 8 levels (EE1-BE2).
Strategy calculates grade level based on score ranges
and assigns points according to CBC standards.

- Created GradingStrategy interface
- Implemented CbcGradingStrategy with score ranges
- Added unit tests with edge cases
- Seeded default CBC scheme in database

Closes #42
```

**Bug Fix:**
```
fix(auth): resolve JWT token expiration issue

Fixed bug where tokens expired 1 hour earlier than configured.
The issue was in millisecond conversion in JwtService.

- Changed expiration calculation to use correct time units
- Added validation test for token expiration
- Updated documentation with correct expiration format

Fixes #58
```

**Documentation:**
```
docs(readme): update Docker setup instructions

- Added troubleshooting section for common Docker issues
- Updated PostgreSQL version requirement to 15+
- Clarified environment variable configuration
```

**Breaking Change:**
```
feat(api)!: change student enrollment endpoint structure

BREAKING CHANGE: The POST /api/students endpoint now requires
guardian information in the request body. Existing API clients
must update to include guardian details.

Migration guide available in docs/migration/v2.0.md

Closes #101
```

### Commit Message Rules

**Subject Line:**
- Use imperative mood: "add" not "added" or "adds"
- Don't capitalize first letter after type
- No period at the end
- Keep under 72 characters

**Body:**
- Explain *what* and *why*, not *how*
- Wrap at 72 characters
- Separate from subject with blank line
- Use bullet points for multiple changes

**Footer:**
- Reference issues: `Closes #123`, `Fixes #456`
- Note breaking changes: `BREAKING CHANGE: ...`
- Credit co-authors: `Co-authored-by: Name <email>`

---

## Pull Request Process

### Before Submitting

- [ ] **Code follows style guide** (run `./mvnw spotless:check`)
- [ ] **All tests pass locally** (`./mvnw test`)
- [ ] **New tests added** for new features/bug fixes
- [ ] **Test coverage maintained** (80%+ for new code)
- [ ] **Documentation updated** (README, Javadoc, API docs)
- [ ] **No merge conflicts** with `develop` branch
- [ ] **Commit messages follow convention**
- [ ] **Branch name follows convention**
- [ ] **No secrets or credentials** in code

### PR Template

When creating a PR, fill in this template:
```markdown
## Description
Brief description of what this PR does and why.

## Type of Change
- [ ] Bug fix (non-breaking change fixing an issue)
- [ ] New feature (non-breaking change adding functionality)
- [ ] Breaking change (fix or feature causing existing functionality to break)
- [ ] Documentation update
- [ ] Refactoring (no functional changes)
- [ ] Performance improvement
- [ ] Test additions/improvements

## Related Issue
Closes #(issue number)
Fixes #(issue number)
Relates to #(issue number)

## How Has This Been Tested?
Describe the tests you ran and how to reproduce.

- [ ] Unit tests
- [ ] Integration tests
- [ ] Manual testing
- [ ] Performance testing

**Test Configuration:**
- Java version: 21
- PostgreSQL version: 15
- Environment: dev/test/prod

## Screenshots (if applicable)
Add screenshots for UI changes or Swagger documentation.

## Checklist
- [ ] My code follows the style guidelines
- [ ] I have performed a self-review
- [ ] I have commented my code, particularly in hard-to-understand areas
- [ ] I have made corresponding changes to the documentation
- [ ] My changes generate no new warnings
- [ ] I have added tests that prove my fix is effective or my feature works
- [ ] New and existing unit tests pass locally
- [ ] Any dependent changes have been merged and published

## Additional Context
Add any other context about the PR here.
```

### Review Process

1. **Automated Checks** (must pass):
    - CI pipeline (build, tests, code quality)
    - Code coverage check (80%+ for new code)
    - Linting and formatting

2. **Code Review**:
    - At least **one approval** from a maintainer required
    - Address all review comments
    - Re-request review after making changes

3. **Testing**:
    - Reviewer may test changes locally
    - Ensure demo data still seeds correctly
    - Check Swagger UI still works

4. **Merge**:
    - Maintainer squashes and merges to `develop`
    - Delete feature branch after merge

### Addressing Review Comments
```bash
# Make requested changes
git add .
git commit -m "refactor: address PR review comments"

# Push to update PR
git push origin feature/your-feature-name

# If you need to amend previous commit
git add .
git commit --amend --no-edit
git push origin feature/your-feature-name --force-with-lease

# If you need to squash commits
git rebase -i HEAD~3  # for last 3 commits
# Follow interactive rebase instructions
git push origin feature/your-feature-name --force-with-lease
```

---

## Testing Requirements

### Test Coverage Goals

- **Overall coverage**: 80%+ across all packages
- **New code coverage**: 80%+ for all new features/fixes
- **Critical paths**: 90%+ for authentication, grading, payments

### Test Categories

1. **Unit Tests** (`src/test/java/.../`):
    - Test individual methods in isolation
    - Mock all dependencies
    - Fast execution (<5 minutes for full suite)
    - Use JUnit 5 + Mockito

2. **Integration Tests** (`src/test/java/.../integration/`):
    - Test multiple components together
    - Use real database (test containers)
    - Slower execution (acceptable)
    - Use Spring Boot Test

3. **End-to-End Tests** (future):
    - Test complete user workflows
    - Use Selenium or similar
    - Run in CI pipeline

### Writing Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {
    
    @Mock
    private StudentRepository studentRepository;
    
    @Mock
    private TenantContext tenantContext;
    
    @Mock
    private StudentMapper studentMapper;
    
    @InjectMocks
    private StudentService studentService;
    
    private UUID tenantId;
    private StudentCreateRequest request;
    private Student student;
    private StudentResponse response;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        request = createValidRequest();
        student = createStudent();
        response = createResponse();
    }
    
    @Test
    void shouldEnrollStudent_WhenValidInput() {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn(tenantId);
        when(studentRepository.existsByAdmissionNumberAndTenantId(anyString(), any()))
            .thenReturn(false);
        when(studentMapper.toEntity(request)).thenReturn(student);
        when(studentRepository.save(any(Student.class))).thenReturn(student);
        when(studentMapper.toResponse(student)).thenReturn(response);
        
        // When
        StudentResponse result = studentService.enrollStudent(request);
        
        // Then
        assertNotNull(result);
        assertEquals(request.getFirstName(), result.getFirstName());
        verify(studentRepository).save(argThat(s -> 
            s.getTenantId().equals(tenantId) && 
            s.getStatus() == StudentStatus.ACTIVE
        ));
    }
    
    @Test
    void shouldThrowException_WhenDuplicateAdmissionNumber() {
        // Given
        when(tenantContext.getCurrentTenantId()).thenReturn(tenantId);
        when(studentRepository.existsByAdmissionNumberAndTenantId(anyString(), any()))
            .thenReturn(true);
        
        // When & Then
        assertThrows(DuplicateAdmissionNumberException.class, () -> {
            studentService.enrollStudent(request);
        });
        
        verify(studentRepository, never()).save(any());
    }
    
    // Helper methods
    private StudentCreateRequest createValidRequest() {
        return StudentCreateRequest.builder()
            .firstName("John")
            .lastName("Doe")
            .admissionNumber("STU001")
            .dateOfBirth(LocalDate.of(2010, 1, 1))
            .gender(Gender.MALE)
            .build();
    }
}
```

### Writing Integration Tests
```java
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/test-data/students.sql")
@ActiveProfiles("test")
class StudentControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldEnrollStudent_AndReturn201() throws Exception {
        // Given
        StudentCreateRequest request = StudentCreateRequest.builder()
            .firstName("Jane")
            .lastName("Smith")
            .admissionNumber("STU999")
            .dateOfBirth(LocalDate.of(2010, 6, 15))
            .gender(Gender.FEMALE)
            .build();
        
        // When & Then
        mockMvc.perform(post("/api/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.admissionNumber").value("STU999"))
            .andExpect(jsonPath("$.data.firstName").value("Jane"))
            .andExpect(jsonPath("$.data.id").exists());
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn409_WhenDuplicateAdmissionNumber() throws Exception {
        // Given - STU001 exists from test-data/students.sql
        StudentCreateRequest request = StudentCreateRequest.builder()
            .firstName("John")
            .lastName("Duplicate")
            .admissionNumber("STU001")
            .dateOfBirth(LocalDate.of(2010, 1, 1))
            .gender(Gender.MALE)
            .build();
        
        // When & Then
        mockMvc.perform(post("/api/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").exists());
    }
}
```

### Running Tests
```bash
# All tests
./mvnw clean test

# Specific test class
./mvnw test -Dtest=StudentServiceTest

# Specific test method
./mvnw test -Dtest=StudentServiceTest#shouldEnrollStudent_WhenValidInput

# With coverage
./mvnw clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html

# Integration tests only
./mvnw verify -P integration-tests

# Skip tests (only for builds, not PRs)
./mvnw clean package -DskipTests
```

---

## Documentation

### Code Documentation

1. **Javadoc for all public classes, interfaces, and methods**
2. **Inline comments for complex logic** (sparingly - code should be self-documenting)
3. **README in each major package** explaining its purpose
4. **API documentation via Swagger annotations**

### Updating Documentation

When making changes, update:

- **README.md** - If adding new features, setup steps, or dependencies
- **CONTRIBUTING.md** - If changing development process
- **API Documentation** - Always update Swagger annotations
- **Database Schema** - Update `docs/database/schema.dbml` if changing schema
- **Architecture Docs** - Update `docs/architecture/` for major changes

---

## Community

### Getting Help

- **GitHub Discussions**: Ask questions, share ideas
- **GitHub Issues**: Report bugs, request features
- **Code Reviews**: Learn from feedback on your PRs

### Recognizing Contributors

We recognize contributors in:
- `README.md` contributors section
- `CHANGELOG.md` release notes
- GitHub releases and tags

### Becoming a Maintainer

Regular contributors may be invited to become maintainers. Criteria:

- **Consistent quality contributions** over 3+ months
- **Deep understanding** of the codebase
- **Helpful code reviews** for other contributors
- **Alignment** with project goals and values

---

## Questions?

If you have questions about contributing:

1. Check existing [GitHub Discussions](https://github.com/your-org/school-management-system/discussions)
2. Review [GitHub Issues](https://github.com/your-org/school-management-system/issues)

---

**Thank you for contributing! 🎉**

Every contribution, no matter how small, helps make this project better for schools across Kenya.