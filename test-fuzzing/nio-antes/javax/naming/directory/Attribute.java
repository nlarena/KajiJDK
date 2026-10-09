package javax.naming.directory;

import java.io.Serializable;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.Attribute -- an attribute of a directory entry.
 *
 * <p>An identifier and <b>zero or more</b> values. That there can be several is the first surprise:
 * in a directory, {@code telephoneNumber} may hold three numbers, and nothing distinguishes "the
 * attribute" from "the attribute's list of values". That is why {@link #get()} without an index
 * returns <b>one of them</b> and not "the" value.
 *
 * <h2>Ordered or not</h2>
 *
 * <p>{@link #isOrdered} splits the interface into two behaviours:
 *
 * <ul>
 *   <li><b>unordered</b> --the usual case-- the values are a set: adding a repeated one does
 *       nothing, and positions mean nothing stable;
 *   <li><b>ordered</b> the values are a list: they repeat if added twice, and the index is part of
 *       the data.
 * </ul>
 *
 * <p>The indexed methods exist for the second case. On an unordered one they work the same, but
 * what they return is not reproducible across implementations.
 *
 * <h2>The two schema methods</h2>
 *
 * <p>{@link #getAttributeDefinition} and {@link #getAttributeSyntaxDefinition} return parts of the
 * directory schema: what rules this attribute has and what syntax its values have. Hardly any
 * implementation supports them, and one that does not throws
 * {@code OperationNotSupportedException}.
 */
public interface Attribute extends Cloneable, Serializable {

    /**
     * The JNDI 1.1.1 value, as in the JDK. An earlier note said changing it breaks deserialization;
     * the JDK marks this field {@code @Deprecated} because a {@code serialVersionUID} in an
     * interface has no effect.
     */
    static final long serialVersionUID = 8707690322213556804L;

    /** All the values. */
    NamingEnumeration<?> getAll() throws NamingException;

    /**
     * One of the values.
     *
     * @throws java.util.NoSuchElementException if it has none
     */
    Object get() throws NamingException;

    /** How many values it has. */
    int size();

    /** The identifier, for example {@code "cn"}. */
    String getID();

    /** Whether it has that value. */
    boolean contains(Object attrVal);

    /**
     * Adds a value.
     *
     * @return whether the attribute changed; false on an unordered one that already had it
     */
    boolean add(Object attrVal);

    /** Removes that value. */
    boolean remove(Object attrval);

    /** Removes them all. */
    void clear();

    /**
     * The schema of the values' syntax.
     *
     * @throws javax.naming.OperationNotSupportedException if the implementation does not have it
     */
    DirContext getAttributeSyntaxDefinition() throws NamingException;

    /** The attribute's schema. */
    DirContext getAttributeDefinition() throws NamingException;

    /** A copy. */
    Object clone();

    /** Whether the values are a list and not a set. See the class note. */
    boolean isOrdered();

    /**
     * The value at that position.
     *
     * @throws IndexOutOfBoundsException if it does not exist
     */
    Object get(int ix) throws NamingException;

    /** Removes the one at that position and returns it. */
    Object remove(int ix);

    /** Inserts at that position. */
    void add(int ix, Object attrVal);

    /** Replaces the one at that position and returns the previous one. */
    Object set(int ix, Object attrVal);
}
