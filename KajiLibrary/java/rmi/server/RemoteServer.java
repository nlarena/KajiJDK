package java.rmi.server;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * La base de las implementaciones de objetos remotos, del lado servidor.
 *
 * <p>Separada de {@link RemoteObject} porque los dos lados heredan cosas distintas: un stub necesita
 * identidad remota, una implementacion necesita ademas saber a quien esta atendiendo. De ahi
 * {@link #getClientHost}, que solo tiene respuesta durante una llamada — ver
 * {@link ServerNotActiveException}.
 */
public abstract class RemoteServer extends RemoteObject {

    private static final long serialVersionUID = -4100238210092549637L;

    private static PrintStream log;

    /** Sin referencia. */
    protected RemoteServer() {
        super();
    }

    /** Con esa referencia. */
    protected RemoteServer(RemoteRef ref) {
        super(ref);
    }

    /**
     * El host del cliente que se esta atendiendo.
     *
     * @throws ServerNotActiveException si no se esta atendiendo ninguna llamada en este hilo
     */
    public static String getClientHost() throws ServerNotActiveException {
        throw new ServerNotActiveException("no hay ninguna llamada remota en curso");
    }

    /**
     * Enciende el registro de llamadas; {@code null} lo apaga.
     *
     * <p>Es estatico y global: no hay un registro por objeto.
     */
    public static void setLog(OutputStream out) {
        log = out == null ? null : new PrintStream(out, true);
    }

    /** Donde va el registro, o {@code null} si esta apagado. */
    public static PrintStream getLog() {
        return log;
    }
}
