package java.util.stream;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.function.Supplier;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;

// KajiLibrary's java.util.stream.Collector<T,A,R> — the recipe a Stream's terminal `collect`
// follows to fold elements of type T into a result of type R, using a mutable accumulator of
// type A: create one (`supplier`), fold each element in (`accumulator`), merge partials
// (`combiner`), and finish (`finisher`). `characteristics` flags optimisations the pipeline
// may exploit.
//
// Nothing is missing any more: the interface is complete. The two reasons the previous pass noted
// for leaving `characteristics()` and the two `of(...)` out were alive then and are not now:
//
//   - a nested type's *identity* survives crossing compilation units today. With the
//     `enum Characteristics` declared here and `CollectorImpl` (in Collectors.java) implementing
//     `Set<Collector.Characteristics> characteristics()`, the override check passes -- compiling
//     the two files together and compiling each against the other's `.class`, which is how
//     tools/apidiff/recompile.py compiles them;
//   - `ACC_VARARGS` is emitted (#200), so `of(..., Characteristics...)`'s descriptor is the JDK's
//     and the real `javap` prints the ellipsis.
//
// What IS copied from the JDK is how the characteristics are READ: they are PERMISSIONS, not
// promises. An empty set is always correct -- it means "I enable no optimisation" -- and that is
// why it is what the collectors in Collectors.java that cannot justify one return by default.
// Declaring IDENTITY_FINISH without being it, on the other hand, would be lying: whoever reads it
// may skip the finisher and keep the accumulator.
public interface Collector<T, A, R> {

    /**
     * The permissions a collector grants whoever runs it.
     *
     * <p>Each constant enables an optimisation. None of them is obligatory to honour, and none is
     * checked: a collector declaring `IDENTITY_FINISH` without its finisher being the identity
     * produces wrong results and nothing will catch it.
     *
     * <p>Our `collect` is sequential, eager and always calls the finisher, so it reads none of the
     * three. They are here all the same because they describe the collector, not the engine: a
     * collector of ours handed to code written against the real JDK has to tell the truth about
     * itself.
     */
    enum Characteristics {

        /** The accumulator supports several threads feeding it at once. */
        CONCURRENT,

        /** The result does not depend on the order the elements arrive in. */
        UNORDERED,

        /**
         * The finisher is the identity: the accumulator already <em>is</em> the result.
         *
         * <p>It enables skipping the call to `finisher()`, and that is why it can only be declared
         * when `A` and `R` are the same type.
         */
        IDENTITY_FINISH
    }

    /**
     * How to create an empty accumulator.
     *
     * @return the supplier
     */
    Supplier<A> supplier();

    /**
     * How to fold an element into the accumulator.
     *
     * @return the accumulator
     */
    BiConsumer<A, T> accumulator();

    /**
     * How to merge two partial accumulators.
     *
     * @return the combiner
     */
    BinaryOperator<A> combiner();

    /**
     * How to go from the accumulator to the result.
     *
     * @return the finisher
     */
    Function<A, R> finisher();

    /**
     * This collector's permissions. It may be empty, and empty is always correct.
     *
     * @return the set, which cannot be modified
     */
    Set<Characteristics> characteristics();

    /**
     * A collector whose accumulator already is the result.
     *
     * <p>`IDENTITY_FINISH` is added to whatever the caller asks for, because the finisher that is
     * built here really is the identity. It is the same thing the JDK does.
     *
     * @param supplier how to create the accumulator
     * @param accumulator how to fold an element in
     * @param combiner how to merge two partials
     * @param characteristics the extra permissions
     * @param <T> the elements' type
     * @param <R> the accumulator's type, which is the result's as well
     * @return the collector
     * @throws NullPointerException if any of the pieces is null
     */
    static <T, R> Collector<T, R, R> of(Supplier<R> supplier, BiConsumer<R, T> accumulator,
                                        BinaryOperator<R> combiner, Characteristics... characteristics) {
        Function<R, R> finisher = new SelfFinisher<R>();
        Set<Characteristics> permissions = CharacteristicSet.of(characteristics, true);
        return new CollectorOf<T, R, R>(supplier, accumulator, combiner, finisher, permissions);
    }

    /**
     * A collector with the five pieces given.
     *
     * @param supplier how to create the accumulator
     * @param accumulator how to fold an element in
     * @param combiner how to merge two partials
     * @param finisher how to go from the accumulator to the result
     * @param characteristics the permissions
     * @param <T> the elements' type
     * @param <A> the accumulator's type
     * @param <R> the result's type
     * @return the collector
     * @throws NullPointerException if any piece is null
     */
    static <T, A, R> Collector<T, A, R> of(Supplier<A> supplier, BiConsumer<A, T> accumulator,
                                           BinaryOperator<A> combiner, Function<A, R> finisher,
                                           Characteristics... characteristics) {
        Set<Characteristics> permissions = CharacteristicSet.of(characteristics, false);
        return new CollectorOf<T, A, R>(supplier, accumulator, combiner, finisher, permissions);
    }
}

// ---- the two factories' pieces -----------------------------------------------------------------
//
// They live in THIS file and not in Collectors.java, even though there is an almost identical
// `CollectorImpl` over there, so as not to create a cycle between the two compilation units: today
// Collectors.java depends on Collector.java and not the other way round, and
// tools/apidiff/recompile.py compiles one file at a time. Four duplicated fields cost less than a
// cycle.

// The permission set: an unmodifiable copy, so that nobody changes it afterwards.
final class CharacteristicSet {

    private CharacteristicSet() {
    }

    static Set<Collector.Characteristics> of(Collector.Characteristics[] requested, boolean identityFinish) {
        HashSet<Collector.Characteristics> s = new HashSet<Collector.Characteristics>();
        if (requested != null) {
            for (int i = 0; i < requested.length; i++) {
                s.add(requested[i]);
            }
        }
        if (identityFinish) {
            s.add(Collector.Characteristics.IDENTITY_FINISH);
        }
        return Collections.unmodifiableSet(s);
    }
}

// The identity finisher of `Collector.of(supplier, accumulator, combiner, ...)`.
final class SelfFinisher<R> implements Function<R, R> {
    public R apply(R acc) {
        return acc;
    }
}

final class CollectorOf<T, A, R> implements Collector<T, A, R> {

    private final Supplier<A> supplier;
    private final BiConsumer<A, T> accumulator;
    private final BinaryOperator<A> combiner;
    private final Function<A, R> finisher;
    private final Set<Collector.Characteristics> characteristics;

    CollectorOf(Supplier<A> supplier, BiConsumer<A, T> accumulator, BinaryOperator<A> combiner,
                Function<A, R> finisher, Set<Collector.Characteristics> characteristics) {
        if (supplier == null || accumulator == null || combiner == null || finisher == null) {
            // A constant message: String concatenation at run time is not
            // available in our VM (#226).
            throw new NullPointerException("a piece of the collector is null");
        }
        this.supplier = supplier;
        this.accumulator = accumulator;
        this.combiner = combiner;
        this.finisher = finisher;
        this.characteristics = characteristics;
    }

    public Supplier<A> supplier() {
        return this.supplier;
    }

    public BiConsumer<A, T> accumulator() {
        return this.accumulator;
    }

    public BinaryOperator<A> combiner() {
        return this.combiner;
    }

    public Function<A, R> finisher() {
        return this.finisher;
    }

    public Set<Collector.Characteristics> characteristics() {
        return this.characteristics;
    }
}
