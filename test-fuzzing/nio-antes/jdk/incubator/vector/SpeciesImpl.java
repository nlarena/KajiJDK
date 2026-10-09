package jdk.incubator.vector;

import java.nio.ByteOrder;
import java.util.function.IntUnaryOperator;

/**
 * The concrete species of this package: a lane type plus a shape.
 *
 * <h2>Which part is real</h2>
 *
 * <p>A species is above all arithmetic. How many lanes fit, how many bits each takes, how far a
 * loop that advances one vector at a time goes, how many parts it takes to go from one species to
 * another: all of that comes from two numbers and needs no vector instruction. It is really
 * implemented and checked against JDK 25, including {@link #partLimit} over the 1152 combinations
 * of type and shape there are.
 *
 * <p>What cannot work is what <strong>makes</strong> things: {@link #zero()}, {@link #fromArray},
 * {@link #maskAll} and the rest would return a vector, and for that the intrinsics are needed.
 * Those throw {@link UnsupportedOperationException}.
 *
 * <h2>Two answers less precise than the JDK's</h2>
 *
 * <p>{@link #vectorType()} answers {@code IntVector.class} where the JDK answers {@code
 * Int128Vector.class}, and {@link #maskType()} answers {@link MaskImpl} where the JDK has one mask
 * class per combination. The JDK's classes are private to their package --nobody can name them from
 * outside-- and they exist because each one carries its intrinsic implementation. Without
 * intrinsics there is nothing that tells them apart, so there is a single one. The answer still
 * keeps what the type promises: it is a vector class, and it is a mask class.
 *
 * @param <E> the lane type, in its boxed form
 */
final class SpeciesImpl<E> implements VectorSpecies<E> {

    private final Class<?> etype;
    private final VectorShape shape;

    private SpeciesImpl(final Class<?> etype, final VectorShape shape) {
        this.etype = etype;
        this.shape = shape;
    }

    /**
     * The species of that type and that shape.
     *
     * @param <E> the lane type, boxed
     * @param etype the lane type
     * @param shape the shape
     * @return the species
     * @throws IllegalArgumentException if the type cannot be a vector lane
     */
    static <E> VectorSpecies<E> create(final Class<E> etype, final VectorShape shape) {
        return new SpeciesImpl<E>(validate(etype), shape);
    }

    private static Class<?> validate(final Class<?> t) {
        if (t != byte.class && t != short.class && t != int.class && t != long.class
                && t != float.class && t != double.class) {
            throw new IllegalArgumentException("Bad vector element type: " + t);
        }
        return t;
    }

