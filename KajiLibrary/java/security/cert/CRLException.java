package java.security.cert;

import java.security.GeneralSecurityException;

// Something went wrong encoding or decoding a revocation list.
//
// It is the sister of `CertificateException` on the CRL side: the same place in the hierarchy, the
// same reason to exist.
public class CRLException extends GeneralSecurityException {

    private static final long serialVersionUID = -6694728944094197147L;

    public CRLException() {
        super();
    }

    public CRLException(String message) {
        super(message);
    }

    public CRLException(String message, Throwable cause) {
        super(message, cause);
    }

    public CRLException(Throwable cause) {
        super(cause);
    }
}
