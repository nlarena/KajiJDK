package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

// A {@link Set} of enum constants represented as a **bitmask**: constant with ordinal `i` is in
// the set exactly when bit `i` of one `long` is set. Membership is one AND, insertion one OR,
// removal one AND-NOT — single instructions, on a set that occupies eight bytes no matter how
// many elements it holds. Against a {@link HashSet} of the same constants (a table, a node and a
// hash per element) it is not a marginal win; it is a different order of magnitude, and it is
// the reason enum flags in Java are an EnumSet rather than the hand-rolled `int` bitfields the
// language inherited from C — you get the speed *and* keep the type safety and the names.
//
// It is abstract, with no public constructor, exactly as in the JDK. That is a deliberate API
// choice rather than an accident: the JDK ships two implementations — one `long` for enums up to
// 64 constants, an array of longs for larger ones — and the static factories pick between them,
// so callers never name a concrete type and never have to care. KajiLibrary ships only the
// single-word one ({@link RegularEnumSet} below, package-private in the same file since a nested
// class inside a *generic* class is miscompiled, finding #13), so an enum with more than 64
// constants is rejected rather than silently mishandled.
//
// The mask alone cannot iterate: bit 3 tells you *that* the fourth constant is present, not
// *which object* that is. The JDK recovers it from `elementType.getEnumConstants()`, the full
// universe of constants in ordinal order. KajiLibrary's {@link Class} has no such method — it
// would need static-field reflection the VM does not implement — so this implementation records
// each constant in a `universe` array as it first sees it. Everything that can be built from
// constants the caller hands us works exactly as in the JDK; the three factories that need the
// *whole* universe up front cannot be written at all, and are omitted rather than half-built:
//
//   - `allOf(Class)`      — needs every constant of the type.
//   - `range(from, to)`   — knows the two endpoints' ordinals but not the constants between.
//   - `complementOf(set)` — needs to know which constants are *missing*.
//
// Also omitted: the varargs `of(E, E...)`, clone, and the bulk Collection operations our
// `Collection` does not have.
public abstract class EnumSet<E extends Enum> implements Set<E> {

    // The enum type this set holds. Package-private, as in the JDK — and assigned by the
    // subclass rather than passed to this constructor: our javac cannot resolve a `super(...)`
    // whose target parameter is a parameterized type mentioning the superclass's own type
    // variable (`Class<E>`), which is exactly the shape the JDK's EnumSet constructor has.
    // Reported this session; see the run notes.
    Class<E> elementType;

    // Constants by ordinal, learned as they arrive — the mask's way back to the objects. Only
    // the slots for constants this set has actually been handed are populated, which is enough
    // because a bit can only be set by handing us the constant it stands for.
    Object[] universe;

    EnumSet() {
        this.universe = new Object[8];
    }

    // Record a constant against its ordinal. Called on the way in by every operation that
    // receives one, so the array is always populated for every bit that is set.
    void remember(int ordinal, Object constant) {
        if (ordinal >= universe.length) {
            int newLength = universe.length * 2;
            while (newLength <= ordinal) {
                newLength = newLength * 2;
            }
            Object[] bigger = new Object[newLength];
            for (int i = 0; i < universe.length; i++) {
                bigger[i] = universe[i];
            }
            universe = bigger;
        }
        universe[ordinal] = constant;
    }

    Object constantAt(int ordinal) {
        Object c = null;
        if (ordinal < universe.length) {
            c = universe[ordinal];
        }
        return c;
    }

    // The ordinal of `o`, or -1 if it is not a constant of this set's type. Bound to an `Enum`
    // local first: a call on a receiver whose static type is a *type variable* is silently
    // dropped by our javac (finding #111), and `E` is one.
    int ordinalOf(Object o) {
        int index = -1;
        if (o != null && elementType.isInstance(o)) {
            Enum e = (Enum) o;
            index = e.ordinal();
        }
        return index;
    }

    // --- factories ------------------------------------------------------------------
    //
    // Static factories rather than constructors, because the concrete class is an implementation
    // detail (see the class comment) and because `noneOf(Colour.class)` reads as what it does
    // where `new RegularEnumSet<Colour>(...)` would not.

    public static <E extends Enum> EnumSet<E> noneOf(Class<E> elementType) {
        if (elementType == null) {
            throw new NullPointerException();
        }
        return new RegularEnumSet<E>(elementType);
    }

    // `getClass()` rather than the JDK's `getDeclaringClass()`: KajiLibrary's {@link Enum} has no
    // getDeclaringClass, so a constant declared with a body — which javac compiles to an
    // anonymous subclass — would report that subclass here. Harmless for a set built entirely
    // through these factories, since every member reports the same class.
    private static <E extends Enum> EnumSet<E> emptyLike(E e) {
        Object o = e;
        if (o == null) {
            throw new NullPointerException();
        }
        Class<E> type = (Class<E>) o.getClass();
        return new RegularEnumSet<E>(type);
    }

    public static <E extends Enum> EnumSet<E> of(E e) {
        EnumSet<E> set = emptyLike(e);
        set.add(e);
        return set;
    }

    public static <E extends Enum> EnumSet<E> of(E e1, E e2) {
        EnumSet<E> set = emptyLike(e1);
        set.add(e1);
        set.add(e2);
        return set;
    }

    public static <E extends Enum> EnumSet<E> of(E e1, E e2, E e3) {
        EnumSet<E> set = emptyLike(e1);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        return set;
    }

    public static <E extends Enum> EnumSet<E> of(E e1, E e2, E e3, E e4) {
        EnumSet<E> set = emptyLike(e1);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        return set;
    }

