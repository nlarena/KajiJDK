package java.lang;

// KajiLibrary's java.lang.NoSuchFieldException — a field was looked up by name and the class has
// no such field. The reflective, checked counterpart of the VM's NoSuchFieldError.
public class NoSuchFieldException extends ReflectiveOperationException {

    public NoSuchFieldException() {
    }

    public NoSuchFieldException(String message) {
        super(message);
    }
}
