package java.security.cert;

// The certificate could not be parsed: the bytes are not a well formed certificate.
//
// It is different from a plain `CertificateException` and the distinction matters: here the problem
// is the **syntax**, not the validity. A certificate that does not parse never got as far as being
// evaluated; one that parses and is not valid was rejected for what it says.
public class CertificateParsingException extends CertificateException {

    private static final long serialVersionUID = -7989222416793322029L;

    public CertificateParsingException() {
        super();
    }

    public CertificateParsingException(String message) {
        super(message);
    }

    public CertificateParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public CertificateParsingException(Throwable cause) {
        super(cause);
    }
}
