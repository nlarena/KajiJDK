package java.awt.image;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An image of several resolutions given as a fixed list of versions.
 *
 * <p>It is the direct implementation: the already made versions are passed to it and it chooses
 * among them. The choice is the smallest one that is **enough** for the size asked for, and only if
 * none is enough is the largest used. Enlarging an image shows much more than shrinking it, so when
 * in doubt the one with some to spare is preferable.
 *
 * <p>The versions have to come ordered from smallest to largest; it is not checked, because
 * checking it would force asking all of them for the width and that can set off the loading of
 * images that are never going to be used.
 */
public class BaseMultiResolutionImage extends AbstractMultiResolutionImage {

    private final int baseImageIndex;
    private final Image[] resolutionVariants;

    /**
     * With the first version as the base one.
     *
     * @throws IllegalArgumentException if no version is passed or one of them is `null`
     */
    public BaseMultiResolutionImage(Image... resolutionVariants) {
        this(0, resolutionVariants);
    }

    /**
     * With the base version given by its position.
     *
     * @throws IllegalArgumentException if no version is passed, if one of them is `null`, or if the
     *     index of the base one does not exist
     */
    public BaseMultiResolutionImage(int baseImageIndex, Image... resolutionVariants) {
        if (resolutionVariants == null || resolutionVariants.length == 0) {
            throw new IllegalArgumentException("Null or empty resolution variants array");
        }
        for (int i = 0; i < resolutionVariants.length; i++) {
            if (resolutionVariants[i] == null) {
                throw new IllegalArgumentException("Null resolution variant");
            }
        }
        if (baseImageIndex < 0 || baseImageIndex >= resolutionVariants.length) {
            throw new IllegalArgumentException("Base image index is out of range");
        }
        this.baseImageIndex = baseImageIndex;
        this.resolutionVariants = Arrays.copyOf(resolutionVariants, resolutionVariants.length);
    }

    /** The version that defines the logical size. */
    protected Image getBaseImage() {
        return this.resolutionVariants[this.baseImageIndex];
    }

    /**
     * The smallest version that is enough for that size.
     *
     * <p>If none is enough the largest is returned, which is the best there is.
     *
     * @throws IllegalArgumentException if either of the two measures is not positive
     */
    public Image getResolutionVariant(double destImageWidth, double destImageHeight) {
        if (destImageWidth <= 0 || destImageHeight <= 0) {
            throw new IllegalArgumentException("Width and height must be > 0");
        }
        for (int i = 0; i < this.resolutionVariants.length; i++) {
            Image v = this.resolutionVariants[i];
            if (v.getWidth(null) >= destImageWidth && v.getHeight(null) >= destImageHeight) {
                return v;
            }
        }
        return this.resolutionVariants[this.resolutionVariants.length - 1];
    }

    /** Every version, from smallest to largest. */
    public List<Image> getResolutionVariants() {
        List<Image> out = new ArrayList<Image>();
        for (int i = 0; i < this.resolutionVariants.length; i++) {
            out.add(this.resolutionVariants[i]);
        }
        return java.util.Collections.unmodifiableList(out);
    }
}
