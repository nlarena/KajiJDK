package javax.naming;

import java.util.Hashtable;

/**
 * A set of name-to-object bindings, and the operations to query and change it.
 *
 * <p>It is JNDI's central interface. A context is a directory: it has names bound to objects, and
 * some of those objects are contexts in turn, which builds the tree. Resolving `a/b/c` is looking
 * up `a` in this context, checking that what came out is a context, and carrying on there with
 * `b/c` --exactly like a file system.
 *
 * <h2>Why every operation is there twice</h2>
 *
 * <p>Every method has a version with `Name` and another with `String`. It is not sugar: the
 * `String` one parses the string **with the context's syntax** and then does the same thing. It
 * serves the common case --writing the name by hand-- but loses in the general case, because a name
 * that crosses several namespaces does not have a single syntax. When the name is built
 * programmatically, the `Name` version is the right one.
 *
 * <h2>The environment properties</h2>
 *
 * <p>The fourteen constants are keys of a configuration `Hashtable` that travels with the context
 * and is inherited by subcontexts. The most important of all is `INITIAL_CONTEXT_FACTORY`: it is
 * the one that says **who** implements all this. Without it there is no provider and there is
 * nothing --see `InitialContext`.
 *
 * <h2>What can be promised here and what cannot</h2>
 *
 * <p>This is an **interface**, and declaring it whole is honest precisely because of that: a
 * contract does not promise that anyone fulfils it. What could not be done without a provider is to
 * ship a class that pretends to serve a `lookup`. The only implementation in the package is
 * `InitialContext`, which serves nothing: it delegates, and when there is nobody to delegate to it
 * says so with `NoInitialContextException`.
 *
 * <p>It is not `AutoCloseable` --the JDK did not make it so either-- so `close()` goes by hand or
 * in a `finally`. Closing a context does **not** invalidate the objects taken out of it.
 */
public interface Context {

    // ---- who implements it: the key that decides whether there is JNDI at all -------------------

    /**
     * Name of the class --an implementation of `javax.naming.spi.InitialContextFactory`-- that
     * makes the initial context. It is the only property without which nothing works.
     */
    String INITIAL_CONTEXT_FACTORY = "java.naming.factory.initial";

    // ---- object factories -----------------------------------------------------------------------

    /** List of factories, separated by `:`, that turn `Reference`s into live objects. */
    String OBJECT_FACTORIES = "java.naming.factory.object";

    /** List of factories, separated by `:`, that go the other way when binding an object. */
    String STATE_FACTORIES = "java.naming.factory.state";

    /**
     * Package prefixes, separated by `:`, where to look for URL contexts.
     *
     * <p>Resolving a name that starts with `java:` looks for the class
     * `<prefix>.java.javaURLContextFactory`. The list always ends, implicitly, in
     * `com.sun.jndi.url`.
     */
    String URL_PKG_PREFIXES = "java.naming.factory.url.pkgs";

    // ---- where to connect -----------------------------------------------------------------------

    /** The service's URL --`ldap://host:389/o=company`-- for the initial provider. */
    String PROVIDER_URL = "java.naming.provider.url";

    /** DNS servers to use, when the provider needs them to locate the service. */
    String DNS_URL = "java.naming.dns.url";

    // ---- how to behave --------------------------------------------------------------------------

    /** `"true"` asks for the most authoritative source, usually slower and always fresher. */
    String AUTHORITATIVE = "java.naming.authoritative";

    /** How many results to fetch per round trip. It is advice to the provider, not a limit. */
    String BATCHSIZE = "java.naming.batchsize";

    /**
     * What to do with referrals: `"follow"` follows them on its own, `"throw"` throws them as
     * `ReferralException` for the caller to decide, `"ignore"` discards them.
     */
    String REFERRAL = "java.naming.referral";

    // ---- security -------------------------------------------------------------------------------

    /** The security protocol --for example `"ssl"`. */
    String SECURITY_PROTOCOL = "java.naming.security.protocol";

    /** The mechanism: `"none"`, `"simple"`, `"strong"`, or one of the provider's own. */
    String SECURITY_AUTHENTICATION = "java.naming.security.authentication";

