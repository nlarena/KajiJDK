package javax.security.cert;

/**
 * KajiLibrary's javax.security.cert.CertificateEncodingException -- no se pudo producir la
 * codificacion.
 *
 * <p>Sale de {@code getEncoded}, y casi siempre significa que el certificado se armo en memoria a
 * partir de partes y que alguna no se puede volver a escribir en DER.
 *
 * <p>Obsoleta <b>y marcada para remocion</b> desde Java 9. El reemplazo es
 * {@code java.security.cert}, que no es una version mejorada de esto sino otra cosa: soporta la
 * version 3 del formato, con extensiones, que es lo unico que sirve para validar una cadena de hoy.
 */
@Deprecated(since = "9", forRemoval = true)
public class CertificateEncodingException extends CertificateException {

    private static final long serialVersionUID = -8187642723048403470L;

    /** Sin detalle. */
    public CertificateEncodingException() {
        super();
    }

    /** Con un mensaje que diga que paso. */
    public CertificateEncodingException(String message) {
        super(message);
    }
}
