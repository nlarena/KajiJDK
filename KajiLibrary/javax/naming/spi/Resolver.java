package javax.naming.spi;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.spi.Resolver -- resuelve lo que puede y devuelve el resto.
 *
 * <p>Es para los contextos <b>parciales</b>: los que entienden el principio de un nombre pero no el
 * final. Un servicio de nombres que resuelve {@code /empresa/rrhh} y de ahi en adelante delega en
 * otro sistema implementa esto en vez de {@link Context}.
 *
 * <p>Lo que devuelve es un {@link ResolveResult}: hasta donde llego, y que quedo sin resolver. Quien
 * llama sigue con el resto contra lo que se resolvio.
 *
 * <p>Se le pasa la <b>clase</b> de contexto que se busca, y ahi esta lo util: un resolvedor puede
 * parar cuando llega a algo que es un {@code DirContext} y no seguir bajando. Sin eso habria que
 * resolver de a un componente y preguntar el tipo en cada paso.
 */
public interface Resolver {

    /**
     * Resuelve hasta encontrar un contexto de esa clase.
     *
     * @param contextType la clase que se busca
     * @throws NamingException si no se puede resolver ni el principio
     */
    ResolveResult resolveToClass(Name name, Class<? extends Context> contextType)
        throws NamingException;

    /** Idem, con el nombre como texto. */
    ResolveResult resolveToClass(String name, Class<? extends Context> contextType)
        throws NamingException;
}
