package java.lang;

// KajiLibrary's java.lang.Exception — the superclass of conditions a program may want to catch.
public class Exception extends Throwable {

    public Exception() {
    }

    public Exception(String message) {
        super(message);
    }

    public Exception(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * The constructor with `Throwable`'s two switches: suppression and writing the stack trace. It
     * is `protected` because it only makes sense for a subclass that wants a **cheap** exception --
     * one thrown many times as a control signal and whose stack nobody is going to look at.
     */
    protected Exception(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public Exception(Throwable cause) {
        super(cause);
    }
}
