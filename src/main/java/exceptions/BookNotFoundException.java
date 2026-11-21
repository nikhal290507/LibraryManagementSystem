package exceptions;

/**
 * Custom Unchecked Exception thrown when a requested Book is not found.
 * Demonstrates unchecked exception design (Lectures 10-15).
 */
public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(String message) {
        super(message);
    }

    public BookNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
