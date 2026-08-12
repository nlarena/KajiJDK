package java.lang;

// KajiLibrary's java.lang.NullPointerException — the VM throws it when code uses a null
// reference (null receiver of a field/method access, null array, etc.).
public class NullPointerException extends RuntimeException {

    public NullPointerException() {
    }

    public NullPointerException(String message) {
        super(message);
    }
}
