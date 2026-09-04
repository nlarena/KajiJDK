package javax.naming.ldap;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

/**
 * Un contexto de directorio que ademas habla las extensiones de LDAP v3.
 *
 * <h2>Que agrega sobre {@link DirContext}</h2>
 *
 * <p>Las dos formas de extensibilidad de LDAP, que {@code javax.naming} no puede tener porque es
 * neutral respecto del protocolo: los {@link Control} y las operaciones extendidas.
 *
 * <h2>Los tres tipos de control, que es lo facil de confundir</h2>
 *
 * <ul>
 * <li><strong>de conexion</strong> — viajan con cada operacion de este contexto y de los que
 *     herede de el. Se fijan al crear el contexto o con {@link #reconnect};</li>
 * <li><strong>de pedido</strong> — solo para las proximas operaciones de <em>este</em> contexto, y
 *     <strong>no</strong> se heredan. Es la diferencia que mas sorprende;</li>
 * <li><strong>de respuesta</strong> — los que mando el servidor con la ultima operacion.</li>
 * </ul>
 *
 * <p>De ahi {@link #newInstance}: para usar otros controles sin pisar los del contexto que ya se
 * tiene, se saca una copia. El contexto original sigue como estaba.
 */
public interface LdapContext extends DirContext {

    /**
     * La propiedad con la lista de fabricas de controles.
     *
     * <p>Separadas por dos puntos, y se consultan en orden hasta que una reconozca el control; ver
     * {@link ControlFactory}.
     */
    String CONTROL_FACTORIES = "java.naming.factory.control";

    /**
     * Ejecuta una operacion extendida.
     *
     * <p>La respuesta la construye el propio pedido — ver {@link ExtendedRequest} para por que.
     */
    ExtendedResponse extendedOperation(ExtendedRequest request) throws NamingException;

    /**
     * Una copia de este contexto con otros controles de pedido.
     *
     * <p>Comparte la conexion: no abre una nueva. Es lo que hace barato tener varias vistas con
     * distinta configuracion sobre el mismo servidor.
     */
    LdapContext newInstance(Control[] requestControls) throws NamingException;

    /**
     * Reconecta con otros controles de conexion.
     *
     * <p>{@code null} saca los que hubiera; un arreglo vacio tambien. La reconexion puede no ser
     * inmediata: el proveedor la puede diferir hasta la proxima operacion.
     */
    void reconnect(Control[] connCtls) throws NamingException;

    /** Los controles de conexion, o {@code null}. */
    Control[] getConnectControls() throws NamingException;

    /** Fija los controles de pedido de este contexto; no se heredan. */
    void setRequestControls(Control[] requestControls) throws NamingException;

    /** Los controles de pedido, o {@code null}. */
    Control[] getRequestControls() throws NamingException;

    /**
     * Los controles que mando el servidor con la ultima operacion.
     *
     * <p>Se leen enseguida: la proxima operacion sobre este contexto los reemplaza.
     */
    Control[] getResponseControls() throws NamingException;
}
