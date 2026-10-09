package java.lang;

// KajiLibrary's java.lang.NoSuchMethodError — a referenced method does not exist on the
// resolved class (the VM throws it during resolution). It sits under
// IncompatibleClassChangeError because the usual cause is exactly that: the method existed
// when the caller was compiled, and the callee changed underneath it.
public class NoSuchMethodError extends IncompatibleClassChangeError {

    public NoSuchMethodError() {
    }

    public NoSuchMethodError(String message) {
        super(message);
    }
}
