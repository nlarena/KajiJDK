package java.security;

// La clave no sirve para lo que se le pidio: mala codificacion, largo equivocado, algoritmo que no
// corresponde.
public class InvalidKeyException extends KeyException {

    public InvalidKeyException() {
        super();
    }

    public InvalidKeyException(String message) {
        super(message);
    }

    public InvalidKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidKeyException(Throwable cause) {
        super(cause);
    }
}
