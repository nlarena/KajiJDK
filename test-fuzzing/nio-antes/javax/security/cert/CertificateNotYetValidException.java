package javax.security.cert;

/**
 * KajiLibrary's javax.security.cert.CertificateNotYetValidException -- it has not started being
 * valid yet.
 *
 * <p>See {@link CertificateExpiredException} for why they are two exceptions and not one.
 *
 * <p>Deprecated <b>and marked for removal</b> since Java 9. The replacement is {@code
 * java.security.cert}, which is not an improved version of this but something else: it supports
 * version 3 of the format, with extensions, which is the only thing that serves to validate a chain
 * today.
 */
@Deprecated(since = "9", forRemoval = true)
public class CertificateNotYetValidException extends CertificateException {

    private static final long serialVersionUID = -8976172474266822818L;

    /** Without detail. */
    public CertificateNotYetValidException() {
        super();
    }

    /** With a message that says what happened. */
    public CertificateNotYetValidException(String message) {
        super(message);
    }
}
