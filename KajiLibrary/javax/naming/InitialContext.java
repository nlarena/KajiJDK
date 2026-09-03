package javax.naming;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * El punto de entrada a JNDI: un `Context` que no hace nada por si mismo y le delega todo a un
 * proveedor.
 *
 * <h2>Por que existe una clase que solo delega</h2>
 *
 * <p>JNDI es una API sin implementacion: quien resuelve nombres de LDAP es un proveedor de LDAP,
 * quien resuelve nombres de DNS es otro, y la biblioteca no trae ninguno. Pero el que escribe
 * `new InitialContext().lookup("jdbc/ventas")` no puede nombrar al proveedor sin atarse a el, que
 * es justo lo que la indireccion viene a evitar. Entonces esta clase resuelve esa pregunta una
 * vez: mira la propiedad `java.naming.factory.initial` del entorno, carga esa fabrica, le pide un
 * contexto, y a partir de ahi cada metodo es una linea que reenvia.
 *
 * <p>La carga es **perezosa** --`getDefaultInitCtx` la hace la primera vez que hace falta y
 * `gotDefault` recuerda que ya se hizo--, salvo que el entorno traiga la propiedad, en cuyo caso
 * el constructor la resuelve enseguida para fallar temprano.
 *
 * <h2>Que pasa en este JDK</h2>
 *
 * <p>La fabrica es un `javax.naming.spi.InitialContextFactory`, y ese subpaquete no esta en este
 * arbol. Entonces **no puede existir un proveedor**: no hay tipo que implementar. La consecuencia
 * es que `getDefaultInitCtx()` siempre tira `NoInitialContextException`, y con eso fallan todas
 * las operaciones de contexto.
 *
 * <p>Eso no es una implementacion falsa: es **exactamente** lo que hace el JDK real cuando no hay
 * proveedor instalado, esta declarado en la firma de todos los metodos --todos tiran
 * `NamingException`, y esta es una-- y es el comportamiento que el javadoc de `getDefaultInitCtx`
 * promete. La unica diferencia con el JDK real aparece cuando hay un proveedor instalado, y aca
 * eso no puede pasar. Por eso la clase se puede traer entera y honesta.
 *
 * <p>Dos consecuencias del mismo agujero, dichas de frente:
 *
 * <ul>
 *   <li>`getURLOrDefaultInitCtx` no busca fabricas de contexto por esquema de URL --eso es
 *       `NamingManager.getURLContext`, tambien de `spi`--, asi que siempre cae en el contexto
 *       default. En el JDK real, sin fabricas de URL instaladas, cae igual.
 *   <li>`init` arma el entorno mezclando lo que se le paso con las propiedades de sistema de
 *       JNDI, pero **no** lee archivos `jndi.properties` del classpath, que es la tercera fuente
 *       que usa el JDK real. Como ninguna de las tres puede terminar cargando un proveedor, la
 *       diferencia no es observable mas alla del contenido de `myProps`.
 * </ul>
 *
 * <h2>Los nombres son relativos al contexto inicial</h2>
 *
 * <p>`composeName` devuelve el nombre tal cual: un contexto inicial nunca esta nombrado relativo a
 * otra cosa que a si mismo, asi que el prefijo tiene que ser el nombre vacio y componer no hace
 * nada. No es una simplificacion, es lo que dice el contrato.
 */
public class InitialContext implements Context {

    /**
     * Las propiedades del entorno, ya mezcladas. `protected` porque las subclases --`InitialDirContext`
     * y las de los proveedores-- las leen y las completan antes de llamar a `init`.
     */
    protected Hashtable<Object, Object> myProps = null;

    /** El contexto del proveedor, una vez resuelto. */
    protected Context defaultInitCtx = null;

    /** Si ya se intento resolverlo. Separado de `defaultInitCtx != null` para no reintentar al pedo. */
    protected boolean gotDefault = false;

    /**
     * Las propiedades de JNDI que se leen de las propiedades de sistema cuando el entorno no las
     * trae. Son las que el JDK real considera "estandar"; las de seguridad no estan a proposito,
     * porque una credencial no tiene por que andar en la linea de comandos.
     */
    private static final String[] PROPS_DE_SISTEMA = {
        Context.INITIAL_CONTEXT_FACTORY,
        Context.OBJECT_FACTORIES,
        Context.URL_PKG_PREFIXES,
        Context.STATE_FACTORIES,
        Context.PROVIDER_URL,
        Context.DNS_URL,
    };

