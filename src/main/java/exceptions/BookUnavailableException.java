package exceptions;

/**
 * Custom Unchecked Exception thrown when all copies of a book are already issued.
 */
public class BookUnavailableException extends RuntimeException {
    public BookUnavailableException(String message) {
        super(message);
    }

    public BookUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
