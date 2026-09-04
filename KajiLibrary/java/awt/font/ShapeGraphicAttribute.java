package java.awt.font;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * Una figura metida en un renglón de texto como si fuera un carácter.
 *
 * <p>Las medidas salen de la figura misma: lo que la figura tenga por encima del origen es el
 * ascenso, lo que tenga por debajo el descenso, y hasta dónde llegue a la derecha el avance. Lo que
 * quede a la **izquierda** del origen no cuenta como avance —los tres se recortan a cero— así que
 * una figura que empiece antes del origen se mete debajo de lo anterior, igual que una letra
 * cursiva.
 *
 * <p>Se puede pedir rellena o trazada. Trazada ocupa un píxel más de ancho y de alto, porque el
 * trazo se dibuja **sobre** el borde y sobresale medio píxel de cada lado.
 */
public final class ShapeGraphicAttribute extends GraphicAttribute {

    /** Que la figura se dibuje trazada. */
    public static final boolean STROKE = true;

    /** Que la figura se dibuje rellena. */
    public static final boolean FILL = false;

    private final Shape shape;
    private final boolean stroke;
    private final Rectangle2D shapeBounds;

    /**
     * Con la figura, la alineación y si va trazada o rellena.
     *
     * @throws IllegalArgumentException si la alineación no es una de las cinco
     * @throws NullPointerException si la figura es `null`
     */
    public ShapeGraphicAttribute(Shape shape, int alignment, boolean stroke) {
        super(alignment);
        this.shape = shape;
        this.stroke = stroke;
        this.shapeBounds = this.shape.getBounds2D();
    }

    /** Lo que la figura sube por encima del origen. */
    public float getAscent() {
        return (float) Math.max(0, -this.shapeBounds.getMinY());
    }

    /** Lo que la figura baja por debajo del origen. */
    public float getDescent() {
        return (float) Math.max(0, this.shapeBounds.getMaxY());
    }

    /** Hasta dónde llega la figura a la derecha del origen. */
    public float getAdvance() {
        return (float) Math.max(0, this.shapeBounds.getMaxX());
    }

    /**
     * Dibuja la figura con su origen en `(x, y)`.
     *
     * <p>El desplazamiento se hace y se deshace sobre el contexto que se recibe, y se deshace en un
     * `finally`: si el dibujo tira, el contexto queda como estaba y no arrastra el corrimiento al
     * resto del renglón.
     */
    public void draw(Graphics2D graphics, float x, float y) {
        graphics.translate((int) x, (int) y);
        try {
            if (this.stroke == STROKE) {
                Stroke oldStroke = graphics.getStroke();
                graphics.setStroke(new BasicStroke());
                graphics.draw(this.shape);
                graphics.setStroke(oldStroke);
            } else {
                graphics.fill(this.shape);
            }
        } finally {
            graphics.translate(-(int) x, -(int) y);
        }
    }

    /**
     * Dónde cae la tinta.
     *
     * <p>Trazada ocupa un píxel más de cada lado, porque el trazo se dibuja sobre el borde.
     */
    public Rectangle2D getBounds() {
        Rectangle2D.Float bounds = new Rectangle2D.Float();
        bounds.setRect(this.shapeBounds);
        if (this.stroke == STROKE) {
            bounds.width = bounds.width + 1;
            bounds.height = bounds.height + 1;
        }
        return bounds;
    }

    /**
     * El contorno de la figura, transformado.
     *
     * <p>Se redefine porque acá sí hay un contorno de verdad: la figura misma, y no su caja.
     */
    public Shape getOutline(AffineTransform tx) {
        if (tx == null) {
            return this.shape;
        }
        return tx.createTransformedShape(this.shape);
    }

    public int hashCode() {
        return this.shape.hashCode();
    }

    /** Igualdad por figura, alineación y modo de dibujo. */
    public boolean equals(Object rhs) {
        if (rhs instanceof ShapeGraphicAttribute) {
            return this.equals((ShapeGraphicAttribute) rhs);
        }
        return false;
    }

    /** Lo mismo, con el tipo ya conocido. */
    public boolean equals(ShapeGraphicAttribute rhs) {
        if (rhs == null) {
            return false;
        }
        if (this == rhs) {
            return true;
        }
        if (this.stroke != rhs.stroke) {
            return false;
        }
        if (this.getAlignment() != rhs.getAlignment()) {
            return false;
        }
        return this.shape.equals(rhs.shape);
    }
}
