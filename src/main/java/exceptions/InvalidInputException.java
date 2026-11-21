package exceptions;

/**
 * Custom Unchecked Exception thrown when user input fails business rule validation.
 */
public class InvalidInputException extends RuntimeException {
    public InvalidInputException(String message) {
        super(message);
    }

    public InvalidInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
