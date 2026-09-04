package javax.naming.spi;

import java.util.Hashtable;
import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.spi.InitialContextFactoryBuilder -- decide que proveedor se usa.
 *
 * <p>Lo mismo que {@link ObjectFactoryBuilder} pero un nivel mas arriba: instalado uno, la propiedad
 * {@code java.naming.factory.initial} <b>deja de mirarse</b> y este constructor decide el proveedor
 * de cada contexto inicial.
 *
 * <p>Es lo que usa un contenedor de aplicaciones para que cada aplicacion vea su propio arbol JNDI
 * sin que ninguna pueda pedir el de otra. Se instala una sola vez por proceso, y esa unicidad es
 * justamente lo que lo hace confiable como frontera.
 */
public interface InitialContextFactoryBuilder {

    /**
     * La fabrica de contexto inicial para ese ambiente.
     *
     * @throws NamingException si no se puede crear ninguna
     */
    InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment)
        throws NamingException;
}
