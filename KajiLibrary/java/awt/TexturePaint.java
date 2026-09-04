package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

/**
 * Rellena con una imagen repetida como baldosa.
 *
 * <p>El rectángulo de anclaje dice **dónde y de qué tamaño** va una copia de la imagen; a partir de
 * ahí se repite en las dos direcciones hasta cubrir lo que haga falta. La imagen se estira al
 * rectángulo, así que el mismo dibujo sirve para baldosas de cualquier tamaño.
 *
 * <p>Que el anclaje esté en coordenadas de usuario y no de la figura es lo que hace que dos figuras
 * distintas pintadas con la misma textura queden **alineadas entre sí**: el patrón pertenece al
 * plano, no a lo que se está rellenando.
 */
public class TexturePaint implements Paint {

    private final BufferedImage bufImg;
    private final double tx;
    private final double ty;
    private final double sx;
    private final double sy;

    /**
     * Con la imagen y el rectángulo donde va una copia.
     *
     * @throws NullPointerException si falta la imagen o el rectángulo
     */
    public TexturePaint(BufferedImage txtr, Rectangle2D anchor) {
        this.bufImg = txtr;
        this.tx = anchor.getX();
        this.ty = anchor.getY();
        this.sx = anchor.getWidth() / this.bufImg.getWidth();
        this.sy = anchor.getHeight() / this.bufImg.getHeight();
    }

    /** La imagen que se repite. */
    public BufferedImage getImage() {
        return this.bufImg;
    }

    /** Dónde va una copia de la imagen. */
    public Rectangle2D getAnchorRect() {
        return new Rectangle2D.Double(this.tx, this.ty, this.sx * this.bufImg.getWidth(),
                this.sy * this.bufImg.getHeight());
    }

    /**
     * `OPAQUE` si la imagen no tiene transparencia, `TRANSLUCENT` si la tiene.
     *
     * <p>Se pregunta al modelo de color de la imagen: una imagen sin canal alfa cubre lo de abajo, y
     * saberlo le permite al dibujado saltearse la composición.
     */
    public int getTransparency() {
        return this.bufImg.getColorModel().getTransparency();
    }

    /**
     * Arma la máquina que genera los píxeles.
     *
     * <p>Si la transformación no se puede invertir, la textura se degrada a un color plano
     * transparente: sin geometría no hay baldosa que ubicar, y pintar de un color inventado sería
     * peor que no pintar.
     */
    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds,
            Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
        try {
            return new Contexto(xform);
        } catch (NoninvertibleTransformException e) {
            return new Color(0, 0, 0, 0).createContext(cm, deviceBounds, userBounds, xform, hints);
        }
    }

    /** El contexto que ubica cada punto dentro de la baldosa. */
    private final class Contexto extends RasterPaintContext {

        Contexto(AffineTransform xform) throws NoninvertibleTransformException {
            super(xform);
        }

        int colorDe(double ux, double uy) {
            TexturePaint p = TexturePaint.this;
            // El resto de la division ubica el punto dentro de una baldosa. Se le suma el ancho y se
            // vuelve a tomar el resto porque el resto de Java conserva el signo, y sin eso las
            // coordenadas negativas caerian fuera de la imagen.
            double ax = (ux - p.tx) / p.sx;
            double ay = (uy - p.ty) / p.sy;
            int w = p.bufImg.getWidth();
            int h = p.bufImg.getHeight();
            int ix = (int) Math.floor(ax) % w;
            int iy = (int) Math.floor(ay) % h;
            if (ix < 0) {
                ix = ix + w;
            }
            if (iy < 0) {
                iy = iy + h;
            }
            return p.bufImg.getRGB(ix, iy);
        }
    }
}
