package javax.print.attribute;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

/**
 * KajiLibrary's javax.print.attribute.HashAttributeSet -- the reference implementation of
 * {@link AttributeSet}, and the only one the package ships.
 *
 * <h2>It is a map with the key tucked inside the value</h2>
 *
 * <p>Inside there is a {@code HashMap} from category to attribute, and that choice is the whole
 * class: the "one attribute per category" rule is not implemented, it is inherited from a map
 * having one entry per key. {@code add} is a {@code put} under the key {@code
 * attribute.getCategory()}, so putting in a second attribute of the same category overwrites the
 * first without having to look for it.
 *
 * <p>What does have to be decided is the **return value**: {@code add} returns whether the set
 * changed, and that is not "whether there was something before" but "whether what there is now
 * differs from what there was". That is why the new attribute is compared with the old one with
 * {@code equals} and it is not a matter of whether the {@code put} returned null. Adding
 * {@code new Copies(3)} where {@code new Copies(3)} already was returns {@code false} even though
 * they are two different objects.
 *
 * <h2>The second field, {@code myInterface}, is what makes the subclasses useful</h2>
 *
 * <p>Each set remembers **which interface** its members have to be of. For a bare {@code
 * HashAttributeSet} it is {@code Attribute.class} and restricts nothing; the four subclasses
 * ({@link HashDocAttributeSet} and company) pass {@code DocAttribute.class} and so on, and with
 * that the category restriction comes for free: {@code add} goes through {@link
 * AttributeSetUtilities#verifyAttributeValue} against that interface and whatever does not fit goes
 * out through {@code ClassCastException}.
 *
 * <p>Note the asymmetry, which is the JDK's and is replicated: {@code add} checks against
 * {@code myInterface} (the subclass's restriction) but {@code get}, {@code remove} and
 * {@code containsKey} check against plain {@code Attribute.class}. That is, a
 * {@code HashDocAttributeSet} can be **asked** about a category it could never contain --it returns
 * null-- but it cannot be given one.
 *
 * <h2>Equality</h2>
 *
 * <p>{@code equals} accepts any {@link AttributeSet}, not only another {@code HashAttributeSet}: it
 * compares size and then asks about each attribute with {@code containsValue}. It is what lets two
 * different implementations of the interface compare with each other. The hash is the **sum** of
 * the attributes' hashes, which is the only thing it can be because the order is undefined.
 *
 * <h2>What was left out</h2>
 *
 * <p>The serialization's {@code private writeObject}/{@code readObject} methods. The note said they
 * cannot be written here because KajiLibrary lacks {@code java.io.ObjectOutputStream} and
 * {@code ObjectInputStream}; both exist now, so that reason no longer holds and they could be
 * written. They are not public API and do not count in the surface; the map field is declared
 * {@code transient} all the same, which is the half of the contract that is kept.
 */
public class HashAttributeSet implements AttributeSet, Serializable {

    private static final long serialVersionUID = 5311560590283707917L;

    // The interface all members have to be an instance of. Attribute.class for this class; a
    // subinterface for each subclass.
    private Class<?> myInterface;

    // transient because the JDK's serialized form writes the attributes one by one, not the map.
    // Here nothing writes it (see the header), but the field is the same.
    private transient HashMap<Class<?>, Attribute> attrMap = new HashMap<Class<?>, Attribute>();

    /** An empty set, with no category restriction beyond being attributes. */
    public HashAttributeSet() {
        this(Attribute.class);
    }

    /** With one attribute inside. NullPointerException if it is null. */
    public HashAttributeSet(Attribute attribute) {
        this(attribute, Attribute.class);
    }

    /**
     * With the array's attributes, added in order from index 0 -- so if the array brings two of the
     * same category the last one wins. A null array gives the empty set.
     */
    public HashAttributeSet(Attribute[] attributes) {
        this(attributes, Attribute.class);
    }

    /** With another set's attributes. A null set gives the empty set. */
    public HashAttributeSet(AttributeSet attributes) {
        this(attributes, Attribute.class);
    }

    /** Empty and restricted to `interfaceName`. NullPointerException if `interfaceName` is null. */
    protected HashAttributeSet(Class<?> interfaceName) {
        if (interfaceName == null) {
            throw new NullPointerException("null interface");
        }
        this.myInterface = interfaceName;
    }

    /**
     * With one attribute, restricted. ClassCastException if the attribute is not of the interface.
     */
    protected HashAttributeSet(Attribute attribute, Class<?> interfaceName) {
        if (interfaceName == null) {
            throw new NullPointerException("null interface");
        }
        this.myInterface = interfaceName;
        add(attribute);
    }

    /** With an array, restricted. */
    protected HashAttributeSet(Attribute[] attributes, Class<?> interfaceName) {
        if (interfaceName == null) {
            throw new NullPointerException("null interface");
        }
        this.myInterface = interfaceName;
        int n = (attributes == null) ? 0 : attributes.length;
        for (int i = 0; i < n; i++) {
            add(attributes[i]);
        }
    }

