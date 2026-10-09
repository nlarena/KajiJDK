package jdk.incubator.vector;

/**
 * The size in bits of a vector, without saying what the lanes are.
 *
 * <h2>Why the size goes apart from the type</h2>
 *
 * <p>A 256-bit vector register is 256 bits whatever is kept inside: it may be 32 {@code byte}s, 8
 * {@code int}s or 4 {@code double}s. The hardware has registers of a fixed width, and what changes
 * is into how many pieces it is looked at.
 *
 * <p>That is the reason why the shape and the lane type are two separate things that combine:
 * {@link #withLanes(Class)} takes a shape and a type and gives the concrete species. A species is a
 * shape with the lanes already decided.
 *
 * <h2>{@link #S_Max_BIT}</h2>
 *
 * <p>The other four constants name a fixed size. This one names <strong>the largest this machine
 * can do</strong>, which is known only at run time. It serves to write code that makes the most of
 * whatever hardware it gets without choosing a width by hand.
 *
 * <h2>What the maximum is on this VM</h2>
 *
 * <p>It is 64, the minimum the API admits. It is not a choice: the real maximum comes from asking
 * the VM's intrinsics how many lanes fit in a register, and this VM does not have them. Saying 512
 * would be lying, and {@link #forBitSize(int)} would return a shape that then cannot be filled.
 *
 * <p>That is why {@link #preferredShape()} and {@link #largestShapeFor(Class)} return {@link
 * #S_64_BIT} here. The sizes and the arithmetic of this class are real and can be used; what there
 * is not is anything to create the vector with.
 *
 * @since 16
 */
public enum VectorShape {

    /** 64-bit vectors. */
    S_64_BIT(64),
    /** 128-bit vectors. */
    S_128_BIT(128),
    /** 256-bit vectors. */
    S_256_BIT(256),
    /** 512-bit vectors. */
    S_512_BIT(512),
    /** The largest vector this machine supports; here, 64 bits. */
    S_Max_BIT(Limits.MAX_BITS);

    /**
     * The maximum this VM can sustain.
     *
     * <p>In the JDK it comes from {@code VectorSupport.getMaxLaneCount}, which is an intrinsic.
     * Here it is the constant the API sets as the floor, because there is nobody to ask.
     *
     * <p>It goes in a separate class and not as a field of this enum because the argument of an
     * enum constant cannot read a static field of the same enum: when the constants are built the
     * class has not finished initialising yet, so the language forbids it.
     */
    static final class Limits {
        static final int MAX_BITS = 64;

        private Limits() {
        }
    }

    private final int vectorBitSize;

    VectorShape(final int vectorBitSize) {
        this.vectorBitSize = vectorBitSize;
    }

    /**
     * The size of the vector in bits.
     *
     * @return the bits
     */
    public int vectorBitSize() {
        return vectorBitSize;
    }

    /**
     * The species that results from filling this shape with lanes of that type.
     *
     * @param <E> the lane type, in its boxed form
     * @param elementType the lane type
     * @return the species
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public <E> VectorSpecies<E> withLanes(final Class<E> elementType) {
        return VectorSpecies.of(elementType, this);
    }

    /**
     * The shape of that size in bits.
     *
     * <p>The four fixed sizes are always recognised. Any other is valid only if it is a multiple of
     * 128 that does not exceed 2048, and then the answer is {@link #S_Max_BIT}: it is the only case
     * in which a size that is not named exists all the same, because the machine's maximum may be
     * any of those.
     *
     * @param bitSize the size in bits
     * @return the shape
     * @throws IllegalArgumentException if there is no shape of that size
     */
    public static VectorShape forBitSize(final int bitSize) {
        switch (bitSize) {
            case 64:
                return S_64_BIT;
            case 128:
                return S_128_BIT;
            case 256:
                return S_256_BIT;
            case 512:
                return S_512_BIT;
            default:
                if (bitSize > 0 && bitSize <= 2048 && bitSize % 128 == 0) {
                    return S_Max_BIT;
                }
                throw new IllegalArgumentException("Bad vector bit-size: " + bitSize);
        }
    }

    /**
     * The shape whose index vector, for that lane size, measures those bits.
     *
     * <p>An index vector goes with another vector saying where each lane comes from, and its
     * indices are always {@code int}. If the original vector has 8-bit lanes, the index one needs
     * four times more room for the same lanes. This method does that computation backwards: given
     * the size the index vector takes, it returns the shape of the other.
     *
     * <p>That is why 32 and 64 both give {@link #S_64_BIT}: a single 32-bit index already
     * corresponds to the smallest vector.
     *
     * @param bitSize the size of the index vector, in bits
     * @param elementSize the lane size of the original vector, in bits
     * @return the shape
     * @throws IllegalArgumentException if there is no corresponding shape
     */
    public static VectorShape forIndexBitSize(final int bitSize, final int elementSize) {
        switch (bitSize) {
            case 32:
            case 64:
                return S_64_BIT;
            case 128:
                return S_128_BIT;
            case 256:
                return S_256_BIT;
            case 512:
                return S_512_BIT;
            default:
                final int max = 2048 / elementSize * 32;
                final int min = 128 / elementSize * 32;
                if (bitSize > 0 && bitSize <= max && bitSize % min == 0) {
                    return S_Max_BIT;
                }
                throw new IllegalArgumentException("Bad vector index bit-size: " + bitSize);
        }
    }

    /**
     * The largest shape this machine can do with lanes of that type.
     *
     * @param etype the lane type
     * @return the largest shape
     */
    public static VectorShape largestShapeFor(final Class<?> etype) {
        return forBitSize(maxBitsFor(etype));
    }

    /**
     * The shape worth using on this machine.
     *
     * <p>It is the largest that serves <strong>all</strong> the lane types, not simply the largest:
     * code that mixes {@code byte} and {@code double} needs a shape both can do.
     *
     * @return the preferred shape
     */
    public static VectorShape preferredShape() {
        return forBitSize(Limits.MAX_BITS);
    }

    /**
     * The maximum in bits for that lane type.
     *
     * <p>The {@code etype} is validated even though the result does not depend on it: the public
     * method that calls here has to reject a type that is not a lane type, and not doing so would
     * be accepting {@code largestShapeFor(String.class)}.
     */
    private static int maxBitsFor(final Class<?> etype) {
        if (etype != byte.class && etype != short.class && etype != int.class
                && etype != long.class && etype != float.class && etype != double.class) {
            throw new IllegalArgumentException("Bad vector element type: " + etype);
        }
        return Limits.MAX_BITS;
    }
}
