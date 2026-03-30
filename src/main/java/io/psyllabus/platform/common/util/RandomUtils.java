package io.psyllabus.platform.common.util;

import lombok.experimental.UtilityClass;

import java.security.SecureRandom;

/**
 * Utility class for generating random values.
 * Uses a single SecureRandom instance for efficiency and better randomness.
 */
@UtilityClass
public class RandomUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * Generates a random alphanumeric string of the specified length.
     *
     * @param length the length of the string to generate
     * @return a random string
     */
    public static String generateRandomAlphanumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
