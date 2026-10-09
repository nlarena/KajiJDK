package jdk.security.jarsigner;

/**
 * What {@link JarSigner#sign} throws when the signing could not be completed.
 *
 * <p>It is the wrapping exception: the cause --a key that does not serve, an algorithm that is not
 * there, an I/O error over the zip-- travels inside. That it is unchecked is on purpose: `sign` can
 * fail for many different reasons and none is handled differently from the others, so forcing them
 * to be declared one by one would give nobody any information.
 */
public class JarSignerException extends RuntimeException {

    private static final long serialVersionUID = -4732217075689309530L;

    /**
     * An exception with that detail and that cause.
     *
     * @param msg the detail
     * @param cause what really failed
     */
    public JarSignerException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
