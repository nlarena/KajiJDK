package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;

/**
 * Un degradé que sale **desde un punto** en círculos concéntricos.
 *
 * <p>La primera parada está en el foco y la última sobre la circunferencia; en el medio, el color
 * depende de qué fracción del camino del foco al borde se recorrió.
 *
 * <p>El **foco** es lo que le da el aspecto de brillo. Con el foco en el centro, los anillos son
 * concéntricos y la cosa se ve plana; corriéndolo, los anillos se apiñan de un lado y se separan del
 * otro, y aparece la ilusión de una esfera iluminada desde ese punto.
 *
 * <p>Un foco sobre la circunferencia o fuera de ella no tiene solución —la fracción se iría a
 * infinito— así que se lo empuja hasta un 99% del radio. Es lo mismo que hace el JDK, y es preferible
 * a tirar por un punto que el que dibuja no eligió a propósito.
 */
public final class RadialGradientPaint extends MultipleGradientPaint {

    private final Point2D center;
    private final Point2D focus;
    private final float radius;

    /**
     * Con centro y radio, el foco en el centro, sin ciclo y en sRGB.
     *
     * @throws IllegalArgumentException si el radio no es positivo o las paradas no son válidas
     */
    public RadialGradientPaint(float cx, float cy, float radius, float[] fractions,
            Color[] colors) {
        this(new Point2D.Float(cx, cy), radius, fractions, colors, CycleMethod.NO_CYCLE);
    }

    /**
     * Lo mismo, con el centro como objeto.
     *
     * @throws IllegalArgumentException si el radio no es positivo o las paradas no son válidas
     */
    public RadialGradientPaint(Point2D center, float radius, float[] fractions, Color[] colors) {
        this(center, radius, fractions, colors, CycleMethod.NO_CYCLE);
    }

    /**
     * Con el ciclo dado.
     *
     * @throws IllegalArgumentException si el radio no es positivo o las paradas no son válidas
     */
    public RadialGradientPaint(float cx, float cy, float radius, float[] fractions,
            Color[] colors, CycleMethod cycleMethod) {
        this(new Point2D.Float(cx, cy), radius, fractions, colors, cycleMethod);
    }

    /**
     * Con el ciclo dado y el centro como objeto.
     *
     * @throws IllegalArgumentException si el radio no es positivo o las paradas no son válidas
     */
    public RadialGradientPaint(Point2D center, float radius, float[] fractions, Color[] colors,
            CycleMethod cycleMethod) {
        this(center, radius, center, fractions, colors, cycleMethod, ColorSpaceType.SRGB,
                new AffineTransform());
    }

    /**
     * Con el foco separado del centro.
     *
     * @throws IllegalArgumentException si el radio no es positivo o las paradas no son válidas
     */
    public RadialGradientPaint(float cx, float cy, float radius, float fx, float fy,
            float[] fractions, Color[] colors, CycleMethod cycleMethod) {
        this(new Point2D.Float(cx, cy), radius, new Point2D.Float(fx, fy), fractions, colors,
                cycleMethod);
    }

    /**
     * Con el foco separado del centro, ambos como objetos.
     *
     * @throws IllegalArgumentException si el radio no es positivo o las paradas no son válidas
     */
    public RadialGradientPaint(Point2D center, float radius, Point2D focus, float[] fractions,
            Color[] colors, CycleMethod cycleMethod) {
        this(center, radius, focus, fractions, colors, cycleMethod, ColorSpaceType.SRGB,
                new AffineTransform());
    }

    /**
     * A partir del rectángulo que encierra al círculo.
     *
     * <p>Con un rectángulo que no sea cuadrado, el degradé sale **elíptico**: la transformación que
     * lleva el cuadrado al rectángulo se guarda como transformación propia del degradé.
     *
     * @throws IllegalArgumentException si el rectángulo es vacío o las paradas no son válidas
     */
    public RadialGradientPaint(Rectangle2D gradientBounds, float[] fractions, Color[] colors,
            CycleMethod cycleMethod) {
        this(new Point2D.Double(gradientBounds.getCenterX(), gradientBounds.getCenterY()),
                1.0f,
                new Point2D.Double(gradientBounds.getCenterX(), gradientBounds.getCenterY()),
                fractions, colors, cycleMethod, ColorSpaceType.SRGB,
                transformacionDe(gradientBounds));
    }

