package java.lang;

// KajiLibrary's java.lang.LayerInstantiationException -- thrown when a module layer cannot be
// created. KajiJDK creates none, so this is surface only.
public class LayerInstantiationException extends RuntimeException {

    public LayerInstantiationException() {
    }

    public LayerInstantiationException(String msg) {
        super(msg);
    }

    public LayerInstantiationException(Throwable cause) {
        super(cause);
    }

    public LayerInstantiationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
