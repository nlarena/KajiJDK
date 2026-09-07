package javax.swing.plaf.basic;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;

import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainView;
import javax.swing.text.View;
import javax.swing.text.WrappedPlainView;

/**
 * El aspecto basico de un area de texto de varias lineas.
 *
 * <h2>Dos vistas distintas segun corte o no</h2>
 *
 * <p>Si el area no corta las lineas largas, la vista es {@link PlainView}: cada linea del documento
 * es una linea en pantalla y el area se hace todo lo ancha que haga falta. Si corta, es
 * {@link WrappedPlainView}, que parte cada linea en el ancho disponible y por lo tanto tiene que
 * rehacer las cuentas cada vez que el area cambia de ancho. Cambiar {@code lineWrap} en caliente
 * obliga a rehacer el arbol de vistas entero, y de eso se ocupa {@link #propertyChange}.
 *
 * <h2>El pixel del cursor</h2>
 *
 * <p>El tamano preferido y el minimo son los de las vistas <em>mas el ancho del cursor</em>. Sin
 * ese pixel, el cursor parado al final de la linea mas larga queda medio afuera y no se ve. El
 * ancho sale de la propiedad de cliente {@code caretWidth} y es uno si no esta puesta; esta medido
 * --con {@code caretWidth} en cinco, el preferido crece cuatro pixeles--.
 *
 * <h2>La linea de base no se mueve</h2>
 *
 * <p>Al contrario que en un campo, la primera linea de un area esta siempre arriba de todo: la base
 * es el margen de arriba mas el ascenso de la fuente, y no depende del alto. Por eso el
 * comportamiento es {@code CONSTANT_ASCENT}, y por eso contesta lo mismo con alto cero.
 */
public class BasicTextAreaUI extends BasicTextUI {

    public BasicTextAreaUI() {
        super();
    }

    /** Uno nuevo por area: un UI de texto guarda el componente. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicTextAreaUI();
    }

    protected String getPropertyPrefix() {
        return "TextArea";
    }

    /**
     * No agrega nada a lo que instala {@link BasicTextUI}.
     *
     * <p>Existe como punto donde una subclase mete lo suyo sin repetir la cadena entera; el basico
     * no tiene nada propio que poner en un area.
     */
    protected void installDefaults() {
        super.installDefaults();
    }

    /** Rehace las vistas cuando cambia como se cortan las lineas o cuanto mide un tabulador. */
    protected void propertyChange(PropertyChangeEvent evt) {
        String nombre = evt.getPropertyName();
        if ("lineWrap".equals(nombre) || "wrapStyleWord".equals(nombre)
                || "tabSize".equals(nombre)) {
            modelChanged();
        }
    }

    /** La vista, segun corte o no; ver la nota de la clase. */
    public View create(Element elem) {
        JTextComponent c = getComponent();
        if (c instanceof JTextArea) {
            JTextArea area = (JTextArea) c;
            if (area.getLineWrap()) {
                return new WrappedPlainView(elem, area.getWrapStyleWord());
            }
            return new PlainView(elem);
        }
        return null;
    }

    /** El de las vistas mas el ancho del cursor; ver la nota de la clase. */
    public Dimension getPreferredSize(JComponent c) {
        return conElCursor(c, super.getPreferredSize(c));
    }

    /** Idem: en un area el minimo y el preferido salen de la misma cuenta. */
    public Dimension getMinimumSize(JComponent c) {
        return conElCursor(c, super.getMinimumSize(c));
    }

    private static Dimension conElCursor(JComponent c, Dimension d) {
        if (d == null) {
            return null;
        }
        Object ancho = c.getClientProperty("caretWidth");
        int px = 1;
        if (ancho instanceof Number) {
            px = ((Number) ancho).intValue();
        }
        d.width += px;
        return d;
    }

    /**
     * Margen de arriba mas ascenso; ver la nota de la clase.
     *
     * @throws NullPointerException si el componente es nulo
     * @throws IllegalArgumentException si el ancho o el alto son negativos
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        Insets insets = c.getInsets();
        FontMetrics fm = c.getFontMetrics(c.getFont());
        return insets.top + fm.getAscent();
    }

    /**
     * {@code CONSTANT_ASCENT}; ver la nota de la clase.
     *
     * @throws NullPointerException si el componente es nulo
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        return Component.BaselineResizeBehavior.CONSTANT_ASCENT;
    }
}
