package java.time;

// KajiLibrary's java.time.DateTimeException — the base unchecked exception for date-time
// errors. A subset: the (String) constructor only (the (String, Throwable) form needs a
// cause-taking RuntimeException constructor KajiLibrary doesn't have yet).
public class DateTimeException extends RuntimeException {

    public DateTimeException(String message) {
        super(message);
    }
}
