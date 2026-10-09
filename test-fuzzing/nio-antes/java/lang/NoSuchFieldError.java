package java.lang;

// KajiLibrary's java.lang.NoSuchFieldError — a referenced field does not exist on the resolved
// class (the VM throws it while resolving the getfield/putfield constant-pool entry).
public class NoSuchFieldError extends IncompatibleClassChangeError {

    public NoSuchFieldError() {
    }

    public NoSuchFieldError(String message) {
        super(message);
    }
}
