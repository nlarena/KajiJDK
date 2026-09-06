package javax.crypto;

import java.security.GeneralSecurityException;

/**
 * Los datos no miden un multiplo del bloque.
 *
 * <p>Un cifrado por bloques sin relleno solo puede trabajar con multiplos exactos del tamano de
 * bloque. Con relleno esto no pasa nunca al cifrar --el relleno es justamente lo que completa el
 * ultimo bloque-- pero si al descifrar, cuando lo que llega esta cortado.
 *
 * @since 1.4
 */
public class IllegalBlockSizeException extends GeneralSecurityException {

    private static final long serialVersionUID = 1L;

    /** Una sin mensaje. */
    public IllegalBlockSizeException() {
        super();
    }

    /**
     * Una con mensaje.
     *
     * @param msg que paso
     */
    public IllegalBlockSizeException(String msg) {
        super(msg);
    }
}
