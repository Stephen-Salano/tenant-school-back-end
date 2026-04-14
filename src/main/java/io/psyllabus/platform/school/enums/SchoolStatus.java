package io.psyllabus.platform.school.enums;

/**
 * Represents the lifecycle status of a school tenant.
 */
public enum SchoolStatus {
    /** School is active and users can log in. */
    ACTIVE,

    /** School is temporarily suspended; users are blocked. */
    SUSPENDED,

    /** School is permanently deactivated. */
    DEACTIVATED
}
