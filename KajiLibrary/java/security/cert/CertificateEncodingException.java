package java.security.cert;

// The certificate could not be encoded.
public class CertificateEncodingException extends CertificateException {

    public CertificateEncodingException() {
        super();
    }

    public CertificateEncodingException(String message) {
        super(message);
    }

    public CertificateEncodingException(String message, Throwable cause) {
        super(message, cause);
    }

    public CertificateEncodingException(Throwable cause) {
        super(cause);
    }
}
