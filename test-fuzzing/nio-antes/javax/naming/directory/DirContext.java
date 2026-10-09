package javax.naming.directory;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.DirContext -- a context that also has attributes.
 *
 * <p>It extends {@link Context} with what sets a <b>directory</b> apart from a naming service: each
 * entry has not just a name and an object, but a set of attributes that can be read, modified and
 * searched.
 *
 * <h2>The two ways to modify</h2>
 *
 * <p>{@code modifyAttributes} comes in two flavours. The one taking a code and some
 * {@link Attributes} applies <b>the same</b> operation to all of them; the one taking an array of
 * {@link ModificationItem} mixes different operations. The second is the one to use when the change
 * has to be atomic and not uniform.
 *
 * <p>The three codes are not symmetric. {@link #ADD_ATTRIBUTE} adds values to the existing ones,
 * {@link #REPLACE_ATTRIBUTE} drops the old ones and puts the new ones, and {@link
 * #REMOVE_ATTRIBUTE} removes the values given --or the whole attribute if given without values.
 * Mixing up the first two on a multi-valued attribute is the classic way to delete data by
 * accident.
 *
 * <h2>The three search families</h2>
 *
 * <ul>
 *   <li>by <b>example attributes</b>: you pass an {@link Attributes} and look for the entries that
 *       have them. It is convenient and only does equality;
 *   <li>by <b>filter</b>, with RFC 2254 syntax: much more expressive --there are or, and, not,
 *       wildcards-- and, built by hand, injectable;
 *   <li>by filter with <b>numbered arguments</b>, where the filter carries {@code {0}}, {@code {1}}
 *       and the values go separately. It is the safe version of the previous one and the one to use
 *       whenever the filter depends on user input.
 * </ul>
 *
 * <p>Every operation comes with {@link Name} and with {@code String}. The {@code Name} one is right
 * when the name is composed or walked: a {@code String} forces thinking about how to escape the
 * namespace's separators, and that is where it breaks.
 */
public interface DirContext extends Context {

    /** Adds values to the ones the attribute already has. */
    public static final int ADD_ATTRIBUTE = 1;

    /** Drops the old values and puts the new ones. */
    public static final int REPLACE_ATTRIBUTE = 2;

    /** Removes the given values, or the whole attribute if no values are given. */
    public static final int REMOVE_ATTRIBUTE = 3;

    /** All the attributes of that entry. */
    Attributes getAttributes(Name name) throws NamingException;

    /** Same, with the name as text. */
    Attributes getAttributes(String name) throws NamingException;

    /**
     * Only those attributes.
     *
     * @param attrIds which ones to fetch; null means all
     */
    Attributes getAttributes(Name name, String[] attrIds) throws NamingException;

    /** Same, with the name as text. */
    Attributes getAttributes(String name, String[] attrIds) throws NamingException;

    /**
     * Applies the same operation to all those attributes.
     *
     * @param mod_op one of the three constants; see the class note
     */
    void modifyAttributes(Name name, int mod_op, Attributes attrs) throws NamingException;

    /** Same, with the name as text. */
    void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException;

    /** Applies a list of different modifications, all or none. */
    void modifyAttributes(Name name, ModificationItem[] mods) throws NamingException;

    /** Same, with the name as text. */
    void modifyAttributes(String name, ModificationItem[] mods) throws NamingException;

    /** Binds an object to a name, with attributes. */
    void bind(Name name, Object obj, Attributes attrs) throws NamingException;

    /** Same, with the name as text. */
    void bind(String name, Object obj, Attributes attrs) throws NamingException;

    /** The same, overwriting whatever was there. */
    void rebind(Name name, Object obj, Attributes attrs) throws NamingException;

    /** Same, with the name as text. */
    void rebind(String name, Object obj, Attributes attrs) throws NamingException;

    /** Creates a subcontext with those attributes. */
    DirContext createSubcontext(Name name, Attributes attrs) throws NamingException;

    /** Same, with the name as text. */
    DirContext createSubcontext(String name, Attributes attrs) throws NamingException;

    /** The schema that governs that entry. */
    DirContext getSchema(Name name) throws NamingException;

    /** Same, with the name as text. */
    DirContext getSchema(String name) throws NamingException;

    /** The object class definitions of that entry. */
    DirContext getSchemaClassDefinition(Name name) throws NamingException;

    /** Same, with the name as text. */
    DirContext getSchemaClassDefinition(String name) throws NamingException;

    /**
     * Searches by example attributes.
     *
     * @param matchingAttributes the ones the entry must have; empty or null returns all
     * @param attributesToReturn which ones to fetch from each result; null means all
     */
    NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes,
                                           String[] attributesToReturn) throws NamingException;

    /** Same, with the name as text. */
    NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes,
                                           String[] attributesToReturn) throws NamingException;

    /** Searches by example attributes, fetching all attributes. */
    NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes)
        throws NamingException;

    /** Same, with the name as text. */
    NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes)
        throws NamingException;

    /**
     * Searches by filter.
     *
     * <p>See the class note: if the filter depends on something a person typed, use the version
     * with numbered arguments.
     */
    NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons)
        throws NamingException;

    /** Same, with the name as text. */
    NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons)
        throws NamingException;

    /**
     * Searches by filter with numbered arguments.
     *
     * @param filterArgs the values of {@code {0}}, {@code {1}}, ...; they do not go through the
     *     parser
     */
    NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs,
                                           SearchControls cons) throws NamingException;

    /** Same, with the name as text. */
    NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs,
                                           SearchControls cons) throws NamingException;
}
