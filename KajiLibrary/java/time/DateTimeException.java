package java.time;

// KajiLibrary's java.time.DateTimeException — the base unchecked exception for date-time errors.
public class DateTimeException extends RuntimeException {

    public DateTimeException(String message) {
        super(message);
    }

    public DateTimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
