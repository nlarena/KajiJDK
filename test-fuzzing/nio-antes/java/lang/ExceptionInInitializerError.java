package java.lang;

// KajiLibrary's java.lang.ExceptionInInitializerError — a static initialiser (`static { }` or a
// static field initialiser) threw. The VM turns that into an Error rather than propagating the
// original, because class initialisation happens implicitly at a use site that never asked for
// it: the code doing `Foo.bar()` cannot be expected to handle Foo's initialiser failing.
//
// The class is then marked erroneous forever — a second use throws NoClassDefFoundError, and the
// original exception is only visible here, through getException().
public class ExceptionInInitializerError extends LinkageError {

    // The exception that came out of the initialiser. Kept in its own field rather than only as
    // the Throwable cause because getException() predates the cause mechanism (Java 1.4); the
    // constructor wires up both so getCause() works too.
    private Throwable exception;

    public ExceptionInInitializerError() {
        this.exception = null;
    }

    public ExceptionInInitializerError(Throwable thrown) {
        super(null, thrown);
        this.exception = thrown;
    }

    public ExceptionInInitializerError(String s) {
        super(s);
        this.exception = null;
    }

    public Throwable getException() {
        return this.exception;
    }
}
