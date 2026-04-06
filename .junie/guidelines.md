---
name: psyllabus
description: Standards, architecture rules, and interaction modes for the Psyllabus school-management platform (Java 21 / Spring Boot 3 / PostgreSQL / Redis / multi-tenant SaaS). Use this skill for ANY task on the Psyllabus project — writing code, planning features, reviewing security, designing architecture, or discussing patterns. Trigger whenever the user mentions Psyllabus, uses a MODE: prefix, asks about tenants/schools/branches, or works on Spring Boot code in this codebase. Also trigger for questions about RBAC, K-DPA, filter chain, or admission numbers in context that matches this project.
---

# Psyllabus Skill

Read the relevant reference file(s) before responding to any task.

## Reference Files

| File | When to read it |
|---|---|
| `references/mode-system.md` | **Always read first.** Determines how to respond (MODE rules, PLAN→IMPLEMENT workflow, output format, disagreement policy). |
| `references/psyllabus-core.md` | Project overview, milestones, non-negotiables, subscription tiers, onboarding flow. |
| `references/spring-boot-java.md` | Any code task — entities, services, controllers, DTOs, migrations, logging, responses. |
| `references/multi-tenant-security.md` | Anything touching tenants, users, auth, filters, RBAC, K-DPA, or data access. |
| `references/oop-solid-patterns.md` | Design decisions, new class creation, pattern selection, anti-pattern avoidance. |

## Non-Negotiables

1. **Tenant scope on every query** — `findByIdAndTenantId(...)`, never bare `findById(...)` for school data.
2. **TenantContext is the only source of truth** for `tenantId` — never from the request body or JWT alone.
3. **Controllers are facades** — no business logic, no repository calls, no tenant resolution.
4. **MODE prefix governs response style** — if no MODE is given, infer from context or ask once.
5. **K-DPA compliance** — data minimisation in DTOs, audit log for sensitive ops, never log PII.
6. **Package by feature** — every new class goes into the right feature package; `common/` for shared code only.

### Source: .gemini/skills/psyllabus/references/mode-system.md

# MODE System — Psyllabus Project

This document defines how to interact on this project.
Prefix every message with a MODE to control behaviour precisely.

---

## Available MODEs

### `MODE: EXPLAIN`
Knowledge or clarification only.
- No file changes, no refactors, no implementations
- Use when learning how something works or why a decision was made
- Response may use analogies and visuals to aid understanding

### `MODE: PLAN`
Break a feature into tasks and subtasks before any code is written.

Every PLAN must include:
- Feature goal
- Acceptance criteria
- Numbered tasks with subtasks
- Dependencies (configs, entities, services that must exist first)
- Logical execution order (bottom-up — config before usage, entities before services, services before controllers)

Non-trivial features **must** start with MODE: PLAN before MODE: IMPLEMENT.

### `MODE: IMPLEMENT <feature-name>`
Write code, config, or migration steps.
- Must reference an approved PLAN unless the change is trivial (≤ one small function or config change)
- If no PLAN exists for a non-trivial feature, auto-run PLAN first then confirm before implementing
- Output: individual artifacts per file, not bundled zips

### `MODE: ARCHITECTURE`
High-level system design, data flow, scaling, compliance notes.
- Diagrams, dependency graphs, ring model
- Does not produce code

### `MODE: SECURITY`
Threat modelling, RBAC review, consent flows, K-DPA mapping.
- Maps threats to defences
- Reviews filter chain, scope enforcement, data exposure

### `MODE: CONSIDER`
Pros, cons, tradeoffs, and recommendation on a decision.
- Gives a clear recommendation with one-line rationale
- Does not implement anything

### `MODE: FALLBACK` / `MODE: RETRY`
Recovery paths when something fails in production or dev.
- What the system does when Redis, RabbitMQ, or the DB is unavailable
- Retry strategies, DLQ handling, graceful degradation

### `MODE: QA`
Tests, acceptance criteria, edge cases.
- Test scenarios for a given feature
- Does not implement production code

---

## PLAN → IMPLEMENT Workflow

```
Non-trivial feature
      ↓
MODE: PLAN               ← get the full picture, confirm order
      ↓
Approve the plan
      ↓
MODE: IMPLEMENT <task>   ← one task at a time, in dependency order
      ↓
Test / review
      ↓
Next task
```

