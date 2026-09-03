package javax.naming;

import java.util.Hashtable;

/**
 * Un conjunto de ataduras nombre-objeto, y las operaciones para consultarlo y cambiarlo.
 *
 * <p>Es la interfaz central de JNDI. Un contexto es un directorio: tiene nombres atados a objetos,
 * y algunos de esos objetos son a su vez contextos, lo que arma el arbol. Resolver `a/b/c` es
 * buscar `a` en este contexto, comprobar que lo que salio es un contexto, y seguir ahi con `b/c`
 * --exactamente igual que un sistema de archivos--.
 *
 * <h2>Por que cada operacion esta dos veces</h2>
 *
 * <p>Cada metodo tiene una version con `Name` y otra con `String`. No es azucar: la de `String`
 * parsea la cadena **con la sintaxis del contexto** y despues hace lo mismo. Sirve para el caso
 * comun --escribir el nombre a mano-- pero pierde en el caso general, porque un nombre que
 * atraviesa varios espacios de nombres no tiene una sola sintaxis. Cuando el nombre se arma
 * programaticamente, la version con `Name` es la correcta.
 *
 * <h2>Las propiedades del entorno</h2>
 *
 * <p>Las catorce constantes son claves de un `Hashtable` de configuracion que viaja con el
 * contexto y se hereda a los subcontextos. La mas importante de todas es
 * `INITIAL_CONTEXT_FACTORY`: es la que dice **quien** implementa todo esto. Sin ella no hay
 * proveedor y no hay nada --ver `InitialContext`--.
 *
 * <h2>Que se puede prometer aca y que no</h2>
 *
 * <p>Esta es una **interfaz**, y declararla entera es honesto justamente por eso: un contrato no
 * promete que alguien lo cumpla. Lo que no se podria hacer sin proveedor es dar una clase que
 * finja atender un `lookup`. La unica implementacion del paquete es `InitialContext`, que no
 * atiende nada: delega, y cuando no hay a quien delegar lo dice con `NoInitialContextException`.
 *
 * <p>No es `AutoCloseable` --el JDK tampoco la hizo-- asi que `close()` va a mano o en un
 * `finally`. Cerrar un contexto **no** invalida los objetos que se sacaron de el.
 */
public interface Context {

    // ---- quien implementa: la clave que decide si hay JNDI o no --------------------------------------

    /**
     * Nombre de la clase --implementacion de `javax.naming.spi.InitialContextFactory`-- que
     * fabrica el contexto inicial. Es la unica propiedad sin la cual nada funciona.
     */
    String INITIAL_CONTEXT_FACTORY = "java.naming.factory.initial";

    // ---- fabricas de objetos ------------------------------------------------------------------------

    /** Lista de fabricas, separadas por `:`, que convierten `Reference`s en objetos vivos. */
    String OBJECT_FACTORIES = "java.naming.factory.object";

    /** Lista de fabricas, separadas por `:`, que hacen el camino inverso al atar un objeto. */
    String STATE_FACTORIES = "java.naming.factory.state";

    /**
     * Prefijos de paquete, separados por `:`, donde buscar contextos de URL.
     *
     * <p>Resolver un nombre que empieza con `java:` busca la clase
     * `<prefijo>.java.javaURLContextFactory`. La lista siempre termina, implicitamente, en
     * `com.sun.jndi.url`.
     */
    String URL_PKG_PREFIXES = "java.naming.factory.url.pkgs";

    // ---- a donde conectarse -------------------------------------------------------------------------

    /** La URL del servicio --`ldap://host:389/o=empresa`-- para el proveedor inicial. */
    String PROVIDER_URL = "java.naming.provider.url";

    /** Servidores DNS a usar, cuando el proveedor los necesita para ubicar al servicio. */
    String DNS_URL = "java.naming.dns.url";

    // ---- como comportarse ---------------------------------------------------------------------------

    /** `"true"` pide la fuente mas autorizada, que suele ser mas lenta y siempre mas fresca. */
    String AUTHORITATIVE = "java.naming.authoritative";

    /** Cuantos resultados traer por viaje. Es un consejo al proveedor, no un limite. */
    String BATCHSIZE = "java.naming.batchsize";

    /**
     * Que hacer con los referrals: `"follow"` los sigue solo, `"throw"` los lanza como
     * `ReferralException` para que decida el que llama, `"ignore"` los descarta.
     */
    String REFERRAL = "java.naming.referral";

    // ---- seguridad ----------------------------------------------------------------------------------

    /** El protocolo de seguridad --por ejemplo `"ssl"`--. */
    String SECURITY_PROTOCOL = "java.naming.security.protocol";

