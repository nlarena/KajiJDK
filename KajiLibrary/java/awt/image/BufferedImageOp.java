package java.awt.image;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Una operación de una {@link BufferedImage} a otra.
 *
 * <p>A diferencia de {@link RasterOp}, ve el **color**: sabe en qué espacio están los píxeles de
 * origen y de destino, y puede convertir entre ellos.
 */
public interface BufferedImageOp {

    /**
     * Aplica la operación.
     *
     * @param dest el destino, o `null` para que se cree uno
     * @return el destino
     */
    BufferedImage filter(BufferedImage src, BufferedImage dest);

    /** El rectángulo que va a ocupar el resultado. */
    Rectangle2D getBounds2D(BufferedImage src);

    /**
     * Un destino vacío del tamaño y formato que corresponde.
     *
     * @param destCM el modelo de color del destino, o `null` para usar el del origen
     */
    BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM);

    /**
     * A dónde va a parar ese punto.
     *
     * @param dstPt dónde escribir el resultado, o `null` para que se cree uno
     */
    Point2D getPoint2D(Point2D srcPt, Point2D dstPt);

    /** Las pistas de dibujo, o `null` si no hay. */
    RenderingHints getRenderingHints();
}