Dependency order rule: config before usage, SDK/library before integration, entities/helpers before functions that depend on them.

Tiny exception: ≤ one small function or config property → list the dependency first, then implement inline without a separate PLAN step.

---

## Bottom-Up Delivery Order

Every feature is built in this strict sequence. Nothing is implemented before its dependency exists. The controller is always last.

1. Config / environment properties
2. Enums
3. Entities (JPA)
4. Repositories
5. DTOs (request + response records)
6. Service interface
7. Service implementation
8. Controller

**Rule:** If step N is missing, do not start step N+1. Flag the gap and build it first.

**Common gaps to check before starting any task:**
- Does the entity exist and have the right fields?
- Does the repository have the query methods the service needs?
- Are the request/response DTOs defined before the service method that uses them?
- Is the config property declared in `application.yml` before the class that injects it?

---

## Code Output Rules

- Each class or meaningful change gets its own artifact
- Do not bundle files into a zip
- For small changes (1–3 lines): show only the diff and where it goes — do not rewrite the full file
- For new files: write the full file as an artifact
- Never create files like QuickStart guides, checklists, or migration summaries unless explicitly asked

---

## Style Rules

1. **Preserve existing code structure** — do not restructure working files
2. **Preserve existing comments** — add to them, never rewrite unless factually wrong (flag first)
3. **Proper Javadoc** — use `@param`, `@return`, `@throws`, `{@code}`, `<p>` tags correctly
4. **Minimal diffs** — if the change is 2 lines, show 2 lines and where they go
5. **Scan before creating** — check for empty shells, existing interfaces, and partial implementations before suggesting a new class
6. **Design patterns silently** — evaluate patterns internally, only surface one when actively using it or suggesting it with a clear reason

---

## Response Structure

Every response follows:

1. **Status line** — e.g. `Planned — ready to implement` or `Implemented ✅`
2. **Main content** — per MODE rules
3. **Next steps** — 1–3 concrete actions
4. **Assumptions / Sources** — only if relevant

Default output is **short** (3–6 bullets, plain language). Only expand when the MODE requires depth or the user explicitly asks.

---

## Disagreement Policy

Do not blindly agree. If a proposed approach is insecure, non-compliant, non-scalable, or likely to fail in production:

1. Clearly explain the risk in plain terms
2. Propose a safer alternative
3. Defend the alternative: what it ensures, what it prevents

If the user insists: require explicit `CONFIRM: proceed with user approach` before implementing, and include a short risk note.

---

## No MODE?

If a message has no MODE prefix, infer the most likely MODE from context:
- "explain this" → MODE: EXPLAIN
- "what's wrong with..." → MODE: CONSIDER
- Otherwise, ask which MODE before proceeding

### Source: .gemini/skills/psyllabus/references/multi-tenant-security.md

# Multi-Tenant Security — Psyllabus

## The Ring Model

Access is structured as concentric rings. Every authenticated user lives in exactly one ring.
A user in an inner ring can never see data from another ring at the same level.

```
┌────────────────────────────────────────────────────────────────┐
│  PLATFORM  (Psyllabus team — us)                               │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  SCHOOL  (principal / school-level admin)                │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │  BRANCH  (branch admin, class teachers, students)  │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
```

`AccessScope` enum maps directly to this: `PLATFORM > SCHOOL > BRANCH`
`UserType` determines category: `PLATFORM_ADMIN`, `ADMIN`, `STAFF`, `STUDENT`, `PARENT`

---

## Filter Chain Order

Every request passes through these filters in strict order:

```
PlatformKeyFilter      → guards /api/schools/onboard + /api/platform/** (API key check)
TenantFilter           → resolves tenantId from subdomain → TenantContext
JwtAuthFilter          → validates Bearer token → SecurityContext
SubscriptionInterceptor → validates feature access for the tenant's plan
```

Each filter only works because the one before it already ran. Order is not optional.

---

## Tenant Resolution

Tenant is always resolved from the **subdomain or custom domain** — never from the request body, query params, or JWT claims alone.

```
sunrise.psyllabus.io       → subdomainPrefix = "sunrise" → TenantContext.set(schoolId)
portal.riverside.ac.ke     → custom domain mapping → TenantContext.set(schoolId)
```

