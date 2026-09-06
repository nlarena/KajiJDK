package jdk.jshell.execution;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;

/**
 * El proveedor del motor remoto por JDI.
 *
 * <h2>Los cuatro parametros</h2>
 *
 * <p>{@link #PARAM_REMOTE_AGENT} es la clase principal del proceso que ejecuta; se puede cambiar
 * para poner un agente propio. {@link #PARAM_TIMEOUT} es cuanto esperar a que aparezca.
 * {@link #PARAM_HOST_NAME} es a que interfaz atarse, y vacio quiere decir la de bucle.
 * {@link #PARAM_LAUNCH} elige entre que JDI lance el proceso o que lo espere.
 *
 * <p>Que el nombre de maquina venga vacio por omision es una decision de seguridad: un puerto de
 * depuracion abierto a la red es control total del proceso para cualquiera que llegue.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Los parametros son reales y {@link #defaultParameters} contesta lo mismo que el JDK.
 * {@link #generate} no puede: poner en marcha la otra maquina es el transporte de JDI, que esta VM
 * no tiene. Quien quiera un motor que ande tiene {@link LocalExecutionControlProvider}, y
 * {@link FailOverExecutionControlProvider} llega a el solo.
 *
 * @since 9
 */
public class JdiExecutionControlProvider implements ExecutionControlProvider {

    /** La clase principal del proceso que ejecuta. */
    public static final String PARAM_REMOTE_AGENT = "remoteAgent";

    /** Cuanto esperar a que la otra maquina aparezca, en milisegundos. */
    public static final String PARAM_TIMEOUT = "timeout";

    /** A que interfaz atarse; vacio es la de bucle. */
    public static final String PARAM_HOST_NAME = "hostname";

    /** Si JDI tiene que lanzar el proceso en vez de esperarlo. */
    public static final String PARAM_LAUNCH = "launch";

    private final JdiDefaultExecutionControl.JdiStarter starter;

    /** Un proveedor que pone en marcha la otra maquina de la forma habitual. */
    public JdiExecutionControlProvider() {
        this(null);
    }

    /**
     * Un proveedor que delega en ese iniciador la puesta en marcha.
     *
     * @param starter como aparece la otra maquina, o {@code null} para la forma habitual
     */
    public JdiExecutionControlProvider(JdiDefaultExecutionControl.JdiStarter starter) {
        this.starter = starter;
    }

    /**
     * El nombre con el que se lo pide.
     *
     * @return {@code "jdi"}
     */
    @Override
    public String name() {
        return "jdi";
    }

    /**
     * Los parametros y sus valores por omision.
     *
     * @return el mapa de parametros
     */
    @Override
    public Map<String, String> defaultParameters() {
        final Map<String, String> out = new HashMap<String, String>();
        out.put(PARAM_REMOTE_AGENT, "jdk.jshell.execution.RemoteExecutionControl");
        out.put(PARAM_TIMEOUT, "5000");
        out.put(PARAM_HOST_NAME, "");
        out.put(PARAM_LAUNCH, "false");
        return out;
    }

    /**
     * Pone en marcha la otra maquina y devuelve el motor con el que hablarle.
     *
     * @param env el entorno de la sesion
     * @param parameters los parametros
     * @return el motor
     * @throws IOException si no se pudo poner en marcha
     */
    @Override
    public ExecutionControl generate(ExecutionEnv env, Map<String, String> parameters)
            throws IOException {
        final Map<String, String> ps = parameters == null || parameters.isEmpty()
                ? defaultParameters() : parameters;
        final int timeout = Integer.parseInt(
                ps.get(PARAM_TIMEOUT) == null ? "5000" : ps.get(PARAM_TIMEOUT));
        final String host = ps.get(PARAM_HOST_NAME);
        final boolean lanzar = Boolean.parseBoolean(ps.get(PARAM_LAUNCH));
        final String agente = ps.get(PARAM_REMOTE_AGENT);
        if (starter != null) {
            // Con un iniciador propio, la puesta en marcha es problema suyo: puede lanzar la otra
            // maquina como quiera. Lo que no puede darle esta biblioteca es la conexion de JDI.
            starter.start(env, ps, 0);
        }
        // Sin iniciador propio hay que hacerlo aca, y aca no se puede.
        new JdiInitiator(0, env == null ? null : env.extraRemoteVMOptions(), agente, lanzar,
                host == null || host.isEmpty() ? null : host, timeout,
                new HashMap<String, String>());
        throw new IOException("esta VM no tiene el transporte de JDI: no hay forma de conectarse "
                + "a otra maquina virtual");
    }
}
