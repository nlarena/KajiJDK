package javax.rmi.ssl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * El socket de escucha que devuelve {@link SslRMIServerSocketFactory#createServerSocket(int)}.
 *
 * <p>No es una clase del JDK: alla es una clase anonima adentro del `createServerSocket`. Aca tiene
 * nombre y es de paquete, que a los efectos de la API es lo mismo --nadie fuera de `javax.rmi.ssl`
 * la puede nombrar-- y ademas se lee.
 *
 * <p>Lo unico que hace es envolver cada conexion aceptada en un {@link SSLSocket} en modo servidor.
 * El TCP lo acepta {@link ServerSocket}; el TLS empieza cuando alguien lee o escribe.
 */
final class SslServerSocket extends ServerSocket {

    private final SSLSocketFactory fabrica;
    private final String[] suites;
    private final String[] protocolos;
    private final boolean pideCertificado;

    SslServerSocket(int port, SSLSocketFactory fabrica, String[] suites, String[] protocolos,
            boolean pideCertificado) throws IOException {
        super(port);
        this.fabrica = fabrica;
        this.suites = suites;
        this.protocolos = protocolos;
        this.pideCertificado = pideCertificado;
    }

    /**
     * Acepta una conexion y la envuelve en SSL.
     *
     * <p>El socket TCP se le entrega al {@link SSLSocketFactory} con `autoClose` en `true`: cerrar
     * el socket SSL tiene que cerrar tambien el de abajo, o la conexion queda a medio soltar.
     */
    public Socket accept() throws IOException {
        Socket plano = super.accept();
        SSLSocket seguro = (SSLSocket) this.fabrica.createSocket(
                plano, plano.getInetAddress().getHostName(), plano.getPort(), true);
        seguro.setUseClientMode(false);
        if (this.suites != null) {
            seguro.setEnabledCipherSuites(this.suites);
        }
        if (this.protocolos != null) {
            seguro.setEnabledProtocols(this.protocolos);
        }
        seguro.setNeedClientAuth(this.pideCertificado);
        return seguro;
    }
}