    /**
     * La transformación que lleva el círculo unitario centrado en el rectángulo al rectángulo.
     *
     * @throws IllegalArgumentException si el rectángulo es vacío
     */
    private static AffineTransform transformacionDe(Rectangle2D r) {
        if (r == null) {
            throw new NullPointerException("Gradient bounds cannot be null");
        }
        if (r.getWidth() <= 0 || r.getHeight() <= 0) {
            throw new IllegalArgumentException("Gradient bounds must be non-empty");
        }
        AffineTransform t = AffineTransform.getTranslateInstance(r.getX(), r.getY());
        t.scale(r.getWidth(), r.getHeight());
        t.translate(-0.5 + 0.5, -0.5 + 0.5);
        AffineTransform g = AffineTransform.getTranslateInstance(r.getCenterX(), r.getCenterY());
        g.scale(r.getWidth() / 2, r.getHeight() / 2);
        g.translate(-r.getCenterX(), -r.getCenterY());
        return g;
    }

    /**
     * El constructor general.
     *
     * @throws NullPointerException si falta cualquiera de los argumentos
     * @throws IllegalArgumentException si el radio no es positivo, si hay menos de dos paradas o si
     *     las fracciones no crecen
     */
    public RadialGradientPaint(Point2D center, float radius, Point2D focus, float[] fractions,
            Color[] colors, CycleMethod cycleMethod, ColorSpaceType colorSpace,
            AffineTransform gradientTransform) {
        super(fractions, colors, cycleMethod, colorSpace, gradientTransform);
        if (center == null) {
            throw new NullPointerException("Center point must be non-null");
        }
        if (focus == null) {
            throw new NullPointerException("Focus point must be non-null");
        }
        if (radius <= 0) {
            throw new IllegalArgumentException("Radius must be greater than zero");
        }
        this.center = new Point2D.Double(center.getX(), center.getY());
        this.radius = radius;
        this.focus = ajustarFoco(center, focus, radius);
    }

    /** El foco, empujado adentro del círculo si hacía falta. */
    private static Point2D ajustarFoco(Point2D center, Point2D focus, float radius) {
        double dx = focus.getX() - center.getX();
        double dy = focus.getY() - center.getY();
        double d = Math.sqrt(dx * dx + dy * dy);
        double maximo = radius * 0.99;
        if (d <= maximo) {
            return new Point2D.Double(focus.getX(), focus.getY());
        }
        double escala = maximo / d;
        return new Point2D.Double(center.getX() + dx * escala, center.getY() + dy * escala);
    }

    /** El centro del círculo. */
    public Point2D getCenterPoint() {
        return new Point2D.Double(this.center.getX(), this.center.getY());
    }

    /** Desde dónde sale el degradé. */
    public Point2D getFocusPoint() {
        return new Point2D.Double(this.focus.getX(), this.focus.getY());
    }

    /** El radio del círculo. */
    public float getRadius() {
        return this.radius;
    }

    /**
     * Arma la máquina que genera los píxeles.
     *
     * <p>Si la transformación combinada no se puede invertir, el degradé se degrada a la primera
     * parada.
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

        Contexto(AffineTransform xform) throws NoninvertibleTransformException {
            super(xform);
        }

        int colorDe(double ux, double uy) {
            RadialGradientPaint p = RadialGradientPaint.this;
            double dx = ux - p.focus.getX();
            double dy = uy - p.focus.getY();
            double a = dx * dx + dy * dy;
            if (a == 0.0) {
                return p.colorEn(0.0f);
            }
            // Hasta donde llega el rayo foco->punto antes de salir del circulo: se resuelve la
            // cuadratica |f + s*d - c|^2 = r^2 y se toma la raiz positiva. La fraccion es 1/s,
            // porque el punto esta a distancia 1 del foco en unidades de ese rayo.
            double fx = p.focus.getX() - p.center.getX();
            double fy = p.focus.getY() - p.center.getY();
            double b = 2 * (fx * dx + fy * dy);
            double c = fx * fx + fy * fy - p.radius * (double) p.radius;
            double disc = b * b - 4 * a * c;
            if (disc < 0) {
                return p.colorEn(1.0f);
            }
            double s = (-b + Math.sqrt(disc)) / (2 * a);
            if (s <= 0) {
                return p.colorEn(1.0f);
            }
            return p.colorEn((float) (1.0 / s));
        }
    }
}
