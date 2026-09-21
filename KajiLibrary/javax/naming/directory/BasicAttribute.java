package javax.naming.directory;

import java.util.Vector;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

/**
 * KajiLibrary's javax.naming.directory.BasicAttribute -- an attribute built in memory.
 *
 * <p>The implementation of {@link Attribute} used to <b>build</b> what is going to be sent to the
 * directory. What comes back from a query is usually another implementation, the provider's.
 *
 * <h2>Comparing values</h2>
 *
 * <p>{@link #contains}, {@link #remove} and {@link #equals} compare with {@code equals}, with one
 * important exception: if the value is an <b>array</b>, its elements are compared. Without that,
 * two attributes with the same bytes would not be equal --{@code byte[].equals} is identity-- and a
 * directory's binary values are precisely byte arrays.
 *
 * <h2>The two schema methods do nothing</h2>
 *
 * <p>{@link #getAttributeDefinition} and {@link #getAttributeSyntaxDefinition} throw {@link
 * OperationNotSupportedException}. It is not a limitation of this library: an attribute built in
 * memory comes from no directory, so there is no schema to speak of. The JDK does the same.
 */
public class BasicAttribute implements Attribute {

    private static final long serialVersionUID = 6743528196119291326L;

    /** The identifier. */
    protected String attrID;

    /**
     * The values. {@code protected} as in the JDK, and {@code transient} because the JDK writes
     * them by hand in {@code writeObject}. This class has no {@code writeObject}/{@code
     * readObject}, so the values are not written at all: an earlier note said they were serialized
     * by hand.
     */
    protected transient Vector<Object> values;

    /** Whether the values are a list and not a set. */
    protected boolean ordered;

    /** No values, unordered. */
    public BasicAttribute(String id) {
        this(id, false);
    }

    /** With one value, unordered. */
    public BasicAttribute(String id, Object value) {
        this(id, value, false);
    }

    /** No values, stating whether it is ordered. */
    public BasicAttribute(String id, boolean ordered) {
        this.attrID = id;
        this.values = new Vector<Object>();
        this.ordered = ordered;
    }

    /** With one value, stating whether it is ordered. */
    public BasicAttribute(String id, Object value, boolean ordered) {
        this(id, ordered);
        this.values.addElement(value);
    }

    /** A copy with the same values. */
    public Object clone() {
        BasicAttribute copy = new BasicAttribute(this.attrID, this.ordered);
        copy.values = new Vector<Object>(this.values);
        return copy;
    }

    /**
     * Equal if the identifier, the ordering and the values match.
     *
     * <p>Unordered, the values are compared as sets: same content in any order. Ordered, position
     * by position.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Attribute)) {
            return false;
        }
        Attribute that = (Attribute) obj;
        if (!this.attrID.equals(that.getID())) {
            return false;
        }
        if (this.ordered != that.isOrdered()) {
            return false;
        }
        if (this.size() != that.size()) {
            return false;
        }
        try {
            int i = 0;
            while (i < this.values.size()) {
                if (this.ordered) {
                    if (!sameValue(this.values.elementAt(i), that.get(i))) {
                        return false;
                    }
                } else {
                    if (!that.contains(this.values.elementAt(i))) {
                        return false;
                    }
                }
                i = i + 1;
            }
        } catch (NamingException e) {
            return false;
        }
        return true;
    }

    /** Consistent with {@link #equals}: it does not depend on order when the attribute has none. */
    public int hashCode() {
        int hash = this.attrID.hashCode();
        int i = 0;
        while (i < this.values.size()) {
            Object v = this.values.elementAt(i);
            if (v != null) {
                hash = hash + valueHash(v);
            }
            i = i + 1;
        }
        return hash;
    }

