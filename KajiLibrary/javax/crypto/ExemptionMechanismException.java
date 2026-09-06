package javax.crypto;

import java.security.GeneralSecurityException;

/**
 * Algo salio mal en el mecanismo de exencion.
 *
 * <p>Ver {@link ExemptionMechanism} para que es un mecanismo de exencion.
 *
 * @since 1.4
 */
public class ExemptionMechanismException extends GeneralSecurityException {

    private static final long serialVersionUID = 1L;

    /** Una sin mensaje. */
    public ExemptionMechanismException() {
        super();
    }

    /**
     * Una con mensaje.
     *
     * @param msg que paso
     */
    public ExemptionMechanismException(String msg) {
        super(msg);
    }
}