    /**
     * El constructor de las subclases que necesitan armar el entorno **antes** de inicializar.
     *
     * <p>Con `lazy` en `true` no llama a `init`: la subclase completa `myProps` a su gusto y llama
     * a `init` ella. Con `false` es igual al constructor sin argumentos. Sin este atajo, una
     * subclase no tendria como meterse entre la construccion y la resolucion del proveedor.
     */
    protected InitialContext(boolean lazy) throws NamingException {
        if (!lazy) {
            init(null);
        }
    }

    public InitialContext() throws NamingException {
        init(null);
    }

    public InitialContext(Hashtable<?, ?> environment) throws NamingException {
        init(environment);
    }

    /**
     * Arma `myProps` y, si el entorno ya dice cual es la fabrica, resuelve el proveedor enseguida.
     *
     * <p>Lo de resolver enseguida es para fallar temprano: si el que llama se tomo el trabajo de
     * nombrar una fabrica, que no ande tiene que salir en el constructor y no tres llamadas
     * despues, cuando ya no se sabe de donde venia.
     */
    protected void init(Hashtable<?, ?> environment) throws NamingException {
        myProps = entornoInicial(environment);
        if (myProps.get(Context.INITIAL_CONTEXT_FACTORY) != null) {
            getDefaultInitCtx();
        }
    }

    /** Copia el entorno y le superpone las propiedades de sistema que falten; lo dado tiene prioridad. */
    private static Hashtable<Object, Object> entornoInicial(Hashtable<?, ?> environment) {
        Hashtable<Object, Object> props = new Hashtable<Object, Object>();
        if (environment != null) {
            for (Enumeration<?> e = environment.keys(); e.hasMoreElements(); ) {
                Object k = e.nextElement();
                props.put(k, environment.get(k));
            }
        }
        for (int i = 0; i < PROPS_DE_SISTEMA.length; i++) {
            String nombre = PROPS_DE_SISTEMA[i];
            if (props.get(nombre) == null) {
                String v = System.getProperty(nombre);
                if (v != null) {
                    props.put(nombre, v);
                }
            }
        }
        return props;
    }

    /**
     * El contexto del proveedor.
     *
     * <p>En este arbol siempre falla, porque no hay `javax.naming.spi` y por lo tanto no hay
     * fabrica posible. Ver la cabecera de la clase.
     *
     * @throws NoInitialContextException siempre, mientras no haya proveedor
     */
    protected Context getDefaultInitCtx() throws NamingException {
        if (!gotDefault) {
            gotDefault = true;
        }
        if (defaultInitCtx == null) {
            throw new NoInitialContextException(
                "Need to specify class name in environment or system property: "
                + Context.INITIAL_CONTEXT_FACTORY);
        }
        return defaultInitCtx;
    }

    /**
     * El contexto que corresponde al esquema de URL del nombre, o el default.
     *
     * <p>Buscar por esquema es `javax.naming.spi.NamingManager.getURLContext`, que no esta; sin
     * fabricas de URL instaladas el JDK real tambien cae en el default, asi que esto es lo mismo.
     */
    protected Context getURLOrDefaultInitCtx(String name) throws NamingException {
        return getDefaultInitCtx();
    }

    protected Context getURLOrDefaultInitCtx(Name name) throws NamingException {
        return getDefaultInitCtx();
    }

    /**
     * Un `lookup` de una sola vez, sin quedarse con el contexto.
     *
     * <p>Es azucar para el caso mas comun --resolver una cosa y olvidarse-- y ademas evita el
     * casteo en el lugar de la llamada, que es lo que gana el `<T>`. El casteo sigue estando, solo
     * que adentro y sin chequear: si el objeto no es del tipo esperado, la
     * `ClassCastException` sale en el que llama igual que antes.
     */
    public static <T> T doLookup(Name name) throws NamingException {
        return (T) (new InitialContext()).lookup(name);
    }

    public static <T> T doLookup(String name) throws NamingException {
        return (T) (new InitialContext()).lookup(name);
    }

    // ---- todo lo que sigue es delegacion pura -------------------------------------------------------
    //
    // Cada par de metodos --el de `Name` y el de `String`-- pide el contexto que corresponde y le
    // reenvia la llamada tal cual. No hay logica que valga la pena comentar de a una.

    @Override
    public Object lookup(String name) throws NamingException {
        return getURLOrDefaultInitCtx(name).lookup(name);
    }

    @Override
    public Object lookup(Name name) throws NamingException {
        return getURLOrDefaultInitCtx(name).lookup(name);
    }

    @Override
    public void bind(String name, Object obj) throws NamingException {
        getURLOrDefaultInitCtx(name).bind(name, obj);
    }

