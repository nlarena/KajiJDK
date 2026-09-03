package javax.security.cert;

/**
 * KajiLibrary's javax.security.cert.CertificateNotYetValidException -- todavia no empezo a valer.
 *
 * <p>Ver {@link CertificateExpiredException} para por que son dos excepciones y no una.
 *
 * <p>Obsoleta <b>y marcada para remocion</b> desde Java 9. El reemplazo es
 * {@code java.security.cert}, que no es una version mejorada de esto sino otra cosa: soporta la
 * version 3 del formato, con extensiones, que es lo unico que sirve para validar una cadena de hoy.
 */
@Deprecated(since = "9", forRemoval = true)
public class CertificateNotYetValidException extends CertificateException {

    private static final long serialVersionUID = -8976172474266822818L;

    /** Sin detalle. */
    public CertificateNotYetValidException() {
        super();
    }

    /** Con un mensaje que diga que paso. */
    public CertificateNotYetValidException(String message) {
        super(message);
    }
}
