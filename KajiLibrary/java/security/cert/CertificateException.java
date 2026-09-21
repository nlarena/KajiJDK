package java.security.cert;

import java.security.GeneralSecurityException;

// The base of the problems with certificates.
public class CertificateException extends GeneralSecurityException {

    public CertificateException() {
        super();
    }

    public CertificateException(String message) {
        super(message);
    }

    public CertificateException(String message, Throwable cause) {
        super(message, cause);
    }

    public CertificateException(Throwable cause) {
        super(cause);
    }
}
