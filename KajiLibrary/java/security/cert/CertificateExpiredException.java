package java.security.cert;

// El certificado ya vencio.
//
// Es una de las dos unicas condiciones de validez que tienen excepcion propia —la otra es
// `CertificateNotYetValidException`— y eso es porque son las unicas que dependen solo del reloj:
// se comprueban sin mirar ninguna firma ni consultar a nadie. Todo el resto de las razones por las
// que un certificado puede no servir viven en `CertPathValidatorException`.
//
// No lleva constructor con causa: no hay nada que envolver, la fecha ya paso.
public class CertificateExpiredException extends CertificateException {

    private static final long serialVersionUID = 9071001339691533771L;

    public CertificateExpiredException() {
        super();
    }

    public CertificateExpiredException(String message) {
        super(message);
    }
}
