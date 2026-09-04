package javax.naming.spi;

import java.util.Hashtable;
import javax.naming.CannotProceedException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;

/**
 * KajiLibrary's javax.naming.spi.NamingManager -- la maquinaria que arma los contextos y los objetos.
 *
 * <p>Todo estatico. Es el punto donde JNDI decide <b>quien</b> atiende cada cosa, y por eso es el
 * punto donde se puede tomar el control de todo.
 *
 * <h2>Los dos constructores se instalan una sola vez</h2>
 *
 * <p>{@link #setInitialContextFactoryBuilder} y {@link #setObjectFactoryBuilder} fallan si ya habia
 * uno. La unicidad no es una comodidad de implementacion: son las dos palancas que deciden que
 * proveedor y que fabricas se usan en <b>todo</b> el proceso, y si se pudieran reemplazar, la
 * primera biblioteca que las use quedaria a merced de la segunda.
 *
 * <h2>Como se busca una fabrica de objetos</h2>
 *
 * <p>Sin constructor instalado, {@link #getObjectInstance} sigue el camino por omision: si el dato es
 * una {@link Reference} con nombre de clase, se carga <b>esa</b> clase y se la usa como fabrica; si
 * no, se recorren las clases nombradas en la propiedad {@code java.naming.factory.object}.
 *
 * <p>Cargar la clase que nombra el dato es lo que hace utiles a las referencias y a la vez es un
 * riesgo real: el nombre de clase viene del directorio, asi que quien pueda escribir ahi elige que
 * codigo se carga. Instalar un {@link ObjectFactoryBuilder} propio es la forma de cerrarlo.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>{@link #getInitialContext} sin constructor y sin la propiedad {@code java.naming.factory.initial}
 * lanza {@link NoInitialContextException}, que es lo que declara para "no hay proveedor". Con la
 * propiedad puesta carga la clase y funciona: la busqueda esta implementada de verdad.
 *
 * <p>{@link #getURLContext} devuelve siempre null. Buscar un contexto por esquema de URL pide la
 * convencion de paquetes de {@code java.naming.factory.url.pkgs} y una implementacion por esquema, y
 * esta biblioteca no trae ninguna. Null es lo que el contrato define como "no hay contexto para ese
 * esquema", asi que quien llama sigue por el camino normal sin enterarse de nada raro.
 */
public class NamingManager {

    /** La clave con la que un contexto de continuacion recibe la excepcion que lo origino. */
    public static final String CPE = "java.naming.spi.CannotProceedException";

    /** El instalado, o null. */
    private static ObjectFactoryBuilder objectFactoryBuilder = null;

    /** El instalado, o null. */
    private static InitialContextFactoryBuilder initialContextFactoryBuilder = null;

    /** Publico por compatibilidad; la clase es solo metodos estaticos. */
    NamingManager() {
    }

    /**
     * Instala el constructor de fabricas de objetos.
     *
     * @throws IllegalStateException si ya habia uno; ver la nota de la clase
     */
    public static synchronized void setObjectFactoryBuilder(ObjectFactoryBuilder builder)
        throws NamingException {
        if (objectFactoryBuilder != null) {
            throw new IllegalStateException("ObjectFactoryBuilder already set");
        }
        objectFactoryBuilder = builder;
    }

    /**
     * El objeto que corresponde a esos datos.
     *
     * <p>Ver el camino de busqueda en la nota de la clase.
     *
     * @return el objeto, o {@code refInfo} tal cual si ninguna fabrica lo reconocio
     */
    public static Object getObjectInstance(Object refInfo, Name name, Context nameCtx,
                                           Hashtable<?, ?> environment) throws Exception {
        ObjectFactoryBuilder builder;
        synchronized (NamingManager.class) {
            builder = objectFactoryBuilder;
        }
        if (builder != null) {
            ObjectFactory factory = builder.createObjectFactory(refInfo, environment);
            Object made = factory.getObjectInstance(refInfo, name, nameCtx, environment);
            return (made == null) ? refInfo : made;
        }
        // Camino por omision: la clase que nombra la propia referencia.
        if (refInfo instanceof Reference) {
            String className = ((Reference) refInfo).getFactoryClassName();
            if (className != null) {
                ObjectFactory factory = loadFactory(className);
                if (factory != null) {
                    Object made = factory.getObjectInstance(refInfo, name, nameCtx, environment);
                    if (made != null) {
                        return made;
                    }
                }
            }
        }
        // Y despues las de la propiedad, en orden.
        String list = property(environment, Context.OBJECT_FACTORIES);
        if (list != null) {
            String[] names = list.split(":");
            int i = 0;
            while (i < names.length) {
                ObjectFactory factory = loadFactory(names[i].trim());
                if (factory != null) {
                    Object made = factory.getObjectInstance(refInfo, name, nameCtx, environment);
                    if (made != null) {
                        return made;
                    }
                }
                i = i + 1;
            }
        }
        // Ninguna lo reconocio: se devuelve lo que entro, que es lo que pide el contrato.
        return refInfo;
    }

    /**
     * El contexto que atiende ese esquema de URL.
     *
     * @return null siempre en KajiLibrary; ver la nota de la clase
     */
    public static Context getURLContext(String scheme, Hashtable<?, ?> environment)
        throws NamingException {
        return null;
    }

