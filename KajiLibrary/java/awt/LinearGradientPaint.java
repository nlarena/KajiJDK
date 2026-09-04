package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;

/**
 * Un degradé lineal de **varias paradas**.
 *
 * <p>Es {@link GradientPaint} con más de dos colores y con las tres opciones de ciclo en vez de dos.
 * La geometría es la misma: el color depende de cuánto se avanzó a lo largo del segmento, y no
 * cambia en la dirección perpendicular.
 */
public final class LinearGradientPaint extends MultipleGradientPaint {

    private final Point2D start;
    private final Point2D end;

    /**
     * Con los extremos por coordenadas, sin ciclo y en sRGB.
     *
     * @throws IllegalArgumentException si los dos extremos coinciden, si hay menos de dos paradas o
     *     si las fracciones no crecen
     * @throws NullPointerException si falta alguno de los arreglos
     */
    public LinearGradientPaint(float startX, float startY, float endX, float endY,
            float[] fractions, Color[] colors) {
        this(new Point2D.Float(startX, startY), new Point2D.Float(endX, endY), fractions, colors,
                CycleMethod.NO_CYCLE);
    }

    /**
     * Con los extremos por coordenadas y el ciclo dado.
     *
     * @throws IllegalArgumentException si los dos extremos coinciden o las paradas no son válidas
     */
    public LinearGradientPaint(float startX, float startY, float endX, float endY,
            float[] fractions, Color[] colors, CycleMethod cycleMethod) {
        this(new Point2D.Float(startX, startY), new Point2D.Float(endX, endY), fractions, colors,
                cycleMethod);
    }

    /**
     * Con los extremos como objetos, sin ciclo y en sRGB.
     *
     * @throws IllegalArgumentException si los dos extremos coinciden o las paradas no son válidas
     */
    public LinearGradientPaint(Point2D start, Point2D end, float[] fractions, Color[] colors) {
        this(start, end, fractions, colors, CycleMethod.NO_CYCLE);
    }

    /**
     * Con los extremos como objetos y el ciclo dado.
     *
     * @throws IllegalArgumentException si los dos extremos coinciden o las paradas no son válidas
     */
    public LinearGradientPaint(Point2D start, Point2D end, float[] fractions, Color[] colors,
            CycleMethod cycleMethod) {
        this(start, end, fractions, colors, cycleMethod, ColorSpaceType.SRGB,
                new AffineTransform());
    }

    /**
     * El constructor general.
     *
     * @throws NullPointerException si falta cualquiera de los argumentos
     * @throws IllegalArgumentException si los dos extremos coinciden, si hay menos de dos paradas o
     *     si las fracciones no crecen
     */
    public LinearGradientPaint(Point2D start, Point2D end, float[] fractions, Color[] colors,
            CycleMethod cycleMethod, ColorSpaceType colorSpace,
            AffineTransform gradientTransform) {
        super(fractions, colors, cycleMethod, colorSpace, gradientTransform);
        if (start == null || end == null) {
            throw new NullPointerException("Start and end points must be non-null");
        }
        if (start.equals(end)) {
            throw new IllegalArgumentException("Start point cannot equal endpoint");
        }
        this.start = new Point2D.Double(start.getX(), start.getY());
        this.end = new Point2D.Double(end.getX(), end.getY());
    }

    /** Donde arranca el degradé. */
    public Point2D getStartPoint() {
        return new Point2D.Double(this.start.getX(), this.start.getY());
    }

    /** Donde termina. */
    public Point2D getEndPoint() {
        return new Point2D.Double(this.end.getX(), this.end.getY());
    }

    /**
     * Arma la máquina que genera los píxeles.
     *
     * <p>Si la transformación combinada no se puede invertir, el degradé se degrada a la primera
     * parada, que es lo único que queda cuando no hay geometría.
     */
    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds,
            Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
        AffineTransform total = new AffineTransform(xform);
        total.concatenate(this.gradientTransform);
        try {
            return new Contexto(total);
        } catch (NoninvertibleTransformException e) {
            return this.colors[0].createContext(cm, deviceBounds, userBounds, xform, hints);
        }
    }

    /** El contexto que calcula el degradé punto por punto. */
    private final class Contexto extends RasterPaintContext {

        private final double dx;
        private final double dy;
        private final double largo2;

        Contexto(AffineTransform xform) throws NoninvertibleTransformException {
            super(xform);
            this.dx = LinearGradientPaint.this.end.getX() - LinearGradientPaint.this.start.getX();
            this.dy = LinearGradientPaint.this.end.getY() - LinearGradientPaint.this.start.getY();
            this.largo2 = this.dx * this.dx + this.dy * this.dy;
        }

        int colorDe(double ux, double uy) {
            double t = ((ux - LinearGradientPaint.this.start.getX()) * this.dx
                    + (uy - LinearGradientPaint.this.start.getY()) * this.dy) / this.largo2;
            return LinearGradientPaint.this.colorEn((float) t);
        }
    }
}
