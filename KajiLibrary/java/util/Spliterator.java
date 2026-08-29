package java.util;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * KajiLibrary's java.util.Spliterator -- a traversal that can be CUT IN HALF.
 *
 * <p>An {@link Iterator} answers one question, "is there another", and answering it is the whole
 * of what it can do. A spliterator answers three more, and every one of them exists so that a
 * traversal can be handed to more than one thread:
 *
 * <ul>
 *   <li>{@link #trySplit()} -- give me a piece of this to work on separately. The whole reason
 *       the type exists.
 *   <li>{@link #estimateSize()} -- how much is left, so a splitter can decide whether the split
 *       is worth its cost.
 *   <li>{@link #characteristics()} -- what is TRUE of this source, so a traversal can skip work
 *       it can prove is unnecessary. A stream over a {@code SORTED} source does not sort again.
 * </ul>
 *
 * <p>And it advances differently: {@link #tryAdvance} does the test and the fetch in ONE call,
 * where an iterator needs {@code hasNext} and then {@code next}. That is not a convenience --
 * two calls cannot be made atomic against a concurrent source, and one can.
 *
 * @param <T> what the traversal yields
 */
public interface Spliterator<T> {

    // ---- the characteristics ----
    //
    // Bits, and each one is a PROMISE about the source that lets a consumer do less work. They
    // are reported, never enforced: a spliterator that claims SORTED and is not produces wrong
    // answers, and nothing will catch it.

    /** The traversal has a defined order, and every traversal repeats it. */
    public static final int ORDERED = 0x00000010;

    /** No two elements are {@code equals} to each other. */
    public static final int DISTINCT = 0x00000001;

    /** The order is a sort order -- see {@link #getComparator()} for which one. */
    public static final int SORTED = 0x00000004;

    /** {@link #estimateSize()} is exact, and stays exact after every split. */
    public static final int SIZED = 0x00000040;

    /** No element is null. */
    public static final int NONNULL = 0x00000100;

    /** The source cannot be modified during the traversal. */
    public static final int IMMUTABLE = 0x00000400;

    /**
     * The source may be modified concurrently, and the spliterator copes.
     *
     * <p>The opposite of {@link #IMMUTABLE} rather than its absence: a source that is neither
     * makes no promise at all, which is what a fail-fast collection reports.
     */
    public static final int CONCURRENT = 0x00001000;

    /**
     * Every spliterator this one yields will be {@link #SIZED} too.
     *
     * <p>A stronger claim than {@code SIZED}, and the one that matters for splitting: an array
     * knows its halves exactly, a hash table knows its total and not how it will divide.
     */
    public static final int SUBSIZED = 0x00004000;

    /**
     * Feeds the next element to {@code action}, if there is one.
     *
     * @param action what to do with it
     * @return whether there was one
     */
    boolean tryAdvance(Consumer<? super T> action);

    /**
     * Feeds every remaining element to {@code action}.
     *
     * <p>The default is the obvious loop, and an implementation overrides it because doing the
     * whole traversal in one call skips a per-element test -- which for an array is most of the
     * cost.
     *
     * @param action what to do with each
     */
    default void forEachRemaining(Consumer<? super T> action) {
        while (this.tryAdvance(action)) {
            // el trabajo lo hace tryAdvance
        }
    }

    /**
     * Splits off a piece of this traversal, or returns {@code null}.
     *
     * <p>The piece covers a PREFIX of what was left, and this spliterator keeps the rest -- so
     * the two together still cover everything exactly once. {@code null} means "not worth
     * splitting, or cannot be", and a splitter has to accept that answer: a source that always
     * returned a piece would never terminate.
     */
    Spliterator<T> trySplit();

    /**
     * How many elements remain, or {@link Long#MAX_VALUE} if that is unknown.
     *
     * <p>An ESTIMATE unless {@link #SIZED} is reported. It is allowed to be wrong, and it is used
     * to decide whether splitting is worth it -- so being wrong costs performance, not
     * correctness.
     */
    long estimateSize();

    /** The exact remaining count, or -1 when it is not known exactly. */
    default long getExactSizeIfKnown() {
        if ((this.characteristics() & Spliterator.SIZED) == 0) {
            return -1L;
        }
        return this.estimateSize();
    }

    /** The characteristic bits of this spliterator and its source. */
    int characteristics();

    /**
     * Whether all of {@code characteristics} are reported.
     *
     * @param characteristics the bits to test, ORed together
     */
    default boolean hasCharacteristics(int characteristics) {
        return (this.characteristics() & characteristics) == characteristics;
    }

    /**
     * The comparator the {@link #SORTED} order follows, or {@code null} for natural order.
     *
     * @throws IllegalStateException if the source is not sorted
     */
    default Comparator<? super T> getComparator() {
        throw new IllegalStateException("this spliterator is not SORTED");
    }

    /**
     * A spliterator over a primitive type.
     *
     * <p>Three type parameters where one would seem to do, and the third is the interesting one:
     * {@code T_SPLITR} names the interface ITSELF, so that {@code trySplit} on an
     * {@code OfInt} answers an {@code OfInt} and not a plain spliterator. Without it every split
     * of a primitive traversal would box.
     *
     * @param <T> the boxed element type
     * @param <T_CONS> the primitive consumer -- {@code IntConsumer} and its relatives
     * @param <T_SPLITR> this interface, so that splitting keeps the primitive type
     */
    public interface OfPrimitive<T, T_CONS, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>>
            extends Spliterator<T> {

        T_SPLITR trySplit();

        /**
         * Feeds the next element to {@code action}, if there is one.
         *
         * @param action what to do with it
         */
        boolean tryAdvance(T_CONS action);

        /**
         * Feeds every remaining element to {@code action}.
         *
         * @param action what to do with each
         */
        default void forEachRemaining(T_CONS action) {
            while (this.tryAdvance(action)) {
                // el trabajo lo hace tryAdvance
            }
        }
    }

    /** A spliterator over {@code int} values, which never boxes unless the caller asks. */
    public interface OfInt extends Spliterator.OfPrimitive<Integer, IntConsumer, Spliterator.OfInt> {

        Spliterator.OfInt trySplit();

        boolean tryAdvance(IntConsumer action);

        default void forEachRemaining(IntConsumer action) {
            while (this.tryAdvance(action)) {
                // el trabajo lo hace tryAdvance
            }
        }

        /**
         * The boxing form, for a caller that has an {@code Consumer<Integer>} and not an
         * {@code IntConsumer}.
         *
         * <p>Present so that an {@code OfInt} can still be used as a plain {@code Spliterator},
         * and every element it feeds through here allocates -- which is the cost the primitive
         * form exists to avoid.
         *
         * @param action what to do with the boxed element
         */
        default boolean tryAdvance(Consumer<? super Integer> action) {
            IntConsumer boxing = new IntConsumer() {
                public void accept(int value) {
                    action.accept(Integer.valueOf(value));
                }
            };
            return this.tryAdvance(boxing);
        }

        /**
         * The boxing form of {@link #forEachRemaining(IntConsumer)}.
         *
         * @param action what to do with each boxed element
         */
        default void forEachRemaining(Consumer<? super Integer> action) {
            IntConsumer boxing = new IntConsumer() {
                public void accept(int value) {
                    action.accept(Integer.valueOf(value));
                }
            };
            this.forEachRemaining(boxing);
        }
    }

    /** A spliterator over {@code long} values. */
    public interface OfLong extends Spliterator.OfPrimitive<Long, LongConsumer, Spliterator.OfLong> {

        Spliterator.OfLong trySplit();

        boolean tryAdvance(LongConsumer action);

        default void forEachRemaining(LongConsumer action) {
            while (this.tryAdvance(action)) {
                // el trabajo lo hace tryAdvance
            }
        }

        /**
         * The boxing form.
         *
         * @param action what to do with the boxed element
         */
        default boolean tryAdvance(Consumer<? super Long> action) {
            LongConsumer boxing = new LongConsumer() {
                public void accept(long value) {
                    action.accept(Long.valueOf(value));
                }
            };
            return this.tryAdvance(boxing);
        }

        /**
         * The boxing form of {@link #forEachRemaining(LongConsumer)}.
         *
         * @param action what to do with each boxed element
         */
        default void forEachRemaining(Consumer<? super Long> action) {
            LongConsumer boxing = new LongConsumer() {
                public void accept(long value) {
                    action.accept(Long.valueOf(value));
                }
            };
            this.forEachRemaining(boxing);
        }
    }

    /** A spliterator over {@code double} values. */
    public interface OfDouble
            extends Spliterator.OfPrimitive<Double, DoubleConsumer, Spliterator.OfDouble> {

        Spliterator.OfDouble trySplit();

        boolean tryAdvance(DoubleConsumer action);

        default void forEachRemaining(DoubleConsumer action) {
            while (this.tryAdvance(action)) {
                // el trabajo lo hace tryAdvance
            }
        }

        /**
         * The boxing form.
         *
         * @param action what to do with the boxed element
         */
        default boolean tryAdvance(Consumer<? super Double> action) {
            DoubleConsumer boxing = new DoubleConsumer() {
                public void accept(double value) {
                    action.accept(Double.valueOf(value));
                }
            };
            return this.tryAdvance(boxing);
        }

        /**
         * The boxing form of {@link #forEachRemaining(DoubleConsumer)}.
         *
         * @param action what to do with each boxed element
         */
        default void forEachRemaining(Consumer<? super Double> action) {
            DoubleConsumer boxing = new DoubleConsumer() {
                public void accept(double value) {
                    action.accept(Double.valueOf(value));
                }
            };
            this.forEachRemaining(boxing);
        }
    }
}
