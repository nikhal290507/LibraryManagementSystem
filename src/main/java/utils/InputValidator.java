package utils;

import exceptions.InvalidInputException;

import java.util.regex.Pattern;

/**
 * Utility class for validating user input (Lectures 10-15 Exception Handling & Validation).
 * Enforces business rules and throws InvalidInputException on violations.
 */
public class InputValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[0-9]{7,15}$"
    );

    // Matches standard ISBN-10 or ISBN-13 with optional hyphens
    private static final Pattern ISBN_PATTERN = Pattern.compile(
            "^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$"
    );

    public static String validateNonEmpty(String input, String fieldName) {
        if (input == null || input.trim().isEmpty()) {
            throw new InvalidInputException(fieldName + " cannot be empty or contain only whitespace.");
        }
        return input.trim();
    }

    public static String validateEmail(String email) {
        String cleanEmail = validateNonEmpty(email, "Email");
        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            throw new InvalidInputException("Invalid email format: '" + cleanEmail + "'. Expected format: user@domain.com");
        }
        return cleanEmail.toLowerCase();
    }

    public static String validatePhone(String phone) {
        String cleanPhone = validateNonEmpty(phone, "Phone number").replaceAll("[\\s-]", "");
        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            throw new InvalidInputException("Invalid phone number: '" + phone + "'. Must contain 7 to 15 digits.");
        }
        return cleanPhone;
    }

    public static String validateISBN(String isbn) {
        String cleanIsbn = validateNonEmpty(isbn, "ISBN").replaceAll("[^0-9X-]", "");
        if (cleanIsbn.length() < 10) {
            throw new InvalidInputException("Invalid ISBN: '" + isbn + "'. Must be at least 10 characters.");
        }
        return cleanIsbn;
    }

    public static int validatePositiveInt(int value, String fieldName) {
        if (value <= 0) {
            throw new InvalidInputException(fieldName + " must be a positive integer greater than zero. Received: " + value);
        }
        return value;
    }

    public static int validateNonNegativeInt(int value, String fieldName) {
        if (value < 0) {
            throw new InvalidInputException(fieldName + " cannot be negative. Received: " + value);
        }
        return value;
    }

    public static double validateNonNegativeDouble(double value, String fieldName) {
        if (value < 0.0) {
            throw new InvalidInputException(fieldName + " cannot be negative. Received: " + value);
        }
        return value;
    }

    public static int validateYear(int year) {
        int currentYear = java.time.Year.now().getValue();
        if (year < 1000 || year > currentYear + 1) {
            throw new InvalidInputException("Publication year must be between 1000 and " + (currentYear + 1) + ". Received: " + year);
        }
        return year;
    }
}
