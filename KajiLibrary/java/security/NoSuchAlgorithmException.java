package java.security;

// No hay implementacion del algoritmo pedido.
//
// La tira toda fabrica `getInstance` cuando ningun proveedor registrado ofrece el servicio. En
// KajiLibrary es el caso normal para casi todo el paquete: la unica fabrica con algoritmos de
// verdad detras es `MessageDigest`, y solo para los tres que estan implementados de cero.
public class NoSuchAlgorithmException extends GeneralSecurityException {

    public NoSuchAlgorithmException() {
        super();
    }

    public NoSuchAlgorithmException(String message) {
        super(message);
    }

    public NoSuchAlgorithmException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchAlgorithmException(Throwable cause) {
        super(cause);
    }
}
