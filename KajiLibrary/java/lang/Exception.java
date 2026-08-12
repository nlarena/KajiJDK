package java.lang;

// KajiLibrary's java.lang.Exception — the checked-exception branch (§11.2): a Throwable
// that a method must either catch or declare in its `throws`, unless it is one of the
// RuntimeException/Error kinds below.
public class Exception extends Throwable {

    public Exception() {
    }

    public Exception(String message) {
        super(message);
    }
}
