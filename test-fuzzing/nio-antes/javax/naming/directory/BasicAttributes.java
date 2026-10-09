package javax.naming.directory;

import java.util.ArrayList;
import java.util.List;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.BasicAttributes -- a set of attributes built in memory.
 *
 * <p>The implementation of {@link Attributes} used to build what is sent to the directory.
 *
 * <p>The decision that defines it is {@link #isCaseIgnored}, and it is fixed at construction. With
 * the rule that ignores case --the one that fits LDAP-- storing {@code "CN"} and then asking for
 * {@code "cn"} works; with the other, it does not. Getting the rule wrong gives a confusing
 * symptom: the attributes "are not there" even though they show up in a dump.
 *
 * <p>A list is kept and not a map, on purpose. With the case-ignoring rule the key would need
 * normalizing, and normalizing loses the identifier's original form -- which is the one that has to
 * be sent to the directory. With few attributes per entry, walking the list costs nothing.
 */
public class BasicAttributes implements Attributes {

    private static final long serialVersionUID = 4980164073184639448L;

    /** Whether identifiers are compared ignoring case. */
    private final boolean ignoreCase;

    /** The attributes, in insertion order. See the class note. */
    private final List<Attribute> attrs = new ArrayList<Attribute>();

    /** Empty, case-sensitive. */
    public BasicAttributes() {
        this(false);
    }

    /** Empty, with the chosen comparison rule. */
    public BasicAttributes(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    /** With one single-valued attribute, case-sensitive. */
    public BasicAttributes(String attrID, Object val) {
        this(attrID, val, false);
    }

    /** With one single-valued attribute and the chosen rule. */
    public BasicAttributes(String attrID, Object val, boolean ignoreCase) {
        this(ignoreCase);
        this.attrs.add(new BasicAttribute(attrID, val));
    }

    /**
     * A copy.
     *
     * <p>It copies the list, not the attributes: the {@link Attribute}s are the same objects. It is
     * what the JDK does, and worth knowing if someone is going to modify one.
     */
    public Object clone() {
        BasicAttributes copy = new BasicAttributes(this.ignoreCase);
        copy.attrs.addAll(this.attrs);
        return copy;
    }

    /** Whether identifiers are compared ignoring case. */
    public boolean isCaseIgnored() {
        return this.ignoreCase;
    }

    /** How many attributes there are. */
    public int size() {
        return this.attrs.size();
    }

    /**
     * The attribute with that identifier.
     *
     * @return null if it is not there
     */
    public Attribute get(String attrID) {
        int i = indexOf(attrID);
        return (i < 0) ? null : this.attrs.get(i);
    }

    /** All the attributes. */
    public NamingEnumeration<Attribute> getAll() {
        return new ListEnumeration<Attribute>(new ArrayList<Attribute>(this.attrs));
    }

    /** Only the identifiers. */
    public NamingEnumeration<String> getIDs() {
        List<String> ids = new ArrayList<String>();
        int i = 0;
        while (i < this.attrs.size()) {
            ids.add(this.attrs.get(i).getID());
            i = i + 1;
        }
        return new ListEnumeration<String>(ids);
    }

    /** Adds a single-valued attribute. */
    public Attribute put(String attrID, Object val) {
        return put(new BasicAttribute(attrID, val));
    }

    /**
     * Adds an already built attribute.
     *
     * @return the one that was there with that identifier, or null
     */
    public Attribute put(Attribute attr) {
        int i = indexOf(attr.getID());
        if (i < 0) {
            this.attrs.add(attr);
            return null;
        }
        Attribute old = this.attrs.get(i);
        this.attrs.set(i, attr);
        return old;
    }

    /** Removes it and returns it. */
    public Attribute remove(String attrID) {
        int i = indexOf(attrID);
        if (i < 0) {
            return null;
        }
        return this.attrs.remove(i);
    }

    /** The attributes, for a log. */
    public String toString() {
        if (this.attrs.isEmpty()) {
            return "No attributes";
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < this.attrs.size()) {
            sb.append(this.attrs.get(i).toString());
            if (i < this.attrs.size() - 1) {
                sb.append("; ");
            }
            i = i + 1;
        }
        return sb.toString();
    }

    /**
     * Equal if they have the same rule and the same attributes.
     *
     * <p>Order does not count: an entry's attributes are a set.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Attributes)) {
            return false;
        }
        Attributes that = (Attributes) obj;
        if (this.ignoreCase != that.isCaseIgnored()) {
            return false;
        }
        if (this.size() != that.size()) {
            return false;
        }
        int i = 0;
        while (i < this.attrs.size()) {
            Attribute mine = this.attrs.get(i);
            Attribute theirs = that.get(mine.getID());
            if (theirs == null || !mine.equals(theirs)) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /** Consistent with {@link #equals}: a sum, so as not to depend on order. */
    public int hashCode() {
        int hash = this.ignoreCase ? 1 : 0;
        int i = 0;
        while (i < this.attrs.size()) {
            hash = hash + this.attrs.get(i).hashCode();
            i = i + 1;
        }
        return hash;
    }

    /** The position of that identifier under the comparison rule, or -1. */
    private int indexOf(String attrID) {
        int i = 0;
        while (i < this.attrs.size()) {
            String id = this.attrs.get(i).getID();
            boolean same = this.ignoreCase ? id.equalsIgnoreCase(attrID) : id.equals(attrID);
            if (same) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    /** The enumeration over a copy. */
    private static final class ListEnumeration<T> implements NamingEnumeration<T> {

        private final List<T> snapshot;

        private int index = 0;

        ListEnumeration(List<T> snapshot) {
            this.snapshot = snapshot;
        }

        public boolean hasMore() throws NamingException {
            return hasMoreElements();
        }

        public T next() throws NamingException {
            return nextElement();
        }

        public void close() throws NamingException {
            this.index = this.snapshot.size();
        }

        public boolean hasMoreElements() {
            return this.index < this.snapshot.size();
        }

        public T nextElement() {
            T v = this.snapshot.get(this.index);
            this.index = this.index + 1;
            return v;
        }
    }
}
