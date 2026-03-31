package io.psyllabus.platform.audit.enums;

/**
 * Categorized actions for the audit trail.
 * Aligns with K-DPA requirements for tracking sensitive operations.
 */
public enum AuditAction {
    // Infrastructure & Auth
    CREATE, UPDATE, DELETE, LOGIN, LOGOUT, ACCESS_DENIED,
    
    // SM3: Academic Structure
    ACADEMIC_SETUP, // For creating Years, Terms, Classes, Subjects
    
    // SM4+: Student & Staff
    ENROLLMENT,
    EMPLOYMENT_CHANGE,
    
    // SM7: Payments
    PAYMENT_REVERSAL
}
