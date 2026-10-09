package javax.management.relation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A list of {@link Role}.
 *
 * <h2>Why it extends {@code ArrayList<Object>} and not {@code ArrayList<Role>}</h2>
 *
 * <p>For compatibility, and the history shows. It was born before generics as an untyped list;
 * when they arrived, changing it to {@code ArrayList<Role>} would have broken all the code
 * already using it. The way out was to leave it over {@code Object} and add typed overloads.
 *
 * <h2>The "typed" mode and the "raw" mode</h2>
 *
 * <p>Hence the detail worth knowing: a list built with the {@code List<Role>} constructor is in
 * <b>typed</b> mode and rejects anything that is not a {@link Role}; one built empty accepts
 * anything until someone calls {@link #asList}, which is the method that converts it.
 *
 * <p>Mixing the two modes is how you get a {@code ClassCastException} from a place that does
 * not mention it.
 */
public class RoleList extends ArrayList<Object> {

    private static final long serialVersionUID = 5568344346499649313L;

    private transient boolean typed = false;

    /** Empty, in raw mode. */
    public RoleList() {
        super();
    }

    /**
     * Empty with that capacity, in raw mode.
     *
     * @throws IllegalArgumentException if the capacity is negative
     */
    public RoleList(int initialCapacity) throws IllegalArgumentException {
        super(initialCapacity);
    }

    /**
     * With those elements, in typed mode.
     *
     * @throws IllegalArgumentException if the list is {@code null}
     */
    public RoleList(List<Role> list) throws IllegalArgumentException {
        super(check(list));
        this.typed = true;
    }

    private static List<Role> check(List<Role> list) {
        if (list == null) {
            throw new IllegalArgumentException("the list cannot be null");
        }
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == null) {
                throw new IllegalArgumentException("an element is null");
            }
        }
        return list;
    }

    /**
     * This same list, seen as {@code List<Role>}, and it switches to typed mode.
     *
     * <p>It is a <b>view</b>, not a copy: changing it changes this one.
     *
     * @throws IllegalArgumentException if it already holds something that is not a {@link Role}
     */
    @SuppressWarnings("unchecked")
    public List<Role> asList() {
        if (!this.typed) {
            for (int i = 0; i < size(); i++) {
                if (!(get(i) instanceof Role)) {
                    throw new IllegalArgumentException(
                            "the list has an element that is not a Role");
                }
            }
            this.typed = true;
        }
        return (List<Role>) (List<?>) this;
    }

    /** Appends at the end. */
    public void add(Role element) throws IllegalArgumentException {
        if (element == null) {
            throw new IllegalArgumentException("the element cannot be null");
        }
        super.add(element);
    }

    /** Inserts at that position. */
    public void add(int index, Role element)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        if (element == null) {
            throw new IllegalArgumentException("the element cannot be null");
        }
        super.add(index, element);
    }

    /** Replaces the one at that position. */
    public void set(int index, Role element)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        if (element == null) {
            throw new IllegalArgumentException("the element cannot be null");
        }
        super.set(index, element);
    }

    /** Appends all at the end. */
    public boolean addAll(RoleList list) throws IndexOutOfBoundsException {
        if (list == null) {
            return true;
        }
        return super.addAll(list);
    }

    /** Inserts them at that position. */
    public boolean addAll(int index, RoleList list)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        if (list == null) {
            throw new IllegalArgumentException("the list cannot be null");
        }
        return super.addAll(index, list);
    }

    /**
     * Adds, rejecting anything that is not a {@link Role} if the list is typed.
     *
     * @throws IllegalArgumentException if it is typed and the element does not fit
     */
    public boolean add(Object o) {
        checkType(o);
        return super.add(o);
    }

    /** Inserts, with the same check. */
    public void add(int index, Object o) {
        checkType(o);
        super.add(index, o);
    }

    /** Adds all, with the same check. */
    public boolean addAll(Collection<?> c) {
        for (Object o : c) {
            checkType(o);
        }
        return super.addAll(c);
    }

    /** Inserts them, with the same check. */
    public boolean addAll(int index, Collection<?> c) {
        for (Object o : c) {
            checkType(o);
        }
        return super.addAll(index, c);
    }

    /** Replaces, with the same check. */
    public Object set(int index, Object o) {
        checkType(o);
        return super.set(index, o);
    }

    /**
     * The check that separates the two modes.
     *
     * <p>It only looks when the list is typed: in raw mode anything is accepted, which is what
     * it did before generics and what old code expects.
     */
    private void checkType(Object o) {
        if (this.typed && !(o instanceof Role)) {
            throw new IllegalArgumentException(
                    "this list holds Role and the element is not one");
        }
    }
}
