package com.intelliflow.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    private PasswordUtil() {}

    /**
     * Generates a BCrypt hash of a plain text password.
     *
     * @param plainTextPassword Password in plain text
     * @return Hashed password string
     */
    public static String hash(String plainTextPassword) {
        if (plainTextPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(10));
    }

    /**
     * Verifies a plain text password against a stored BCrypt hash.
     *
     * @param plainTextPassword Plain text password to check
     * @param hashedPassword Hashed password signature stored in database
     * @return true if matches, false otherwise
     */
    public static boolean verify(String plainTextPassword, String hashedPassword) {
        if (plainTextPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainTextPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}