    /** The identifier and the values, for a log. */
    public String toString() {
        StringBuilder sb = new StringBuilder(this.attrID).append(": ");
        if (this.values.size() == 0) {
            sb.append("No values");
            return sb.toString();
        }
        int i = 0;
        while (i < this.values.size()) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(this.values.elementAt(i));
            i = i + 1;
        }
        return sb.toString();
    }

    /** All the values. */
    public NamingEnumeration<?> getAll() throws NamingException {
        return new ValueEnumeration(new Vector<Object>(this.values));
    }

    /**
     * The first of the values.
     *
     * @throws java.util.NoSuchElementException if it has none
     */
    public Object get() throws NamingException {
        if (this.values.size() == 0) {
            throw new java.util.NoSuchElementException("Attribute " + this.attrID + " has no value");
        }
        return this.values.elementAt(0);
    }

    /** How many values it has. */
    public int size() {
        return this.values.size();
    }

    /** The identifier. */
    public String getID() {
        return this.attrID;
    }

    /** Whether it has that value. See the class note about arrays. */
    public boolean contains(Object attrVal) {
        return indexOf(attrVal) >= 0;
    }

    /**
     * Adds a value.
     *
     * @return false if the attribute is unordered and already had it
     */
    public boolean add(Object attrVal) {
        if (!this.ordered && contains(attrVal)) {
            return false;
        }
        this.values.addElement(attrVal);
        return true;
    }

    /** Removes the first occurrence of that value. */
    public boolean remove(Object attrval) {
        int i = indexOf(attrval);
        if (i < 0) {
            return false;
        }
        this.values.removeElementAt(i);
        return true;
    }

    /** Removes them all. */
    public void clear() {
        this.values.setSize(0);
    }

    /** Whether the values are a list and not a set. */
    public boolean isOrdered() {
        return this.ordered;
    }

    /**
     * The value at that position.
     *
     * @throws IndexOutOfBoundsException if it does not exist
     */
    public Object get(int ix) throws NamingException {
        return this.values.elementAt(ix);
    }

    /** Removes the one at that position. */
    public Object remove(int ix) {
        Object old = this.values.elementAt(ix);
        this.values.removeElementAt(ix);
        return old;
    }

    /** Inserts at that position. */
    public void add(int ix, Object attrVal) {
        if (!this.ordered && contains(attrVal)) {
            throw new IllegalStateException(
                "Cannot add duplicate to unordered attribute " + this.attrID);
        }
        this.values.insertElementAt(attrVal, ix);
    }

    /** Replaces the one at that position. */
    public Object set(int ix, Object attrVal) {
        if (!this.ordered && contains(attrVal)) {
            throw new IllegalStateException(
                "Cannot add duplicate to unordered attribute " + this.attrID);
        }
        Object old = this.values.elementAt(ix);
        this.values.setElementAt(attrVal, ix);
        return old;
    }

    /**
     * There is no schema.
     *
     * @throws OperationNotSupportedException always; see the class note
     */
    public DirContext getAttributeSyntaxDefinition() throws NamingException {
        throw new OperationNotSupportedException("attribute syntax");
    }

    /**
     * There is no schema.
     *
     * @throws OperationNotSupportedException always
     */
    public DirContext getAttributeDefinition() throws NamingException {
        throw new OperationNotSupportedException("attribute definition");
    }

    /** The position of that value, or -1. See the class note about arrays. */
    private int indexOf(Object candidate) {
        int i = 0;
        while (i < this.values.size()) {
            if (sameValue(this.values.elementAt(i), candidate)) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    /** Value equality, with arrays compared by content. */
    private static boolean sameValue(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.getClass().isArray() && b.getClass().isArray()) {
            int lenA = java.lang.reflect.Array.getLength(a);
            if (lenA != java.lang.reflect.Array.getLength(b)) {
                return false;
            }
            int i = 0;
            while (i < lenA) {
                if (!sameValue(java.lang.reflect.Array.get(a, i),
                               java.lang.reflect.Array.get(b, i))) {
                    return false;
                }
                i = i + 1;
            }
            return true;
        }
        return a.equals(b);
    }

    /** Hash of a value, consistent with {@link #sameValue}. */
    private static int valueHash(Object v) {
        if (!v.getClass().isArray()) {
            return v.hashCode();
        }
        int hash = 0;
        int len = java.lang.reflect.Array.getLength(v);
        int i = 0;
        while (i < len) {
            Object e = java.lang.reflect.Array.get(v, i);
            hash = hash + (e == null ? 0 : valueHash(e));
            i = i + 1;
        }
        return hash;
    }

    /** The enumeration over a copy of the values. */
    private static final class ValueEnumeration implements NamingEnumeration<Object> {

        private final Vector<Object> snapshot;

        private int index = 0;

        ValueEnumeration(Vector<Object> snapshot) {
            this.snapshot = snapshot;
        }

        public boolean hasMore() throws NamingException {
            return hasMoreElements();
        }

        public Object next() throws NamingException {
            return nextElement();
        }

        public void close() throws NamingException {
            this.index = this.snapshot.size();
        }

        public boolean hasMoreElements() {
            return this.index < this.snapshot.size();
        }

        public Object nextElement() {
            Object v = this.snapshot.elementAt(this.index);
            this.index = this.index + 1;
            return v;
        }
    }
}