`TenantContext` is a `ThreadLocal<UUID>`. It is set at the start of every request and cleared in a `finally` block. Never read tenant from anywhere else.

### PLATFORM_ADMIN exception
Platform admins have no tenant. `TenantFilter` detects `user_type = PLATFORM_ADMIN` from the JWT and skips subdomain resolution entirely — sets context to null and continues. Platform admin endpoints never call `TenantContext.getCurrentTenantId()`.

---

## The Golden Rule of Repository Queries

Every single repository query that touches school data **must** include `tenant_id`:

```java
// Always — tenant-scoped
studentRepository.findByIdAndTenantId(id, tenantId)

// Never — unscoped query is a data leak
studentRepository.findById(id)
```

If a repository method doesn't have `AndTenantId` in the name or `WHERE tenant_id = ?` in the query, it should not exist in a service that handles school data.

---

## ScopeGuard — Branch Access Enforcement

`common/security/ScopeGuard` centralises branch access checks so the logic isn't copy-pasted into every service.

```java
// In any branch-sensitive service method
scopeGuard.requireBranchAccess(branchId);
// SCHOOL or PLATFORM scope → passes
// BRANCH scope + matching branchId → passes
// BRANCH scope + different branchId → throws InsufficientPermissionException
```

---

## RBAC via @PreAuthorize

Controllers use method-level security. Never rely solely on path matchers in `SecurityConfig`.

```java
// Platform-level only
@PreAuthorize("hasRole('PLATFORM_ADMIN')")

// School admin only
@PreAuthorize("hasRole('ADMIN')")

// Staff and above
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
```

`UserType` maps to Spring Security roles — `UserType.ADMIN` → `ROLE_ADMIN`.

---

## Platform Admin Security Model

- No `tenantId` on the `User` record
- `AccessScope.PLATFORM`
- Endpoints at `/api/platform/**`
- Protected by both `PlatformKeyFilter` (API key) AND `@PreAuthorize("hasRole('PLATFORM_ADMIN')")`
- Cannot access school-scoped endpoints
- `TenantFilter` skips tenant resolution for this user type

---

## K-DPA Compliance Checklist

Kenya Data Protection Act requirements that affect implementation:

- **Data minimisation** — only collect and return fields that are needed. DTOs must not expose full entity fields blindly.
- **Consent for minors** — AI-generated insights about students require parental consent flag check before processing.
- **Audit trail** — sensitive operations (student data access, credential issuance, status changes) must write to `AuditLog`.
- **Audit log is append-only** — never update or delete audit records.
- **Identifiers** — admission numbers must be unguessable (Luhn-generated random, not sequential).
- **PII in logs** — never log names, emails, phone numbers, admission numbers at INFO or above.

---

## Admission Numbers as Credentials

Students and staff use their identifier (admission/employee number) as both username and default password. This means identifiers must be:

- **Random, not sequential** — sequential numbers are enumerable (attacker guesses 1001, 1002, 1003...)
- **Validated with Luhn check digit** — catches typos, not a security feature
- **Unique per tenant** — must be unique within the school

`IdentifierService` handles generation and validation. `StudentServiceImpl` and `StaffServiceImpl` call it when the school's `useSystemAdmissionNumbers` flag is true.

---

## First Login — Force Password Reset

All system-created accounts have `mustChangePassword = true`. On login:

1. Credentials valid → return `LoginResponse { passwordChangeRequired: true, changeToken: "..." }`
2. `changeToken` is short-lived (15 min), stored in Redis (fallback: DB table)
3. Access token returned at this point has restricted claims — can only call `POST /api/auth/reset-required`
4. Calling any other endpoint with this restricted token → 403
5. After successful reset → `mustChangePassword = false`, issue real access token

---

## Bulk Import Security Surface

CSV uploads are an attack surface. Before processing any row:

1. File size limit: 5MB max
2. Content-type: byte inspection, not just MIME
3. Row count limit: 500 rows per upload
4. Each row validated against same rules as single-create endpoint
5. Cross-tenant class assignment attempts → `ResourceNotFoundException`, row marked as error
6. Parameterised queries via Hibernate — no SQL injection possible

