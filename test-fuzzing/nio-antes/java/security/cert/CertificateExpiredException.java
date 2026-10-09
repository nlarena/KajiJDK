package java.security.cert;

// The certificate has expired.
//
// It is one of the only two validity conditions that have an exception of their own —the other is
// `CertificateNotYetValidException`— and that is because they are the only ones that depend on the
// clock alone: they are checked without looking at any signature or consulting anybody. All the
// rest of the reasons why a certificate may not serve live in `CertPathValidatorException`.
//
// It carries no constructor with a cause: there is nothing to wrap, the date has passed.
public class CertificateExpiredException extends CertificateException {

    private static final long serialVersionUID = 9071001339691533771L;

    public CertificateExpiredException() {
        super();
    }

    public CertificateExpiredException(String message) {
        super(message);
    }
}
