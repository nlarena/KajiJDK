package javax.naming.event;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;

/**
 * KajiLibrary's javax.naming.event.EventDirContext -- escuchar el resultado de una <b>busqueda</b>.
 *
 * <p>Junta {@link EventContext} y {@link DirContext}, y lo que agrega de propio es lo interesante: en
 * vez de escuchar un nombre, se escucha un <b>filtro</b>. El oyente recibe eventos por las entradas
 * que coinciden, incluidas las que pasan a coincidir despues.
 *
 * <p>Eso es mucho mas util que escuchar un nombre para lo que se usan los directorios: "avisame
 * cuando alguien entre al grupo de administradores" es un filtro, no un nombre. Con un nombre habria
 * que escuchar el grupo entero y filtrar del lado del cliente.
 *
 * <p>Las cuatro sobrecargas son las mismas dos combinaciones que en {@code DirContext#search}:
 * nombre como {@link Name} o como texto, y filtro directo o con argumentos numerados. Vale la misma
 * advertencia: un filtro armado concatenando texto es inyectable, y la version con argumentos es la
 * que hay que usar cuando el filtro depende de una entrada del usuario.
 *
 * <p>El {@link SearchControls} de aca controla el alcance y que atributos vienen en los eventos, no
 * cuantos resultados: una suscripcion no tiene un tope de resultados que tenga sentido.
 */
public interface EventDirContext extends EventContext, DirContext {

    /**
     * Escucha las entradas que coincidan con el filtro.
     *
     * @param filter con la sintaxis de RFC 2254
     * @param ctls el alcance y que atributos traer
     */
    void addNamingListener(Name target, String filter, SearchControls ctls, NamingListener l)
        throws NamingException;

    /** Idem, con el nombre como texto. */
    void addNamingListener(String target, String filter, SearchControls ctls, NamingListener l)
        throws NamingException;

    /**
     * Idem, con argumentos numerados.
     *
     * @param filterArgs los valores de {@code {0}}, {@code {1}}, ...; no pasan por el parser
     */
    void addNamingListener(Name target, String filter, Object[] filterArgs, SearchControls ctls,
                           NamingListener l) throws NamingException;

    /** Idem, con el nombre como texto. */
    void addNamingListener(String target, String filter, Object[] filterArgs, SearchControls ctls,
                           NamingListener l) throws NamingException;
}
