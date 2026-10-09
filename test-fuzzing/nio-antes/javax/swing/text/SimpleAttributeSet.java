package javax.swing.text;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * The usual mutable attribute set: a table from name to value.
 *
 * <p>It is the one used to <em>build</em> attributes --{@code StyleConstants.setBold(attr,
 * true)}-- and pass them to a document. A document does not keep it as it is: it passes it
 * through its {@code StyleContext}, which returns an immutable and shared set. That is why this
 * class can be a plain table without worrying about memory.
 *
 * <p>The resolving parent is kept as one more attribute, under the key
 * {@link AttributeSet#ResolveAttribute}. It is a detail that shows: {@link #getAttributeCount}
 * counts it, and {@link #getAttributeNames} names it.
 */
public class SimpleAttributeSet implements MutableAttributeSet, Serializable, Cloneable {

    /** An empty and immutable set, so as not to create one every time "none" is needed. */
    public static final AttributeSet EMPTY = new EmptyAttributeSet();

    private transient Hashtable<Object, Object> table = new Hashtable<Object, Object>(3);

    /** An empty set. */
    public SimpleAttributeSet() {
    }

    /** A copy of that set. */
    public SimpleAttributeSet(AttributeSet source) {
        addAttributes(source);
    }

    public boolean isEmpty() {
        return table.isEmpty();
    }

    public int getAttributeCount() {
        return table.size();
    }

    public boolean isDefined(Object attrName) {
        return table.get(attrName) != null;
    }

    /** Whether both have the same number of attributes and this one contains all of the other's. */
    public boolean isEqual(AttributeSet attr) {
        return ((getAttributeCount() == attr.getAttributeCount())
                && containsAttributes(attr));
    }

    /** A copy of its own; whoever receives it can change it without touching this one. */
    public AttributeSet copyAttributes() {
        return (AttributeSet) clone();
    }

    public Enumeration<?> getAttributeNames() {
        return table.keys();
    }

    /** The value, or whatever the resolving parent says; see {@link MutableAttributeSet}. */
    public Object getAttribute(Object name) {
        Object value = table.get(name);
        if (value == null) {
            AttributeSet parent = getResolveParent();
            if (parent != null) {
                value = parent.getAttribute(name);
            }
        }
        return value;
    }

    public boolean containsAttribute(Object name, Object value) {
        return value.equals(getAttribute(name));
    }

    public boolean containsAttributes(AttributeSet attributes) {
        boolean result = true;
        Enumeration<?> names = attributes.getAttributeNames();
        while (result && names.hasMoreElements()) {
            Object name = names.nextElement();
            result = attributes.getAttribute(name).equals(getAttribute(name));
        }
        return result;
    }

    public void addAttribute(Object name, Object value) {
        table.put(name, value);
    }

    public void addAttributes(AttributeSet attributes) {
        Enumeration<?> names = attributes.getAttributeNames();
        while (names.hasMoreElements()) {
            Object name = names.nextElement();
            addAttribute(name, attributes.getAttribute(name));
        }
    }

    public void removeAttribute(Object name) {
        table.remove(name);
    }

    public void removeAttributes(Enumeration<?> names) {
        while (names.hasMoreElements()) {
            removeAttribute(names.nextElement());
        }
    }

    /** See {@link MutableAttributeSet#removeAttributes(AttributeSet)}'s note. */
    public void removeAttributes(AttributeSet attributes) {
        if (attributes == this) {
            table.clear();
        } else {
            Enumeration<?> names = attributes.getAttributeNames();
            while (names.hasMoreElements()) {
                Object name = names.nextElement();
                Object value = attributes.getAttribute(name);
                if (value.equals(getAttribute(name))) {
                    removeAttribute(name);
                }
            }
        }
    }

    public AttributeSet getResolveParent() {
        return (AttributeSet) table.get(AttributeSet.ResolveAttribute);
    }

    public void setResolveParent(AttributeSet parent) {
        addAttribute(AttributeSet.ResolveAttribute, parent);
    }

    /** A copy with a table of its own. */
    public Object clone() {
        SimpleAttributeSet attr;
        try {
            attr = (SimpleAttributeSet) super.clone();
            attr.table = new Hashtable<Object, Object>(table);
        } catch (CloneNotSupportedException cnse) {
            attr = null;
        }
        return attr;
    }

    public int hashCode() {
        return table.hashCode();
    }

    /** Equal to another set that has exactly the same attributes. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AttributeSet) {
            AttributeSet attrs = (AttributeSet) obj;
            return isEqual(attrs);
        }
        return false;
    }

    public String toString() {
        String s = "";
        Enumeration<?> names = getAttributeNames();
        while (names.hasMoreElements()) {
            Object key = names.nextElement();
            Object value = getAttribute(key);
            if (value instanceof AttributeSet) {
                // A resolving parent is summarized: printing it whole might never end.
                s = s + key + "=**AttributeSet** ";
            } else {
                s = s + key + "=" + value + " ";
            }
        }
        return s;
    }

    /**
     * {@link SimpleAttributeSet#EMPTY}'s empty set.
     *
     * <p>It is a separate class, and not a {@code SimpleAttributeSet} with nothing in it, so that
     * it is really immutable: an empty but mutable one could be filled by accident and the error
     * would appear very far from where it was made.
     */
    static class EmptyAttributeSet implements AttributeSet, Serializable {

        public int getAttributeCount() {
            return 0;
        }

        public boolean isDefined(Object attrName) {
            return false;
        }

        public boolean isEqual(AttributeSet attr) {
            return (attr.getAttributeCount() == 0);
        }

        public AttributeSet copyAttributes() {
            return this;
        }

        public Object getAttribute(Object key) {
            return null;
        }

        public Enumeration<Object> getAttributeNames() {
            return new java.util.Vector<Object>().elements();
        }

        public boolean containsAttribute(Object name, Object value) {
            return false;
        }

        public boolean containsAttributes(AttributeSet attributes) {
            return (attributes.getAttributeCount() == 0);
        }

        public AttributeSet getResolveParent() {
            return null;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            return ((obj instanceof AttributeSet) && (((AttributeSet) obj).getAttributeCount() == 0));
        }

        public int hashCode() {
            return 0;
        }
    }
}
