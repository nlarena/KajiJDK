package javax.crypto;

import java.security.GeneralSecurityException;

/**
 * El arreglo que se dio para la salida no alcanza.
 *
 * <p>Las versiones que escriben en un arreglo dado existen para no reservar memoria en cada paso,
 * que en un flujo de datos importa. El precio es que hay que preguntar antes cuanto va a salir, con
 * {@link Cipher#getOutputSize}; esta excepcion es lo que pasa cuando no se pregunto.
 *
 * @since 1.4
 */
public class ShortBufferException extends GeneralSecurityException {

    private static final long serialVersionUID = 1L;

    /** Una sin mensaje. */
    public ShortBufferException() {
        super();
    }

    /**
     * Una con mensaje.
     *
     * @param msg que paso
     */
    public ShortBufferException(String msg) {
        super(msg);
    }
}
