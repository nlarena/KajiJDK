package java.awt.image;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Una operación de un {@link Raster} a otro.
 *
 * <p>Trabaja sobre píxeles **sin interpretar**: no sabe qué color son, sólo qué números tienen. Es
 * la diferencia con {@link BufferedImageOp}, que sí ve el color y por eso puede convertir de un
 * espacio a otro. Las mismas clases suelen implementar las dos interfaces, y hacen cuentas distintas
 * según por cuál se las llame.
 *
 * <p>{@link #getPoint2D} existe porque una operación puede mover los píxeles de lugar: en una
 * transformación afín el punto de destino no es el mismo que el de origen, y hay que poder
 * preguntarlo sin aplicar la operación entera.
 */
public interface RasterOp {

    /**
     * Aplica la operación.
     *
     * @param dest el destino, o `null` para que se cree uno
     * @return el destino
     */
    WritableRaster filter(Raster src, WritableRaster dest);

    /** El rectángulo que va a ocupar el resultado. */
    Rectangle2D getBounds2D(Raster src);

    /**
     * Un destino vacío del tamaño y formato que corresponde.
     *
     * @throws IllegalArgumentException si el origen no le sirve a esta operación
     */
    WritableRaster createCompatibleDestRaster(Raster src);

    /**
     * A dónde va a parar ese punto.
     *
     * @param dstPt dónde escribir el resultado, o `null` para que se cree uno
     */
    Point2D getPoint2D(Point2D srcPt, Point2D dstPt);

    /** Las pistas de dibujo, o `null` si no hay. */
    RenderingHints getRenderingHints();
}
