package java.lang;

// KajiLibrary's java.lang.Error — the Throwable branch for serious problems not meant
// to be caught in normal code (linkage failures, VM errors). Root of the linkage errors.
public class Error extends Throwable {

    public Error() {
    }

    public Error(String message) {
        super(message);
    }

    // Errors carry a cause like any other Throwable: a VM error is often the visible
    // symptom of something thrown further down (an ExceptionInInitializerError wrapping
    // whatever the static initialiser threw is the classic case).
    public Error(String message, Throwable cause) {
        super(message, cause);
    }

    public Error(Throwable cause) {
        super(cause);
    }
}
