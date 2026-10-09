package javax.security.cert;

/**
 * KajiLibrary's javax.security.cert.CertificateException -- something went wrong with a
 * certificate.
 *
 * <p>It is the root of the exceptions of this package, which is the <b>old</b> certificate one: it
 * exists because {@code javax.net.ssl.SSLSession} uses it in its API and it cannot be changed
 * without breaking compiled code. For everything else there is {@link
 * java.security.cert.CertificateException}, which is the one to use.
 *
 * <p>Deprecated <b>and marked for removal</b> since Java 9. The replacement is {@code
 * java.security.cert}, which is not an improved version of this but something else: it supports
 * version 3 of the format, with extensions, which is the only thing that serves to validate a chain
 * today.
 */
@Deprecated(since = "9", forRemoval = true)
public class CertificateException extends Exception {

    private static final long serialVersionUID = -5757213374030785290L;

    /** Without detail. */
    public CertificateException() {
        super();
    }

    /** With a message that says what happened. */
    public CertificateException(String msg) {
        super(msg);
    }
}
