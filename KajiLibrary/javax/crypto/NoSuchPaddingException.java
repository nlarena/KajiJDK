package javax.crypto;

import java.security.GeneralSecurityException;

/**
 * Se pidio un relleno que el proveedor no tiene.
 *
 * <p>Va aparte de {@link java.security.NoSuchAlgorithmException} porque el nombre que se le pasa a
 * {@link Cipher#getInstance} lleva tres cosas --algoritmo, modo y relleno-- y saber cual de las tres
 * falto cambia el mensaje de error.
 *
 * @since 1.4
 */
public class NoSuchPaddingException extends GeneralSecurityException {

    private static final long serialVersionUID = 1L;

    /** Una sin mensaje. */
    public NoSuchPaddingException() {
        super();
    }

    /**
     * Una con mensaje.
     *
     * @param msg que paso
     */
    public NoSuchPaddingException(String msg) {
        super(msg);
    }
}
