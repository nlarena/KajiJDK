package javax.naming.directory;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.NotContextException;

/**
 * KajiLibrary's javax.naming.directory.InitialDirContext -- the entry point to a directory.
 *
 * <p>The same as {@link InitialContext} but for {@link DirContext}: it implements no operation, it
 * resolves <b>which</b> context serves each name and forwards the call to it. The twenty-six
 * methods are the same gesture repeated. (An earlier note said twenty-eight.)
 *
 * <p>The protected constructor with a {@code boolean} exists for subclasses that want to initialize
 * in two steps: with {@code true} the constructor does <b>not</b> build the context, and the
 * subclass does it later with {@code init}. It helps when the configuration is computed in the
 * subclass constructor, that is, after the base class one has already run.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>This library ships no JNDI provider --no LDAP, no DNS-- and {@link InitialContext}'s
 * {@code getDefaultInitCtx()} throws {@link NoInitialContextException}, so every operation here
 * ends there. An earlier note said that with a provider registered this works unchanged; it does
 * not: {@code InitialContext} never calls {@code javax.naming.spi.NamingManager.getInitialContext},
 * so a configured factory is ignored (see the {@code InitialContext} class header).
 *
 * <p>Also, a provider that resolves a name to a {@link Context} that is <b>not</b> a
 * {@link DirContext} produces {@link NotContextException}: asking a context that is not a
 * directory for attributes makes no sense, and saying so with the right type is better than a
 * {@code ClassCastException}.
 */
public class InitialDirContext extends InitialContext implements DirContext {

    /**
     * For a subclass that initializes in two steps.
     *
     * @param lazy with true, the context is not built yet; see the class note
     */
    protected InitialDirContext(boolean lazy) throws NamingException {
        super(lazy);
    }

    /** With the default environment. */
    public InitialDirContext() throws NamingException {
        super();
    }

    /** With an environment of its own. */
    public InitialDirContext(Hashtable<?, ?> environment) throws NamingException {
        super(environment);
    }

    /**
     * The context that serves that name, already checked to be a directory.
     *
     * @throws NotContextException if what it resolves to is not a {@link DirContext}
     */
    private DirContext dirOf(Name name) throws NamingException {
        return asDir(getURLOrDefaultInitCtx(name));
    }

    /** Same, with the name as text. */
    private DirContext dirOf(String name) throws NamingException {
        return asDir(getURLOrDefaultInitCtx(name));
    }

    /** The common check; see the class note. */
    private static DirContext asDir(Context ctx) throws NamingException {
        if (ctx instanceof DirContext) {
            return (DirContext) ctx;
        }
        if (ctx == null) {
            throw new NoInitialContextException("No initial directory context");
        }
        throw new NotContextException(
            "Not an instance of DirContext: " + ctx.getClass().getName());
    }

    /** Forwards to the context that serves the name; see the class note. */
    public Attributes getAttributes(Name name) throws NamingException {
        return dirOf(name).getAttributes(name);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public Attributes getAttributes(String name) throws NamingException {
        return dirOf(name).getAttributes(name);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public Attributes getAttributes(Name name, String[] attrIds) throws NamingException {
        return dirOf(name).getAttributes(name, attrIds);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
        return dirOf(name).getAttributes(name, attrIds);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public void modifyAttributes(Name name, int mod_op, Attributes attrs) throws NamingException {
        dirOf(name).modifyAttributes(name, mod_op, attrs);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException {
        dirOf(name).modifyAttributes(name, mod_op, attrs);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public void modifyAttributes(Name name, ModificationItem[] mods) throws NamingException {
        dirOf(name).modifyAttributes(name, mods);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException {
        dirOf(name).modifyAttributes(name, mods);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public void bind(Name name, Object obj, Attributes attrs) throws NamingException {
        dirOf(name).bind(name, obj, attrs);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public void bind(String name, Object obj, Attributes attrs) throws NamingException {
        dirOf(name).bind(name, obj, attrs);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public void rebind(Name name, Object obj, Attributes attrs) throws NamingException {
        dirOf(name).rebind(name, obj, attrs);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public void rebind(String name, Object obj, Attributes attrs) throws NamingException {
        dirOf(name).rebind(name, obj, attrs);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public DirContext createSubcontext(Name name, Attributes attrs) throws NamingException {
        return dirOf(name).createSubcontext(name, attrs);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public DirContext createSubcontext(String name, Attributes attrs) throws NamingException {
        return dirOf(name).createSubcontext(name, attrs);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public DirContext getSchema(Name name) throws NamingException {
        return dirOf(name).getSchema(name);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public DirContext getSchema(String name) throws NamingException {
        return dirOf(name).getSchema(name);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public DirContext getSchemaClassDefinition(Name name) throws NamingException {
        return dirOf(name).getSchemaClassDefinition(name);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public DirContext getSchemaClassDefinition(String name) throws NamingException {
        return dirOf(name).getSchemaClassDefinition(name);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
        return dirOf(name).search(name, matchingAttributes, attributesToReturn);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
        return dirOf(name).search(name, matchingAttributes, attributesToReturn);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes) throws NamingException {
        return dirOf(name).search(name, matchingAttributes);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes) throws NamingException {
        return dirOf(name).search(name, matchingAttributes);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons) throws NamingException {
        return dirOf(name).search(name, filter, cons);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons) throws NamingException {
        return dirOf(name).search(name, filter, cons);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
        return dirOf(name).search(name, filterExpr, filterArgs, cons);
    }

    /** Forwards to the context that serves the name; see the class note. */
    public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
        return dirOf(name).search(name, filterExpr, filterArgs, cons);
    }
}
