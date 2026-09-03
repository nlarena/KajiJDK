package javax.security.cert;

/**
 * KajiLibrary's javax.security.cert.CertificateException -- algo salio mal con un certificado.
 *
 * <p>Es la raiz de las excepciones de este paquete, que es el <b>viejo</b> de certificados: existe
 * porque {@code javax.net.ssl.SSLSession} lo usa en su API y no se puede cambiar sin romper codigo
 * compilado. Para todo lo demas esta {@link java.security.cert.CertificateException}, que es la que
 * hay que usar.
 *
 * <p>Obsoleta <b>y marcada para remocion</b> desde Java 9. El reemplazo es
 * {@code java.security.cert}, que no es una version mejorada de esto sino otra cosa: soporta la
 * version 3 del formato, con extensiones, que es lo unico que sirve para validar una cadena de hoy.
 */
@Deprecated(since = "9", forRemoval = true)
public class CertificateException extends Exception {

    private static final long serialVersionUID = -5757213374030785290L;

    /** Sin detalle. */
    public CertificateException() {
        super();
    }

    /** Con un mensaje que diga que paso. */
    public CertificateException(String msg) {
        super(msg);
    }
}
