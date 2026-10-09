package javax.security.cert;

/**
 * KajiLibrary's javax.security.cert.CertificateParsingException -- the bytes are not a certificate.
 *
 * <p>It differs from the date ones in something important: here there never was a certificate. A
 * parsing error says nothing about whether the issuer is trustworthy, because there was no issuer
 * to read.
 *
 * <p>Deprecated <b>and marked for removal</b> since Java 9. The replacement is {@code
 * java.security.cert}, which is not an improved version of this but something else: it supports
 * version 3 of the format, with extensions, which is the only thing that serves to validate a chain
 * today.
 */
@Deprecated(since = "9", forRemoval = true)
public class CertificateParsingException extends CertificateException {

    private static final long serialVersionUID = -8449352422951136229L;

    /** Without detail. */
    public CertificateParsingException() {
        super();
    }

    /** With a message that says what happened. */
    public CertificateParsingException(String message) {
        super(message);
    }
}
