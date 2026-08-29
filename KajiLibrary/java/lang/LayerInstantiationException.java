package java.lang;

/**
 * KajiLibrary's java.lang.LayerInstantiationException — a module layer could not be created,
 * because the configuration it was asked for is impossible: a module that would be defined
 * twice to the same loader, a package split across two modules, a missing parent.
 */
public class LayerInstantiationException extends RuntimeException {

    public LayerInstantiationException() {
    }

    public LayerInstantiationException(String message) {
        super(message);
    }

    public LayerInstantiationException(Throwable cause) {
        super(cause);
    }

    public LayerInstantiationException(String message, Throwable cause) {
        super(message, cause);
    }
}
