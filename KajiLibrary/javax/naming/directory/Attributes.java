package javax.naming.directory;

import java.io.Serializable;
import javax.naming.NamingEnumeration;

/**
 * KajiLibrary's javax.naming.directory.Attributes -- the set of attributes of an entry.
 *
 * <p>A map from identifier to {@link Attribute}, with one peculiarity that decides everything else:
 * {@link #isCaseIgnored}. LDAP directories are <b>case-insensitive</b> in attribute names
 * --{@code cn}, {@code CN} and {@code Cn} are the same-- and so the collection has to know which
 * rule it looks up with.
 *
 * <p>That is not a convenience detail: two collections with the same information and a different
 * rule behave differently for the same query, which is why the rule is fixed at construction and
 * cannot change afterwards.
 *
 * <p>{@link #put(String, Object)} is a shortcut that builds the {@link Attribute} inside. Worth
 * knowing that it wraps a <b>single</b> value: for an attribute with several, build it and use the
 * other overload.
 */
public interface Attributes extends Cloneable, Serializable {

    /** Whether identifiers are compared ignoring case. See the class note. */
    boolean isCaseIgnored();

    /** How many attributes there are. */
    int size();

    /**
     * The attribute with that identifier.
     *
     * @return null if it is not there
     */
    Attribute get(String attrID);

    /** All the attributes. */
    NamingEnumeration<? extends Attribute> getAll();

    /** Only the identifiers. */
    NamingEnumeration<String> getIDs();

    /**
     * Adds a single-valued attribute.
     *
     * @return the one that was there with that identifier, or null
     */
    Attribute put(String attrID, Object val);

    /** Adds an already built attribute. */
    Attribute put(Attribute attr);

    /** Removes it and returns it. */
    Attribute remove(String attrID);

    /** A copy. */
    Object clone();
}