---

## Threat Model Summary

| Threat | Defence |
|---|---|
| Cross-tenant data leak | TenantFilter + tenant-scoped all repository queries |
| Privilege escalation | JWT signature validation + `@PreAuthorize` against DB-loaded User entity |
| Unauthorized school provisioning | PlatformKeyFilter + PLATFORM_ADMIN JWT (SM1+) |
| Brute force login | Bucket4j rate limiting — 5 attempts/min/IP, returns 429 with Retry-After |
| Session hijacking via refresh token | SHA-256 hash stored only, revoke sets `revoked=true` immediately |
| K-DPA violation | Data minimisation in DTOs, AuditLog, parental consent checks |
| Timing attack on API key | Constant-time XOR comparison in PlatformKeyFilter |

### Source: .gemini/skills/psyllabus/references/oop-solid-patterns.md

# OOP, SOLID & Design Patterns — Psyllabus

## Core Principle

Patterns are evaluated silently before every implementation decision.
A pattern is only surfaced in a response when:
- It is actively being used in the current implementation, **or**
- It is being suggested with a specific, concrete reason for this codebase

Patterns are never used for their own sake. Complexity must be justified.

---

## SOLID in This Codebase

### Single Responsibility
Each class does one thing. The split is:
- `Controller` → HTTP handling only (delegate everything to service)
- `ServiceImpl` → business logic only (no HTTP context, no repository queries outside of what the service owns)
- `Repository` → data access only
- `Filter` → one security concern per filter (one file = one job)

### Open/Closed
Enums like `UserType`, `AccessScope`, `PlanType` are extended by adding values — existing code that switches on them is not modified, it is extended with new cases.

### Liskov Substitution
`StudentServiceImpl implements StudentService` — anywhere `StudentService` is injected, the impl can be swapped without callers noticing (e.g. for testing with a mock).

### Interface Segregation
Service interfaces only declare what callers actually need. `UserService` is not bloated with admin operations that only `AdminService` needs.

### Dependency Inversion
Controllers depend on `StudentService` (interface), not `StudentServiceImpl` (concrete). Spring injects the impl at runtime.

---

## Patterns Actively Used

### Template Method — `BaseController`
`BaseController` defines the invariant skeleton:
```
get current user → get tenant → call service → wrap response
```
Concrete controllers fill in only the service call step. They never repeat the boilerplate.

Used by: every controller that extends `BaseController`.

### Builder — Entities and ErrorResponse
Lombok `@Builder` on all JPA entities and `ErrorResponse`. Used heavily for object construction in service layer and tests.

`ErrorResponse` is a class (not record) specifically to support the builder pattern with mutability.

### Facade — Controllers
Controllers are facades. They present a simplified interface to the HTTP layer, hiding all internal complexity (tenant resolution, caching, transaction scope). No business logic in controllers — ever.

### Strategy — Environment-Aware Config
`CorsConfig.getAllowedOrigins()` and `SecurityConfig` switch behaviour based on active Spring profile. The profile is the strategy selector.

---

## Patterns to Be Aware Of (Not Yet Implemented)

### Factory Method — `IdentifierService` (SM4)
`generateStudentId()` and `generateStaffId()` follow a factory-like pattern — same algorithm, different prefix and uniqueness check per type. If a third identifier type is added, the factory structure makes it clean to extend.

### Observer — RabbitMQ Events (SM4)
When a student is created, the creation event is published to RabbitMQ. `EmailNotificationListener` observes and reacts. Publishers don't know about listeners — this is the Observer pattern at the infrastructure level.

### Chain of Responsibility — Filter Chain
`PlatformKeyFilter → TenantFilter → JwtAuthFilter → SubscriptionInterceptor` is a chain of responsibility. Each handler either passes the request along or terminates it. Adding a new security concern means adding a new link, not modifying existing ones.

---

## Base Classes and Inheritance Guidelines

