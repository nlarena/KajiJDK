package jdk.incubator.vector;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;
import java.util.function;
import java.util.function.IntUnaryOperator;
import jdk.internal.vm.vector.VectorSupport;

/**
 * A permutation of lanes: where each lane of the result comes from.
 *
 * <p>It is the operation that has no cheap scalar equivalent. Reordering, interleaving, reversing
 * or repeating lanes costs one instruction, and doing it with indices in a loop costs one memory
 * access per element.
 *
 * @since 16
 */
public abstract class VectorShuffle<E extends Object> extends VectorSupport.VectorShuffle<E> {

    /**
     * With that payload.
     *
     * <p>In the JDK this constructor is package-private, so it does not show up in the dumps. It is
     * written anyway because the superclass has no no-argument one: without it, the implicit
     * default constructor would call a {@code super()} that does not exist. The note said javac
     * then emits an invalid class file (finding #515); that finding is closed in the source javac,
     * which now rejects the class instead, but the frozen {@code bin/javac.exe} predates the fix.
     * The constructor is needed either way.
     *
     * @param payload the array of lanes
     */
    VectorShuffle(Object payload) {
        super(payload);
    }

    /**
     * The species of the vectors this shuffle knows how to rearrange.
     *
     * @return the {@code VectorSpecies<E>}
     */
    public abstract VectorSpecies<E> vectorSpecies();

    /**
     * How many lanes the shuffle has.
     *
     * @return the number
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final int length() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The same shuffle for another species of the same length.
     *
     * @param <F> the type, in its boxed form
     * @param vectorSpecies the {@code VectorSpecies<F>}
     * @return the {@code VectorShuffle<F>}
     */
    public abstract <F extends Object> VectorShuffle<F> cast(VectorSpecies<F> vectorSpecies);

    /**
     * Checks that the shuffle is of that species and returns the same, already typed.
     *
     * @param <F> the type, in its boxed form
     * @param vectorSpecies the {@code VectorSpecies<F>}
     * @return the {@code VectorShuffle<F>}
     */
    public abstract <F extends Object> VectorShuffle<F> check(VectorSpecies<F> vectorSpecies);

    /**
     * Checks that that index falls inside.
     *
     * @param i the {@code int}
     * @return the number
     */
    public abstract int checkIndex(int i);

    /**
     * That index brought into range by wrapping around.
     *
     * @param i the {@code int}
     * @return the number
     */
    public abstract int wrapIndex(int i);

    /**
     * Checks that all the indices fall inside.
     *
     * @return the {@code VectorShuffle<E>}
     */
    public abstract VectorShuffle<E> checkIndexes();

    /**
     * The shuffle with all its indices brought into range.
     *
     * @return the {@code VectorShuffle<E>}
     */
    public abstract VectorShuffle<E> wrapIndexes();

    /**
     * The mask of the indices that fall inside the vector.
     *
     * @return the {@code VectorMask<E>}
     */
    public abstract VectorMask<E> laneIsValid();

    /**
     * A shuffle with those indices.
     *
     * @param <E> the type, in its boxed form
     * @param vectorSpecies the {@code VectorSpecies<E>}
     * @param i the {@code int...}
     * @return the {@code VectorShuffle<E>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static <E extends Object> VectorShuffle<E> fromValues(VectorSpecies<E> vectorSpecies,
            int... i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A shuffle read from that array of indices.
     *
     * @param <E> the type, in its boxed form
     * @param vectorSpecies the {@code VectorSpecies<E>}
     * @param is the {@code int[]}
     * @param i the {@code int}
     * @return the {@code VectorShuffle<E>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static <E extends Object> VectorShuffle<E> fromArray(VectorSpecies<E> vectorSpecies,
            int[] is, int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A shuffle read from that memory segment.
     *
     * @param <E> the type, in its boxed form
     * @param vectorSpecies the {@code VectorSpecies<E>}
     * @param memorySegment the {@code java.lang.foreign.MemorySegment}
     * @param l the {@code long}
     * @param byteOrder the {@code java.nio.ByteOrder}
     * @return the {@code VectorShuffle<E>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static <E extends Object> VectorShuffle<E> fromMemorySegment(
            VectorSpecies<E> vectorSpecies, java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A shuffle whose indices that function computes from the lane.
     *
     * @param <E> the type, in its boxed form
     * @param vectorSpecies the {@code VectorSpecies<E>}
     * @param intUnaryOperator the {@code java.util.function.IntUnaryOperator}
     * @return the {@code VectorShuffle<E>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static <E extends Object> VectorShuffle<E> fromOp(VectorSpecies<E> vectorSpecies,
            java.util.function.IntUnaryOperator intUnaryOperator) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A shuffle that counts: it starts at a value and advances one step at a time.
     *
     * @param <E> the type, in its boxed form
     * @param vectorSpecies the {@code VectorSpecies<E>}
     * @param i the {@code int}
     * @param i2 the {@code int}
     * @param flag the {@code boolean}
     * @return the {@code VectorShuffle<E>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static <E extends Object> VectorShuffle<E> iota(VectorSpecies<E> vectorSpecies, int i,
            int i2, boolean flag) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The shuffle that interleaves two vectors.
     *
     * @param <E> the type, in its boxed form
     * @param vectorSpecies the {@code VectorSpecies<E>}
     * @param i the {@code int}
     * @return the {@code VectorShuffle<E>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static <E extends Object> VectorShuffle<E> makeZip(VectorSpecies<E> vectorSpecies, int i
            ) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The shuffle that undoes the interleaving.
     *
     * @param <E> the type, in its boxed form
     * @param vectorSpecies the {@code VectorSpecies<E>}
     * @param i the {@code int}
     * @return the {@code VectorShuffle<E>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static <E extends Object> VectorShuffle<E> makeUnzip(VectorSpecies<E> vectorSpecies,
            int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The indices in a new array.
     *
     * @return the {@code int[]}
     */
    public abstract int[] toArray();

    /**
     * Writes the indices into that array.
     *
     * @param is the {@code int[]}
     * @param i the {@code int}
     */
    public abstract void intoArray(int[] is, int i);

    /**
     * Writes the indices into that memory segment.
     *
     * @param memorySegment the {@code java.lang.foreign.MemorySegment}
     * @param l the {@code long}
     * @param byteOrder the {@code java.nio.ByteOrder}
     */
    public abstract void intoMemorySegment(java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder);

    /**
     * The indices seen as a vector.
     *
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> toVector();

    /**
     * Which source lane that lane comes from.
     *
     * @param i the {@code int}
     * @return the number
     */
    public abstract int laneSource(int i);

    /**
     * Composes this shuffle with the other.
     *
     * @param vectorShuffle the {@code VectorShuffle<E>}
     * @return the {@code VectorShuffle<E>}
     */
    public abstract VectorShuffle<E> rearrange(VectorShuffle<E> vectorShuffle);

    /**
     * The indices written as a list.
     *
     * @return the text
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final String toString() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Whether the other shuffle has the same indices.
     *
     * @param obj the {@code Object}
     * @return true or false, as the case may be
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final boolean equals(Object obj) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The hash code.
     *
     * @return the number
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final int hashCode() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }
}
