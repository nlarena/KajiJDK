package java.security;

// Fallo en una operacion de digest.
//
// En la practica la tira un solo metodo, `MessageDigest.digest(byte[], int, int)`, cuando el
// buffer que le dan no tiene lugar para el resultado. Los demas caminos del digest no pueden
// fallar: alimentar bytes a una funcion de hash no tiene modo de error.
public class DigestException extends GeneralSecurityException {

    public DigestException() {
        super();
    }

    public DigestException(String message) {
        super(message);
    }

    public DigestException(String message, Throwable cause) {
        super(message, cause);
    }

    public DigestException(Throwable cause) {
        super(cause);
    }
}
