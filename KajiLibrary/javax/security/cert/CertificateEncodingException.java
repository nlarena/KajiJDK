package javax.security.cert;

/**
 * KajiLibrary's javax.security.cert.CertificateEncodingException -- the encoding could not be
 * produced.
 *
 * <p>It comes out of {@code getEncoded}, and almost always means the certificate was put together
 * in memory from parts and one of them cannot be written back in DER.
 *
 * <p>Deprecated <b>and marked for removal</b> since Java 9. The replacement is {@code
 * java.security.cert}, which is not an improved version of this but something else: it supports
 * version 3 of the format, with extensions, which is the only thing that serves to validate a chain
 * today.
 */
@Deprecated(since = "9", forRemoval = true)
public class CertificateEncodingException extends CertificateException {

    private static final long serialVersionUID = -8187642723048403470L;

    /** Without detail. */
    public CertificateEncodingException() {
        super();
    }

    /** With a message that says what happened. */
    public CertificateEncodingException(String message) {
        super(message);
    }
}
