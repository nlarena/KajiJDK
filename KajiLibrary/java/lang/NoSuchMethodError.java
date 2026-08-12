package java.lang;

// KajiLibrary's java.lang.NoSuchMethodError — a referenced method does not exist on the
// resolved class (the VM throws it during resolution).
public class NoSuchMethodError extends LinkageError {

    public NoSuchMethodError() {
    }

    public NoSuchMethodError(String message) {
        super(message);
    }
}
