package java.lang;

// KajiLibrary's java.lang.SecurityException — an operation was refused for security reasons.
// Historically thrown by SecurityManager checks; with the security manager now defunct it
// survives mostly as the type reflection and file APIs throw when access is denied.
public class SecurityException extends RuntimeException {

    public SecurityException() {
    }

    public SecurityException(String message) {
        super(message);
    }

    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    public SecurityException(Throwable cause) {
        super(cause);
    }
}
