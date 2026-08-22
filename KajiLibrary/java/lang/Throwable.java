package java.lang;

// KajiLibrary's java.lang.Throwable — the superclass of all errors and exceptions. Carries a detail
// message and an optional cause (another Throwable), with the JDK's `cause == this` sentinel meaning
// "not yet initialised". A KajiLibrary subset: stack-trace capture and suppressed exceptions are not
// modelled (they need VM support); the message/cause plumbing is pure Java.
public class Throwable {

    private String message;
    private Throwable cause;

    public Throwable() {
        this.message = null;
        this.cause = this;
    }

    public Throwable(String message) {
        this.message = message;
        this.cause = this;
    }

    public Throwable(String message, Throwable cause) {
        this.message = message;
        this.cause = cause;
    }

    public Throwable(Throwable cause) {
        if (cause == null) {
            this.message = null;
        } else {
            this.message = cause.toString();
        }
        this.cause = cause;
    }

    public String getMessage() {
        return this.message;
    }

    public String getLocalizedMessage() {
        return getMessage();
    }

    public Throwable getCause() {
        if (this.cause == this) {
            return null;
        }
        return this.cause;
    }

    public Throwable initCause(Throwable cause) {
        this.cause = cause;
        return this;
    }

    public String toString() {
        String name = this.getClass().getName();
        if (this.message == null) {
            return name;
        }
        return name + ": " + this.message;
    }
}
