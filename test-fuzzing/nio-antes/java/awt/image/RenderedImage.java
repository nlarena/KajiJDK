package java.awt.image;

import java.awt.Rectangle;
import java.util.Vector;

/**
 * A read-only image, possibly **by tiles**.
 *
 * <p>It is the abstraction with which one can talk about an image bigger than memory. Instead of a
 * rectangle of pixels there is a grid of tiles, each one a {@link Raster}, and whoever uses it asks
 * for one at a time. A small image such as {@link BufferedImage} is the degenerate case: a single
 * tile that takes it all.
 *
 * <p>The grid does not have to be aligned with the origin of the image, and that is why
 * `getTileGridXOffset` and `getMinTileX` are there: tile (0,0) can start anywhere, and the tile
 * indices can even be negative.
 */
public interface RenderedImage {

    /** The images this one is computed from, or `null` if there are none. */
    Vector<RenderedImage> getSources();

    /**
     * A property of the image.
     *
     * @return the value, or `Image.UndefinedProperty` if it is not defined
     */
    Object getProperty(String name);

    /** The names of the properties, or `null` if there are none. */
    String[] getPropertyNames();

    /** The colour model, or `null` if the data cannot be read as colour. */
    ColorModel getColorModel();

    /** How the pixels are laid out. */
    SampleModel getSampleModel();

    /** Width, in pixels. */
    int getWidth();

    /** Height, in pixels. */
    int getHeight();

    /** X coordinate of the top left corner. */
    int getMinX();

    /** Y coordinate of the top left corner. */
    int getMinY();

    /** How many tiles there are across. */
    int getNumXTiles();

    /** How many tiles there are down. */
    int getNumYTiles();

    /** The smallest tile index across. */
    int getMinTileX();

    /** The smallest tile index down. */
    int getMinTileY();

    /** Width of a tile, in pixels. */
    int getTileWidth();

    /** Height of a tile, in pixels. */
    int getTileHeight();

    /** Where tile (0,0) starts with respect to the origin of the image. */
    int getTileGridXOffset();

    /** The same on the other axis. */
    int getTileGridYOffset();

    /** One tile, as a read-only raster. */
    Raster getTile(int tileX, int tileY);

    /** The whole image in one raster. */
    Raster getData();

    /** A region of the image in one raster. */
    Raster getData(Rectangle rect);

    /**
     * Copies the image into the given raster, or into a new one if it is `null`.
     *
     * <p>Unlike {@link #getData()}, this always copies: the result shares no data with the image.
     */
    WritableRaster copyData(WritableRaster raster);
}
