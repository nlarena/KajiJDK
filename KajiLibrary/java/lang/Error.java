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

    /**
     * The constructor with `Throwable`'s two switches: suppression and writing the stack trace. It
     * is `protected` because it only makes sense for a subclass that wants a **cheap** exception --
     * one thrown many times as a control signal and whose stack nobody is going to look at.
     */
    protected Error(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public Error(Throwable cause) {
        super(cause);
    }
}
