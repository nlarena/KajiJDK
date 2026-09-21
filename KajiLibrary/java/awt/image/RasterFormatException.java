package java.awt.image;

/**
 * A {@link Raster} with invalid parameters: a rectangle that does not fall inside, bands that do
 * not exist, a negative size.
 *
 * <p>It is a runtime exception and unchecked, and it makes sense that it is: it almost always comes
 * from a mistaken coordinate calculation in the caller, not from data that arrived from outside.
 */
public class RasterFormatException extends RuntimeException {

    private static final long serialVersionUID = 96598996116164315L;

    /** With that message. */
    public RasterFormatException(String s) {
        super(s);
    }
}
