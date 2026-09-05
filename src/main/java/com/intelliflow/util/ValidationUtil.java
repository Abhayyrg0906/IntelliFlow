package com.intelliflow.util;

import com.intelliflow.exception.ValidationException;
import java.time.LocalDate;
import java.util.regex.Pattern;

public class ValidationUtil {

    // Standard email matching regex pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    );

    // Password rules: minimum 8 characters, at least 1 uppercase letter, 1 lowercase letter, 1 digit, and 1 special character
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_\\-*?&])(?=\\S+$).{8,}$"
    );

    // Username rules: 3 to 50 alphanumeric characters, underscores, hyphens, and dots
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]{3,50}$");

    private ValidationUtil() {}

    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        return USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        if (password == null) return false;
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    public static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    public static void validateDateRange(LocalDate start, LocalDate end) throws ValidationException {
        if (start == null || end == null) {
            throw new ValidationException("Start date and deadline cannot be null.");
        }
        if (end.isBefore(start)) {
            throw new ValidationException("The deadline date cannot be before the start date.");
        }
    }
}
