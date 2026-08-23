package java.lang.invoke;

// The lambda metafactory could not build the call site: the functional interface and the
// implementation method do not fit together. Checked, because it is raised while LINKING a
// bootstrap and the bootstrap is expected to report why.
public class LambdaConversionException extends Exception {

    public LambdaConversionException() {
        super();
    }

    public LambdaConversionException(String message) {
        super(message);
    }

    // The cause-carrying forms. A conversion failure is usually a REFLECTION failure underneath —
    // the implementation method was not found, or was not accessible — so the interesting
    // information is the cause and not this exception's own message.
    public LambdaConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    public LambdaConversionException(Throwable cause) {
        super(cause);
    }

    // DIVERGENCE, and a small one: in the JDK the two flags turn off suppression and make the
    // stack trace non-writable. KajiLibrary's `Throwable` has neither feature — no
    // `addSuppressed`, no non-writable trace — so there is nothing for them to switch off and
    // they are accepted and ignored. The alternative, leaving the constructor out, would have
    // been worse: a caller that passes `false, false` gets the JDK's behaviour for a `Throwable`
    // WITH suppression, which is exactly what it would get here anyway.
    public LambdaConversionException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause);
    }
}
