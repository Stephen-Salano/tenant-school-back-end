# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- **Security**: Implemented `PlatformKeyFilter` to protect critical endpoints (`/api/schools/onboard`, `/api/platform/**`) using `X-Platform-Key`.
- **Security**: Added timing-safe comparison for API keys to prevent side-channel attacks.
- **Auth**: Comprehensive authentication flow with Refresh Tokens (rotation, hashing, and database storage).
- **Auth**: `RefreshToken` entity and repository for persistent session management.
- **Auth**: Timezone configuration for JWT tokens to ensure consistency across environments.
- **Infra**: Implementation of User and School controllers for platform administration.
- **Academic**: Initial implementation of `AcademicController` for academic year and term management.
- **Guardian**: Added `LinkGuardianRequest` for linking students to guardians.

### Fixed
- **Auth**: Aligned dashboard URLs with frontend route structures to resolve navigation issues.
- **Auth**: Prevented duplicate sessions by handling repeated login attempts correctly.
- **Auth**: Resolved `TenantContext` null pointer issues during authentication.
- **API**: Fixed various endpoint errors and improved error response consistency.

### Changed
- **Dependencies**: Upgraded to Spring Boot 3.5.9 and JJWT 0.13.0.
- **Security**: Tightened JWT validation including minimum key length and environment checks.

---

## [1.0.0] - 2026-06-30

### 🎉 Initial Release - Production Launch



---

## Release Notes

### Version 1.0.0 Highlights

---

## Versioning Strategy

This project uses [Semantic Versioning](https://semver.org/):

- **MAJOR** version (X.0.0): Incompatible API changes
- **MINOR** version (0.X.0): New features, backwards-compatible
- **PATCH** version (0.0.X): Backwards-compatible bug fixes

### Version Naming Convention
- **Unreleased**: Changes in `develop` branch not yet released
- **X.Y.Z-alpha**: Early testing, may have bugs
- **X.Y.Z-beta**: Feature-complete, testing for bugs
- **X.Y.Z-rc.N**: Release candidate, final testing
- **X.Y.Z**: Stable release

---

## Change Categories

Each release documents changes in these categories:

- **Added**: New features
- **Changed**: Changes in existing functionality
- **Deprecated**: Soon-to-be removed features
- **Removed**: Removed features
- **Fixed**: Bug fixes
- **Security**: Security improvements

---

## Roadmap


---

## Support

For questions about releases or to report issues:

---

## Contributors

Thanks to all contributors who made this release possible:

- [Stephen Salano] - Lead Backend Developer
- [Jeff James] - Lead Frontend Developer
- [Khadija Nur] - Business Lead

---

**Last Updated**: 2026-01-20