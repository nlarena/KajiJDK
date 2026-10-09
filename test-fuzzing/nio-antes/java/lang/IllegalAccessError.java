package java.lang;

// KajiLibrary's java.lang.IllegalAccessError — access to a field or method that was legal at
// compile time and is not any more (it was narrowed to private/package-private since).
// The Error twin of IllegalAccessException, which is the reflective, checked version.
public class IllegalAccessError extends IncompatibleClassChangeError {

    public IllegalAccessError() {
    }

    public IllegalAccessError(String message) {
        super(message);
    }
}