Use a base class when:
- Multiple concrete classes share identical boilerplate that would otherwise be copy-pasted
- The shared logic is truly invariant (doesn't vary by subclass)

Example: `BaseController` — invariant: get user, get tenant, wrap response.

Use an interface when:
- The contract must be defined but implementation varies
- You want to swap implementations (e.g. for testing)

Example: `StudentService` interface → `StudentServiceImpl` in prod, mock in tests.

Use `abstract` when:
- Some methods are invariant (provide a default implementation)
- Some methods must be overridden (declare as `abstract`)

Do not extend for code reuse alone. Prefer composition when the relationship is not genuinely "is-a".

---

## What to Evaluate Silently Before Implementing

Before writing any non-trivial class, check:

1. Is there already a base class this should extend?
2. Is there an interface this should implement rather than a new one being created?
3. Does this class have one clear responsibility?
4. Would a future developer opening this file understand its purpose in under 30 seconds?
5. Am I about to copy-paste logic that already exists in another class?

If the answer to 5 is yes → extract to a shared helper or base class first.

---

## Anti-Patterns to Avoid

| Anti-pattern | What it looks like | Fix |
|---|---|---|
| Fat controller | Business logic in controller method | Move to service |
| Tenant leak | `findById()` without `AndTenantId` | Always scope to tenant |
| God service | One service does everything | Split by responsibility |
| Anemic model | Entity is just a bag of fields with no behaviour | Add domain methods where appropriate |
| Premature abstraction | Interface with only one implementation that will never be swapped | Just use the class directly |
| Magic strings | `"ADMIN"`, `"BRANCH"` scattered in conditionals | Use enums |

### Source: .gemini/skills/psyllabus/references/psyllabus-core.md

# Psyllabus Core — Non-Negotiables & Project Rules

## What This Project Is

Multi-tenant school management SaaS. Kenya-based. Three subscription tiers: Basic, Professional, Enterprise.
Every school is a tenant. Every tenant is isolated. Data leaks between tenants = critical failure.

---

## Non-Negotiables

These rules are never overridden. Flag any user request that violates them.

1. **Tenant scope on every query** — `findByIdAndTenantId(...)`, never bare `findById(...)` for school data.
2. **TenantContext is the only source of truth** for `tenantId` — never from the request body or JWT alone.
3. **Controllers are facades** — no business logic, no repository calls, no tenant resolution.
4. **MODE prefix governs response style** — if no MODE is given, infer from context or ask once.
5. **K-DPA compliance** — data minimisation in DTOs, audit log for sensitive ops, never log PII.
6. **Package by feature** — every new class goes into the right feature package; `common/` for shared code only.

---

## Subscription Tiers

| Tier | Who it's for | Key differentiator |
|---|---|---|
| Basic | Small schools (50–200 students) | Digitise ops, static reports, no AI |
| Professional | Medium schools (200–800 students) | AI diagnostics for teachers + parents |
| Enterprise | Large / multi-campus (800+ students) | Longitudinal AI, multi-campus dashboards, K-DPA compliance reports |

All tiers include all stakeholders. What changes is depth of analytics and AI access.

---

## Stakeholder Differentiation

Every feature and AI output must be scoped by stakeholder:

- **Institution / Admin** — governance, compliance, system-wide trends, audit
- **Staff (teachers)** — classroom management, diagnostics, workload reduction
- **Parents** — simple motivational insights, transparency, opt-in AI controls
- **Students** — class management, assignments, grades visibility

---

## School Onboarding Flow (4 Phases)

```
Phase 1: AGREEMENT   → Platform Admin provisions school + hands ADMIN credentials
Phase 2: SETUP       → School Admin runs setup wizard (profile, curriculum, branches, co-admins)
Phase 3: SYNC        → Staff upload Excel templates → validate → RabbitMQ jobs create all accounts
Phase 4: ACCESS      → All users first login → forced password reset → dashboard loads
```

RabbitMQ job order during sync (strict — dependencies matter):
1. Academic structure
2. Staff accounts
3. Guardian accounts
4. Student accounts
5. Historical data (optional)

---

## Milestone Overview (SM0–SM6)

| Milestone | What it delivers |
|---|---|
| SM0 | PlatformKeyFilter — API key guard on provisioning endpoint |
| SM1 | PLATFORM_ADMIN JWT auth — proper platform admin identity |
| SM2 | School settings (SchoolProfile split from School) |
| SM3 | Academic structure (AcademicYear → Term → Class → TeacherAssignment) |
| SM4 | User identity stack (Student, Staff, Guardian + IdentifierService) |
| SM5 | Subscription feature gating (SubscriptionInterceptor active) |
| SM6 | Bulk import (onboarding Excel templates + RabbitMQ sync engine) |

---

## Key Architectural Decisions

**JWT is lean.** Only identity claims. Subscription and permissions fetched via `GET /api/auth/claims` after login (cached in Redis, 5 min TTL).

**School table is split.** `schools` (lean, cached, read on every request) vs `school_profiles` (loaded on demand, admin settings only).

**Email is async.** All emails published to RabbitMQ `psyllabus.notifications` exchange. Provisioning never waits for SendGrid.

**Bulk import uses per-batch transactions.** Each batch of 50 rows is its own `@Transactional` scope. One bad row doesn't wipe out 399 successful ones. Final status: `COMPLETED_WITH_ERRORS`.

**Admission numbers are Luhn-generated.** Random 7-digit + check digit + type prefix (e.g. `S8492015`). Not sequential — prevents enumeration attacks. Satisfies K-DPA data minimisation on identifiers.

---

## Environment Matrix

| Behaviour | Dev | Staging | Prod |
|---|---|---|---|
| `X-Tenant-ID` header bypass | ✅ Active | ❌ Off | ❌ Off |
| Subscription checking | ❌ Skipped | ✅ Active | ✅ Active |
| Activation emails | Mailhog | SendGrid (test) | SendGrid (live) |
| DataSeeder | ✅ Runs | ❌ Off | ❌ Off |
| Swagger UI | ✅ | ✅ | ❌ Disabled |
| Rate limiting | 100 req/min | 5 req/min | 5 req/min |
| Actuator | All | All | `/health` + `/prometheus` only |

---

## Output Quality Rules

- Do not create useless files (QuickStarts, checklists, migration summaries) unless explicitly asked
- Short and direct by default — 3–6 bullets or 2–3 sentences
- Expand only when a MODE requires it or the user explicitly asks
- Plain language — explain like the user is new to the concept if they're clearly learning
- If showing an opinion or recommendation, state the reason in one short sentence

### Source: .gemini/skills/psyllabus/references/spring-boot-java.md

# Spring Boot & Java — Psyllabus Project Standards

## Stack
- Java 21, Spring Boot 3.x
- PostgreSQL + PGBouncer
- Redis (caching, refresh token store)
- Flyway (migrations)
- Docker / GraalVM native images
- Prometheus + Grafana + Sentry

---

## Package Structure
Package by **feature**, not by layer. Every feature package is self-contained and can grow into a microservice.

```
<feature>/
├── controller/      ← thin HTTP facade, no logic
├── service/         ← interface only
│   └── impl/        ← implementation class
├── entities/        ← JPA entities
├── repository/      ← Spring Data JPA
├── dto/
│   ├── request/     ← Java Records
│   └── responses/   ← Java Records
└── enums/           ← feature-specific enums
```

Common/shared code lives in `common/`:
```
common/
├── config/          ← SecurityConfig, CorsConfig, RedisConfig, etc.
├── controller/      ← BaseController (abstract)
├── dto/             ← ApiResponse, ErrorResponse, PageResponse
├── filter/          ← OncePerRequestFilter implementations
├── interceptor/     ← HandlerInterceptor implementations
├── util/            ← TenantContext, helpers
└── exceptions/      ← GlobalExceptionHandler, typed exceptions
```

---

## Naming Conventions

| Thing | Convention | Example |
|---|---|---|
| Service interface | `FeatureService` | `StudentService` |
| Service implementation | `FeatureServiceImpl` | `StudentServiceImpl` |
| Request DTO | `FeatureActionRequest` | `StudentCreateRequest` |
| Response DTO | `FeatureResponse` | `StudentResponse` |
| Controller | `FeatureController` | `StudentController` |
| Repository | `FeatureRepository` | `StudentRepository` |

---

## DTOs — Always Java Records

Request DTOs use Records with Bean Validation annotations. Response DTOs are Records.
`ErrorResponse` is a class (not record) specifically because it needs mutability via Lombok `@Builder`.

```java
// Request — always a Record with validation
public record StudentCreateRequest(
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    String firstName,

    @NotNull(message = "Date of birth is required")
    @Past
    LocalDate dateOfBirth
) {}

// Response — always a Record
public record StudentResponse(
    UUID id,
    String admissionNumber,
    String firstName,
    String lastName,
    StudentStatus status
) {}
```

---

## Controllers — Always Extend BaseController

Controllers are **facades only**. No business logic, no repository calls, no tenant resolution.
They extract the principal, call the service, and wrap the response using `BaseController` helpers.

```java
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Students", description = "...")
public class StudentController extends BaseController {

    private final StudentService studentService;

    @PostMapping
    @Operation(summary = "Enroll a new student")
    public ResponseEntity<ApiResponse<StudentResponse>> enroll(
            @Valid @RequestBody StudentCreateRequest request,
            Authentication authentication) {
        log.info("Enrol student request: tenant={}", getCurrentTenantId());
        return created(studentService.createStudent(request), "Student enrolled");
    }
}
```

`BaseController` provides: `getCurrentUser(auth)`, `getCurrentTenantId()`, `ok()`, `created()`, `okMessage()`, `paged()`.

---

## Services — Interface + Impl Split

Always define an interface first. The implementation is in `impl/` and `implements` the interface.

```java
// StudentService.java (interface)
public interface StudentService {
    StudentResponse createStudent(StudentCreateRequest request);
    StudentResponse getById(UUID id);
}

// StudentServiceImpl.java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StudentServiceImpl implements StudentService {
    // implementation
}
```

---

## Multi-Tenancy Rules in Services

- Always get `tenantId` from `TenantContext.getCurrentTenantId()`, **never from the request body**.
- Every repository query must be scoped to `tenantId`.
- Validate cross-entity ownership: if a `classId` is passed in, confirm it belongs to the current tenant before using it.

```java
// Always do this
UUID tenantId = TenantContext.getCurrentTenantId();
Student student = studentRepository.findByIdAndTenantId(id, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

// Never do this
Student student = studentRepository.findById(id).orElseThrow(...); // ← no tenant scope = data leak
```

---

## Transactions

- Annotate `@Transactional` at **class level** on `ServiceImpl` classes.
- Keep transactions **short** — PGBouncer uses transaction-mode pooling.
- Long-running operations (bulk import) are processed in small batches, each in its own transaction scope.
- Use `@Transactional(readOnly = true)` on query-only methods.

---

## Database & JPA

- Pagination on all list queries — never unbounded `findAll()`.
- Avoid N+1: use DTO projections or `JOIN FETCH` where appropriate.
- Use `@Version` for optimistic locking on entities that can be concurrently updated.
- Indexes defined on `@Table` annotation, not added later.
- Flyway manages all schema changes — no DDL in entities in prod.

---

## Logging

- `log.info()` — significant business events (user created, school provisioned).
- `log.debug()` — internals, cache hits, filter decisions.
- `log.warn()` — unexpected but recoverable (tenant resolution miss, missing header).
- `log.error()` — exceptions that affect the response.
- Never log passwords, tokens, or raw PII.
- Use MDC for `traceId`, `tenantId`, `userId` in structured logs.

---

## Responses — Always Wrap in ApiResponse

```java
// Success
ApiResponse.success(data, "Operation successful")

// Simple error (in GlobalExceptionHandler)
ApiResponse.error("Resource not found")

// Detailed error (validation failures)
ApiResponse.error("Validation failed", errorDetails)
```

Paginated responses use `PageResponse.from(page)` and are wrapped in `ApiResponse<PageResponse<T>>`.

---

## Environment Awareness

Always use Spring profiles for environment-specific behaviour. Never hardcode environment checks as string literals outside of config classes.

```
dev     → header tenant bypass, skip subscription check, DataSeeder runs, Mailhog, H2 console
staging → real subdomains, real subscription check, no seeder, Swagger on
prod    → no Swagger, no seeder, no header bypass, fail-fast on missing env vars
```

---

## Scan Before Creating

Before writing a new class, check:
1. Is there an empty shell in the relevant package?
2. Is there an interface that just needs an `impl`?
3. Is there a base class this can extend?
4. Does `common/` already have what's needed?
5. Is the delivery order correct? (config → enum → entity → repo → DTO → service interface → impl → controller)

Create new only when nothing fits.