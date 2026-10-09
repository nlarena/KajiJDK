package java.lang;

// KajiLibrary's java.lang.BootstrapMethodError — an invokedynamic call site could not be linked
// because its bootstrap method failed. Lambdas and string concatenation compile to indy, so
// this is the failure mode of the machinery that materialises them on first use.
public class BootstrapMethodError extends LinkageError {

    public BootstrapMethodError() {
    }

    public BootstrapMethodError(String message) {
        super(message);
    }

    public BootstrapMethodError(String message, Throwable cause) {
        super(message, cause);
    }

    public BootstrapMethodError(Throwable cause) {
        super(cause == null ? null : cause.toString(), cause);
    }
}
