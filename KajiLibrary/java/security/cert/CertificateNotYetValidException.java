package java.security.cert;

// The certificate has not come into force yet.
//
// In practice it almost always means that the clock of whoever validates is behind, not that the
// certificate is from the future. It is worth keeping in mind before going out to look for the
// problem in the certificate.
public class CertificateNotYetValidException extends CertificateException {

    private static final long serialVersionUID = 4355919900041064702L;

    public CertificateNotYetValidException() {
        super();
    }

    public CertificateNotYetValidException(String message) {
        super(message);
    }
}
