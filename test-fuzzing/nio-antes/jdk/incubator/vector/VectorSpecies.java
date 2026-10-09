package jdk.incubator.vector;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;
import java.util.function;
import java.util.function.IntUnaryOperator;

/**
 * The lane type plus the vector's shape: how many lanes there are and of what size.
 *
 * <p>It is the first thing asked for and what decides everything else. A 256-bit {@code
 * VectorSpecies<Integer>} has eight lanes; the same type with 128 bits has four.
 *
 * <p>{@link #loopBound} is the method one has to use and the one that gets forgotten: it returns
 * how far the vector loop goes, and what is left over --the remainder-- is done one element at a
 * time. Without it, an array whose length is not a multiple of the lane count is processed wrongly
 * or goes out of range.
 *
 * <p>In this library the metadata is real: {@link #length}, {@link #vectorBitSize}, {@link
 * #elementSize} and {@link #loopBound} really compute. What cannot work is making vectors.
 *
 * @since 16
 */
public interface VectorSpecies<E extends Object> {

    /**
     * The type of the lanes.
     *
     * @return the {@code Class<E>}
     */
    Class<E> elementType();

    /**
     * The class of the vectors of this species.
     *
     * @return the {@code Class<? extends Vector<E>>}
     */
    Class<? extends Vector<E>> vectorType();

    /**
     * The class of the masks of this species.
     *
     * @return the {@code Class<? extends VectorMask<E>>}
     */
    Class<? extends VectorMask<E>> maskType();

    /**
     * The size of a lane, in bits.
     *
     * @return the number
     */
    int elementSize();

    /**
     * The shape of this species.
     *
     * @return the {@code VectorShape}
     */
    VectorShape vectorShape();

    /**
     * How many lanes it has.
     *
     * @return the number
     */
    int length();

    /**
     * The size of the vector, in bits.
     *
     * @return the number
     */
    int vectorBitSize();

    /**
     * The size of the vector, in bytes.
     *
     * @return the number
     */
    int vectorByteSize();

    /**
     * The largest multiple of the lane count that does not exceed that number.
     *
     * @param i the {@code int}
     * @return the number
     */
    int loopBound(int i);

    /**
     * The largest multiple of the lane count that does not exceed that number.
     *
     * @param l the {@code long}
     * @return the number
     */
    long loopBound(long l);

    /**
     * The mask of the lanes whose index still falls within the range.
     *
     * @param i the {@code int}
     * @param i2 the {@code int}
     * @return the {@code VectorMask<E>}
     */
    VectorMask<E> indexInRange(int i, int i2);

    /**
     * The mask of the lanes whose index still falls within the range.
     *
     * @param l the {@code long}
     * @param l2 the {@code long}
     * @return the {@code VectorMask<E>}
     */
    VectorMask<E> indexInRange(long l, long l2);

    /**
     * Checks that the lane type is that one and returns the same, already typed.
     *
     * @param <F> the type, in its boxed form
     * @param classArg the {@code Class<F>}
     * @return the {@code VectorSpecies<F>}
     */
    <F extends Object> VectorSpecies<F> check(Class<F> classArg);

    /**
     * How many parts it takes to go to the other species.
     *
     * @param vectorSpecies the {@code VectorSpecies<?>}
     * @param flag the {@code boolean}
     * @return the number
     */
    int partLimit(VectorSpecies<?> vectorSpecies, boolean flag);

    /**
     * The same shape with another lane type.
     *
     * @param <F> the type, in its boxed form
     * @param classArg the {@code Class<F>}
     * @return the {@code VectorSpecies<F>}
     */
    <F extends Object> VectorSpecies<F> withLanes(Class<F> classArg);

    /**
     * The same lane type with another shape.
     *
     * @param vectorShape the {@code VectorShape}
     * @return the {@code VectorSpecies<E>}
     */
    VectorSpecies<E> withShape(VectorShape vectorShape);

