package java.security;

// Los parametros que se le pasaron al algoritmo no son los que el algoritmo acepta.
//
// No confundir con `InvalidParameterException`, que es no chequeada y hereda de
// `IllegalArgumentException`: aquella señala un error de programacion del llamador, esta señala
// una combinacion parametro/algoritmo que solo se puede descubrir en runtime.
public class InvalidAlgorithmParameterException extends GeneralSecurityException {

    public InvalidAlgorithmParameterException() {
        super();
    }

    public InvalidAlgorithmParameterException(String message) {
        super(message);
    }

    public InvalidAlgorithmParameterException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidAlgorithmParameterException(Throwable cause) {
        super(cause);
    }
}
