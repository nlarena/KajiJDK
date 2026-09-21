package java.security;

// It wraps the checked exception a `PrivilegedExceptionAction` threw.
//
// It exists because of a problem of types and not of security: `AccessController.doPrivileged`
// cannot declare a `throws` of something that depends on the action it is passed, so it wraps
// whatever comes out in this one and declares only this one. The `run()` of the action declares
// `throws Exception`, and this is the envelope it arrives in on the other side.
//
// Deprecated in the JDK along with the whole mechanism of privileges, which no longer governs
// anything since the `SecurityManager` was disabled. It is implemented because it goes on being the
// type that appears in the signatures.
@Deprecated
public class PrivilegedActionException extends Exception {

    // The wrapped exception. It is kept apart from the cause of `Throwable` because this type is
    // older than the chaining of causes and its serialisation has the field of its own.
    private final Exception exception;

    public PrivilegedActionException(Exception exception) {
        super((Throwable) null);
        this.exception = exception;
    }

    // The exception the action threw.
    public Exception getException() {
        return this.exception;
    }

    @Override
    public Throwable getCause() {
        return this.exception;
    }

    @Override
    public String toString() {
        String s = this.getClass().getName();
        if (this.exception != null) {
            return s + ": " + this.exception.toString();
        }
        return s;
    }
}
