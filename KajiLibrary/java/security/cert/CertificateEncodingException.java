package java.security.cert;

// El certificado no se pudo codificar.
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
