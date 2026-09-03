package javax.security.cert;

/**
 * KajiLibrary's javax.security.cert.CertificateParsingException -- los bytes no son un certificado.
 *
 * <p>Se distingue de las de fecha en algo importante: aca no se llego a tener un certificado. Un
 * error de parseo no dice nada sobre si el emisor es confiable, porque no hubo emisor que leer.
 *
 * <p>Obsoleta <b>y marcada para remocion</b> desde Java 9. El reemplazo es
 * {@code java.security.cert}, que no es una version mejorada de esto sino otra cosa: soporta la
 * version 3 del formato, con extensiones, que es lo unico que sirve para validar una cadena de hoy.
 */
@Deprecated(since = "9", forRemoval = true)
public class CertificateParsingException extends CertificateException {

    private static final long serialVersionUID = -8449352422951136229L;

    /** Sin detalle. */
    public CertificateParsingException() {
        super();
    }

    /** Con un mensaje que diga que paso. */
    public CertificateParsingException(String message) {
        super(message);
    }
}
