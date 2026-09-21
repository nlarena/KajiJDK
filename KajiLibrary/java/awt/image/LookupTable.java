package java.awt.image;

/**
 * A lookup table: for each component of a pixel, which value comes out for each value that goes in.
 *
 * <p>It is the way of expressing any transformation **per component and without memory** -- adjust
 * the brightness, invert, apply a curve-- in such a way that applying it is reading an array and
 * not evaluating a function per pixel.
 *
 * <h2>The offset</h2>
 *
 * <p>{@link #getOffset} is what is **subtracted** from the input value before indexing. A table
 * that only covers the range 100..200 is stored with 101 entries and offset 100, instead of with
 * 201 entries of which the first 100 are not used. Forgetting to subtract it gives a shifted image.
 *
 * <h2>One table or one per component</h2>
 *
 * <p>The subclasses accept both forms. With a **single** table, it is applied to every component;
 * with **several**, one to each. The second form is the one that allows, for example, raising the
 * red without touching the green.
 */
public abstract class LookupTable {

    private final int numComponents;
    private final int offset;

    /**
     * A table for `numComponents` components, with that offset.
     *
     * @throws IllegalArgumentException if the offset is negative or there are no components
     */
    protected LookupTable(int offset, int numComponents) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be greater than or equal to 0");
        }
        if (numComponents < 1) {
            throw new IllegalArgumentException("Number of components must be at least 1");
        }
        this.numComponents = numComponents;
        this.offset = offset;
    }

    /** How many components it covers. */
    public int getNumComponents() {
        return this.numComponents;
    }

    /** What is subtracted from the input before indexing. See the note of the class. */
    public int getOffset() {
        return this.offset;
    }

    /**
     * Applies the table to a pixel.
     *
     * @param src the components that go in
     * @param dst where to leave the ones that come out, or null for one to be reserved
     */
    public abstract int[] lookupPixel(int[] src, int[] dst);
}
