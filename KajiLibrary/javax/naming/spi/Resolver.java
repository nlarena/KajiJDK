package javax.naming.spi;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.spi.Resolver -- resolves what it can and returns the rest.
 *
 * <p>It is for <b>partial</b> contexts: the ones that understand the beginning of a name but not
 * the end. A naming service that resolves {@code /company/hr} and from there on delegates to
 * another system implements this instead of {@link Context}.
 *
 * <p>What it returns is a {@link ResolveResult}: how far it got, and what was left unresolved. The
 * caller carries on with the rest against what was resolved.
 *
 * <p>It is given the <b>class</b> of context being looked for, and that is the useful part: a
 * resolver can stop when it reaches something that is a {@code DirContext} and not go further down.
 * Without that you would have to resolve one component at a time and ask the type at each step.
 */
public interface Resolver {

    /**
     * Resolves until it finds a context of that class.
     *
     * @param contextType the class being looked for
     * @throws NamingException if not even the beginning can be resolved
     */
    ResolveResult resolveToClass(Name name, Class<? extends Context> contextType)
        throws NamingException;

    /** Same, with the name as text. */
    ResolveResult resolveToClass(String name, Class<? extends Context> contextType)
        throws NamingException;
}
