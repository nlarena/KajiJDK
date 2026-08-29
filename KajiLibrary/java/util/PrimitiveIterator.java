package java.util;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * KajiLibrary's java.util.PrimitiveIterator -- an iterator that can hand over a primitive without
 * boxing it.
 *
 * <p>An {@code Iterator<Integer>} allocates an {@code Integer} for every element, and over a
 * traversal of any size that allocation IS the cost. The three nested interfaces exist to give a
 * caller a way to say "give me the {@code int}", and the boxing {@code next()} stays available
 * for a caller that cannot.
 *
 * @param <T> the boxed element type
 * @param <T_CONS> the primitive consumer -- {@code IntConsumer} and its relatives
 */
public interface PrimitiveIterator<T, T_CONS> extends Iterator<T> {

    /**
     * Feeds every remaining element to {@code action}, unboxed.
     *
     * @param action what to do with each
     */
    void forEachRemaining(T_CONS action);

    /** An iterator over {@code int} values. */
    public interface OfInt extends PrimitiveIterator<Integer, IntConsumer> {

        /**
         * The next value, unboxed.
         *
         * @throws NoSuchElementException if there is none
         */
        int nextInt();

        default void forEachRemaining(IntConsumer action) {
            while (this.hasNext()) {
                action.accept(this.nextInt());
            }
        }

        /**
         * The next value, boxed.
         *
         * <p>Here so that this is still an {@code Iterator}, and every call allocates -- which is
         * exactly what {@link #nextInt()} exists to avoid.
         */
        default Integer next() {
            return Integer.valueOf(this.nextInt());
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

    /** An iterator over {@code long} values. */
    public interface OfLong extends PrimitiveIterator<Long, LongConsumer> {

        /**
         * The next value, unboxed.
         *
         * @throws NoSuchElementException if there is none
         */
        long nextLong();

        default void forEachRemaining(LongConsumer action) {
            while (this.hasNext()) {
                action.accept(this.nextLong());
            }
        }

        /** The next value, boxed. */
        default Long next() {
            return Long.valueOf(this.nextLong());
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

    /** An iterator over {@code double} values. */
    public interface OfDouble extends PrimitiveIterator<Double, DoubleConsumer> {

        /**
         * The next value, unboxed.
         *
         * @throws NoSuchElementException if there is none
         */
        double nextDouble();

        default void forEachRemaining(DoubleConsumer action) {
            while (this.hasNext()) {
                action.accept(this.nextDouble());
            }
        }

        /** The next value, boxed. */
        default Double next() {
            return Double.valueOf(this.nextDouble());
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
