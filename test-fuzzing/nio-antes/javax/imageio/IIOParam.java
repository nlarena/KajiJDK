package javax.imageio;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * KajiLibrary's javax.imageio.IIOParam -- which part of the image, and how.
 *
 * <p>What {@link ImageReadParam} and {@link ImageWriteParam} share: cropping, subsampling, choosing
 * bands and offsetting the destination.
 *
 * <h2>Subsampling has an offset, which is why it is four numbers</h2>
 *
 * <p>{@link #setSourceSubsampling} takes the step in X and Y <b>and also</b> which pixel to start
 * from. With a step of 2 and offset 0 the even pixels are taken; with offset 1, the odd ones.
 *
 * <p>That is what allows reading a huge image in four passes that together cover it whole, or
 * making a thumbnail without loading everything. The offsets must be <b>smaller</b> than the step:
 * otherwise the whole first block would be skipped.
 *
 * <h2>Cropping and subsampling combine in that order</h2>
 *
 * <p>First the image is cropped to the region, and then subsampled within the crop -- and the
 * subsampling offset counts from the edge of the region, not of the image.
 *
 * <p>{@link #setSourceRegion} with null removes the crop, just as {@link #setSourceBands} with null
 * goes back to all bands. It is the whole class's convention: null means "the usual".
 *
 * <h2>{@link #setDestinationOffset}</h2>
 *
 * <p>Where to put what was read within the destination image. It is how a mosaic is built by
 * reading pieces of several images into a single one.
 */
public abstract class IIOParam {

    /** Which piece of the source, or null for all of it. */
    protected Rectangle sourceRegion = null;

    /** Every how many pixels in X. */
    protected int sourceXSubsampling = 1;

    /** Every how many in Y. */
    protected int sourceYSubsampling = 1;

    /** Which pixel to start from in X. See the class note. */
    protected int subsamplingXOffset = 0;

    /** Same in Y. */
    protected int subsamplingYOffset = 0;

    /** Which bands, or null for all. */
    protected int[] sourceBands = null;

    /** What type the destination has to be, or null. */
    protected ImageTypeSpecifier destinationType = null;

    /** Where to place what was read in the destination. */
    protected Point destinationOffset = new Point(0, 0);

    /** The default controller, or null. */
    protected IIOParamController defaultController = null;

    /** The one set now. */
    protected IIOParamController controller = null;

    /** For the subclasses. */
    protected IIOParam() {
    }

    /**
     * Sets the source crop; null removes it.
     *
     * @throws IllegalArgumentException if the width or height are zero or negative, or if the
     *     corner is negative
     */
    public void setSourceRegion(Rectangle sourceRegion) {
        if (sourceRegion != null) {
            if (sourceRegion.x < 0) {
                throw new IllegalArgumentException("sourceRegion.x < 0!");
            }
            if (sourceRegion.y < 0) {
                throw new IllegalArgumentException("sourceRegion.y < 0!");
            }
            if (sourceRegion.width <= 0) {
                throw new IllegalArgumentException("sourceRegion.width <= 0!");
            }
            if (sourceRegion.height <= 0) {
                throw new IllegalArgumentException("sourceRegion.height <= 0!");
            }
            // A crop so small that subsampling would not take even one pixel is not a valid crop:
            // the result would be an empty image.
            if (sourceRegion.width <= this.subsamplingXOffset) {
                throw new IllegalArgumentException("sourceRegion.width <= subsamplingXOffset!");
            }
            if (sourceRegion.height <= this.subsamplingYOffset) {
                throw new IllegalArgumentException("sourceRegion.height <= subsamplingYOffset!");
            }
            this.sourceRegion = (Rectangle) sourceRegion.clone();
        } else {
            this.sourceRegion = null;
        }
    }

    /** The crop, or null. It is a copy. */
    public Rectangle getSourceRegion() {
        if (this.sourceRegion == null) {
            return null;
        }
        return (Rectangle) this.sourceRegion.clone();
    }

    /**
     * Sets the subsampling. See the class note.
     *
     * @param sourceXSubsampling every how many pixels in X; at least 1
     * @param subsamplingXOffset which one to start from; smaller than the step
     * @throws IllegalArgumentException if the steps are not positive, if the offsets are negative
     *     or not smaller than their step, or if the crop is not big enough
     */
    public void setSourceSubsampling(int sourceXSubsampling, int sourceYSubsampling,
                                     int subsamplingXOffset, int subsamplingYOffset) {
        if (sourceXSubsampling <= 0) {
            throw new IllegalArgumentException("sourceXSubsampling <= 0!");
        }
        if (sourceYSubsampling <= 0) {
            throw new IllegalArgumentException("sourceYSubsampling <= 0!");
        }
        if (subsamplingXOffset < 0 || subsamplingXOffset >= sourceXSubsampling) {
            throw new IllegalArgumentException("subsamplingXOffset out of range!");
        }
        if (subsamplingYOffset < 0 || subsamplingYOffset >= sourceYSubsampling) {
            throw new IllegalArgumentException("subsamplingYOffset out of range!");
        }
        if (this.sourceRegion != null) {
            if (subsamplingXOffset >= this.sourceRegion.width
                || subsamplingYOffset >= this.sourceRegion.height) {
                throw new IllegalArgumentException("region contains no pixels!");
            }
        }
        this.sourceXSubsampling = sourceXSubsampling;
        this.sourceYSubsampling = sourceYSubsampling;
        this.subsamplingXOffset = subsamplingXOffset;
        this.subsamplingYOffset = subsamplingYOffset;
    }

    /** Every how many pixels in X. */
    public int getSourceXSubsampling() {
        return this.sourceXSubsampling;
    }

    /** Every how many in Y. */
    public int getSourceYSubsampling() {
        return this.sourceYSubsampling;
    }

    /** Which one to start from in X. */
    public int getSubsamplingXOffset() {
        return this.subsamplingXOffset;
    }

    /** Same in Y. */
    public int getSubsamplingYOffset() {
        return this.subsamplingYOffset;
    }

    /**
     * Which source bands to use; null means all.
     *
     * <p>The array is copied, and checked for repeats: a band asked for twice means nothing and is
     * almost always an index error.
     *
     * <p>An empty array is accepted, even though it means nothing useful: it is what the JDK does.
     *
     * @throws IllegalArgumentException if it has negatives or repeats one
     */
    public void setSourceBands(int[] sourceBands) {
        if (sourceBands == null) {
            this.sourceBands = null;
            return;
        }
        int numBands = sourceBands.length;
        int i = 0;
        while (i < numBands) {
            int band = sourceBands[i];
            if (band < 0) {
                throw new IllegalArgumentException("sourceBands[" + i + "] < 0!");
            }
            int j = i + 1;
            while (j < numBands) {
                if (band == sourceBands[j]) {
                    throw new IllegalArgumentException("Duplicate band value!");
                }
                j = j + 1;
            }
            i = i + 1;
        }
        this.sourceBands = new int[numBands];
        System.arraycopy(sourceBands, 0, this.sourceBands, 0, numBands);
    }

    /** Which bands, or null. It is a copy. */
    public int[] getSourceBands() {
        if (this.sourceBands == null) {
            return null;
        }
        int[] copy = new int[this.sourceBands.length];
        System.arraycopy(this.sourceBands, 0, copy, 0, this.sourceBands.length);
        return copy;
    }

    /** What type the destination has to be; null leaves it up to the reader. */
    public void setDestinationType(ImageTypeSpecifier destinationType) {
        this.destinationType = destinationType;
    }

    /** What type, or null. */
    public ImageTypeSpecifier getDestinationType() {
        return this.destinationType;
    }

    /**
     * Where to place what was read in the destination. See the class note.
     *
     * <p>Negative coordinates are allowed: that is how the top or left part of what was read is
     * discarded.
     *
     * @throws IllegalArgumentException if it is null
     */
    public void setDestinationOffset(Point destinationOffset) {
        if (destinationOffset == null) {
            throw new IllegalArgumentException("destinationOffset == null!");
        }
        this.destinationOffset = (Point) destinationOffset.clone();
    }

    /** Where to place it. It is a copy. */
    public Point getDestinationOffset() {
        return (Point) this.destinationOffset.clone();
    }

    /** Who fills in this parameter; null uses the default one. */
    public void setController(IIOParamController controller) {
        this.controller = controller;
    }

    /** The one that is set. */
    public IIOParamController getController() {
        return this.controller;
    }

    /** The default one, or null. */
    public IIOParamController getDefaultController() {
        return this.defaultController;
    }

    /** Whether one is set. */
    public boolean hasController() {
        return getController() != null;
    }

    /**
     * Asks the controller to fill in this parameter.
     *
     * @return whether the user accepted
     * @throws IllegalStateException if there is no controller
     */
    public boolean activateController() {
        if (!hasController()) {
            throw new IllegalStateException("hasController() == false!");
        }
        return getController().activate(this);
    }
}