    /**
     * El contexto inicial del proveedor configurado.
     *
     * @throws NoInitialContextException si no hay proveedor
     */
    public static Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        InitialContextFactoryBuilder builder;
        synchronized (NamingManager.class) {
            builder = initialContextFactoryBuilder;
        }
        if (builder != null) {
            return builder.createInitialContextFactory(environment).getInitialContext(environment);
        }
        String className = property(environment, Context.INITIAL_CONTEXT_FACTORY);
        if (className == null) {
            throw new NoInitialContextException(
                "Need to specify class name in environment or system property, or in an "
                    + "application resource file: " + Context.INITIAL_CONTEXT_FACTORY);
        }
        try {
            Class<?> found = Class.forName(className, true, contextLoader());
            Object made = found.getConstructor(new Class<?>[0]).newInstance(new Object[0]);
            if (!(made instanceof InitialContextFactory)) {
                throw new NoInitialContextException(
                    className + " is not an InitialContextFactory");
            }
            return ((InitialContextFactory) made).getInitialContext(environment);
        } catch (NamingException e) {
            throw e;
        } catch (Exception e) {
            NoInitialContextException failure = new NoInitialContextException(
                "Cannot instantiate class: " + className);
            failure.setRootCause(e);
            throw failure;
        }
    }

    /**
     * Instala el constructor de fabricas de contexto inicial.
     *
     * @throws IllegalStateException si ya habia uno
     */
    public static synchronized void setInitialContextFactoryBuilder(
        InitialContextFactoryBuilder builder) throws NamingException {
        if (initialContextFactoryBuilder != null) {
            throw new IllegalStateException("InitialContextFactoryBuilder already set");
        }
        initialContextFactoryBuilder = builder;
    }

    /** Si ya hay uno instalado. */
    public static boolean hasInitialContextFactoryBuilder() {
        synchronized (NamingManager.class) {
            return initialContextFactoryBuilder != null;
        }
    }

    /**
     * El contexto donde seguir una operacion que se corto.
     *
     * <p>Cuando un contexto no puede seguir resolviendo un nombre lanza
     * {@link CannotProceedException} con el objeto donde se corto; esto lo convierte de vuelta en un
     * contexto para retomar desde ahi.
     *
     * <p>La excepcion se pasa en el ambiente bajo la clave {@link #CPE}: el contexto nuevo puede
     * necesitar saber de donde viene, y no hay otro canal para decirselo.
     */
    public static Context getContinuationContext(CannotProceedException cpe)
        throws NamingException {
        Hashtable<Object, Object> env = new Hashtable<Object, Object>();
        if (cpe.getEnvironment() != null) {
            env.putAll(cpe.getEnvironment());
        }
        env.put(CPE, cpe);
        Object obj = cpe.getResolvedObj();
        try {
            Object made = getObjectInstance(obj, cpe.getAltName(), cpe.getAltNameCtx(), env);
            if (made instanceof Context) {
                return (Context) made;
            }
        } catch (NamingException e) {
            throw e;
        } catch (Exception e) {
            // No se pudo continuar: se propaga la original, que es la que explica el corte.
        }
        throw cpe;
    }

    /**
     * Lo que hay que guardar en lugar de ese objeto.
     *
     * <p>Recorre las fabricas de la propiedad {@code java.naming.factory.state}. Si el objeto es
     * {@link Referenceable} y ninguna lo reconoce, se guarda su referencia.
     */
    public static Object getStateToBind(Object obj, Name name, Context nameCtx,
                                        Hashtable<?, ?> environment) throws NamingException {
        String list = property(environment, Context.STATE_FACTORIES);
        if (list != null) {
            String[] names = list.split(":");
            int i = 0;
            while (i < names.length) {
                StateFactory factory = loadStateFactory(names[i].trim());
                if (factory != null) {
                    Object made = factory.getStateToBind(obj, name, nameCtx, environment);
                    if (made != null) {
                        return made;
                    }
                }
                i = i + 1;
            }
        }
        return obj;
    }

    /** El valor de esa propiedad: primero el ambiente, despues el sistema. */
    static String property(Hashtable<?, ?> environment, String key) {
        if (environment != null) {
            Object v = environment.get(key);
            if (v instanceof String) {
                return (String) v;
            }
        }
        try {
            return System.getProperty(key);
        } catch (SecurityException e) {
            return null;
        }
    }

    /** El cargador con el que se buscan las clases nombradas por configuracion. */
    static ClassLoader contextLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = NamingManager.class.getClassLoader();
        }
        return loader;
    }

    /** Una fabrica de objetos por nombre de clase, o null si no se pudo. */
    private static ObjectFactory loadFactory(String className) {
        Object made = instantiate(className);
        return (made instanceof ObjectFactory) ? (ObjectFactory) made : null;
    }

    /** Una fabrica de estado por nombre de clase, o null. */
    private static StateFactory loadStateFactory(String className) {
        Object made = instantiate(className);
        return (made instanceof StateFactory) ? (StateFactory) made : null;
    }

    /**
     * Instancia esa clase, o null.
     *
     * <p>Se traga la falla a proposito: una fabrica que no carga no es un error de la operacion,
     * es una fabrica menos en la lista. El contrato pide seguir con la que viene.
     */
    private static Object instantiate(String className) {
        try {
            Class<?> found = Class.forName(className, true, contextLoader());
            return found.getConstructor(new Class<?>[0]).newInstance(new Object[0]);
        } catch (Exception e) {
            return null;
        }
    }
}
