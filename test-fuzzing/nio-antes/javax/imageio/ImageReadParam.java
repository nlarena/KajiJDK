package javax.imageio;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

/**
 * KajiLibrary's javax.imageio.ImageReadParam -- how to read an image.
 *
 * <p>It adds to {@link IIOParam} what only makes sense when reading: where to write the result,
 * which destination bands to use, the render size, and up to which pass to read.
 *
 * <h2>{@link #setDestination} versus {@code setDestinationType}</h2>
 *
 * <p>Both say something about the destination and they are not the same:
 *
 * <ul>
 *   <li>the <b>type</b> ({@link IIOParam#setDestinationType}) says what class of image to create;
 *   <li>the <b>destination</b> ({@link #setDestination}) gives an image <b>that already exists</b>
 *       and has to be written into.
 * </ul>
 *
 * <p>Giving both is contradictory, and {@link #setDestinationType}, redefined here, clears the
 * destination: the last one set wins. (An earlier note said the destination always wins; the
 * JDK's override also clears it.) Here it also clears the destination bands, which the JDK does
 * not. Reusing an image is what allows reading an animation frame by frame without allocating a
 * new one each time.
 *
 * <h2>The render size</h2>
 *
 * <p>{@link #setSourceRenderSize} only works in formats that <b>scale while decoding</b> --vector
 * ones, or progressive ones with levels. That is why you have to ask first with
 * {@link #canSetSourceRenderSize}: asking for it when it cannot be done throws
 * {@link UnsupportedOperationException}.
 *
 * <p>It is not the same as subsampling: subsampling drops pixels, this asks the decoder to produce
 * the wanted size directly, which usually comes out quite a bit better.
 *
 * <h2>Progressive passes</h2>
 *
 * <p>A progressive JPEG is decoded in passes, each one sharper.
 * {@link #setSourceProgressivePasses} allows stopping early: reading only the first ones gives a
 * blurry image in a fraction of the time, which is exactly what a preview needs.
 */
public class ImageReadParam extends IIOParam {

    /** Whether this reader can scale while decoding. */
    protected boolean canSetSourceRenderSize = false;

    /** What size to render at, or null. */
    protected Dimension sourceRenderSize = null;

    /** Where to write, or null to have one created. */
    protected BufferedImage destination = null;

    /** Which destination bands to use, or null for all. */
    protected int[] destinationBands = null;

    /** From which pass. */
    protected int minProgressivePass = 0;

    /** How many passes to read; {@link Integer#MAX_VALUE} is all of them. */
    protected int numProgressivePasses = Integer.MAX_VALUE;

    /** Everything by default. */
    public ImageReadParam() {
    }

    /**
     * What type to create the destination as.
     *
     * <p>Redefined so that setting the type clears the destination (and, in this library, the
     * destination bands); see the class note.
     */
    @Override
    public void setDestinationType(ImageTypeSpecifier destinationType) {
        super.setDestinationType(destinationType);
        setDestination(null);
        setDestinationBands(null);
    }

    /** Where to write; null makes the reader create an image. */
    public void setDestination(BufferedImage destination) {
        this.destination = destination;
    }

    /** Where to write, or null. */
    public BufferedImage getDestination() {
        return this.destination;
    }

    /**
     * Which destination bands to write; null means all.
     *
     * <p>Same rules as {@link IIOParam#setSourceBands}, without repeats and without negatives,
     * except that here an empty array is rejected (the JDK accepts it, as it does for source
     * bands).
     *
     * @throws IllegalArgumentException if it is empty, has negatives or repeats one
     */
    public void setDestinationBands(int[] destinationBands) {
        if (destinationBands == null) {
            this.destinationBands = null;
            return;
        }
        int numBands = destinationBands.length;
        if (numBands == 0) {
            throw new IllegalArgumentException("destinationBands.length == 0!");
        }
        int i = 0;
        while (i < numBands) {
            int band = destinationBands[i];
            if (band < 0) {
                throw new IllegalArgumentException("destinationBands[" + i + "] < 0!");
            }
            int j = i + 1;
            while (j < numBands) {
                if (band == destinationBands[j]) {
                    throw new IllegalArgumentException("Duplicate band value!");
                }
                j = j + 1;
            }
            i = i + 1;
        }
        this.destinationBands = new int[numBands];
        System.arraycopy(destinationBands, 0, this.destinationBands, 0, numBands);
    }

    /** Which destination bands, or null. It is a copy. */
    public int[] getDestinationBands() {
        if (this.destinationBands == null) {
            return null;
        }
        int[] copy = new int[this.destinationBands.length];
        System.arraycopy(this.destinationBands, 0, copy, 0, this.destinationBands.length);
        return copy;
    }

    /** Whether this reader can scale while decoding. See the class note. */
    public boolean canSetSourceRenderSize() {
        return this.canSetSourceRenderSize;
    }

    /**
     * What size to render at; null goes back to the natural one.
     *
     * @throws UnsupportedOperationException if this reader cannot do it
     * @throws IllegalArgumentException if the width or height are not positive
     */
    public void setSourceRenderSize(Dimension size) throws UnsupportedOperationException {
        if (!canSetSourceRenderSize()) {
            throw new UnsupportedOperationException("Can't set source render size!");
        }
        if (size == null) {
            this.sourceRenderSize = null;
            return;
        }
        if (size.width <= 0 || size.height <= 0) {
            throw new IllegalArgumentException("width or height <= 0!");
        }
        this.sourceRenderSize = (Dimension) size.clone();
    }

    /** What size, or null. It is a copy. */
    public Dimension getSourceRenderSize() {
        if (this.sourceRenderSize == null) {
            return null;
        }
        return (Dimension) this.sourceRenderSize.clone();
    }

    /**
     * Which progressive passes to read. See the class note.
     *
     * @param minPass the first one, from 0
     * @param numPasses how many; {@link Integer#MAX_VALUE} is all there are
     * @throws IllegalArgumentException if the first is negative, if the count is not positive, or
     *     if the sum overflows
     */
    public void setSourceProgressivePasses(int minPass, int numPasses) {
        if (minPass < 0) {
            throw new IllegalArgumentException("minPass < 0!");
        }
        if (numPasses <= 0) {
            throw new IllegalArgumentException("numPasses <= 0!");
        }
        if (numPasses != Integer.MAX_VALUE && minPass + numPasses - 1 > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("minPass + numPasses - 1 > INTEGER.MAX_VALUE!");
        }
        this.minProgressivePass = minPass;
        this.numProgressivePasses = numPasses;
    }

    /** From which pass. */
    public int getSourceMinProgressivePass() {
        return this.minProgressivePass;
    }

    /**
     * Up to which.
     *
     * <p>{@link Integer#MAX_VALUE} means "all there are", not a pass number two billion.
     */
    public int getSourceMaxProgressivePass() {
        if (this.numProgressivePasses == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return getSourceMinProgressivePass() + this.numProgressivePasses - 1;
    }

    /** How many. */
    public int getSourceNumProgressivePasses() {
        return this.numProgressivePasses;
    }
}
