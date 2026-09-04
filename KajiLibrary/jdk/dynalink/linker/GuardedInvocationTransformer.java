package jdk.dynalink.linker;

/**
 * Ve cada invocacion ya enlazada, con el pedido que la produjo, y puede cambiarla.
 *
 * <h2>Por que no es un {@link MethodHandleTransformer}</h2>
 *
 * <p>Porque recibe el {@link LinkRequest} y los {@link LinkerServices} ademas del metodo. Eso lo
 * habilita a decidir <strong>segun el sitio</strong>: envolver solo las lecturas de propiedad,
 * agregar una guarda extra cuando el receptor es de cierta clase, contar invocaciones por
 * operacion. Un transformador de metodos a secas no tiene con que distinguir.
 *
 * <p>Es el punto de enganche que usa quien hospeda a Dynalink para instrumentar todos los enlaces
 * sin tocar ningun enlazador.
 *
 * @since 9
 */
@FunctionalInterface
public interface GuardedInvocationTransformer {

    /**
     * La invocacion transformada.
     *
     * @param inv la invocacion enlazada
     * @param linkRequest el pedido que la produjo
     * @param linkerServices los servicios del que hospeda
     * @return la transformada, o {@code inv} si no hay nada que cambiar
     */
    GuardedInvocation filter(GuardedInvocation inv, LinkRequest linkRequest,
            LinkerServices linkerServices);
}
