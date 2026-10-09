package java.awt.image;

/**
 * Whoever wants to hear that a part of a tiled image has been taken for writing or released.
 *
 * <p>It serves for knowing when a tile has finished being modified and can be dumped to disk or to
 * the screen without catching it half written.
 */
public interface TileObserver {

    /**
     * Tells that a tile was taken for writing, or released.
     *
     * @param willBeWritable `true` if it has just been taken, `false` if it was released
     */
    void tileUpdate(WritableRenderedImage source, int tileX, int tileY, boolean willBeWritable);
}