    @Override
    public void bind(Name name, Object obj) throws NamingException {
        getURLOrDefaultInitCtx(name).bind(name, obj);
    }

    @Override
    public void rebind(String name, Object obj) throws NamingException {
        getURLOrDefaultInitCtx(name).rebind(name, obj);
    }

    @Override
    public void rebind(Name name, Object obj) throws NamingException {
        getURLOrDefaultInitCtx(name).rebind(name, obj);
    }

    @Override
    public void unbind(String name) throws NamingException {
        getURLOrDefaultInitCtx(name).unbind(name);
    }

    @Override
    public void unbind(Name name) throws NamingException {
        getURLOrDefaultInitCtx(name).unbind(name);
    }

    /** Los dos nombres se resuelven contra el contexto del **primero**; renombrar no cruza proveedores. */
    @Override
    public void rename(String oldName, String newName) throws NamingException {
        getURLOrDefaultInitCtx(oldName).rename(oldName, newName);
    }

    @Override
    public void rename(Name oldName, Name newName) throws NamingException {
        getURLOrDefaultInitCtx(oldName).rename(oldName, newName);
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        return getURLOrDefaultInitCtx(name).list(name);
    }

    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        return getURLOrDefaultInitCtx(name).list(name);
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        return getURLOrDefaultInitCtx(name).listBindings(name);
    }

    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        return getURLOrDefaultInitCtx(name).listBindings(name);
    }

    @Override
    public void destroySubcontext(String name) throws NamingException {
        getURLOrDefaultInitCtx(name).destroySubcontext(name);
    }

    @Override
    public void destroySubcontext(Name name) throws NamingException {
        getURLOrDefaultInitCtx(name).destroySubcontext(name);
    }

    @Override
    public Context createSubcontext(String name) throws NamingException {
        return getURLOrDefaultInitCtx(name).createSubcontext(name);
    }

    @Override
    public Context createSubcontext(Name name) throws NamingException {
        return getURLOrDefaultInitCtx(name).createSubcontext(name);
    }

    @Override
    public Object lookupLink(String name) throws NamingException {
        return getURLOrDefaultInitCtx(name).lookupLink(name);
    }

    @Override
    public Object lookupLink(Name name) throws NamingException {
        return getURLOrDefaultInitCtx(name).lookupLink(name);
    }

    @Override
    public NameParser getNameParser(String name) throws NamingException {
        return getURLOrDefaultInitCtx(name).getNameParser(name);
    }

    @Override
    public NameParser getNameParser(Name name) throws NamingException {
        return getURLOrDefaultInitCtx(name).getNameParser(name);
    }

    /**
     * Devuelve `name` sin tocarlo, y no es una simplificacion.
     *
     * <p>Componer un nombre con el nombre del contexto solo tiene sentido si el contexto esta
     * nombrado relativo a otro. El contexto inicial no lo esta nunca --es el origen del sistema de
     * coordenadas--, asi que `prefix` tiene que ser el nombre vacio y el resultado es `name`.
     */
    @Override
    public String composeName(String name, String prefix) throws NamingException {
        return name;
    }

    /** Clona porque un `Name` es mutable y el resultado no puede ser el mismo objeto que el argumento. */
    @Override
    public Name composeName(Name name, Name prefix) throws NamingException {
        return (Name) name.clone();
    }

    /** Cambia las dos: el entorno propio, que sobrevive, y el del proveedor, que es el que actua. */
    @Override
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        myProps.put(propName, propVal);
        return getDefaultInitCtx().addToEnvironment(propName, propVal);
    }

    @Override
    public Object removeFromEnvironment(String propName) throws NamingException {
        myProps.remove(propName);
        return getDefaultInitCtx().removeFromEnvironment(propName);
    }

    /** El del proveedor y no `myProps`: el proveedor pudo haberle agregado defaults propios. */
    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return getDefaultInitCtx().getEnvironment();
    }

    /**
     * Suelta el entorno y cierra el contexto del proveedor si llego a haber uno.
     *
     * <p>Deja `gotDefault` en `false`: el objeto queda utilizable de nuevo, aunque sin entorno.
     * Es lo que hace el JDK real y es lo que permite que cerrar dos veces no explote.
     */
    @Override
    public void close() throws NamingException {
        myProps = null;
        if (defaultInitCtx != null) {
            defaultInitCtx.close();
            defaultInitCtx = null;
        }
        gotDefault = false;
    }

    @Override
    public String getNameInNamespace() throws NamingException {
        return getDefaultInitCtx().getNameInNamespace();
    }
}