    /**
     * With another set, restricted.
     *
     * <p>This is the only one of the four that does **not** reject a null `interfaceName`, just as
     * in the JDK: if `attributes` is also null or empty, the set ends up built with `myInterface`
     * null and the first `add` blows up with NullPointerException only then. It is an inconsistency
     * of the original, not a simplification of ours, and it is replicated because it is observable.
     */
    protected HashAttributeSet(AttributeSet attributes, Class<?> interfaceName) {
        this.myInterface = interfaceName;
        if (attributes != null) {
            Attribute[] attribArray = attributes.toArray();
            int n = (attribArray == null) ? 0 : attribArray.length;
            for (int i = 0; i < n; i++) {
                add(attribArray[i]);
            }
        }
    }

    /**
     * The attribute of that category, or null.
     *
     * <p>It checks against `Attribute.class`, not against `myInterface`: any attribute category can
     * be asked about even if this set cannot contain it.
     */
    public Attribute get(Class<?> category) {
        return this.attrMap.get(
                AttributeSetUtilities.verifyAttributeCategory(category, Attribute.class));
    }

    /**
     * Adds, replacing the one of the same category.
     *
     * <p>It returns whether the set **changed**, that is whether the new attribute is not equal to
     * the one there was; not whether there was something. See the header.
     */
    public boolean add(Attribute attribute) {
        Object oldAttribute = this.attrMap.put(attribute.getCategory(),
                AttributeSetUtilities.verifyAttributeValue(attribute, this.myInterface));
        return !attribute.equals(oldAttribute);
    }

    /**
     * Removes the one of that category. A null category is not an error: it does nothing and gives
     * false.
     */
    public boolean remove(Class<?> category) {
        return category != null
                && AttributeSetUtilities.verifyAttributeCategory(category, Attribute.class) != null
                && this.attrMap.remove(category) != null;
    }

    /**
     * Removes that attribute.
     *
     * <p>Mind the fine print, which is the JDK's: it removes **by category**, without comparing the
     * value. `remove(new Copies(3))` on a set that has `new Copies(5)` removes the five copies and
     * returns true. A null attribute is not an error: it gives false.
     */
    public boolean remove(Attribute attribute) {
        return attribute != null && this.attrMap.remove(attribute.getCategory()) != null;
    }

    /** Whether there is something of that category. A null category gives false. */
    public boolean containsKey(Class<?> category) {
        return category != null
                && AttributeSetUtilities.verifyAttributeCategory(category, Attribute.class) != null
                && this.attrMap.get(category) != null;
    }

    /** Whether that exact attribute (by equals) is there. Here the value is compared. */
    public boolean containsValue(Attribute attribute) {
        return attribute != null && attribute.equals(this.attrMap.get(attribute.getCategory()));
    }

    /**
     * Adds them all, with the same replacement rule.
     *
     * <p>It returns true if at least one changed. If it throws halfway, the ones already in stay
     * in: there is no transaction, just as in the JDK.
     */
    public boolean addAll(AttributeSet attributes) {
        Attribute[] attrs = attributes.toArray();
        boolean result = false;
        for (int i = 0; i < attrs.length; i++) {
            Attribute newValue =
                    AttributeSetUtilities.verifyAttributeValue(attrs[i], this.myInterface);
            Object oldValue = this.attrMap.put(newValue.getCategory(), newValue);
            result = (!newValue.equals(oldValue)) || result;
        }
        return result;
    }

    public int size() {
        return this.attrMap.size();
    }

    /**
     * The attributes in a new array, in no defined order.
     *
     * <p>The JDK writes `attrMap.values().toArray(attrs)`; here it walks with the iterator so as
     * not to depend on `Collection.toArray(T[])`. Same result.
     */
    public Attribute[] toArray() {
        Attribute[] attrs = new Attribute[size()];
        int i = 0;
        Iterator<Attribute> it = this.attrMap.values().iterator();
        while (it.hasNext() && i < attrs.length) {
            attrs[i] = it.next();
            i++;
        }
        return attrs;
    }

    public void clear() {
        this.attrMap.clear();
    }

    public boolean isEmpty() {
        return this.attrMap.isEmpty();
    }

    /**
     * Equal to any {@link AttributeSet} with the same category-value pairs.
     *
     * <p>It does not ask the other to be a HashAttributeSet: it asks through the interface, which
     * is what lets two different implementations be compared.
     */
    public boolean equals(Object object) {
        if (!(object instanceof AttributeSet)) {
            return false;
        }
        AttributeSet aset = (AttributeSet) object;
        if (aset.size() != size()) {
            return false;
        }
        Attribute[] attrs = toArray();
        for (int i = 0; i < attrs.length; i++) {
            if (!aset.containsValue(attrs[i])) {
                return false;
            }
        }
        return true;
    }

    /** The sum of the hashes. It is the only thing consistent with an undefined order. */
    public int hashCode() {
        int hcode = 0;
        Attribute[] attrs = toArray();
        for (int i = 0; i < attrs.length; i++) {
            hcode += attrs[i].hashCode();
        }
        return hcode;
    }
}
