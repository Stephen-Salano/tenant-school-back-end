package io.psyllabus.platform.school.enums;

/**
 * The operational model of the school.
 * Stored as a column on {@code school_profiles} — used for reporting and filtering.
 */
public enum SchoolType {
    /** School where students attend during the day only. */
    PRIVATE_DAY,

    /** Government-funded school. */
    PUBLIC,

    /** School where students live on campus. */
    BOARDING
}
