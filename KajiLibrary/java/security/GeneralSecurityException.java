package java.security;

// La raiz de las excepciones chequeadas del paquete.
//
// Existe para que un llamador que no quiere distinguir entre "no esta el algoritmo", "la clave no
// sirve" y "la firma no se pudo procesar" pueda atrapar las tres con un solo catch. Casi todo el
// paquete tira alguna de sus subclases, y las dos que se salen de la jerarquia lo hacen a
// proposito: `ProviderException` es no chequeada porque señala un proveedor roto, e
// `InvalidParameterException` porque señala un error de programacion del llamador.
public class GeneralSecurityException extends Exception {

    public GeneralSecurityException() {
        super();
    }

    public GeneralSecurityException(String message) {
        super(message);
    }

    public GeneralSecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    public GeneralSecurityException(Throwable cause) {
        super(cause);
    }
}
