package java.lang;

// KajiLibrary's java.lang.IllegalAccessException — a reflective access was denied because the
// member is not visible from the caller. Checked: reflection routes around the compiler's
// access checks, so the check has to reappear at run time as something you must handle.
public class IllegalAccessException extends ReflectiveOperationException {

    public IllegalAccessException() {
    }

    public IllegalAccessException(String message) {
        super(message);
    }
}
