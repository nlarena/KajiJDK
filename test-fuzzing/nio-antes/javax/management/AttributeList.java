package javax.management;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A list of {@link Attribute}s, which extends {@code ArrayList&lt;Object&gt;} and not
 * {@code ArrayList&lt;Attribute&gt;}.
 *
 * <p>That odd inheritance is a compatibility scar, not an oversight. The class predates generics,
 * and when they arrived there was already code putting anything inside; parameterizing it with
 * {@code Attribute} would have broken that code on recompilation. The JDK's way out was to leave it
 * at {@code Object} and add {@link #asList()}, which is the typed view and the one worth using.
 *
 * <p>The price: {@code add(Object)} accepts anything. The JDK marks the list as "tainted" when that
 * happens and {@link #asList()} stops working; the same is done here.
 */
public class AttributeList extends ArrayList<Object> {

    private static final long serialVersionUID = -4077085769279709076L;

    private transient volatile boolean typed;

    private transient volatile boolean tainted;

    /** Empty. */
    public AttributeList() {
        super();
    }

    /** Empty, with reserved capacity. */
    public AttributeList(int initialCapacity) {
        super(initialCapacity);
    }

    /** A copy of another. */
    public AttributeList(AttributeList list) {
        super(list);
    }

    /** From an already typed list; marks it as typed from the start. */
    public AttributeList(List<Attribute> list) {
        if (list == null) {
            throw new IllegalArgumentException("Null parameter");
        }
        Iterator<Attribute> it = list.iterator();
        while (it.hasNext()) {
            Attribute a = it.next();
            if (a == null) {
                throw new IllegalArgumentException("Null attribute in list");
            }
            super.add(a);
        }
        typed = true;
    }

    /**
     * The typed view.
     *
     * <p>It is a <b>view</b>, not a copy: adding through here adds there. And from the first call
     * on the list is marked as typed, so a later {@code add(Object)} with something that is not an
     * {@code Attribute} is an error.
     *
     * @throws IllegalArgumentException if something that is not an {@code Attribute} was already
     *     put in
     */
    @SuppressWarnings("unchecked")
    public List<Attribute> asList() {
        typed = true;
        if (tainted) {
            typed = false;
            throw new IllegalArgumentException("AttributeList contains non-Attribute objects");
        }
        return (List<Attribute>) (List<?>) this;
    }

    /** Appends at the end. */
    public void add(Attribute object) {
        super.add(object);
    }

    /** Inserts at the given position. */
    public void add(int index, Attribute object) {
        super.add(index, object);
    }

    /** Replaces the given position. */
    public void set(int index, Attribute object) {
        super.set(index, object);
    }

    /** Appends all at the end. */
    public boolean addAll(AttributeList list) {
        return super.addAll(list);
    }

    /** Inserts all starting at the given position. */
    public boolean addAll(int index, AttributeList list) {
        return super.addAll(index, list);
    }

    /**
     * @throws IllegalArgumentException if the list was already declared typed and this is not an
     *     {@code Attribute}
     */
    public boolean add(Object element) {
        check(element);
        return super.add(element);
    }

    /** Ver {@link #add(Object)}. */
    public void add(int index, Object element) {
        check(element);
        super.add(index, element);
    }

    /** Ver {@link #add(Object)}. */
    public boolean addAll(Collection<?> c) {
        check(c);
        return super.addAll(c);
    }

    /** Ver {@link #add(Object)}. */
    public boolean addAll(int index, Collection<?> c) {
        check(c);
        return super.addAll(index, c);
    }

    /** Ver {@link #add(Object)}. */
    public Object set(int index, Object element) {
        check(element);
        return super.set(index, element);
    }

    private void check(Object x) {
        if (x instanceof Attribute) {
            return;
        }
        if (typed) {
            throw new IllegalArgumentException("Not an Attribute: " + x);
        }
        tainted = true;
    }

    private void check(Collection<?> c) {
        if (c == null) {
            return;
        }
        Iterator<?> it = c.iterator();
        while (it.hasNext()) {
            check(it.next());
        }
    }
}
