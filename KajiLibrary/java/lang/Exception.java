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

    public Exception(Throwable cause) {
        super(cause);
    }
}
