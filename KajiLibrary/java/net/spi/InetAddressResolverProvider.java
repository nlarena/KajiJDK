package java.net.spi;

/**
 * KajiLibrary's java.net.spi.InetAddressResolverProvider -- de donde sale el resolvedor.
 *
 * <p>Se carga como servicio y la plataforma toma <b>uno solo</b>: la resolucion de nombres es global
 * al proceso y no tendria sentido que dos partes del programa vieran Internet distinto. Por eso hay
 * {@link #name()}, que sirve para poder decir cual quedo cuando algo no resuelve como se esperaba.
 *
 * <p>{@link Configuration#builtinResolver()} es la pieza importante del diseño: le entrega al
 * proveedor el resolvedor del sistema, asi que lo normal no es reemplazar la resolucion sino
 * <b>envolverla</b> -- resolver unos pocos nombres propios y delegar el resto. Sin eso, cualquier
 * proveedor tendria que reimplementar DNS entero para poder interceptar un nombre.
 */
public abstract class InetAddressResolverProvider {

    /** Para las subclases. */
    protected InetAddressResolverProvider() {
    }

    /**
     * El resolvedor de este proveedor.
     *
     * @param configuration lo que la plataforma le presta; ver la nota de la clase sobre envolver
     */
    public abstract InetAddressResolver get(Configuration configuration);

    /** Un nombre para identificarlo en diagnosticos. */
    public abstract String name();

    /** Lo que la plataforma le da al proveedor cuando le pide el resolvedor. */
    public interface Configuration {

        /** El resolvedor del sistema, para delegarle lo que no se quiera manejar. */
        InetAddressResolver builtinResolver();

        /**
         * El nombre local de esta maquina.
         *
         * <p>Va aca --y no se busca-- porque averiguarlo suele necesitar resolver, y el proveedor
         * todavia no existe cuando se lo esta construyendo.
         */
        String lookupLocalHostName();
    }
}
