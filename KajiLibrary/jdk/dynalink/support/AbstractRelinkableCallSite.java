package jdk.dynalink.support;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MutableCallSite;
import java.util.Objects;

import jdk.dynalink.CallSiteDescriptor;
import jdk.dynalink.RelinkableCallSite;

/**
 * La base de un sitio de invocacion reenlazable: guarda el descriptor y hace la instalacion inicial.
 *
 * <h2>Que resuelve el {@code initialize}</h2>
 *
 * <p>El problema del huevo y la gallina de un sitio dinamico. Cuando la JVM llega por primera vez a
 * un {@code invokedynamic}, el sitio todavia no sabe a que llamar — para saberlo hace falta ver los
 * argumentos, y para ver los argumentos hace falta que la llamada ocurra.
 *
 * <p>La salida es instalar como destino inicial un metodo que <strong>enlaza y despues invoca</strong>:
 * la primera llamada entra ahi, ese metodo mira los argumentos, decide, se reinstala como destino y
 * recien entonces llama. De la segunda vez en adelante el sitio ya apunta a lo que corresponde.
 *
 * <h2>Por que extiende {@link MutableCallSite}</h2>
 *
 * <p>Porque el destino tiene que poder cambiar despues de instalado, que es exactamente lo que
 * "reenlazable" significa. Un {@code ConstantCallSite} no serviria, y un {@code VolatileCallSite}
 * seria mas caro sin hacer falta: la carrera entre dos hilos que reenlazan el mismo sitio termina
 * con uno de los dos destinos puestos, y los dos son correctos.
 *
 * <h2>Lo que no hace</h2>
 *
 * <p>No implementa {@code relink} ni {@code resetAndRelink}: esa es la estrategia de cache y es lo
 * que distingue a {@link SimpleRelinkableCallSite} de {@link ChainedCallSite}.
 *
 * @since 9
 */
public abstract class AbstractRelinkableCallSite extends MutableCallSite
        implements RelinkableCallSite {

    private final CallSiteDescriptor descriptor;

    /**
     * Un sitio con ese descriptor.
     *
     * @param descriptor el descriptor; su firma es la del sitio
     * @throws NullPointerException si es {@code null}
     */
    protected AbstractRelinkableCallSite(final CallSiteDescriptor descriptor) {
        super(descriptor.getMethodType());
        this.descriptor = descriptor;
    }

    /**
     * El descriptor, que no cambia en toda la vida del sitio.
     *
     * @return el descriptor
     */
    public CallSiteDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * Instala el metodo que enlaza y despues invoca.
     *
     * <p>Lo llama {@code DynamicLinker.link} una sola vez. Volver a llamarlo tirando lo que el sitio
     * hubiera aprendido no esta previsto por el contrato.
     *
     * @param relinkAndInvoke el metodo que enlaza y despues invoca
     */
    public void initialize(final MethodHandle relinkAndInvoke) {
        setTarget(Objects.requireNonNull(relinkAndInvoke));
    }
}
