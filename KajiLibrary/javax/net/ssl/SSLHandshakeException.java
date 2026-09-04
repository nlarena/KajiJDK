package javax.net.ssl;

/**
 * El handshake no llego a terminar, asi que <strong>nunca hubo sesion</strong>.
 *
 * <p>Es la falla mas informativa de las cuatro: significa que las dos puntas no se pudieron poner de
 * acuerdo. Las causas tipicas son que no compartan ninguna suite de cifrado, que el certificado no
 * valide, o que una pida autenticacion de cliente y la otra no la tenga.
 */
public class SSLHandshakeException extends SSLException {

    private static final long serialVersionUID = -5045881315018326890L;

    /** Con un mensaje. */
    public SSLHandshakeException(String reason) {
        super(reason);
    }

    /** Con un mensaje y la causa de fondo. */
    public SSLHandshakeException(String message, Throwable cause) {
        super(message, cause);
    }
}
