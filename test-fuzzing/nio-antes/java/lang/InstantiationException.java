package java.lang;

// KajiLibrary's java.lang.InstantiationException — a reflective instantiation of something that
// cannot be instantiated: an abstract class, an interface, a primitive type.
public class InstantiationException extends ReflectiveOperationException {

    public InstantiationException() {
    }

    public InstantiationException(String message) {
        super(message);
    }
}
