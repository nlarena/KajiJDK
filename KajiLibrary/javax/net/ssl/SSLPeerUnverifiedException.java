package javax.net.ssl;

/**
 * No se pudo verificar quien esta del otro lado.
 *
 * <p>La sesion puede existir igual: SSL admite suites anonimas, y con esas hay cifrado pero no
 * identidad. Por eso pedirle a una {@link SSLSession} el certificado del par puede fallar aunque
 * todo lo demas funcione — cifrar y autenticar son dos cosas distintas, y esta excepcion es el lugar
 * donde esa distincion se vuelve visible.
 */
public class SSLPeerUnverifiedException extends SSLException {

    private static final long serialVersionUID = -8919512675153181392L;

    /** Con un mensaje. */
    public SSLPeerUnverifiedException(String reason) {
        super(reason);
    }

    /** Con un mensaje y la causa de fondo. */
    public SSLPeerUnverifiedException(String message, Throwable cause) {
        super(message, cause);
    }
}