    /**
     * The size in bits of a lane of that type.
     *
     * @param t the type
     * @return 8, 16, 32 o 64
     */
    static int bitsOf(final Class<?> t) {
        if (t == byte.class) {
            return 8;
        }
        if (t == short.class) {
            return 16;
        }
        if (t == int.class || t == float.class) {
            return 32;
        }
        if (t == long.class || t == double.class) {
            return 64;
        }
        throw new IllegalArgumentException("Bad vector element type: " + t);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<E> elementType() {
        return (Class<E>) etype;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends Vector<E>> vectorType() {
        final Class<?> c;
        if (etype == byte.class) {
            c = ByteVector.class;
        } else if (etype == short.class) {
            c = ShortVector.class;
        } else if (etype == int.class) {
            c = IntVector.class;
        } else if (etype == long.class) {
            c = LongVector.class;
        } else if (etype == float.class) {
            c = FloatVector.class;
        } else {
            c = DoubleVector.class;
        }
        return (Class<? extends Vector<E>>) c;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends VectorMask<E>> maskType() {
        return (Class<? extends VectorMask<E>>) (Class<?>) MaskImpl.class;
    }

    @Override
    public int elementSize() {
        return bitsOf(etype);
    }

    @Override
    public VectorShape vectorShape() {
        return shape;
    }

    @Override
    public int length() {
        return vectorBitSize() / elementSize();
    }

    @Override
    public int vectorBitSize() {
        return shape.vectorBitSize();
    }

    @Override
    public int vectorByteSize() {
        return vectorBitSize() / 8;
    }

    /**
     * The largest multiple of {@link #length()} that does not exceed that number.
     *
     * <p>It is what goes in the condition of a loop that advances a whole vector at a time, to know
     * where to stop and move on to the remainder. It is done with bits and not with a division
     * because the lane count is always a power of two; besides, that way it rounds down with
     * negatives too, which is what the JDK does.
     *
     * @param length the length
     * @return the multiple
     */
    @Override
    public int loopBound(final int length) {
        return length & ~(length() - 1);
    }

    @Override
    public long loopBound(final long length) {
        return length & ~((long) length() - 1);
    }

    @Override
    public VectorMask<E> indexInRange(final int offset, final int limit) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public VectorMask<E> indexInRange(final long offset, final long limit) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> VectorSpecies<F> check(final Class<F> elementType) {
        if (elementType != etype) {
            throw new ClassCastException(this + ": required " + elementType.getName()
                    + " but found " + etype.getName());
        }
        return (VectorSpecies<F>) this;
    }

    /**
     * How many parts it takes to go from this species to the other.
     *
     * <p>When the target does not fit in the source the result is negative, and then the number
     * says how many times one has to <strong>expand</strong> instead of split. Zero means neither
     * is needed.
     *
     * <p>The computation is between logarithms because everything involved is a power of two: the
     * size of the vector and that of the lane. With {@code lanewise} the change of lane size is
     * counted as well, which is what tells reinterpreting from converting.
     *
     * @param toSpecies the target species
     * @param lanewise whether it converts lane by lane
     * @return the number of parts, negative if it has to expand
     */
    @Override
    public int partLimit(final VectorSpecies<?> toSpecies, final boolean lanewise) {
        int source = log2(shape.vectorBitSize());
        final int target = log2(toSpecies.vectorShape().vectorBitSize());
        if (lanewise) {
            source += log2(toSpecies.elementSize()) - log2(elementSize());
        }
        final int d = source - target;
        if (d == 0) {
            return 0;
        }
        return d > 0 ? 1 << d : -(1 << -d);
    }

    private static int log2(final int n) {
        return Integer.numberOfTrailingZeros(n);
    }

    @Override
    public <F> VectorSpecies<F> withLanes(final Class<F> elementType) {
        return SpeciesImpl.create(elementType, shape);
    }

    @Override
    public VectorSpecies<E> withShape(final VectorShape newShape) {
        return new SpeciesImpl<E>(etype, newShape);
    }

    @Override
    public Vector<E> zero() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public Vector<E> fromArray(final Object a, final int offset) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public Vector<E> fromMemorySegment(final java.lang.foreign.MemorySegment ms, final long offset,
            final ByteOrder bo) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public VectorMask<E> loadMask(final boolean[] bits, final int offset) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public VectorMask<E> maskAll(final boolean bit) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public Vector<E> broadcast(final long e) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Checks that the value fits in a lane of this species.
     *
     * <p>The test is a round trip: it is converted to the lane type and brought back. If that does
     * not give the same, it did not fit. It serves the integers --where high bits are lost-- and
     * {@code float}, where what is lost is significant digits, alike.
     *
     * @param e the value
     * @return the same value
     * @throws IllegalArgumentException if it does not fit
     */
    @Override
    public long checkValue(final long e) {
        final long roundTrip;
        final String castText;
        if (etype == byte.class) {
            roundTrip = (byte) e;
            castText = String.valueOf((byte) e);
        } else if (etype == short.class) {
            roundTrip = (short) e;
            castText = String.valueOf((short) e);
        } else if (etype == int.class) {
            roundTrip = (int) e;
            castText = String.valueOf((int) e);
        } else if (etype == float.class) {
            roundTrip = (long) (float) e;
            castText = String.valueOf((float) e);
        } else if (etype == double.class) {
            roundTrip = (long) (double) e;
            castText = String.valueOf((double) e);
        } else {
            return e;
        }
        if (roundTrip != e) {
            throw new IllegalArgumentException("Vector creation failed: value " + e
                    + " cannot be represented in ETYPE " + etype.getName()
                    + "; result of cast is " + castText);
        }
        return e;
    }

    @Override
    public VectorShuffle<E> shuffleFromValues(final int... sourceIndexes) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public VectorShuffle<E> shuffleFromArray(final int[] sourceIndexes, final int offset) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public VectorShuffle<E> shuffleFromOp(final IntUnaryOperator fn) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public VectorShuffle<E> iotaShuffle(final int start, final int step, final boolean wrap) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public String toString() {
        return "Species[" + etype.getName() + ", " + length() + ", " + shape + "]";
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SpeciesImpl)) {
            return false;
        }
        final SpeciesImpl<?> o = (SpeciesImpl<?>) obj;
        return etype == o.etype && shape == o.shape;
    }

    @Override
    public int hashCode() {
        return etype.hashCode() * 31 + shape.hashCode();
    }
}
