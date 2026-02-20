package io.psyllabus.platform.auth.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
/**
 * Implementation of PasswordService using BCrypt.
 * BCrypt automatically handles salting and is resistant to rainbow table attacks.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordServiceImpl implements PasswordService {

    // injected from the SecurityConfig @Bean
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public String hashPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()){
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        log.debug("Hashing password (length: {})", rawPassword.length());
        String hashed = passwordEncoder.encode(rawPassword);
        log.debug("Password hashed successfully");

        return hashed;
    }

    @Override
    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null){
            throw new IllegalArgumentException("Password and hash cannot be null");
        }
        log.debug("Verifying Password");
        boolean matches = passwordEncoder.matches(rawPassword, hashedPassword);
        log.debug("Password verification result: {}", matches);
        return matches;
    }

    @Override
    public boolean isPasswordStrong(String password) {
        if (password == null){
            return false;
        }

        // At least8 characters
        if (password.length() < 8){
            return false;
        }

        // Contains at least one upercase letter
        if (password.chars().noneMatch(Character::isUpperCase)){
            return false;
        }

        return password.chars().anyMatch(Character::isDigit);
    }
}
