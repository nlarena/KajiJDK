package java.util;

import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

// KajiLibrary's java.util.Comparator — an external ordering: `compare(a, b)` returns
// negative / zero / positive, letting a caller sort by a rule other than the type's own
// natural ordering. A functional interface.
//
// One abstract method, and sixteen that BUILD comparators out of other comparators. That
// proportion is the design: a sort key is almost never a single field, and the alternative to
// `comparing(...).thenComparing(...)` is a hand-written comparator per combination of fields.
public interface Comparator<T> {

    /**
     * Negative, zero or positive as {@code o1} sorts before, with, or after {@code o2}.
     *
     * <p>The contract is not just "some number of the right sign": the ordering has to be a
     * total order, or a sort built on it is free to misbehave. It must be antisymmetric
     * ({@code compare(a, b)} and {@code compare(b, a)} have opposite signs), transitive, and
     * consistent — equal keys keep comparing equal.
     */
    int compare(T o1, T o2);

    /**
     * Redeclared from {@code Object} on purpose, exactly as the JDK does: this is where the
     * contract lives.
     *
     * <p>Two comparators are equal only when they impose the <em>same ordering</em> — a
     * question no inherited {@code equals} can answer, since it would have to compare
     * behaviour and not state. So the identity comparison inherited from {@code Object} is the
     * honest default, and this declaration exists to say that out loud rather than to change
     * anything.
     */
    boolean equals(Object obj);

    /**
     * The opposite ordering.
     *
     * <p>Swapping the arguments, not negating the result. Negating looks equivalent and is not:
     * a comparator is allowed to return {@code Integer.MIN_VALUE}, and {@code -MIN_VALUE} is
     * {@code MIN_VALUE} again — the one input where negation silently keeps the original sign.
     */
    default Comparator<T> reversed() {
        Comparator<T> self = this;
        return (T a, T b) -> self.compare(b, a);
    }

    /**
     * This ordering, with {@code other} breaking its ties.
     *
     * <p>{@code other} is only consulted when this comparator reports equality, which is what
     * makes chaining cheap: a secondary key costs nothing on the elements the primary key
     * already separated.
     */
    default Comparator<T> thenComparing(Comparator<? super T> other) {
        Objects.requireNonNull(other);
        Comparator<T> self = this;
        return (T a, T b) -> {
            int first = self.compare(a, b);
            if (first != 0) {
                return first;
            }
            return other.compare(a, b);
        };
    }

    /** This ordering, with the natural order of an extracted key breaking its ties. */
    default <U extends Comparable<? super U>> Comparator<T> thenComparing(
            Function<? super T, ? extends U> keyExtractor) {
        return this.thenComparing(Comparator.<T, U>comparing(keyExtractor));
    }

    /** This ordering, with an extracted key compared by {@code keyComparator} breaking ties. */
    default <U> Comparator<T> thenComparing(Function<? super T, ? extends U> keyExtractor,
            Comparator<? super U> keyComparator) {
        return this.thenComparing(Comparator.<T, U>comparing(keyExtractor, keyComparator));
    }

    /**
     * This ordering, with an {@code int} key breaking its ties.
     *
     * <p>The primitive variants exist so the key does not get boxed on every comparison. A
     * sort does O(n log n) comparisons, so an {@code Integer} per comparison is garbage
     * measured in the same order as the work itself.
     */
    default Comparator<T> thenComparingInt(ToIntFunction<? super T> keyExtractor) {
        return this.thenComparing(Comparator.<T>comparingInt(keyExtractor));
    }

    /** This ordering, with a {@code long} key breaking its ties, unboxed. */
    default Comparator<T> thenComparingLong(ToLongFunction<? super T> keyExtractor) {
        return this.thenComparing(Comparator.<T>comparingLong(keyExtractor));
    }

    /** This ordering, with a {@code double} key breaking its ties, unboxed. */
    default Comparator<T> thenComparingDouble(ToDoubleFunction<? super T> keyExtractor) {
        return this.thenComparing(Comparator.<T>comparingDouble(keyExtractor));
    }

    /**
     * The type's own ordering, as a comparator.
     *
     * <p>The bound is {@code Comparable<? super T>} and not {@code Comparable<T>} because a
     * type usually inherits its {@code compareTo} from a supertype — the ordering of a
     * subclass of something comparable is declared on the parent, and requiring it on the
     * subclass itself would reject the common case.
     */
    static <T extends Comparable<? super T>> Comparator<T> naturalOrder() {
        return (T a, T b) -> {
            // El local tipado nombra la cota: adentro del cuerpo de una lambda nuestro javac
            // pierde el `extends Comparable<...>` de la variable de tipo del metodo que la
            // contiene, y `compareTo` no resuelve. Afuera de la lambda, el mismo codigo
            // compila (#281).
            Comparable<? super T> key = a;
            return key.compareTo(b);
        };
    }

    /** The reverse of the type's own ordering. */
    static <T extends Comparable<? super T>> Comparator<T> reverseOrder() {
        return (T a, T b) -> {
            Comparable<? super T> key = b;
            return key.compareTo(a);
        };
    }

