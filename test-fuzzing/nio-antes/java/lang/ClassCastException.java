package java.lang;

// KajiLibrary's java.lang.ClassCastException — thrown by a failed `checkcast` (an object
// cast to a type it is not an instance of).
public class ClassCastException extends RuntimeException {

    public ClassCastException() {
    }

    public ClassCastException(String message) {
        super(message);
    }
}
