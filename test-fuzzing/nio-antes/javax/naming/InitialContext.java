package javax.naming;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * The entry point to JNDI: a `Context` that does nothing by itself and delegates everything to a
 * provider.
 *
 * <h2>Why there is a class that only delegates</h2>
 *
 * <p>JNDI is an API without an implementation: whoever resolves LDAP names is an LDAP provider,
 * whoever resolves DNS names is another, and the library ships none. But whoever writes
 * `new InitialContext().lookup("jdbc/sales")` cannot name the provider without tying themselves
 * to it, which is exactly what the indirection is there to avoid. So this class answers that
 * question once: in the JDK it looks at the environment's `java.naming.factory.initial` property,
 * loads that factory, asks it for a context, and from then on every method is a one-line forward.
 *
 * <p>The loading is **lazy** --`getDefaultInitCtx` does it the first time it is needed and
 * `gotDefault` remembers it was done--, unless the environment carries the property, in which case
 * the constructor resolves it right away in order to fail early.
 *
 * <h2>What happens in this library</h2>
 *
 * <p>`getDefaultInitCtx()` always throws `NoInitialContextException`, and with it every context
 * operation fails. An earlier note explained this by saying `javax.naming.spi` was not in this
 * tree, so no provider could exist. That is no longer true: the subpackage is here, and
 * `javax.naming.spi.NamingManager.getInitialContext` does load the class named by
 * `java.naming.factory.initial` (or asks an installed `InitialContextFactoryBuilder`). But this
 * class never calls it: `getDefaultInitCtx` sets `gotDefault` and throws whenever `defaultInitCtx`
 * is null, and nothing assigns `defaultInitCtx`. So even with a factory configured and on the
 * class path, `new InitialContext()` fails here, where the JDK would use that factory. Without a
 * configured factory the behaviour is the same as the JDK's.
 *
 * <p>Two more gaps against the JDK:
 *
 * <ul>
 *   <li>`getURLOrDefaultInitCtx` does not look up context factories by URL scheme --that is
 *       `NamingManager.getURLContext`, which in this library always returns null--, so it always
 *       falls back to the default context. In the JDK, with no URL factories installed, it falls
 *       back the same way.
 *   <li>`init` builds the environment by merging what it was given with the JNDI system
 *       properties, but does **not** read `jndi.properties` files from the class path, which is
 *       the third source the JDK uses. Since no source can end up loading a provider here, the
 *       difference is not observable beyond the contents of `myProps`.
 * </ul>
 *
 * <h2>Names are relative to the initial context</h2>
 *
 * <p>`composeName` returns the name as is: an initial context is never named relative to anything
 * but itself, so the prefix must be the empty name and composing does nothing. It is not a
 * simplification, it is what the contract says.
 */
public class InitialContext implements Context {

    /**
     * The environment properties, already merged. `protected` because subclasses
     * --`InitialDirContext` and the providers' own-- read and complete them before calling `init`.
     */
    protected Hashtable<Object, Object> myProps = null;

    /** The provider's context, once resolved. */
    protected Context defaultInitCtx = null;

    /**
     * Whether resolving it was already tried. Kept apart from `defaultInitCtx != null` to not
     * retry.
     */
    protected boolean gotDefault = false;

    /**
     * The JNDI properties read from the system properties when the environment does not carry them.
     * They are the ones the JDK considers "standard"; the security ones are left out on purpose,
     * because a credential has no business on the command line. The JDK's list
     * (`com.sun.naming.internal.VersionHelper.PROPS`) also has `java.naming.factory.control`, which
     * this one lacks.
     */
    private static final String[] SYSTEM_PROPS = {
        Context.INITIAL_CONTEXT_FACTORY,
        Context.OBJECT_FACTORIES,
        Context.URL_PKG_PREFIXES,
        Context.STATE_FACTORIES,
        Context.PROVIDER_URL,
        Context.DNS_URL,
    };

