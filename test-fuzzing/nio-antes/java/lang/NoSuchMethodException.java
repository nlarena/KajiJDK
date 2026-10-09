package java.lang;

// KajiLibrary's java.lang.NoSuchMethodException — a method was looked up by name and signature and
// the class has no match. The reflective, checked counterpart of NoSuchMethodError.
public class NoSuchMethodException extends ReflectiveOperationException {

    public NoSuchMethodException() {
    }

    public NoSuchMethodException(String message) {
        super(message);
    }
}
