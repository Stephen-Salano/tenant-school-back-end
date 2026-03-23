package io.psyllabus.platform.school.enums;

/**
 * The student admission gender policy of the school.
 * Stored as a column on {@code school_profiles} — used for enrollment validation and UI display.
 */
public enum GenderPolicy {
    /** School admits both male and female students. */
    CO_EDUCATIONAL,

    /** School admits male students only. */
    BOYS_ONLY,

    /** School admits female students only. */
    GIRLS_ONLY
}
