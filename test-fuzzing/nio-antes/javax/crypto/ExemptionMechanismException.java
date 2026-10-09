package javax.crypto;

import java.security.GeneralSecurityException;

/**
 * Something went wrong in the exemption mechanism.
 *
 * <p>See {@link ExemptionMechanism} for what an exemption mechanism is.
 *
 * @since 1.4
 */
public class ExemptionMechanismException extends GeneralSecurityException {

    private static final long serialVersionUID = 1L;

    /** One with no message. */
    public ExemptionMechanismException() {
        super();
    }

    /**
     * One with a message.
     *
     * @param msg what happened
     */
    public ExemptionMechanismException(String msg) {
        super(msg);
    }
}
