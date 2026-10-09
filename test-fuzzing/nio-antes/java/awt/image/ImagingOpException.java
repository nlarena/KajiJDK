package java.awt.image;

/**
 * An operation over an image could not be applied.
 *
 * <p>The distinction from {@link RasterFormatException} is about **who** is wrong: there the
 * parameters of the raster, here the operation itself -- a filter that does not know how to handle
 * that kind of image, a transformation that cannot be inverted.
 */
public class ImagingOpException extends RuntimeException {

    private static final long serialVersionUID = 8026288481846276658L;

    /** With that message. */
    public ImagingOpException(String s) {
        super(s);
    }
}