    /**
     * {@code comparator}, extended to accept nulls and sort them first.
     *
     * <p>{@code comparator} may itself be null, and that is not an oversight: it asks for
     * "nulls first, everything else equal", which is the ordering you want when null-handling
     * is the only thing being specified and a real key comes later via
     * {@link #thenComparing(Comparator)}.
     */
    static <T> Comparator<T> nullsFirst(Comparator<? super T> comparator) {
        return new NullComparator<T>(true, comparator);
    }

    /** {@code comparator}, extended to accept nulls and sort them last. */
    static <T> Comparator<T> nullsLast(Comparator<? super T> comparator) {
        return new NullComparator<T>(false, comparator);
    }

    /** Orders by an extracted key, in that key's natural order. */
    static <T, U extends Comparable<? super U>> Comparator<T> comparing(
            Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return (T a, T b) -> {
            Comparable<? super U> key = keyExtractor.apply(a);
            return key.compareTo(keyExtractor.apply(b));
        };
    }

    /** Orders by an extracted key, compared by {@code keyComparator}. */
    static <T, U> Comparator<T> comparing(Function<? super T, ? extends U> keyExtractor,
            Comparator<? super U> keyComparator) {
        Objects.requireNonNull(keyExtractor);
        Objects.requireNonNull(keyComparator);
        return (T a, T b) -> keyComparator.compare(keyExtractor.apply(a), keyExtractor.apply(b));
    }

    /**
     * Orders by an extracted {@code int} key, without boxing it.
     *
     * <p>{@code Integer.compare} and not {@code a - b}: the subtraction overflows whenever the
     * two keys are more than {@code Integer.MAX_VALUE} apart, and returns a number with the
     * *wrong sign*. It is the classic comparator bug, and it only shows up on the inputs
     * nobody tests with.
     */
    static <T> Comparator<T> comparingInt(ToIntFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return (T a, T b) -> Integer.compare(keyExtractor.applyAsInt(a),
                keyExtractor.applyAsInt(b));
    }

    /** Orders by an extracted {@code long} key, without boxing it. */
    static <T> Comparator<T> comparingLong(ToLongFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return (T a, T b) -> Long.compare(keyExtractor.applyAsLong(a),
                keyExtractor.applyAsLong(b));
    }

    /**
     * Orders by an extracted {@code double} key, without boxing it.
     *
     * <p>{@code Double.compare} and not {@code <}: it is the only one that gives a total
     * order over doubles. {@code NaN} compares as greater than everything including itself,
     * and {@code -0.0} sorts below {@code 0.0} — both false under the primitive operators,
     * and both required for a sort not to lose elements.
     */
    static <T> Comparator<T> comparingDouble(ToDoubleFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return (T a, T b) -> Double.compare(keyExtractor.applyAsDouble(a),
                keyExtractor.applyAsDouble(b));
    }
}

// What `nullsFirst`/`nullsLast` return. A same-file top-level class rather than a nested one:
// a class declared inside a *generic* type is miscompiled by our javac (finding #13).
//
// A lambda would cover `compare` and nothing else, and that is not enough — `thenComparing` and
// `reversed` have to be overridden, or they compose wrongly:
//
//   - `thenComparing`: when BOTH values are null this comparator reports equality, and the
//     generic default would then hand the two nulls to the tie-breaker, which has every right
//     to throw. The null decision has to stay outermost, with the chaining happening *inside*.
//   - `reversed`: flipping the ordering has to flip which end the nulls go to as well.
final class NullComparator<T> implements Comparator<T> {

    private final boolean nullFirst;

    // Null is a legal value here — it means "every non-null is equal" (see `nullsFirst`).
    //
    // Declared `Comparator<T>` while the constructor takes `Comparator<? super T>`, with the
    // cast that bridges them, exactly as the JDK does. It is sound: a comparator over a
    // supertype of T orders every T, so the only thing the cast discards is the compiler's
    // ability to see that. Keeping the wildcard in the field would make `thenComparing` below
    // unwritable — `other` is a `Comparator<? super T>`, which says nothing about the captured
    // supertype the field would be parameterised on.
    private final Comparator<T> real;

    NullComparator(boolean nullFirst, Comparator<? super T> real) {
        this.nullFirst = nullFirst;
        this.real = (Comparator<T>) real;
    }

    public int compare(T a, T b) {
        if (a == null) {
            if (b == null) {
                return 0;
            }
            if (this.nullFirst) {
                return -1;
            }
            return 1;
        }
        if (b == null) {
            if (this.nullFirst) {
                return 1;
            }
            return -1;
        }
        if (this.real == null) {
            return 0;
        }
        return this.real.compare(a, b);
    }

    public Comparator<T> reversed() {
        if (this.real == null) {
            return new NullComparator<T>(!this.nullFirst, null);
        }
        return new NullComparator<T>(!this.nullFirst, this.real.reversed());
    }

    public Comparator<T> thenComparing(Comparator<? super T> other) {
        Objects.requireNonNull(other);
        if (this.real == null) {
            return new NullComparator<T>(this.nullFirst, other);
        }
        return new NullComparator<T>(this.nullFirst, this.real.thenComparing(other));
    }
}
