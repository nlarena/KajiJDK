package java.security.cert;

import java.security.GeneralSecurityException;

// A certification path could not be built.
//
// That it is the same exception for "there is no path" and for "the builder broke" is of the API
// and is worth knowing: the absence of a path is not an error of the program, it is a result.
// Telling one from the other forces one to look at the cause.
public class CertPathBuilderException extends GeneralSecurityException {

    private static final long serialVersionUID = 5316471420178794402L;

    public CertPathBuilderException() {
        super();
    }

    public CertPathBuilderException(String msg) {
        super(msg);
    }

    public CertPathBuilderException(Throwable cause) {
        super(cause);
    }

    public CertPathBuilderException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
