package java.awt.image;

import java.awt.Rectangle;
import java.util.Vector;

/**
 * Una imagen de sólo lectura, posiblemente **por mosaicos**.
 *
 * <p>Es la abstracción con la que se puede hablar de una imagen más grande que la memoria. En vez de
 * un rectángulo de píxeles hay una grilla de mosaicos, cada uno un {@link Raster}, y quien la use
 * pide de a uno. Una imagen chica como {@link BufferedImage} es el caso degenerado: un solo mosaico
 * que ocupa todo.
 *
 * <p>La grilla no tiene por qué estar alineada con el origen de la imagen, y por eso están
 * `getTileGridXOffset` y `getMinTileX`: el mosaico (0,0) puede empezar en cualquier lado, e incluso
 * los índices de mosaico pueden ser negativos.
 */
public interface RenderedImage {

    /** Las imágenes de las que ésta se calcula, o `null` si no hay. */
    Vector<RenderedImage> getSources();

    /**
     * Una propiedad de la imagen.
     *
     * @return el valor, o `Image.UndefinedProperty` si no está definida
     */
    Object getProperty(String name);

    /** Los nombres de las propiedades, o `null` si no hay ninguna. */
    String[] getPropertyNames();

    /** El modelo de color, o `null` si los datos no se pueden interpretar como color. */
    ColorModel getColorModel();

    /** Cómo están dispuestos los píxeles. */
    SampleModel getSampleModel();

    /** Ancho, en píxeles. */
    int getWidth();

    /** Alto, en píxeles. */
    int getHeight();

    /** Coordenada X del ángulo superior izquierdo. */
    int getMinX();

    /** Coordenada Y del ángulo superior izquierdo. */
    int getMinY();

    /** Cuántos mosaicos hay a lo ancho. */
    int getNumXTiles();

    /** Cuántos mosaicos hay a lo alto. */
    int getNumYTiles();

    /** El menor índice de mosaico a lo ancho. */
    int getMinTileX();

    /** El menor índice de mosaico a lo alto. */
    int getMinTileY();

    /** Ancho de un mosaico, en píxeles. */
    int getTileWidth();

    /** Alto de un mosaico, en píxeles. */
    int getTileHeight();

    /** Dónde empieza el mosaico (0,0) respecto del origen de la imagen. */
    int getTileGridXOffset();

    /** Lo mismo en el otro eje. */
    int getTileGridYOffset();

    /** Un mosaico, como ráster de sólo lectura. */
    Raster getTile(int tileX, int tileY);

    /** La imagen entera en un ráster. */
    Raster getData();

    /** Una región de la imagen en un ráster. */
    Raster getData(Rectangle rect);

    /**
     * Copia la imagen en el ráster dado, o en uno nuevo si es `null`.
     *
     * <p>A diferencia de {@link #getData()}, esto siempre copia: el resultado no comparte datos con
     * la imagen.
     */
    WritableRaster copyData(WritableRaster raster);
}
