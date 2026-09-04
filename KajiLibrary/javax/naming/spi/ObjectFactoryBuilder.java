package javax.naming.spi;

import java.util.Hashtable;
import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.spi.ObjectFactoryBuilder -- decide que fabrica atiende cada dato.
 *
 * <p>Un nivel mas de indireccion sobre {@link ObjectFactory}, y sirve para tomarle el control a la
 * plataforma: instalado un constructor, la busqueda por omision --que lee el nombre de la clase de la
 * propia {@code Reference} y la carga-- <b>deja de usarse</b>.
 *
 * <p>Eso es lo que lo hace interesante y lo que lo hace peligroso. Cargar una clase que nombra el
 * dato guardado en el directorio es ejecutar codigo elegido por quien escribio en el directorio;
 * instalar un constructor propio es la forma de cortar eso de raiz.
 *
 * <p>Se instala <b>una sola vez por proceso</b> con {@link NamingManager#setObjectFactoryBuilder}, y
 * el segundo intento falla.
 */
public interface ObjectFactoryBuilder {

    /**
     * La fabrica que atiende esos datos.
     *
     * @throws NamingException si no se puede crear ninguna
     */
    ObjectFactory createObjectFactory(Object obj, Hashtable<?, ?> environment)
        throws NamingException;
}