    /** El mecanismo: `"none"`, `"simple"`, `"strong"`, o uno propio del proveedor. */
    String SECURITY_AUTHENTICATION = "java.naming.security.authentication";

    /** Quien se dice ser. */
    String SECURITY_PRINCIPAL = "java.naming.security.principal";

    /** Con que lo prueba. */
    String SECURITY_CREDENTIALS = "java.naming.security.credentials";

    /** Idioma preferido para lo que devuelva el servicio, en la forma de RFC 1766. */
    String LANGUAGE = "java.naming.language";

    // ---- resolver -----------------------------------------------------------------------------------

    /**
     * Resuelve el nombre.
     *
     * <p>Si lo atado es un enlace, lo sigue; si es una `Reference`, la convierte en objeto. El
     * nombre vacio devuelve una copia de este contexto, que es la manera pactada de duplicarlo.
     */
    Object lookup(Name name) throws NamingException;

    Object lookup(String name) throws NamingException;

    /** Como `lookup`, pero **sin** seguir el ultimo enlace: devuelve el enlace en si. */
    Object lookupLink(Name name) throws NamingException;

    Object lookupLink(String name) throws NamingException;

    // ---- atar y desatar -----------------------------------------------------------------------------

    /** Ata, y falla con `NameAlreadyBoundException` si el nombre ya estaba. */
    void bind(Name name, Object obj) throws NamingException;

    void bind(String name, Object obj) throws NamingException;

    /** Ata pisando lo que hubiera. Es `bind` sin la negativa. */
    void rebind(Name name, Object obj) throws NamingException;

    void rebind(String name, Object obj) throws NamingException;

    /**
     * Desata el nombre.
     *
     * <p>Desatar un nombre que no estaba atado **no** es error --es idempotente-- salvo que algun
     * componente intermedio no exista.
     */
    void unbind(Name name) throws NamingException;

    void unbind(String name) throws NamingException;

    void rename(Name oldName, Name newName) throws NamingException;

    void rename(String oldName, String newName) throws NamingException;

    // ---- listar -------------------------------------------------------------------------------------

    /**
     * Los nombres atados en este contexto con la clase de cada uno, sin traer los objetos.
     *
     * <p>Va aparte de `listBindings` porque construir los objetos puede ser carisimo --cada uno
     * puede ser una conexion-- y para mostrar un arbol alcanza con los nombres.
     */
    NamingEnumeration<NameClassPair> list(Name name) throws NamingException;

    NamingEnumeration<NameClassPair> list(String name) throws NamingException;

    /** Como `list`, pero construyendo cada objeto. */
    NamingEnumeration<Binding> listBindings(Name name) throws NamingException;

    NamingEnumeration<Binding> listBindings(String name) throws NamingException;

    // ---- subcontextos -------------------------------------------------------------------------------

    /** Destruye el subcontexto; falla con `ContextNotEmptyException` si tiene algo adentro. */
    void destroySubcontext(Name name) throws NamingException;

    void destroySubcontext(String name) throws NamingException;

    Context createSubcontext(Name name) throws NamingException;

    Context createSubcontext(String name) throws NamingException;

    // ---- sintaxis y composicion ---------------------------------------------------------------------

    /** El parser del espacio de nombres donde vive `name`; ver `NameParser`. */
    NameParser getNameParser(Name name) throws NamingException;

    NameParser getNameParser(String name) throws NamingException;

    /**
     * Compone un nombre relativo a este contexto con el nombre de este contexto relativo a otro.
     *
     * <p>No es concatenar: la composicion la hace el proveedor porque puede tener que cambiar la
     * sintaxis --y hasta el orden-- al cruzar de un espacio de nombres al otro.
     */
    Name composeName(Name name, Name prefix) throws NamingException;

    String composeName(String name, String prefix) throws NamingException;

    // ---- entorno y ciclo de vida --------------------------------------------------------------------

    /** Agrega o pisa una propiedad; devuelve el valor anterior. */
    Object addToEnvironment(String propName, Object propVal) throws NamingException;

    Object removeFromEnvironment(String propName) throws NamingException;

    /** El entorno efectivo. El `Hashtable` que devuelve no hay que modificarlo. */
    Hashtable<?, ?> getEnvironment() throws NamingException;

    /**
     * Suelta los recursos del contexto.
     *
     * <p>Invocarlo dos veces no es error. Los objetos que salieron de este contexto siguen
     * sirviendo despues de cerrarlo.
     */
    void close() throws NamingException;

    /**
     * El nombre completo de este contexto **en su propio espacio de nombres**.
     *
     * <p>Tira `OperationNotSupportedException` cuando el espacio de nombres no tiene un nombre
     * absoluto para el contexto, que es el caso de todos los espacios con mas de una raiz.
     */
    String getNameInNamespace() throws NamingException;
}
