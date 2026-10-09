package javax.naming.event;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;

/**
 * KajiLibrary's javax.naming.event.EventDirContext -- listening to the result of a <b>search</b>.
 *
 * <p>It joins {@link EventContext} and {@link DirContext}, and what it adds of its own is the
 * interesting part: instead of listening to a name, you listen to a <b>filter</b>. The listener
 * gets events for the entries that match, including the ones that start matching later.
 *
 * <p>That is much more useful than listening to a name for what directories are used for: "tell me
 * when someone joins the administrators group" is a filter, not a name. With a name you would have
 * to listen to the whole group and filter on the client side.
 *
 * <p>The four overloads are the same two combinations as in {@code DirContext#search}: the name as
 * a {@link Name} or as text, and a direct filter or one with numbered arguments. The same warning
 * applies: a filter built by concatenating text is injectable, and the version with arguments is
 * the one to use when the filter depends on user input.
 *
 * <p>The {@link SearchControls} here are there for the scope and for which attributes come in the
 * events; a subscription has no meaningful result cap. The JDK's javadoc does not say which fields
 * a provider must honour, so this is the intent, not a guarantee.
 */
public interface EventDirContext extends EventContext, DirContext {

    /**
     * Listens to the entries that match the filter.
     *
     * @param filter with RFC 2254 syntax
     * @param ctls the scope and which attributes to fetch
     */
    void addNamingListener(Name target, String filter, SearchControls ctls, NamingListener l)
        throws NamingException;

    /** Same, with the name as text. */
    void addNamingListener(String target, String filter, SearchControls ctls, NamingListener l)
        throws NamingException;

    /**
     * Same, with numbered arguments.
     *
     * @param filterArgs the values of {@code {0}}, {@code {1}}, ...; they do not go through the
     *     parser
     */
    void addNamingListener(Name target, String filter, Object[] filterArgs, SearchControls ctls,
                           NamingListener l) throws NamingException;

    /** Same, with the name as text. */
    void addNamingListener(String target, String filter, Object[] filterArgs, SearchControls ctls,
                           NamingListener l) throws NamingException;
}
