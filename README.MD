# School Management System

![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)]()
![IntelliJ IDEA](https://img.shields.io/badge/IntelliJIDEA-000000.svg?style=for-the-badge&logo=intellij-idea&logoColor=white)
![ChatGPT](https://img.shields.io/badge/chatGPT-74aa9c?style=for-the-badge&logo=openai&logoColor=white)
![Neo4J](https://img.shields.io/badge/Neo4j-008CC1?style=for-the-badge&logo=neo4j&logoColor=white)
![Postgres](https://img.shields.io/badge/postgres-%23316192.svg?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/redis-%23DD0031.svg?style=for-the-badge&logo=redis&logoColor=white)
![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![YAML](https://img.shields.io/badge/yaml-%23ffffff.svg?style=for-the-badge&logo=yaml&logoColor=151515)
![Hibernate](https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=Hibernate&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)
![Notion](https://img.shields.io/badge/Notion-%23000000.svg?style=for-the-badge&logo=notion&logoColor=white)
![openapi initiative](https://img.shields.io/badge/openapiinitiative-%23000000.svg?style=for-the-badge&logo=openapiinitiative&logoColor=white)
![Postman](https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white)
![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=Prometheus&logoColor=white)
![Grafana](https://img.shields.io/badge/grafana-%23F46800.svg?style=for-the-badge&logo=grafana&logoColor=white)
![Apache](https://img.shields.io/badge/apache-%23D42029.svg?style=for-the-badge&logo=apache&logoColor=white)
![Nginx](https://img.shields.io/badge/nginx-%23009639.svg?style=for-the-badge&logo=nginx&logoColor=white)
![Sentry](https://img.shields.io/badge/sentry-%23362D59.svg?style=for-the-badge&logo=sentry&logoColor=white)
![Git](https://img.shields.io/badge/git-%23F05033.svg?style=for-the-badge&logo=git&logoColor=white)
![Docusaurus](https://img.shields.io/badge/docusaurus-%233ECC5F.svg?style=for-the-badge&logo=docusaurus&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-black?style=for-the-badge&logo=JSON%20web%20tokens)
![RabbitMQ](https://img.shields.io/badge/Rabbitmq-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)

A comprehensive, multi-tenant SaaS platform for managing Kenyan schools with support for CBC grading, subscription-based access, and AI-powered insights for administrators, teachers, and parents.

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
    - [1. Clone the Repository](#1-clone-the-repository)
    - [2. Environment Setup](#2-environment-setup)
    - [3. Docker Services](#3-docker-services)
    - [4. Database Setup](#4-database-setup)
    - [5. Run the Application](#5-run-the-application)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Configuration](#configuration)
- [Development Workflow](#development-workflow)
- [Testing](#testing)
- [Contributing](#contributing)
- [Troubleshooting](#troubleshooting)
- [Team](#team)
- [License](#license)

---

## Overview

The School Management System is a modern, scalable platform designed specifically for Kenyan schools. It supports:

- **Multi-tenancy**: Each school is an isolated tenant with their own data
- **CBC Grading System**: Kenya's Competency-Based Curriculum with 8 grading levels (EE1-BE2)
- **Subscription-Based Access**: Basic, Professional, and Enterprise plans with feature gating
- **Stakeholder Dashboards**: Separate interfaces for institutions, teachers, and parents
- **AI-Powered Insights**: OpenAI-powered assistants for personalized recommendations

### Target Users
- **School Administrators**: Manage students, staff, academic structure, and analytics
- **Teachers**: Track attendance, enter grades, view class performance
- **Parents**: Monitor child's academic progress, attendance, and receive insights
- **Students**: Indirect beneficiaries through improved teaching and parental support

---

## Key Features

### 🏫 Core School Management
- Multi-branch support for large schools
- Academic year and term management
- Class and subject administration
- Department-based organization

### 👥 User Management
- Student enrollment with admission numbers (no email required)
- Staff management with role assignments (HOD, Class Teacher, etc.)
- Guardian linking with relationship types
- Role-based access control (RBAC)

### 📊 Academic Operations
- CBC Grading System (8-level: EE1, EE2, ME1, ME2, AE1, AE2, BE1, BE2)
- Traditional A-F grading support
- Assessment creation and grade entry
- Teacher-to-class assignments per term
- Daily attendance tracking

### 💰 Financial Management
- Fee invoicing per term
- Payment recording (M-Pesa integration ready)
- Balance tracking and payment history

### 📈 Analytics & Insights
- Institutional dashboard (school-wide performance)
- Department analytics and comparisons
- Teacher dashboards (class performance)
- Parent dashboards (child's progress)
- Auto-flagging struggling students

### 🤖 AI Features (Professional/Enterprise Plans)
- **Teacher AI**: Analyze patterns, suggest interventions
- **Parent AI**: Explain grades, provide study tips
- **Institutional AI**: Strategic insights, trend analysis

### 💬 Communication
- Direct messaging (teacher-parent)
- Announcements and broadcasts
- Email notifications
- SMS alerts (optional)

### 📋 Compliance
- Audit logging (Kenya Data Protection Act)
- JSONB change tracking (old/new values)
- Export to CSV/JSON for compliance

---

## Technology Stack

### Backend
- **Java 21** (LTS)
- **Spring Boot 3.2+** (Web, Security, Data JPA)
- **Spring Security** with JWT authentication
- **Hibernate/JPA** for ORM

### Database
- **PostgreSQL 15+** (primary database)
- **Redis 7+** (caching, session management)
- **Flyway** for database migrations

### API Documentation
- **SpringDoc OpenAPI 3** (formerly Swagger)
- Interactive Swagger UI at `/swagger-ui.html`

### Build Tools
- **Maven 3.9+** or **Gradle 8+**
- Maven Wrapper (`./mvnw`) included

### Containerization
- **Docker** and **Docker Compose**
- Pre-configured services for PostgreSQL, Redis, Prometheus, Grafana

### Monitoring (Optional)
- **Prometheus** for metrics collection
- **Grafana** for visualization dashboards

### External Services
- **OpenAI GPT-4o API** for AI features
- **Africa's Talking** for SMS 
- **SendGrid** for email notifications

---

## Project Structure
```
school-management-system/
├── src/
│   ├── main/
│   │   ├── java/io/psyllabus/platform/
│   │   │   ├── academic/           # Academic years, terms, classes, subjects
│   │   │   ├── activity/           # Clubs, sports, competitions
│   │   │   ├── ai/                 # AI assistants (Teacher, Parent, Institutional)
│   │   │   ├── analytics/          # Performance dashboards, flags
│   │   │   ├── assessment/         # Exams, quizzes, grade entry
│   │   │   ├── attendance/         # Daily attendance tracking
│   │   │   ├── audit/              # Audit logging, change tracking
│   │   │   ├── auth/               # Authentication, JWT, user management
│   │   │   ├── branch/             # Multi-campus support, transfers
│   │   │   ├── common/             # Shared utilities, exceptions, DTOs
│   │   │   ├── department/         # Department management
│   │   │   ├── grading/            # CBC/Traditional grading systems
│   │   │   ├── guardian/           # Parent/guardian management
│   │   │   ├── messaging/          # Communication, notifications
│   │   │   ├── payments/           # Fee management, payment tracking
│   │   │   ├── specialclass/       # Optional paid classes (Chinese, coding)
│   │   │   ├── staff/              # Teachers, staff roles
│   │   │   ├── student/            # Student enrollment, roles
│   │   │   ├── subscription/       # Plan-based feature gating
│   │   │   ├── teacherassignment/  # Teacher scheduling
│   │   │   └── tenant/             # School (tenant) management
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       └── db/migration/       # Flyway migration scripts
│   └── test/                       # Unit and integration tests
├── docker/
│   ├── docker-compose.yml          # All services (PostgreSQL, Redis, Prometheus, Grafana)
│   ├── docker-compose.dev.yml      # Development overrides
│   ├── postgres/
│   │   └── init.sql                # Initial database setup
│   ├── prometheus/
│   │   └── prometheus.yml
│   └── grafana/
│       └── dashboards/
├── docs/
│   ├── api/                        # API documentation
│   ├── database/
│   │   ├── schema.dbml             # Database schema (DBML format)
│   │   └── er-diagram.png          # ER diagram
│   ├── architecture/               # Architecture decisions
│   └── implementation-roadmap.pdf  # 6-month implementation plan
├── scripts/
│   ├── setup-local-env.sh          # Local environment setup
│   ├── seed-demo-data.sh           # Seed demo schools
│   └── backup-db.sh                # Database backup script
├── .github/
│   └── workflows/
│       ├── ci.yml                  # Continuous Integration
│       └── code-quality.yml        # Code quality checks
├── .gitignore
├── .editorconfig
├── docker-compose.yml
├── Makefile                        # Common commands
├── mvnw / mvnw.cmd                 # Maven wrapper
├── pom.xml                         # Maven dependencies
├── README.md                       # This file
└── CONTRIBUTING.md                 # Contribution guidelines
```

### Package Organization

The project follows a **feature-based package structure** (not layered). Each package contains:
```
<package>/
├── entity/          # JPA entities
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic
├── controller/      # REST controllers
├── dto/
│   ├── request/     # Request DTOs
│   └── response/    # Response DTOs
├── enums/           # Enums specific to this feature
└── (strategy/       # Strategy pattern implementations, if needed)
```

---

## Prerequisites

Before you begin, ensure you have the following installed:

| Tool | Version | Purpose |
|------|---------|---------|
| **Java JDK** | 21 (LTS) | Runtime environment |
| **Maven** | 3.9+ | Build tool (or use `./mvnw`) |
| **Docker** | 24+ | Container runtime |
| **Docker Compose** | V2 (2.x) | Multi-container orchestration |
| **Git** | 2.x | Version control |
| **IDE** | IntelliJ IDEA / VS Code | Development environment |
| **Postman** (optional) | Latest | API testing |

### Verify Installation
```bash
java -version        # Should show Java 21
mvn -version         # Should show Maven 3.9+
docker --version     # Should show Docker 24+
docker compose version  # Should show V2 (not V1)
```

---

## Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/your-org/school-management-system.git
cd school-management-system
```

### 2. Environment Setup

#### A. Copy Environment Template
```bash
cp .env.example .env
```

#### B. Configure Environment Variables

Edit `.env` file with your local settings:
```properties
# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=school_mgmt_db
POSTGRES_USER=school_admin
POSTGRES_PASSWORD=change_me_in_production

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=your-256-bit-secret-key-change-in-production
JWT_EXPIRATION_MS=86400000

# OpenAI (for AI features)
OPENAI_API_KEY=sk-your-openai-api-key

# SendGrid (for email notifications)
SENDGRID_API_KEY=SG.your-sendgrid-api-key
SENDGRID_FROM_EMAIL=noreply@yourschool.com

# Africa's Talking (for SMS - optional)
AFRICASTALKING_USERNAME=sandbox
AFRICASTALKING_API_KEY=your-api-key

# Application
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080
```

### 3. Docker Services

Start all required services (PostgreSQL, Redis, Prometheus, Grafana):
```bash
docker compose up -d
```

**Services will be available at:**
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (admin/admin)

**Check service health:**
```bash
docker compose ps
```

**View logs:**
```bash
docker compose logs -f postgres
docker compose logs -f redis
```

**Stop services:**
```bash
docker compose down
```

**Stop and remove volumes (⚠️ deletes data):**
```bash
docker compose down -v
```

### 4. Database Setup

#### A. Automatic Setup (Recommended)

Flyway migrations run automatically on application startup. The database schema will be created from `src/main/resources/db/migration/` scripts.

#### B. Manual Setup (Optional)

If you need to manually initialize the database:
```bash
# Connect to PostgreSQL
docker exec -it school-mgmt-postgres psql -U school_admin -d school_mgmt_db

# Check tables
\dt

# Exit
\q
```

#### C. Seed Demo Data

To populate the database with demo schools and test data:
```bash
./scripts/seed-demo-data.sh
```

This creates:
- 2 demo schools (Basic and Professional plans)
- 150 students per school
- 10 teachers per school
- 3 academic terms with grades and attendance

### 5. Run the Application

#### Option A: Using Maven Wrapper (Recommended)
```bash
./mvnw spring-boot:run
```

#### Option B: Using Your IDE

1. Import the project as a Maven project
2. Set Java 21 as the project SDK
3. Run `PsyllabusPlatformApiApplication.java`

#### Option C: Build and Run JAR
```bash
# Build
./mvnw clean package -DskipTests

# Run
java -jar target/school-management-system-1.0.0.jar
```

#### Verify Application Started
```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

---

## API Documentation

### Swagger UI (Interactive)

Once the application is running, access the Swagger UI:
```
http://localhost:8080/swagger-ui.html
```

### Authentication in Swagger

The API uses JWT Bearer authentication. To test secured endpoints in Swagger:

1. **Register a school and user** (or use demo credentials)
2. **Login** via `POST /api/auth/login` to get JWT token
3. **Click "Authorize" button** (top right in Swagger UI)
4. **Enter**: `Bearer <your-jwt-token>` (replace `<your-jwt-token>`)
5. **Click "Authorize"** - now you can test secured endpoints

### Demo Credentials

If you ran the seed script:
```
Admin User:
Email: admin@demo-school.com
Password: Admin@123

Teacher:
Email: teacher@demo-school.com
Password: Teacher@123

Parent:
Email: parent@demo-school.com
Password: Parent@123
```

### OpenAPI JSON

Raw OpenAPI specification available at:
```
http://localhost:8080/v3/api-docs
```

### Security Configuration

Swagger is **publicly accessible** in local development for easy testing.

For production, you can:
- **Disable Swagger entirely**: Set `springdoc.swagger-ui.enabled=false` in `application-prod.properties`
- **Password-protect it**: Configure Spring Security basic auth for Swagger endpoints

---

## Database Schema

### Overview

The system uses 36 tables organized into logical groups:

| Group | Tables | Description |
|-------|--------|-------------|
| **Core Multi-tenancy** | 3 | schools, subscriptions, users |
| **Academic Structure** | 5 | academic_years, terms, classes, subjects, departments |
| **User Management** | 9 | students, staff, guardians, role types/assignments |
| **Grading** | 4 | grading_schemes, grading_levels, assessments, grades |
| **Operations** | 4 | attendance_records, teacher_assignments, payments |
| **Enhancement** | 7 | activities, special_classes, ai_interactions, messages |
| **Compliance** | 4 | audit_logs, branches, branch_transfers |

### Schema Documentation

Full schema with relationships available in:
```
docs/database/schema.dbml
docs/database/er-diagram.png
```

### Key Design Decisions

1. **Multi-tenancy**: `tenant_id` on every table for data isolation
2. **JSONB Fields**: Used for flexible data (subscription features, audit changes)
3. **CBC Grading**: Separate `grading_schemes` and `grading_levels` tables for flexibility
4. **Strategy Pattern**: `grading_schemes.grading_system` determines which strategy to use
5. **Audit Logging**: `old_values` and `new_values` as JSONB for any entity type

### Sample Queries

**Get school's student count:**
```sql
SELECT COUNT(*) FROM students WHERE tenant_id = ?
```

**Get student's grades for a term:**
```sql
SELECT s.name, sub.name, a.title, g.score, gl.level_name, gl.points
FROM grades g
JOIN assessments a ON g.assessment_id = a.id
JOIN subjects sub ON a.subject_id = sub.id
JOIN students s ON g.student_id = s.id
JOIN grading_levels gl ON g.grade_level_id = gl.id
WHERE s.id = ? AND a.academic_term_id = ?
ORDER BY sub.name
```

---

## Configuration

### Application Profiles

The application supports multiple profiles:

| Profile | Purpose | Active When |
|---------|---------|-------------|
| `dev` | Local development | `SPRING_PROFILES_ACTIVE=dev` |
| `test` | Integration tests | During test execution |
| `prod` | Production deployment | `SPRING_PROFILES_ACTIVE=prod` |

### Key Configuration Properties

#### `application.properties` (Common)
```properties
# Application
spring.application.name=School Management System
server.port=8080

# Database (overridden by profiles)
spring.datasource.url=jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}
spring.datasource.username=${POSTGRES_USER}
spring.datasource.password=${POSTGRES_PASSWORD}

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# Flyway
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true

# Redis
spring.data.redis.host=${REDIS_HOST}
spring.data.redis.port=${REDIS_PORT}

# JWT
jwt.secret=${JWT_SECRET}
jwt.expiration=${JWT_EXPIRATION_MS}

# OpenAI
openai.api.key=${OPENAI_API_KEY}
openai.model=gpt-4o

# Swagger
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operationsSorter=method
```

#### `application-dev.properties` (Development)
```properties
# Enable debug logging
logging.level.io.psyllabus.platform=DEBUG
logging.level.org.hibernate.SQL=DEBUG

# Show SQL with values
spring.jpa.show-sql=true

# Hot reload
spring.devtools.restart.enabled=true

# Swagger enabled
springdoc.swagger-ui.enabled=true
```

#### `application-prod.properties` (Production)
```properties
# Disable debug
logging.level.io.psyllabus.platform=INFO
spring.jpa.show-sql=false

# Security
jwt.expiration=3600000

# Swagger disabled (or secured)
springdoc.swagger-ui.enabled=false
```

---

## Development Workflow

### Branch Strategy

We follow **Git Flow**:

- `main`: Production-ready code
- `develop`: Integration branch for features
- `feature/*`: New features (e.g., `feature/cbc-grading`)
- `bugfix/*`: Bug fixes
- `hotfix/*`: Critical production fixes

### Creating a Feature
```bash
# Start from develop
git checkout develop
git pull origin develop

# Create feature branch
git checkout -b feature/your-feature-name

# Make changes, commit
git add .
git commit -m "feat: add your feature description"

# Push and create PR
git push origin feature/your-feature-name
```

### Commit Message Convention

We follow **Conventional Commits**:
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting)
- `refactor`: Code refactoring
- `test`: Adding/updating tests
- `chore`: Build/tooling changes

**Examples:**
```
feat(grading): implement CBC grading strategy

Added CbcGradingStrategy with 8 levels (EE1-BE2).
Strategy calculates grade level based on score ranges.

Closes #42
```
```
fix(auth): resolve JWT token expiration issue

Fixed bug where tokens expired 1 hour earlier than configured.
Changed expiration calculation to use milliseconds.

Fixes #58
```

---

## Testing

### Running Tests
```bash
# All tests
./mvnw test

# Specific test class
./mvnw test -Dtest=AuthServiceTest

# With coverage
./mvnw test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Test Structure
```
src/test/java/io/psyllabus/platform/
├── auth/
│   ├── AuthServiceTest.java
│   ├── JwtServiceTest.java
│   └── AuthControllerIntegrationTest.java
├── student/
│   ├── StudentServiceTest.java
│   └── StudentControllerIntegrationTest.java
└── ...
```

### Test Categories

- **Unit Tests**: Test individual methods in isolation (mocked dependencies)
- **Integration Tests**: Test multiple components together (with database)
- **E2E Tests**: Test full user workflows (future)

### Writing Tests
```java
@SpringBootTest
@AutoConfigureMockMvc
class StudentControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldEnrollStudent() throws Exception {
        mockMvc.perform(post("/api/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"John\",\"lastName\":\"Doe\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.admissionNumber").exists());
    }
}
```

---

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

### Quick Start for Contributors

1. **Fork the repository**
2. **Clone your fork**
3. **Create a feature branch** from `develop`
4. **Make your changes**
5. **Write/update tests**
6. **Ensure all tests pass**: `./mvnw test`
7. **Commit with conventional commit messages**
8. **Push to your fork**
9. **Create a Pull Request** to `develop` branch

### Code Style

- Use **Google Java Style Guide**
- Configure your IDE with `.editorconfig`
- Run `./mvnw spotless:apply` before committing
- Maximum line length: 120 characters

### PR Checklist

- [ ] Tests added/updated
- [ ] All tests passing
- [ ] Code follows style guide
- [ ] Commit messages follow convention
- [ ] Documentation updated (if needed)
- [ ] No merge conflicts with `develop`

---

## Troubleshooting

### Docker Issues

**Problem**: `Cannot connect to Docker daemon`

**Solution**:
```bash
sudo systemctl start docker
sudo usermod -aG docker $USER
# Log out and log back in
```

**Problem**: `Port 5432 already in use`

**Solution**:
```bash
# Check what's using the port
sudo lsof -i :5432

# Stop local PostgreSQL
sudo systemctl stop postgresql

# Or change port in docker-compose.yml
```

### Database Issues

**Problem**: `Flyway validation failed`

**Solution**:
```bash
# Reset database
docker compose down -v
docker compose up -d postgres
./mvnw flyway:clean flyway:migrate
```

**Problem**: `Connection refused to PostgreSQL`

**Solution**:
```bash
# Check PostgreSQL is running
docker compose ps

# Check logs
docker compose logs postgres

# Wait for PostgreSQL to be ready (30 seconds)
```

### Application Issues

**Problem**: `JWT token invalid or expired`

**Solution**:
- Ensure `JWT_SECRET` is set correctly
- Check token hasn't expired (24 hours)
- Login again to get new token

**Problem**: `Redis connection timeout`

**Solution**:
```bash
# Check Redis is running
docker compose ps redis

# Test Redis connection
docker exec -it school-mgmt-redis redis-cli ping
# Should return: PONG
```

**Problem**: `OpenAI API rate limit exceeded`

**Solution**:
- Check your OpenAI account quota
- Implement rate limiting on AI endpoints
- Cache common AI responses in Redis

### Common Errors

| Error | Cause | Solution |
|-------|-------|----------|
| `Table 'schools' doesn't exist` | Flyway not run | Check Flyway is enabled, restart app |
| `Tenant not found` | Missing `tenant_id` in JWT | Ensure JWT contains `tenant_id` claim |
| `Feature not available` | Subscription limitation | Upgrade school's subscription plan |
| `401 Unauthorized` | Invalid/missing JWT | Login to get valid token, add to headers |

---

## Team

- **Lead Developer Backend**: [Stephen Salano]
- **Lead Developer Frontend**: [Jeff James]
- **Business Lead**: [Nur Khadija]

---

## License

Copyright © 2026 [Name to Be set]. All rights reserved.

This project is proprietary software. Unauthorized copying, distribution, or modification is strictly prohibited.

---

## Additional Resources

- [Implementation Roadmap](docs/implementation-roadmap.pdf) - 6-month development plan
- [Architecture Decisions](docs/architecture/ADR.md) - Key design decisions
- [API Examples](docs/api/examples.md) - Sample API requests
- [Deployment Guide](docs/deployment/DEPLOY.md) - Production deployment steps

---

## Changelog

See [CHANGELOG.md](CHANGELOG.MD) for version history and release notes.

---

**Happy Coding! 🚀**