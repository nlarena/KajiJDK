package javax.crypto;

import java.security.GeneralSecurityException;

/**
 * No se pudo recuperar el secreto encapsulado.
 *
 * <p>Casi siempre porque la encapsulacion llego alterada o porque la clave privada no es la que
 * corresponde. Igual que con la etiqueta de un cifrado autenticado, conviene no decir hacia afuera
 * cual de las dos cosas fue.
 *
 * @since 21
 */
public class DecapsulateException extends GeneralSecurityException {

    private static final long serialVersionUID = 1L;

    /**
     * Una con mensaje.
     *
     * @param message que paso
     */
    public DecapsulateException(String message) {
        super(message);
    }

    /**
     * Una con mensaje y causa.
     *
     * @param message que paso
     * @param cause por que
     */
    public DecapsulateException(String message, Throwable cause) {
        super(message, cause);
    }
}
