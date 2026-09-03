package java.security.cert;

// El certificado no se pudo parsear: los bytes no son un certificado bien formado.
//
// Es distinto de `CertificateException` a secas y la distincion importa: aca el problema es la
// **sintaxis**, no la validez. Un certificado que no parsea nunca llego a evaluarse; uno que
// parsea y no vale se rechazo por lo que dice.
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
