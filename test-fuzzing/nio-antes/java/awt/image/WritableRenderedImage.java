package java.awt.image;

import java.awt.Point;

/**
 * A {@link RenderedImage} that can also be written into.
 *
 * <p>Writing a tile is **borrowed and counted**: it is asked for with {@link #getWritableTile} and
 * given back with {@link #releaseWritableTile}, and the image keeps count of how many loans are
 * live over each one. Only when the count returns to zero is the tile considered finished and the
 * {@link TileObserver}s told.
 *
 * <p>Hence releasing one too many is an error: it means somebody gave back a loan they did not ask
 * for, and the count no longer describes reality.
 */
public interface WritableRenderedImage extends RenderedImage {

    /** Adds a tile observer. */
    void addTileObserver(TileObserver to);

    /** Removes that observer. */
    void removeTileObserver(TileObserver to);

    /**
     * Takes a tile for writing and adds one to its loan count.
     *
     * <p>It has to be given back with {@link #releaseWritableTile}.
     */
    WritableRaster getWritableTile(int tileX, int tileY);

    /**
     * Gives a tile back and subtracts one from its count.
     *
     * <p>Releasing one that had not been taken is an error.
     */
    void releaseWritableTile(int tileX, int tileY);

    /** Whether that tile is taken by somebody. */
    boolean isTileWritable(int tileX, int tileY);

    /** The indices of the taken tiles, or `null` if there are none. */
    Point[] getWritableTileIndices();

    /** Whether there is any tile taken. */
    boolean hasTileWriters();

    /**
     * Writes a raster into the image.
     *
     * <p>Only the part that falls inside is written; the rest is discarded.
     */
    void setData(Raster r);
}
