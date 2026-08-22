package java.lang;

// KajiLibrary's java.lang.IllegalCallerException — a method that is only meaningful when called
// from a particular context was called from somewhere else (the caller-sensitive APIs added
// with the module system throw it).
public class IllegalCallerException extends RuntimeException {

    public IllegalCallerException() {
    }

    public IllegalCallerException(String message) {
        super(message);
    }

    public IllegalCallerException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalCallerException(Throwable cause) {
        super(cause);
    }
}