    /** Who one claims to be. */
    String SECURITY_PRINCIPAL = "java.naming.security.principal";

    /** What proves it. */
    String SECURITY_CREDENTIALS = "java.naming.security.credentials";

    /** Preferred language for what the service returns, in RFC 1766 form. */
    String LANGUAGE = "java.naming.language";

    // ---- resolving ------------------------------------------------------------------------------

    /**
     * Resolves the name.
     *
     * <p>If what is bound is a link, it follows it; if it is a `Reference`, it turns it into an
     * object. The empty name returns a new instance of this context, which is the agreed way to
     * duplicate it.
     */
    Object lookup(Name name) throws NamingException;

    Object lookup(String name) throws NamingException;

    /** Like `lookup`, but **without** following the last link: it returns the link itself. */
    Object lookupLink(Name name) throws NamingException;

    Object lookupLink(String name) throws NamingException;

    // ---- binding and unbinding ------------------------------------------------------------------

    /** Binds, and fails with `NameAlreadyBoundException` if the name was already bound. */
    void bind(Name name, Object obj) throws NamingException;

    void bind(String name, Object obj) throws NamingException;

    /** Binds, overwriting whatever was there. It is `bind` without the refusal. */
    void rebind(Name name, Object obj) throws NamingException;

    void rebind(String name, Object obj) throws NamingException;

    /**
     * Unbinds the name.
     *
     * <p>Unbinding a name that was not bound is **not** an error --it is idempotent-- unless some
     * intermediate component does not exist.
     */
    void unbind(Name name) throws NamingException;

    void unbind(String name) throws NamingException;

    void rename(Name oldName, Name newName) throws NamingException;

    void rename(String oldName, String newName) throws NamingException;

    // ---- listing --------------------------------------------------------------------------------

    /**
     * The names bound in this context with each one's class, without fetching the objects.
     *
     * <p>It is separate from `listBindings` because building the objects can be very costly --each
     * one may be a connection-- and to show a tree the names are enough.
     */
    NamingEnumeration<NameClassPair> list(Name name) throws NamingException;

    NamingEnumeration<NameClassPair> list(String name) throws NamingException;

    /** Like `list`, but building each object. */
    NamingEnumeration<Binding> listBindings(Name name) throws NamingException;

    NamingEnumeration<Binding> listBindings(String name) throws NamingException;

    // ---- subcontexts ----------------------------------------------------------------------------

    /** Destroys the subcontext; fails with `ContextNotEmptyException` if it has anything inside. */
    void destroySubcontext(Name name) throws NamingException;

    void destroySubcontext(String name) throws NamingException;

    Context createSubcontext(Name name) throws NamingException;

    Context createSubcontext(String name) throws NamingException;

    // ---- syntax and composition -----------------------------------------------------------------

    /** The parser of the namespace where `name` lives; see `NameParser`. */
    NameParser getNameParser(Name name) throws NamingException;

    NameParser getNameParser(String name) throws NamingException;

    /**
     * Composes a name relative to this context with the name of this context relative to another.
     *
     * <p>It is not concatenation: the provider composes because it may have to change the
     * syntax --and even the order-- when crossing from one namespace into the other.
     */
    Name composeName(Name name, Name prefix) throws NamingException;

    String composeName(String name, String prefix) throws NamingException;

    // ---- environment and life cycle -------------------------------------------------------------

    /** Adds or overwrites a property; returns the previous value. */
    Object addToEnvironment(String propName, Object propVal) throws NamingException;

    Object removeFromEnvironment(String propName) throws NamingException;

    /** The effective environment. The returned `Hashtable` must not be modified. */
    Hashtable<?, ?> getEnvironment() throws NamingException;

    /**
     * Releases the context's resources.
     *
     * <p>Calling it twice is not an error. The objects that came out of this context keep working
     * after closing it.
     */
    void close() throws NamingException;

    /**
     * The full name of this context **in its own namespace**.
     *
     * <p>Throws `OperationNotSupportedException` when the namespace has no full name for
     * the context, which is the case for every namespace with more than one root.
     */
    String getNameInNamespace() throws NamingException;
}
