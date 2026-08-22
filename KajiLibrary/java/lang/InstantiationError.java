package java.lang;

// KajiLibrary's java.lang.InstantiationError — a `new` of what is now an abstract class or an
// interface. The Error twin of InstantiationException (the reflective, checked version).
public class InstantiationError extends IncompatibleClassChangeError {

    public InstantiationError() {
    }

    public InstantiationError(String message) {
        super(message);
    }
}
