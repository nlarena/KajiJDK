package javax.security.cert;

/**
 * KajiLibrary's javax.security.cert.CertificateExpiredException -- the certificate already expired.
 *
 * <p>Being an exception apart from {@link CertificateNotYetValidException} is not decoration: both
 * mean "the date does not fall in the range", but the causes and what has to be done are opposite.
 * Expired is a certificate that has to be renewed; not-yet-valid is almost always the machine's
 * clock being wrong.
 *
 * <p>Deprecated <b>and marked for removal</b> since Java 9. The replacement is {@code
 * java.security.cert}, which is not an improved version of this but something else: it supports
 * version 3 of the format, with extensions, which is the only thing that serves to validate a chain
 * today.
 */
@Deprecated(since = "9", forRemoval = true)
public class CertificateExpiredException extends CertificateException {

    private static final long serialVersionUID = 5091601212177261883L;

    /** Without detail. */
    public CertificateExpiredException() {
        super();
    }

    /** With a message that says what happened. */
    public CertificateExpiredException(String message) {
        super(message);
    }
}
