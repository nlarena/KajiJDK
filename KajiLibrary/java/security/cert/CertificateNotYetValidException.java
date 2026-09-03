package java.security.cert;

// El certificado todavia no entro en vigencia.
//
// En la practica casi siempre significa que el reloj de quien valida esta atrasado, no que el
// certificado sea del futuro. Vale tenerlo presente antes de salir a buscar el problema en el
// certificado.
public class CertificateNotYetValidException extends CertificateException {

    private static final long serialVersionUID = 4355919900041064702L;

    public CertificateNotYetValidException() {
        super();
    }

    public CertificateNotYetValidException(String message) {
        super(message);
    }
}
