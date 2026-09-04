package javax.net.ssl;

import java.util.EventListener;

/**
 * Se entera cuando un handshake termino sobre un {@link SSLSocket}.
 *
 * <p>Sirve porque el handshake no es solo el arranque: TLS admite <em>renegociar</em> sobre una
 * conexion en curso, y ahi la sesion cambia — otra suite de cifrado, otro certificado del par. Un
 * programa que decidio algo mirando la sesion tiene que poder enterarse de que esa decision quedo
 * vieja.
 */
public interface HandshakeCompletedListener extends EventListener {

    /** El handshake termino; el evento trae la sesion que quedo. */
    void handshakeCompleted(HandshakeCompletedEvent event);
}
