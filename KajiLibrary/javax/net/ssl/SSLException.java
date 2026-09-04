package javax.net.ssl;

import java.io.IOException;

/**
 * Algo fallo en la capa SSL/TLS.
 *
 * <p>Es una {@link IOException} y no algo aparte, y eso es una decision de diseno con consecuencia
 * practica: quien escribe sobre un socket seguro no tiene que aprender una jerarquia nueva. Un
 * {@code catch (IOException)} que ya existia sigue sirviendo, y quien quiera distinguir el fallo
 * criptografico del corte de red atrapa esta.
 *
 * <p>Sus tres subclases dicen <em>en que etapa</em> se rompio: ver {@link SSLHandshakeException},
 * {@link SSLKeyException}, {@link SSLPeerUnverifiedException} y {@link SSLProtocolException}.
 */
public class SSLException extends IOException {

    private static final long serialVersionUID = 4511006460650708967L;

    /** Con un mensaje. */
    public SSLException(String reason) {
        super(reason);
    }

    /** Con un mensaje y la causa de fondo. */
    public SSLException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Envolviendo lo que realmente fallo. */
    public SSLException(Throwable cause) {
        super(cause);
    }
}
