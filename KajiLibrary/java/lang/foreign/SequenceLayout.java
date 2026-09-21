package java.lang.foreign;

/**
 * KajiLibrary's java.lang.foreign.SequenceLayout -- N copies of a layout, one after the other.
 *
 * <p>It is C's array. The size is `N * element`, and the alignment **is the element's**: the
 * sequence imposes none of its own because, if each element falls aligned, so does the whole
 * sequence.
 */
public interface SequenceLayout extends MemoryLayout {

    /** The layout being repeated. */
    MemoryLayout elementLayout();

    /** How many times. */
    long elementCount();

    /**
     * The same sequence with another number of elements.
     *
     * @throws IllegalArgumentException if it is negative, or if the total size overflows a `long`
     */
    SequenceLayout withElementCount(long elementCount);

    /**
     * A single-dimension sequence with the same elements.
     *
     * <p>It flattens the nested levels: a sequence of 3 sequences of 4 `int` becomes one of 12. It
     * is for walking a matrix as though it were flat, which is how it sits in memory.
     */
    SequenceLayout flatten();

    /**
     * The same number of elements, spread over the dimensions asked for.
     *
     * <p>One of the dimensions may be `-1`: it is worked out from the others. It is the inverse of
     * {@link #flatten()}.
     *
     * @throws IllegalArgumentException if there is more than one `-1`, if some dimension is not
     *     positive, or if the product does not give the number of elements there are
     */
    SequenceLayout reshape(long... elementCounts);

    SequenceLayout withName(String name);

    SequenceLayout withoutName();

    SequenceLayout withByteAlignment(long byteAlignment);
}
