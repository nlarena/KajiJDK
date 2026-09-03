package javax.security.cert;

/**
 * KajiLibrary's javax.security.cert.CertificateExpiredException -- el certificado ya vencio.
 *
 * <p>Que sea una excepcion aparte de {@link CertificateNotYetValidException} no es decoracion: las
 * dos significan "la fecha no entra en el rango", pero las causas y lo que hay que hacer son
 * opuestas. Vencido es un certificado que hay que renovar; todavia-no-valido casi siempre es el
 * reloj de la maquina que esta mal.
 *
 * <p>Obsoleta <b>y marcada para remocion</b> desde Java 9. El reemplazo es
 * {@code java.security.cert}, que no es una version mejorada de esto sino otra cosa: soporta la
 * version 3 del formato, con extensiones, que es lo unico que sirve para validar una cadena de hoy.
 */
@Deprecated(since = "9", forRemoval = true)
public class CertificateExpiredException extends CertificateException {

    private static final long serialVersionUID = 5091601212177261883L;

    /** Sin detalle. */
    public CertificateExpiredException() {
        super();
    }

    /** Con un mensaje que diga que paso. */
    public CertificateExpiredException(String message) {
        super(message);
    }
}
