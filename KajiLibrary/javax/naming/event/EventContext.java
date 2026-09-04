package javax.naming.event;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.event.EventContext -- un contexto al que se le puede escuchar.
 *
 * <p>Agrega a {@link Context} el registro de oyentes. Los tres alcances dicen <b>que parte</b> del
 * arbol se escucha, y la eleccion tiene consecuencias de costo directas: {@link #SUBTREE_SCOPE}
 * sobre una raiz grande es una suscripcion que el servidor tiene que mantener sobre todo.
 *
 * <h2>El nombre puede no existir todavia</h2>
 *
 * <p>{@link #targetMustExist} contesta si esta implementacion permite escuchar un nombre que
 * <b>todavia no esta</b>. Cuando lo permite, se puede esperar a que algo aparezca; cuando no,
 * registrar sobre un nombre inexistente falla con {@code NameNotFoundException}.
 *
 * <p>Hay una carrera inevitable ahi y conviene tenerla presente: entre consultar y suscribirse, la
 * entrada puede cambiar. La forma de no perder ese cambio es suscribirse <b>primero</b> y consultar
 * despues.
 *
 * <h2>Dar de baja es por oyente, no por nombre</h2>
 *
 * <p>{@link #removeNamingListener} recibe el oyente y saca <b>todas</b> sus suscripciones en este
 * contexto. No hay forma de sacar una sola: un oyente registrado sobre tres nombres se da de baja de
 * los tres o de ninguno. Si hace falta granularidad, van oyentes distintos.
 */
public interface EventContext extends Context {

    /** Solo la entrada nombrada. */
    public static final int OBJECT_SCOPE = 0;

    /** Sus hijos directos, sin ella. */
    public static final int ONELEVEL_SCOPE = 1;

    /** Ella y todo su subarbol. */
    public static final int SUBTREE_SCOPE = 2;

    /**
     * Registra un oyente.
     *
     * @param scope uno de los tres de arriba
     * @throws NamingException si el nombre no existe y {@link #targetMustExist} es true
     */
    void addNamingListener(Name target, int scope, NamingListener l) throws NamingException;

    /** Idem, con el nombre como texto. */
    void addNamingListener(String target, int scope, NamingListener l) throws NamingException;

    /** Saca todas las suscripciones de ese oyente. Ver la nota de la clase. */
    void removeNamingListener(NamingListener l) throws NamingException;

    /** Si el nombre tiene que existir para poder escucharlo. */
    boolean targetMustExist() throws NamingException;
}
