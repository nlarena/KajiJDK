package javax.imageio.spi;

/**
 * KajiLibrary's javax.imageio.spi.RegisterableService -- el proveedor quiere enterarse de que lo
 * registran.
 *
 * <p>Es opcional: un proveedor que no la implemente funciona igual. Sirve para dos cosas concretas:
 *
 * <ul>
 *   <li><b>declarar preferencias</b>. Al registrarse, un proveedor puede llamar
 *       {@code ServiceRegistry.setOrdering} para decir que va antes que otro. Es la unica forma de que
 *       un lector especializado gane sobre uno generico;
 *   <li><b>liberar</b>. {@link #onDeregistration} es donde se cierra lo que se haya abierto.
 * </ul>
 *
 * <p>Un mismo proveedor puede estar registrado en varias categorias --leer y escribir, por ejemplo--,
 * y por eso los dos metodos reciben <b>cual</b>: los avisos llegan una vez por categoria.
 */
public interface RegisterableService {

    /**
     * Lo acaban de registrar en esa categoria.
     *
     * @param category en cual; ver la nota de la clase
     */
    void onRegistration(ServiceRegistry registry, Class<?> category);

    /** Lo acaban de dar de baja de esa categoria. */
    void onDeregistration(ServiceRegistry registry, Class<?> category);
}
