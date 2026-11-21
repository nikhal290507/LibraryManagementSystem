package exceptions;

/**
 * Custom Unchecked Exception thrown when attempting to add or update a book with an existing ISBN.
 */
public class DuplicateISBNException extends RuntimeException {
    public DuplicateISBNException(String message) {
        super(message);
    }

    public DuplicateISBNException(String message, Throwable cause) {
        super(message, cause);
    }
}
