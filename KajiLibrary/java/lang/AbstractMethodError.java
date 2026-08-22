package java.lang;

// KajiLibrary's java.lang.AbstractMethodError — an abstract method was invoked with no
// implementation to dispatch to. Impossible to produce from a consistent compile: it means
// a superclass grew a new abstract method after its subclass was compiled.
public class AbstractMethodError extends IncompatibleClassChangeError {

    public AbstractMethodError() {
    }

    public AbstractMethodError(String message) {
        super(message);
    }
}