    /**
     * The species of that type and that shape.
     *
     * @param <E> the type, in its boxed form
     * @param classArg the {@code Class<E>}
     * @param vectorShape the {@code VectorShape}
     * @return the {@code VectorSpecies<E>}
     */
    static <E extends Object> VectorSpecies<E> of(Class<E> classArg, VectorShape vectorShape) {
        return SpeciesImpl.create(classArg, vectorShape);
    }

    /**
     * The species of that type with this machine's largest shape.
     *
     * @param <E> the type, in its boxed form
     * @param classArg the {@code Class<E>}
     * @return the {@code VectorSpecies<E>}
     */
    static <E extends Object> VectorSpecies<E> ofLargestShape(Class<E> classArg) {
        return SpeciesImpl.create(classArg, VectorShape.largestShapeFor(classArg));
    }

    /**
     * The species of that type with this machine's preferred shape.
     *
     * @param <E> the type, in its boxed form
     * @param classArg the {@code Class<E>}
     * @return the {@code VectorSpecies<E>}
     */
    static <E extends Object> VectorSpecies<E> ofPreferred(Class<E> classArg) {
        return SpeciesImpl.create(classArg, VectorShape.preferredShape());
    }

    /**
     * The size of a lane, in bits.
     *
     * @param classArg the {@code Class<?>}
     * @return the number
     */
    static int elementSize(Class<?> classArg) {
        return SpeciesImpl.bitsOf(classArg);
    }

    /**
     * A vector with every lane at zero.
     *
     * @return the {@code Vector<E>}
     */
    Vector<E> zero();

    /**
     * A vector read from that array.
     *
     * @param obj the {@code Object}
     * @param i the {@code int}
     * @return the {@code Vector<E>}
     */
    Vector<E> fromArray(Object obj, int i);

    /**
     * A vector read from that memory segment.
     *
     * @param memorySegment the {@code java.lang.foreign.MemorySegment}
     * @param l the {@code long}
     * @param byteOrder the {@code java.nio.ByteOrder}
     * @return the {@code Vector<E>}
     */
    Vector<E> fromMemorySegment(java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder);

    /**
     * A mask read from that array of flags.
     *
     * @param flags the {@code boolean[]}
     * @param i the {@code int}
     * @return the {@code VectorMask<E>}
     */
    VectorMask<E> loadMask(boolean[] flags, int i);

    /**
     * A mask with every lane at that value.
     *
     * @param flag the {@code boolean}
     * @return the {@code VectorMask<E>}
     */
    VectorMask<E> maskAll(boolean flag);

    /**
     * A vector with the same value in every lane.
     *
     * @param l the {@code long}
     * @return the {@code Vector<E>}
     */
    Vector<E> broadcast(long l);

    /**
     * Checks that the value fits in a lane of this species.
     *
     * @param l the {@code long}
     * @return the number
     */
    long checkValue(long l);

    /**
     * A shuffle with those indices.
     *
     * @param i the {@code int...}
     * @return the {@code VectorShuffle<E>}
     */
    VectorShuffle<E> shuffleFromValues(int... i);

    /**
     * A shuffle read from that array.
     *
     * @param is the {@code int[]}
     * @param i the {@code int}
     * @return the {@code VectorShuffle<E>}
     */
    VectorShuffle<E> shuffleFromArray(int[] is, int i);

    /**
     * A shuffle whose indices that function computes.
     *
     * @param intUnaryOperator the {@code java.util.function.IntUnaryOperator}
     * @return the {@code VectorShuffle<E>}
     */
    VectorShuffle<E> shuffleFromOp(java.util.function.IntUnaryOperator intUnaryOperator);

    /**
     * A counting shuffle, of this species.
     *
     * @param i the {@code int}
     * @param i2 the {@code int}
     * @param flag the {@code boolean}
     * @return the {@code VectorShuffle<E>}
     */
    VectorShuffle<E> iotaShuffle(int i, int i2, boolean flag);

    /**
     * A readable representation.
     *
     * @return the text
     */
    String toString();

    /**
     * Whether the other one is equal to this one.
     *
     * @param obj the {@code Object}
     * @return true or false, as the case may be
     */
    boolean equals(Object obj);

    /**
     * The hash code.
     *
     * @return the number
     */
    int hashCode();
}
