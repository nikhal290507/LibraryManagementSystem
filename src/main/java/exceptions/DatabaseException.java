package exceptions;

/**
 * Custom Checked Exception representing database communication or SQL failures.
 * Demonstrates Checked Exceptions and exception wrapping (Lectures 10-15).
 * Forces calling code to either handle using try-catch or declare using throws.
 */
public class DatabaseException extends Exception {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
