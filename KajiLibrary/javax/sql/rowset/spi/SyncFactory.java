package javax.sql.rowset.spi;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * El registro de proveedores de sincronizacion: donde un {@code RowSet} consigue el suyo.
 *
 * <h2>Todo estatico, y por que</h2>
 *
 * <p>Porque el registro es del proceso. Que haya {@link #getSyncFactory()} devolviendo una instancia
 * es historico —la clase se penso como una fabrica con estado y termino siendo una tabla global— y
 * ninguno de los metodos de instancia existe. Se conserva porque es API.
 *
 * <h2>De donde salen los proveedores</h2>
 *
 * <p>De cuatro lados, y el orden importa porque el ultimo que registra un identificador gana:
 *
 * <ol>
 *   <li>el proveedor por omision, que siempre esta;
 *   <li>la propiedad de sistema {@link #ROWSET_SYNC_PROVIDER};
 *   <li>los servicios declarados que {@link ServiceLoader} encuentre;
 *   <li>lo que alguien registre a mano con {@link #registerProvider}.
 * </ol>
 *
 * <p>La idea es que una aplicacion pueda cambiar el proveedor sin tocar codigo —con una propiedad—
 * y que una biblioteca pueda aportar el suyo con solo estar en el classpath.
 *
 * <h2>Un identificador es un nombre de clase</h2>
 *
 * <p>No hay tabla de nombres a implementaciones: el identificador <strong>es</strong> el nombre
 * completo de la clase, y {@link #getInstance} la carga por reflexion. Es lo que permite registrar
 * un proveedor sin que esta clase lo conozca.
 *
 * <p>La consecuencia es que registrar no valida nada: un identificador se acepta al registrarlo y
 * recien falla al pedir la instancia, si la clase no esta o no es un {@link SyncProvider}.
 *
 * @since 1.5
 */
public class SyncFactory {

    /** La propiedad de sistema con el nombre de clase del proveedor. */
    public static final String ROWSET_SYNC_PROVIDER = "rowset.provider.classname";

    /** La propiedad de sistema con el nombre del fabricante. */
    public static final String ROWSET_SYNC_VENDOR = "rowset.provider.vendor";

    /** La propiedad de sistema con la version del proveedor. */
    public static final String ROWSET_SYNC_PROVIDER_VERSION = "rowset.provider.version";

    /**
     * El proveedor por omision del JDK.
     *
     * <p>Esta biblioteca no lo trae, asi que pedirlo falla con {@link SyncFactoryException}
     * diciendo que la clase no esta. Se conserva el nombre porque es el que la especificacion
     * nombra y el que una aplicacion portada va a pedir.
     */
    private static final String POR_OMISION = "com.sun.rowset.providers.RIOptimisticProvider";

    /** Identificador de proveedor a nombre de clase; hoy son lo mismo, ver la nota de la clase. */
    private static final Map<String, String> registrados = new TreeMap<String, String>();

    private static final SyncFactory INSTANCIA = new SyncFactory();

    private static Logger logger;
    private static Context contextoJNDI;
    private static boolean inicializada;

    private SyncFactory() {
    }

    /**
     * Carga los proveedores de las tres fuentes automaticas, una sola vez.
     *
     * <p>Es perezosa y no un inicializador estatico: recorrer el {@link ServiceLoader} carga clases
     * de terceros, y eso no deberia pasar por el solo hecho de que alguien nombre esta clase.
     */
    private static synchronized void inicializar() {
        if (inicializada) {
            return;
        }
        inicializada = true;
        registrados.put(POR_OMISION, POR_OMISION);

        final String delSistema = System.getProperty(ROWSET_SYNC_PROVIDER);
        if (delSistema != null && delSistema.length() > 0) {
            registrados.put(delSistema, delSistema);
        }

        try {
            for (final SyncProvider p : ServiceLoader.load(SyncProvider.class)) {
                registrados.put(p.getProviderID(), p.getClass().getName());
            }
        } catch (final java.util.ServiceConfigurationError e) {
            // Un servicio mal declarado no puede tumbar a los que si estan bien: se anota y se
            // sigue con el resto.
            if (logger != null) {
                logger.log(Level.WARNING, "un proveedor declarado no se pudo cargar", e);
            }
        }
    }

    /**
     * Registra un proveedor por su identificador, que es el nombre completo de su clase.
     *
     * <p>No valida: la clase se busca recien en {@link #getInstance}.
     *
     * @param providerID el identificador
     * @throws SyncFactoryException si el identificador es {@code null} o vacio
     */
    public static synchronized void registerProvider(final String providerID)
            throws SyncFactoryException {
        if (providerID == null || providerID.length() == 0) {
            throw new SyncFactoryException("el identificador de proveedor no puede ser vacio");
        }
        inicializar();
        registrados.put(providerID, providerID);
    }

    /**
     * La instancia de la fabrica.
     *
     * <p>No sirve para nada: todos los metodos utiles son estaticos. Existe porque la API la
     * declara.
     *
     * @return la instancia
     */
    public static SyncFactory getSyncFactory() {
        return INSTANCIA;
    }

    /**
     * Saca un proveedor del registro.
     *
     * @param providerID el identificador
     * @throws SyncFactoryException si el identificador es {@code null} o vacio
     */
    public static synchronized void unregisterProvider(final String providerID)
            throws SyncFactoryException {
        if (providerID == null || providerID.length() == 0) {
            throw new SyncFactoryException("el identificador de proveedor no puede ser vacio");
        }
        inicializar();
        registrados.remove(providerID);
    }

    /**
     * Una instancia nueva del proveedor con ese identificador.
     *
     * <p>Nueva y no compartida: un proveedor tiene estado —el nivel de candado, por ejemplo— y dos
     * {@code RowSet} distintos no deberian pisarselo.
     *
     * @param providerID el identificador
     * @return el proveedor
     * @throws SyncFactoryException si el identificador es vacio, la clase no esta, no es un
     *     {@link SyncProvider}, o no se pudo instanciar
     */
    public static SyncProvider getInstance(final String providerID) throws SyncFactoryException {
        if (providerID == null || providerID.length() == 0) {
            throw new SyncFactoryException("el identificador de proveedor no puede ser vacio");
        }
        inicializar();
        final String clase;
        synchronized (SyncFactory.class) {
            final String r = registrados.get(providerID);
            clase = r != null ? r : providerID;
        }
        try {
            final Class<?> c = Class.forName(clase, true,
                    Thread.currentThread().getContextClassLoader());
            final Object o = c.getDeclaredConstructor().newInstance();
            if (!(o instanceof SyncProvider)) {
                throw new SyncFactoryException(clase + " no es un SyncProvider");
            }
            return (SyncProvider) o;
        } catch (final SyncFactoryException e) {
            throw e;
        } catch (final ClassNotFoundException e) {
            final SyncFactoryException s =
                    new SyncFactoryException("no se encontro la clase del proveedor " + clase);
            s.initCause(e);
            throw s;
        } catch (final ReflectiveOperationException e) {
            final SyncFactoryException s =
                    new SyncFactoryException("no se pudo instanciar el proveedor " + clase);
            s.initCause(e);
            throw s;
        }
    }

    /**
     * Los proveedores registrados, ya instanciados.
     *
     * <p>Los que no se puedan instanciar se saltean en vez de hacer fallar la enumeracion entera:
     * un proveedor roto no deberia esconder a los que andan.
     *
     * @return la enumeracion
     * @throws SyncFactoryException si el registro no se pudo leer
     */
    public static Enumeration<SyncProvider> getRegisteredProviders()
            throws SyncFactoryException {
        inicializar();
        final Vector<SyncProvider> out = new Vector<SyncProvider>();
        final String[] ids;
        synchronized (SyncFactory.class) {
            ids = registrados.keySet().toArray(new String[registrados.size()]);
        }
        for (int i = 0; i < ids.length; i++) {
            try {
                out.add(getInstance(ids[i]));
            } catch (final SyncFactoryException e) {
                if (logger != null) {
                    logger.log(Level.FINE, "proveedor no instanciable: " + ids[i], e);
                }
            }
        }
        return out.elements();
    }

    /**
     * Fija el registro por donde la fabrica deja rastro.
     *
     * @param logger el registro
     * @throws NullPointerException si es {@code null}
     */
    public static void setLogger(final Logger logger) {
        if (logger == null) {
            throw new NullPointerException("el logger no puede ser null");
        }
        SyncFactory.logger = logger;
    }

    /**
     * Fija el registro y su nivel.
     *
     * @param logger el registro
     * @param level el nivel
     * @throws NullPointerException si el registro es {@code null}
     */
    public static void setLogger(final Logger logger, final Level level) {
        if (logger == null) {
            throw new NullPointerException("el logger no puede ser null");
        }
        logger.setLevel(level);
        SyncFactory.logger = logger;
    }

    /**
     * El registro que se fijo.
     *
     * <p>Falla si no se fijo ninguno, en vez de devolver uno por omision: quien pide el logger
     * quiere el que configuro, y devolverle otro haria que sus mensajes salieran por un lado que no
     * espera.
     *
     * @return el registro
     * @throws SyncFactoryException si no se fijo ninguno
     */
    public static Logger getLogger() throws SyncFactoryException {
        final Logger l = logger;
        if (l == null) {
            throw new SyncFactoryException("(SyncFactory) : No logger has been set");
        }
        return l;
    }

    /**
     * Fija un contexto JNDI del cual leer proveedores registrados en el directorio.
     *
     * <p>Es para un servidor de aplicaciones, que publica sus proveedores en el arbol JNDI en vez
     * de en una propiedad de sistema. Lo que se busca son objetos {@link SyncProvider}; el resto de
     * lo que haya en el contexto se ignora.
     *
     * @param ctx el contexto
     * @throws SyncFactoryException si es {@code null} o no se pudo recorrer
     */
    public static synchronized void setJNDIContext(final Context ctx) throws SyncFactoryException {
        if (ctx == null) {
            throw new SyncFactoryException("el contexto JNDI no puede ser null");
        }
        inicializar();
        contextoJNDI = ctx;
        try {
            final Hashtable<String, SyncProvider> hallados =
                    new Hashtable<String, SyncProvider>();
            recorrer(ctx, hallados);
            for (final Map.Entry<String, SyncProvider> e : hallados.entrySet()) {
                registrados.put(e.getKey(), e.getValue().getClass().getName());
            }
        } catch (final NamingException e) {
            final SyncFactoryException s =
                    new SyncFactoryException("no se pudo leer el contexto JNDI");
            s.initCause(e);
            throw s;
        }
    }

    private static void recorrer(final Context ctx, final Map<String, SyncProvider> out)
            throws NamingException {
        final NamingEnumeration<javax.naming.Binding> e = ctx.listBindings("");
        while (e.hasMore()) {
            final javax.naming.Binding b = e.next();
            final Object o = b.getObject();
            if (o instanceof Context) {
                recorrer((Context) o, out);
            } else if (o instanceof SyncProvider) {
                final SyncProvider p = (SyncProvider) o;
                out.put(p.getProviderID(), p);
            }
        }
    }
}
