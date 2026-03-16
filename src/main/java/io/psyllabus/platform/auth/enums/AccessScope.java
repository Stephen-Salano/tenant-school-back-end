package io.psyllabus.platform.auth.enums;
/**
 * Access scope — determines how much data a user can see.
 * Ordered narrowest to widest: BRANCH → SCHOOL → SYSTEM → PLATFORM.
 * PLATFORM is reserved for {@code PLATFORM_ADMIN} users only.
 */
public enum AccessScope {
    BRANCH, SCHOOL, SYSTEM, PLATFORM
}