    /**
     * The constructor for subclasses that need to build the environment **before** initializing.
     *
     * <p>With `lazy` set to `true` it does not call `init`: the subclass completes `myProps` as it
     * likes and calls `init` itself. With `false` it is the same as the no-argument constructor.
     * Without this shortcut, a subclass would have no way to get between construction and resolving
     * the provider.
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
     * Builds `myProps` and, if the environment already says which factory it is, resolves the
     * provider right away.
     *
     * <p>Resolving right away is to fail early: if the caller took the trouble to name a factory,
     * it not working has to come out in the constructor and not three calls later, when nobody
     * knows where it came from.
     */
    protected void init(Hashtable<?, ?> environment) throws NamingException {
        myProps = initialEnvironment(environment);
        if (myProps.get(Context.INITIAL_CONTEXT_FACTORY) != null) {
            getDefaultInitCtx();
        }
    }

    /** Copies the environment and overlays the missing system properties; what was given wins. */
    private static Hashtable<Object, Object> initialEnvironment(Hashtable<?, ?> environment) {
        Hashtable<Object, Object> props = new Hashtable<Object, Object>();
        if (environment != null) {
            for (Enumeration<?> e = environment.keys(); e.hasMoreElements(); ) {
                Object k = e.nextElement();
                props.put(k, environment.get(k));
            }
        }
        for (int i = 0; i < SYSTEM_PROPS.length; i++) {
            String propName = SYSTEM_PROPS[i];
            if (props.get(propName) == null) {
                String v = System.getProperty(propName);
                if (v != null) {
                    props.put(propName, v);
                }
            }
        }
        return props;
    }

    /**
     * The provider's context.
     *
     * <p>In this library it always fails, because nothing assigns `defaultInitCtx`: unlike the JDK,
     * it does not call `NamingManager.getInitialContext`. See the class header.
     *
     * @throws NoInitialContextException always, in this library
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
     * The context for the name's URL scheme, or the default one.
     *
     * <p>Looking up by scheme is `javax.naming.spi.NamingManager.getURLContext`, which is not
     * called here (and in this library always returns null); with no URL factories installed the
     * JDK also falls back to the default, so this is the same.
     */
    protected Context getURLOrDefaultInitCtx(String name) throws NamingException {
        return getDefaultInitCtx();
    }

    protected Context getURLOrDefaultInitCtx(Name name) throws NamingException {
        return getDefaultInitCtx();
    }

    /**
     * A one-shot `lookup`, without keeping the context.
     *
     * <p>It is sugar for the most common case --resolve one thing and forget-- and it also avoids
     * the cast at the call site, which is what the `<T>` buys. The cast is still there, only inside
     * and unchecked: if the object is not of the expected type, the `ClassCastException` comes out
     * at the caller just as before.
     */
    public static <T> T doLookup(Name name) throws NamingException {
        return (T) (new InitialContext()).lookup(name);
    }

    public static <T> T doLookup(String name) throws NamingException {
        return (T) (new InitialContext()).lookup(name);
    }

    // ---- everything that follows is pure delegation ---------------------------------------------
    //
    // Each pair of methods --the `Name` one and the `String` one-- asks for the right context and
    // forwards the call as is. There is no logic worth commenting one by one.

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

    /**
     * Both names are resolved against the **first** one's context; renaming does not cross
     * providers.
     */
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
     * Returns `name` untouched, and it is not a simplification.
     *
     * <p>Composing a name with the context's name only makes sense if the context is named relative
     * to another. The initial context never is --it is the origin of the coordinate system--, so
     * `prefix` must be the empty name and the result is `name`.
     */
    @Override
    public String composeName(String name, String prefix) throws NamingException {
        return name;
    }

    /**
     * Clones because a `Name` is mutable and the result cannot be the same object as the argument.
     */
    @Override
    public Name composeName(Name name, Name prefix) throws NamingException {
        return (Name) name.clone();
    }

    /**
     * Changes both: its own environment, which survives, and the provider's, which is what acts.
     */
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

    /** The provider's and not `myProps`: the provider may have added defaults of its own. */
    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return getDefaultInitCtx().getEnvironment();
    }

    /**
     * Drops the environment and closes the provider's context if there ever was one.
     *
     * <p>Leaves `gotDefault` at `false`: the object is usable again, though without an environment.
     * It is what the real JDK does, and it is what lets closing twice not blow up.
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
