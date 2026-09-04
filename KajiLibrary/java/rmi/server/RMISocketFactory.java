package java.rmi.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * La fabrica de sockets que RMI usa cuando un objeto no trae la suya.
 *
 * <p>Implementa las dos interfaces, y esa union tiene sentido justo aca: es la configuracion
 * <strong>global</strong> del proceso, donde los dos lados se fijan juntos. Las fabricas por objeto
 * van por separado, porque la del cliente viaja y la del servidor no — ver
 * {@link RMIClientSocketFactory}.
 *
 * <p>{@link #setSocketFactory} se puede llamar <strong>una sola vez</strong>. No es capricho:
 * cambiarla con conexiones abiertas dejaria sockets creados por una fabrica y cerrados por otra.
 */
public abstract class RMISocketFactory implements RMIClientSocketFactory, RMIServerSocketFactory {

    private static RMISocketFactory laElegida;
    private static RMISocketFactory laDefault;
    private static RMIFailureHandler manejador;

    /** Para las implementaciones. */
    public RMISocketFactory() {
    }

    /** Abre una conexion al servidor. */
    public abstract Socket createSocket(String host, int port) throws IOException;

    /** Abre un socket de escucha. */
    public abstract ServerSocket createServerSocket(int port) throws IOException;

    /**
     * Fija la fabrica global.
     *
     * @throws IOException si ya se habia fijado una
     */
    public static synchronized void setSocketFactory(RMISocketFactory fac) throws IOException {
        if (laElegida != null) {
            throw new IOException("la fabrica de sockets ya estaba fijada");
        }
        laElegida = fac;
    }

    /** La fabrica global, o {@code null} si no se fijo ninguna. */
    public static synchronized RMISocketFactory getSocketFactory() {
        return laElegida;
    }

    /**
     * La fabrica por omision: sockets comunes.
     *
     * <p>Nunca es {@code null}, a diferencia de {@link #getSocketFactory}. La distincion importa:
     * una dice que se configuro y la otra que se usa cuando no se configuro nada.
     */
    public static synchronized RMISocketFactory getDefaultSocketFactory() {
        if (laDefault == null) {
            laDefault = new FabricaComun();
        }
        return laDefault;
    }

    /** Fija que hacer cuando no se puede crear un socket; ver {@link RMIFailureHandler}. */
    public static synchronized void setFailureHandler(RMIFailureHandler fh) {
        manejador = fh;
    }

    /** El manejador de fallas, o {@code null}. */
    public static synchronized RMIFailureHandler getFailureHandler() {
        return manejador;
    }
}
