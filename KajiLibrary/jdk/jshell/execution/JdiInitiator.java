package jdk.jshell.execution;

import java.util.List;
import java.util.Map;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.ListeningConnector;

/**
 * Quien pone en marcha la otra maquina virtual y se conecta a ella.
 *
 * <h2>Las dos formas de encontrarse</h2>
 *
 * <p><strong>Lanzando</strong>: JDI arranca el proceso y se conecta. Es lo mas simple y necesita
 * poder lanzar procesos.
 *
 * <p><strong>Escuchando</strong>: se abre un puerto, se lanza el proceso por separado con la orden
 * de conectarse ahi, y se espera. Es mas trabajo y es lo que sirve cuando el proceso tiene que
 * arrancar de una manera particular --otra version de Java, otro usuario, un contenedor--.
 *
 * <p>Este iniciador usa la segunda, que es la que da control sobre como se lanza.
 *
 * <h2>El tiempo de espera</h2>
 *
 * <p>Existe porque el otro proceso puede no llegar nunca, y quedarse esperando para siempre a algo
 * que fallo al arrancar seria peor que rendirse.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Nada de esto puede funcionar: {@link Bootstrap#virtualMachineManager} necesita una
 * implementacion de JDI, que es codigo nativo mas el protocolo de depuracion, y esta VM no lo tiene.
 * El API de {@code com.sun.jdi} esta completo y las llamadas de aca son las que corresponden; lo que
 * falta esta abajo.
 *
 * @since 9
 */
public class JdiInitiator {

    private final int port;
    private final List<String> remoteVMOptions;
    private final String remoteAgent;
    private final boolean isLaunch;
    private final String host;
    private final int timeout;
    private final Map<String, String> connectorOptions;

    private VirtualMachine vm;
    private Process process;

    /**
     * Un iniciador con esa configuracion.
     *
     * @param port el puerto por el que se van a encontrar
     * @param remoteVMOptions las opciones con que arrancar la otra maquina
     * @param remoteAgent la clase principal del agente
     * @param isLaunch si JDI tiene que lanzar el proceso, en vez de esperarlo
     * @param host la maquina a la que conectarse, o {@code null} para la local
     * @param timeout cuanto esperar, en milisegundos
     * @param connectorOptions opciones adicionales del conector
     */
    public JdiInitiator(int port, List<String> remoteVMOptions, String remoteAgent,
            boolean isLaunch, String host, int timeout, Map<String, String> connectorOptions) {
        this.port = port;
        this.remoteVMOptions = remoteVMOptions;
        this.remoteAgent = remoteAgent;
        this.isLaunch = isLaunch;
        this.host = host;
        this.timeout = timeout;
        this.connectorOptions = connectorOptions;
        arrancar();
    }

    /**
     * La maquina virtual del otro lado.
     *
     * @return la maquina
     */
    public VirtualMachine vm() {
        return vm;
    }

    /**
     * El proceso que se lanzo, si se lanzo alguno.
     *
     * @return el proceso, o {@code null}
     */
    public Process process() {
        return process;
    }

    /**
     * Abre el puerto de escucha, lanza el proceso y espera a que se conecte.
     *
     * <p>El orden importa y no es intercambiable: primero se empieza a escuchar y despues se lanza.
     * Al reves, el proceso podria intentar conectarse antes de que haya alguien escuchando y morirse
     * en el intento.
     *
     * @param connectorName el nombre del conector de escucha
     * @param remotePort el puerto
     * @param remoteVMOptions las opciones de la otra maquina
     * @param processStarted a quien avisarle cuando el proceso arranco
     */
    protected void runListenProcess(String connectorName, int remotePort,
            List<String> remoteVMOptions, ProcessStarted processStarted) {
        final ListeningConnector conector = escucha(connectorName);
        final Map<String, Connector.Argument> args = conector.defaultArguments();
        final Connector.Argument p = args.get("port");
        if (p != null) {
            p.setValue(Integer.toString(remotePort));
        }
        try {
            conector.startListening(args);
            final ProcessBuilder pb = new ProcessBuilder(comando(remoteVMOptions, remotePort));
            process = pb.start();
            processStarted.processStarted(process);
            vm = conector.accept(args);
        } catch (Throwable e) {
            throw new IllegalStateException("no se pudo poner en marcha la otra maquina", e);
        }
    }

    /** Arranca segun la forma configurada. */
    private void arrancar() {
        if (isLaunch) {
            throw new IllegalStateException("launch: esta VM no tiene el transporte de JDI");
        }
        runListenProcess("com.sun.jdi.SocketListen", port, remoteVMOptions,
                new ProcessStarted() {
                    public void processStarted(Process proceso) throws Throwable {
                    }
                });
    }

    /** El conector de escucha con ese nombre. */
    private ListeningConnector escucha(String nombre) {
        for (final ListeningConnector c
                : Bootstrap.virtualMachineManager().listeningConnectors()) {
            if (c.name().equals(nombre)) {
                return c;
            }
        }
        throw new IllegalStateException("no hay conector de escucha: " + nombre);
    }

    /** La linea de comando con que se lanza la otra maquina. */
    private List<String> comando(List<String> opciones, int remotePort) {
        final List<String> out = new java.util.ArrayList<String>();
        out.add(System.getProperty("java.home") + "/bin/java");
        out.add("-agentlib:jdwp=transport=dt_socket,address="
                + (host == null ? "" : host + ":") + remotePort + ",suspend=y");
        if (opciones != null) {
            out.addAll(opciones);
        }
        if (connectorOptions != null) {
            for (final Map.Entry<String, String> e : connectorOptions.entrySet()) {
                out.add("-D" + e.getKey() + "=" + e.getValue());
            }
        }
        out.add(remoteAgent);
        out.add(Integer.toString(timeout));
        return out;
    }

    /**
     * A quien avisarle cuando el proceso arranco.
     *
     * <p>Existe para que quien lanza pueda enganchar sus flujos de entrada y salida antes de que el
     * proceso escriba nada. Si se esperara a que la conexion de JDI estuviera lista, lo que el
     * proceso imprimio mientras tanto ya se habria perdido.
     *
     * @since 9
     */
    public interface ProcessStarted {

        /**
         * El proceso ya arranco.
         *
         * @param process el proceso
         * @throws Throwable si el que escucha no pudo hacer lo suyo
         */
        void processStarted(Process process) throws Throwable;
    }
}
