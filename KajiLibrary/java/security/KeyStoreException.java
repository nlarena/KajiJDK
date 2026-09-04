package java.security;

// Fallo generico de un almacen de claves.
public class KeyStoreException extends GeneralSecurityException {

    public KeyStoreException() {
        super();
    }

    public KeyStoreException(String message) {
        super(message);
    }

    public KeyStoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public KeyStoreException(Throwable cause) {
        super(cause);
    }
}
