package javax.management;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * La unica via legitima para conseguir un {@link MBeanServer}.
 *
 * <p>Toda la clase gira alrededor de una distincion de dos palabras que es facil de pasar por alto:
 *
 * <ul>
 *   <li>{@code createMBeanServer} <b>guarda</b> el agente en una tabla estatica, y por lo tanto
 *       cualquiera en la misma maquina virtual lo encuentra con {@link #findMBeanServer}. Es lo que
 *       hace que un agente de monitoreo cargado despues pueda engancharse con la aplicacion;
 *   <li>{@code newMBeanServer} <b>no</b> lo guarda. Es un agente privado del que lo pidio.
 * </ul>
 *
 * <p>Y de esa distincion sale el peligro que explica {@link #releaseMBeanServer}: como la tabla es
 * estatica y guarda referencias fuertes, un agente creado con `createMBeanServer` <b>no se junta
 * nunca</b> aunque nadie lo use. Hay que soltarlo a mano. `newMBeanServer` no tiene ese problema
 * justamente porque no lo guarda nadie.
 *
 * <p>{@link #getClassLoaderRepository} esta: `javax.management.loading` ya existe en esta
 * biblioteca, que era lo unico que faltaba.
 */
public class MBeanServerFactory {

    /** No se instancia: es una fabrica estatica. */
    private MBeanServerFactory() {
    }

    /**
     * Los agentes creados con `createMBeanServer`, por `MBeanServerId`.
     *
     * <p>Con el orden de creacion conservado porque {@link #findMBeanServer} con `null` los
     * devuelve todos, y un orden estable es una respuesta reproducible.
     */
    private static final Map<String, MBeanServer> creados =
            new LinkedHashMap<String, MBeanServer>();

    /** El constructor de agentes, resuelto una sola vez. */
    private static MBeanServerBuilder constructor = null;

    /**
     * Suelta la referencia que {@code createMBeanServer} dejo.
     *
     * @throws IllegalArgumentException si el agente no estaba en la tabla --nunca se creo con
     *         `createMBeanServer`, o ya se solto--. Fallar es correcto: soltar dos veces
     *         normalmente significa que alguien cree tener un agente que ya no existe
     */
    public static void releaseMBeanServer(MBeanServer mbeanServer) {
        synchronized (MBeanServerFactory.class) {
            for (Map.Entry<String, MBeanServer> e : creados.entrySet()) {
                if (e.getValue() == mbeanServer) {
                    creados.remove(e.getKey());
                    return;
                }
            }
        }
        throw new IllegalArgumentException("Ese MBeanServer no fue creado con createMBeanServer");
    }

    /** Encontrable, con el dominio por omision. */
    public static MBeanServer createMBeanServer() {
        return createMBeanServer(null);
    }

    /** Encontrable, con el dominio dado. */
    public static MBeanServer createMBeanServer(String domain) {
        MBeanServer s = armar(domain);
        synchronized (MBeanServerFactory.class) {
            creados.put(idDe(s), s);
        }
        return s;
    }

    /** Privado: no queda registrado y se junta cuando nadie lo referencia. */
    public static MBeanServer newMBeanServer() {
        return newMBeanServer(null);
    }

    /** Privado, con el dominio dado. */
    public static MBeanServer newMBeanServer(String domain) {
        return armar(domain);
    }

    private static MBeanServer armar(String domain) {
        MBeanServerBuilder b = builder();
        MBeanServerDelegate d = b.newMBeanServerDelegate();
        // `outer` va en null: no hay envoltorio, el agente se presenta a si mismo.
        return b.newMBeanServer(domain, null, d);
    }

    /**
     * El constructor de agentes, del sistema o el que diga
     * `javax.management.builder.initial`.
     */
    private static synchronized MBeanServerBuilder builder() {
        if (constructor != null) {
            return constructor;
        }
        String clase = System.getProperty("javax.management.builder.initial");
        if (clase == null || clase.length() == 0) {
            constructor = new MBeanServerBuilder();
        } else {
            try {
                constructor = (MBeanServerBuilder)
                        Class.forName(clase).getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                // La especificacion pide fallar: caer en silencio al constructor del sistema
                // dejaria al que configuro la propiedad creyendo que su agente esta corriendo.
                throw new JMRuntimeException(
                    "No se pudo instanciar el MBeanServerBuilder " + clase + ": " + e);
            }
        }
        return constructor;
    }

    private static String idDe(MBeanServer s) {
        try {
            return (String) s.getAttribute(MBeanServerDelegate.DELEGATE_NAME, "MBeanServerId");
        } catch (Exception e) {
            // Un agente sin delegado no cumple la especificacion, pero la tabla necesita una clave
            // igual; la identidad del objeto alcanza y no colisiona.
            return "sin-id-" + System.identityHashCode(s);
        }
    }

    /**
     * Los agentes encontrables.
     *
     * @param agentId `null` los devuelve todos; si no, el que tenga ese `MBeanServerId`
     */
    public static synchronized ArrayList<MBeanServer> findMBeanServer(String agentId) {
        ArrayList<MBeanServer> r = new ArrayList<MBeanServer>();
        if (agentId == null) {
            r.addAll(creados.values());
        } else {
            MBeanServer s = creados.get(agentId);
            if (s != null) {
                r.add(s);
            }
        }
        return r;
    }

    /**
     * El repositorio de cargadores de ese agente.
     *
     * <p>Es la lista de cargadores que el agente consulta para cargar una clase cuyo nombre le llego
     * de afuera. Se arma en cada consulta y no se congela, porque un cargador se registra como
     * cualquier otro MBean y en cualquier momento.
     *
     * @throws IllegalArgumentException si {@code server} es null, o si no es un agente de esta
     *     fabrica -- un agente ajeno tiene sus propios cargadores y esta clase no los conoce;
     *     devolver los de otro seria peor que fallar
     */
    public static javax.management.loading.ClassLoaderRepository getClassLoaderRepository(
            MBeanServer server) {
        if (server instanceof LocalServer) {
            return ((LocalServer) server).getClassLoaderRepository();
        }
        throw new IllegalArgumentException(
                server == null ? "server is null" : "not an MBeanServer of this factory");
    }
}
