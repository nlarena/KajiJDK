package javax.swing.plaf.basic;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Element;
import javax.swing.text.FieldView;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;

/**
 * El aspecto basico de un campo de texto de una sola linea.
 *
 * <p>Casi todo el trabajo lo hace {@link BasicTextUI}; lo propio de esta clase son dos cosas: la
 * vista que arma --{@link FieldView}, la unica que sabe centrar verticalmente una linea sola y
 * correrse cuando el texto no entra-- y la linea de base.
 *
 * <h2>La linea de base se mueve con el alto</h2>
 *
 * <p>Un campo centra su unica linea, asi que si el campo crece la linea baja la mitad de lo que
 * crecio. Por eso el comportamiento es {@code CENTER_OFFSET} y no {@code CONSTANT_ASCENT}: quien
 * alinea un campo con una etiqueta al lado tiene que volver a preguntar cada vez que cambia el
 * alto. Medido: con 20 de alto la base esta en 15 y con 30 en 20.
 *
 * <p>Con alto cero no hay donde poner nada y la respuesta es -1.
 */
public class BasicTextFieldUI extends BasicTextUI {

    public BasicTextFieldUI() {
        super();
    }

    /** Uno nuevo por campo: un UI de texto guarda el componente. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicTextFieldUI();
    }

    protected String getPropertyPrefix() {
        return "TextField";
    }

    /**
     * La vista del campo.
     *
     * <p>Siempre una {@link FieldView}. El JDK tiene una variante para texto que se lee de derecha
     * a izquierda mezclado con texto que se lee al reves; aca no esta, y esta dicho: un campo con
     * texto bidireccional se dibuja en un solo sentido.
     */
    public View create(Element elem) {
        return new FieldView(elem);
    }

    /**
     * Donde apoya el texto; ver la nota de la clase.
     *
     * @throws NullPointerException si el componente es nulo
     * @throws IllegalArgumentException si el ancho o el alto son negativos
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        View rootView = getRootView((JTextComponent) c);
        if (rootView.getViewCount() > 0) {
            Insets insets = c.getInsets();
            height = height - insets.top - insets.bottom;
            if (height > 0) {
                int baseline = insets.top;
                View fieldView = rootView.getView(0);
                int vspan = (int) fieldView.getPreferredSpan(View.Y_AXIS);
                if (height != vspan) {
                    // La linea va centrada: la mitad de lo que sobra queda arriba.
                    int slop = height - vspan;
                    baseline += slop / 2;
                }
                FontMetrics fm = c.getFontMetrics(c.getFont());
                baseline += fm.getAscent();
                return baseline;
            }
        }
        return -1;
    }

    /**
     * {@code CENTER_OFFSET}; ver la nota de la clase.
     *
     * @throws NullPointerException si el componente es nulo
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        return Component.BaselineResizeBehavior.CENTER_OFFSET;
    }
}