    public static <E extends Enum> EnumSet<E> of(E e1, E e2, E e3, E e4, E e5) {
        EnumSet<E> set = emptyLike(e1);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        set.add(e5);
        return set;
    }

    // Copying another EnumSet is a mask copy — no iteration, no hashing, no comparisons.
    public static <E extends Enum> EnumSet<E> copyOf(EnumSet<E> s) {
        RegularEnumSet<E> copy = new RegularEnumSet<E>(s.elementType);
        for (int i = 0; i < s.universe.length; i++) {
            if (s.universe[i] != null) {
                copy.remember(i, s.universe[i]);
            }
        }
        copy.setMask(s.mask());
        return copy;
    }

    public static <E extends Enum> EnumSet<E> copyOf(Collection<E> c) {
        Iterator<E> it = c.iterator();
        if (!it.hasNext()) {
            // With no element there is nothing to read the enum type off, and no universe to
            // consult. The JDK has the same hole and throws for the same reason.
            throw new IllegalArgumentException("Collection is empty");
        }
        E first = it.next();
        EnumSet<E> set = emptyLike(first);
        set.add(first);
        while (it.hasNext()) {
            set.add(it.next());
        }
        return set;
    }

    // The bitmask, exposed package-private so copyOf can move it wholesale. Abstract because the
    // representation belongs to the concrete class, not to this one.
    abstract long mask();
}

// The single-word implementation: one `long`, so at most 64 constants. The JDK splits here,
// falling back to a `long[]`-backed JumboEnumSet past 64; KajiLibrary ships only this one and
// says so out loud rather than truncating.
//
// Top-level package-private rather than nested, since a nested class inside a *generic* class is
// miscompiled (finding #13) — which happens to match the JDK, where RegularEnumSet is also a
// package-private top-level class.
final class RegularEnumSet<E extends Enum> extends EnumSet<E> {

    // Bit `i` set ⇔ the constant with ordinal `i` is a member.
    private long elements;

    RegularEnumSet(Class<E> elementType) {
        this.elementType = elementType;
    }

    long mask() {
        return elements;
    }

    void setMask(long elements) {
        this.elements = elements;
    }

    // Population count of the mask, split into halves because KajiLibrary's `Long` has no
    // bitCount. This is O(1) in the number of *elements*, unlike every other Set here.
    public int size() {
        int lo = Integer.bitCount((int) elements);
        int hi = Integer.bitCount((int) (elements >>> 32));
        return lo + hi;
    }

    public boolean isEmpty() {
        return elements == 0L;
    }

    public boolean contains(Object o) {
        boolean present = false;
        int i = ordinalOf(o);
        if (i >= 0 && i < 64) {
            present = (elements & (1L << i)) != 0L;
        }
        return present;
    }

    public boolean add(E e) {
        int i = ordinalOf(e);
        if (i < 0) {
            throw new ClassCastException("element is not a constant of this set's enum type");
        }
        if (i >= 64) {
            // Refusing beats silently wrapping: `1L << 64` is `1L << 0` in Java, so an
            // unchecked shift would quietly alias ordinal 64 onto ordinal 0.
            throw new IllegalArgumentException("enum has more than 64 constants");
        }
        // Record the constant *before* setting the bit, so the universe is never behind the mask
        // — an iterator that found a set bit with no constant behind it would have nothing to
        // hand back.
        remember(i, e);
        long bit = 1L << i;
        boolean added = (elements & bit) == 0L;
        elements = elements | bit;
        return added;
    }

    public boolean remove(Object o) {
        boolean removed = false;
        int i = ordinalOf(o);
        if (i >= 0 && i < 64) {
            long bit = 1L << i;
            removed = (elements & bit) != 0L;
            elements = elements & ~bit;
        }
        return removed;
    }

    public void clear() {
        elements = 0L;
    }

    // Iterates in **ordinal order**, i.e. in the order the constants are declared — not in
    // insertion order and not in hash order. That falls out of the representation rather than
    // being arranged: the bits are scanned from 0 up, and the declaration order is the one thing
    // an ordinal encodes.
    public Iterator<E> iterator() {
        return new RegularEnumSetItr<E>(this);
    }

    // Package-private view for the iterator, which needs to test bits without owning them.
    boolean hasBit(int ordinal) {
        return (elements & (1L << ordinal)) != 0L;
    }

    public boolean equals(Object o) {
        boolean same;
        if (o == this) {
            same = true;
        } else if (!(o instanceof RegularEnumSet)) {
            same = false;
        } else {
            RegularEnumSet<E> other = (RegularEnumSet<E>) o;
            same = elements == other.elements;
        }
        return same;
    }
}

// Walks the mask from bit 0 up, handing back the constant recorded for each set bit. Top-level
// package-private for the same reason as the set itself (finding #13).
final class RegularEnumSetItr<E extends Enum> implements Iterator<E> {

    private final RegularEnumSet<E> set;
    private int next;

    RegularEnumSetItr(RegularEnumSet<E> set) {
        this.set = set;
        this.next = 0;
        advance();
    }

    // Park `next` on the next set bit, or on 64 if there is none left.
    private void advance() {
        while (next < 64 && !set.hasBit(next)) {
            next = next + 1;
        }
    }

    public boolean hasNext() {
        return next < 64;
    }

    public E next() {
        if (next >= 64) {
            throw new NoSuchElementException();
        }
        Object constant = set.constantAt(next);
        next = next + 1;
        advance();
        return (E) constant;
    }
}
