package javax.swing.border;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Rectangle2D$Float;

/**
 * Un borde dibujado con un {@link BasicStroke}, o sea con toda la maquinaria de trazos de Java2D.
 *
 * <h2>Que puede que los otros no</h2>
 *
 * <p>Los demas bordes de este paquete dibujan lineas de un pixel con {@code drawLine}. Este delega
 * en Java2D, asi que hereda gratis lo que un {@code BasicStroke} sabe hacer: guiones, puntas
 * redondeadas, uniones biseladas, grosores fraccionarios. Es el unico borde <em>configurable</em>
 * del paquete en ese sentido — los otros tienen la forma que tienen.
 *
 * <p>El {@link Paint} es opcional, y cuando falta se usa el color del componente. Eso permite que un
 * mismo borde punteado siga el color de texto de cada componente que lo lleve.
 *
 * <h2>La cuenta de los insets</h2>
 *
 * <p>Un trazo de grosor {@code n} se dibuja <strong>centrado</strong> sobre la linea: la mitad para
 * afuera y la mitad para adentro. Por eso los insets son el grosor redondeado hacia arriba y no el
 * grosor a secas — con menos, la mitad interna del trazo taparia el contenido.
 */
public class StrokeBorder extends AbstractBorder {

    private final BasicStroke stroke;
    private final Paint paint;

    /**
     * Con el trazo dado, pintado con el color del componente.
     *
     * @throws NullPointerException si {@code stroke} es {@code null}
     */
    public StrokeBorder(BasicStroke stroke) {
        this(stroke, null);
    }

    /**
     * Con el trazo y la pintura dados.
     *
     * @throws NullPointerException si {@code stroke} es {@code null}
     */
    public StrokeBorder(BasicStroke stroke, Paint paint) {
        if (stroke == null) {
            throw new NullPointerException("El trazo no puede ser null");
        }
        this.stroke = stroke;
        this.paint = paint;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        float grosor = this.stroke.getLineWidth();
        if (grosor <= 0.0f) {
            return;
        }
        if (!(g instanceof Graphics2D)) {
            // Sin Java2D no hay trazo que aplicar. Declinar es mejor que dibujar un rectangulo
            // liso: eso seria un borde distinto del que se pidio, y en silencio.
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setStroke(this.stroke);
        if (this.paint != null) {
            g2.setPaint(this.paint);
        } else {
            g2.setPaint(c.getForeground());
        }
        // El rectangulo se encoge medio grosor de cada lado para que el trazo, que se dibuja
        // centrado, quede entero adentro del area del borde.
        float mitad = grosor / 2.0f;
        // `Rectangle2D$Float` con el nombre binario: el nombre Java de un tipo anidado de otro
        // archivo no resuelve en nuestro compilador (#101), igual que en `AbstractBorder`.
        Shape r = new Rectangle2D$Float(x + mitad, y + mitad,
                (float) width - grosor, (float) height - grosor);
        g2.draw(r);
        g2.dispose();
    }

    public Insets getBorderInsets(Component c, Insets insets) {
        int lado = (int) Math.ceil((double) this.stroke.getLineWidth());
        insets.top = lado;
        insets.left = lado;
        insets.right = lado;
        insets.bottom = lado;
        return insets;
    }

    /** El trazo con el que se dibuja. Nunca {@code null}. */
    public BasicStroke getStroke() {
        return this.stroke;
    }

    /** La pintura, o {@code null} si sigue el color del componente. */
    public Paint getPaint() {
        return this.paint;
    }
}
