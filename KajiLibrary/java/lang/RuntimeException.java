package java.lang;

// KajiLibrary's java.lang.RuntimeException — the superclass of unchecked exceptions.
public class RuntimeException extends Exception {

    public RuntimeException() {
    }

    public RuntimeException(String message) {
        super(message);
    }

    public RuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * The constructor with `Throwable`'s two switches: suppression and writing the stack trace. It
     * is `protected` because it only makes sense for a subclass that wants a **cheap** exception --
     * one thrown many times as a control signal and whose stack nobody is going to look at.
     */
    protected RuntimeException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public RuntimeException(Throwable cause) {
        super(cause);
    }
}
