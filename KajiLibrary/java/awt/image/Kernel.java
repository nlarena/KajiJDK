package java.awt.image;

/**
 * The matrix of a convolution: the weights with which each neighbouring pixel contributes to the
 * result.
 *
 * <p>The only thing to understand about this class is **where the centre is**, because everything
 * else follows from it. The origin is the pixel being computed, and it is taken in the middle:
 * `(width - 1) / 2` and `(height - 1) / 2`. For a 3x3 kernel it is (1,1), that is, the middle one;
 * for a 4x4 one it is (1,1) as well -- with an even side there is no exact centre and the integer
 * division moves it up and to the left. That shifts the image half a pixel, and it is the reason
 * why kernels are made with an odd side.
 *
 * <p>The data go **by rows**: `data[y * width + x]`.
 *
 * <p>It is immutable: the array is copied on the way in and {@link #getKernelData} returns another
 * copy. A kernel somebody could change after configuring the filter would give a different result
 * in each strip of the image.
 */
public class Kernel implements Cloneable {

    private final int width;
    private final int height;
    private final int xOrigin;
    private final int yOrigin;
    private final float[] data;

    /**
     * A kernel of `width` by `height` with those weights.
     *
     * @throws IllegalArgumentException if the array has fewer than `width * height` weights, or is
     *     `null`
     */
    public Kernel(int width, int height, float[] data) {
        this.width = width;
        this.height = height;
        this.xOrigin = (width - 1) >> 1;
        this.yOrigin = (height - 1) >> 1;
        int n = width * height;
        if (data == null || data.length < n) {
            throw new IllegalArgumentException("Data array too small (is " +
                    (data == null ? 0 : data.length) + " and should be " + n);
        }
        this.data = new float[n];
        System.arraycopy(data, 0, this.data, 0, n);
    }

    /** The column of the origin. See the note of the class. */
    public final int getXOrigin() {
        return this.xOrigin;
    }

    /** The row of the origin. */
    public final int getYOrigin() {
        return this.yOrigin;
    }

    /** The width. */
    public final int getWidth() {
        return this.width;
    }

    /** The height. */
    public final int getHeight() {
        return this.height;
    }

    /**
     * The weights, by rows.
     *
     * @param data where to leave them, or null for one to be reserved
     * @throws IllegalArgumentException if the given array is smaller than the kernel
     */
    public final float[] getKernelData(float[] data) {
        if (data == null) {
            float[] out = new float[this.data.length];
            System.arraycopy(this.data, 0, out, 0, this.data.length);
            return out;
        }
        if (data.length < this.data.length) {
            throw new IllegalArgumentException(
                    "Data array too small (should be " + this.data.length + ")");
        }
        System.arraycopy(this.data, 0, data, 0, this.data.length);
        return data;
    }

    /**
     * A copy. The constructor copies the weights, so the new kernel shares nothing with this one.
     */
    public Object clone() {
        return new Kernel(this.width, this.height, this.data);
    }
}
