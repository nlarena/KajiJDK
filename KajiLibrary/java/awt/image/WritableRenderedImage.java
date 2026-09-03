package java.awt.image;

import java.awt.Point;

/**
 * Una {@link RenderedImage} en la que además se puede escribir.
 *
 * <p>La escritura de un mosaico es **prestada y contada**: se pide con {@link #getWritableTile} y se
 * devuelve con {@link #releaseWritableTile}, y la imagen lleva la cuenta de cuántos préstamos hay
 * vivos sobre cada uno. Recién cuando la cuenta vuelve a cero el mosaico se considera terminado y se
 * les avisa a los {@link TileObserver}.
 *
 * <p>De ahí que soltar de más sea un error: significa que alguien devolvió un préstamo que no pidió,
 * y la cuenta ya no describe la realidad.
 */
public interface WritableRenderedImage extends RenderedImage {

    /** Suma un observador de mosaicos. */
    void addTileObserver(TileObserver to);

    /** Saca a ese observador. */
    void removeTileObserver(TileObserver to);

    /**
     * Toma un mosaico para escribir y suma uno a su cuenta de préstamos.
     *
     * <p>Hay que devolverlo con {@link #releaseWritableTile}.
     */
    WritableRaster getWritableTile(int tileX, int tileY);

    /**
     * Devuelve un mosaico y resta uno a su cuenta.
     *
     * <p>Soltar uno que no se había tomado es un error.
     */
    void releaseWritableTile(int tileX, int tileY);

    /** Si ese mosaico está tomado por alguien. */
    boolean isTileWritable(int tileX, int tileY);

    /** Los índices de los mosaicos tomados, o `null` si no hay ninguno. */
    Point[] getWritableTileIndices();

    /** Si hay algún mosaico tomado. */
    boolean hasTileWriters();

    /**
     * Escribe un ráster en la imagen.
     *
     * <p>Sólo se escribe la parte que caiga adentro; el resto se descarta.
     */
    void setData(Raster r);
}
