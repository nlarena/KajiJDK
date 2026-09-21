package java.awt.image;

import java.awt.Image;
import java.util.List;

/**
 * An image that exists in **several resolutions** and chooses which one to use.
 *
 * <p>It is what makes an icon look sharp on a high density screen: the same logical image keeps a
 * version of 16 pixels and another of 32, and whoever draws it asks for the one that serves for the
 * size it is going to be shown at.
 */
public interface MultiResolutionImage {

    /**
     * The version that best serves for drawing at that size.
     *
     * @throws IllegalArgumentException if either of the two measures is not positive
     */
    Image getResolutionVariant(double destImageWidth, double destImageHeight);

    /** Every version, from smallest to largest. */
    List<Image> getResolutionVariants();
}
