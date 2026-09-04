package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;

/**
 * Un degradé lineal entre **dos** colores.
 *
 * <p>Se define por dos puntos y dos colores: en el primer punto el color es el primero, en el
 * segundo el segundo, y en el medio se interpola sobre la recta que los une. Perpendicularmente a
 * esa recta el color no cambia, que es lo que hace que un degradé se vea como bandas paralelas.
 *
 * <p>Fuera del segmento hay dos comportamientos. Sin ciclo, el color se estira: todo lo que esté más
 * allá del segundo punto queda del segundo color. Con ciclo, el degradé rebota entre los dos puntos,
 * y como rebota **en espejo** no queda costura donde se repite.
 */
public class GradientPaint implements Paint {

    private final Point2D.Float p1;
    private final Point2D.Float p2;
    private final Color color1;
    private final Color color2;
    private final boolean cyclic;

    /**
     * Con los dos puntos dados por coordenadas, sin ciclo.
     *
     * @throws NullPointerException si falta alguno de los dos colores
     */
    public GradientPaint(float x1, float y1, Color color1, float x2, float y2, Color color2) {
        this(x1, y1, color1, x2, y2, color2, false);
    }

    /**
     * Con los dos puntos dados como objetos, sin ciclo.
     *
     * @throws NullPointerException si falta alguno de los cuatro
     */
    public GradientPaint(Point2D pt1, Color color1, Point2D pt2, Color color2) {
        this(pt1, color1, pt2, color2, false);
    }

    /**
     * Con los dos puntos dados por coordenadas.
     *
     * @throws NullPointerException si falta alguno de los dos colores
     */
    public GradientPaint(float x1, float y1, Color color1, float x2, float y2, Color color2,
            boolean cyclic) {
        if (color1 == null || color2 == null) {
            throw new NullPointerException("Colors cannot be null");
        }
        this.p1 = new Point2D.Float(x1, y1);
        this.p2 = new Point2D.Float(x2, y2);
        this.color1 = color1;
        this.color2 = color2;
        this.cyclic = cyclic;
    }

    /**
     * Con los dos puntos dados como objetos.
     *
     * @throws NullPointerException si falta alguno de los cuatro
     */
    public GradientPaint(Point2D pt1, Color color1, Point2D pt2, Color color2, boolean cyclic) {
        if (color1 == null || color2 == null || pt1 == null || pt2 == null) {
            throw new NullPointerException("Colors and points should be non-null");
        }
        this.p1 = new Point2D.Float((float) pt1.getX(), (float) pt1.getY());
        this.p2 = new Point2D.Float((float) pt2.getX(), (float) pt2.getY());
        this.color1 = color1;
        this.color2 = color2;
        this.cyclic = cyclic;
    }

    /** El punto donde el color es {@link #getColor1}. */
    public Point2D getPoint1() {
        return new Point2D.Float(this.p1.x, this.p1.y);
    }

    /** El color del primer punto. */
    public Color getColor1() {
        return this.color1;
    }

    /** El punto donde el color es {@link #getColor2}. */
    public Point2D getPoint2() {
        return new Point2D.Float(this.p2.x, this.p2.y);
    }

    /** El color del segundo punto. */
    public Color getColor2() {
        return this.color2;
    }

    /** Si el degradé rebota entre los dos puntos en vez de estirarse. */
    public boolean isCyclic() {
        return this.cyclic;
    }

    /** `OPAQUE` si los dos colores son opacos, `TRANSLUCENT` si alguno no. */
    public int getTransparency() {
        if (this.color1.getAlpha() == 0xFF && this.color2.getAlpha() == 0xFF) {
            return Transparency.OPAQUE;
        }
        return Transparency.TRANSLUCENT;
    }

    /**
     * Arma la máquina que genera los píxeles.
     *
     * <p>Si la transformación no se puede invertir, el degradé se degrada a un color plano: el
     * primero. Es lo único que se puede hacer sin poder llevar un píxel de vuelta a coordenadas de
     * usuario, y es preferible a tirar en medio de un dibujado.
     */
    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds,
            Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
        try {
            return new Contexto(xform);
        } catch (NoninvertibleTransformException e) {
            return this.color1.createContext(cm, deviceBounds, userBounds, xform, hints);
        }
    }

    /** El contexto que calcula el degradé punto por punto. */
    private final class Contexto extends RasterPaintContext {

        private final double dx;
        private final double dy;
        private final double largo2;

        Contexto(AffineTransform xform) throws NoninvertibleTransformException {
            super(xform);
            this.dx = GradientPaint.this.p2.x - GradientPaint.this.p1.x;
            this.dy = GradientPaint.this.p2.y - GradientPaint.this.p1.y;
            this.largo2 = this.dx * this.dx + this.dy * this.dy;
        }

        int colorDe(double ux, double uy) {
            if (this.largo2 == 0.0) {
                return GradientPaint.this.color2.getRGB();
            }
            // La proyeccion escalar del punto sobre el segmento, normalizada: cuanto del camino de
            // p1 a p2 llevamos recorrido. Lo perpendicular al segmento no interviene, y por eso el
            // color no cambia en esa direccion.
            double t = ((ux - GradientPaint.this.p1.x) * this.dx
                    + (uy - GradientPaint.this.p1.y) * this.dy) / this.largo2;
            if (GradientPaint.this.cyclic) {
                double doble = t - Math.floor(t / 2) * 2;
                t = doble > 1.0 ? 2.0 - doble : doble;
            } else if (t < 0.0) {
                t = 0.0;
            } else if (t > 1.0) {
                t = 1.0;
            }
            Color a = GradientPaint.this.color1;
            Color b = GradientPaint.this.color2;
            int al = redondear(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);
            int r = redondear(a.getRed() + (b.getRed() - a.getRed()) * t);
            int g = redondear(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
            int bl = redondear(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
            return (al << 24) | (r << 16) | (g << 8) | bl;
        }
    }

    /** Un valor llevado a un byte. */
    private static int redondear(double v) {
        int i = (int) (v + 0.5);
        if (i < 0) {
            return 0;
        }
        if (i > 255) {
            return 255;
        }
        return i;
    }
}
