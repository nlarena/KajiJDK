package java.security;

// El caso particular de `UnrecoverableEntryException` en el que la entrada que no se pudo
// recuperar es una clave.
public class UnrecoverableKeyException extends UnrecoverableEntryException {

    public UnrecoverableKeyException() {
        super();
    }

    public UnrecoverableKeyException(String message) {
        super(message);
    }
}
