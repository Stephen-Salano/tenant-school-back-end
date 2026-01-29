package io.psyllabus.platform.auth.services;

/**
 * Service for password hashing and verification.
 * Uses BCrypt for secure password storage.
 */
public interface PasswordService {

    /**
     * Hash a raw password using BCrypt
     * @param rawPassword the plain-text password
     * @return the hashed password (Bcrypt format)
     * @throws IllegalArgumentException if password is null or empty
     */
    String hashPassword(String rawPassword);

    /**
     * Verify a raw password against a hashed password
     * @param rawPassword the plain text password
     * @param hashedPassword the stored hashed password
     * @return true if passwords match, false otherwise
     * @throws IllegalArgumentException if either parameter is null
     */
    boolean verifyPassword(String rawPassword, String hashedPassword);

    /**
     * Check if a password meets minimum security requirements.
     * Requirements:
     * - At least 8 characters
     * - Contains at least one uppercase letter
     * - Contains at least one lowercase letter
     * - Contains at least one digit
     *
     * @param password the password to validate
     * @return true if password is strong enough, false otherwise
     */
    boolean isPasswordStrong(String password);

}
