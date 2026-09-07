package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeEvent;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.text.JTextComponent;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

/**
 * El aspecto basico de un panel de texto con estilos.
 *
 * <h2>El color y la fuente van al estilo, no al componente</h2>
 *
 * <p>Es la unica diferencia con {@link BasicEditorPaneUI}, y no es chica. En un panel sin estilos
 * el color del componente <em>es</em> el color del texto. En uno con estilos, el texto se dibuja
 * con lo que diga su estilo, y el color del componente no lo mira nadie. Asi que lo que instala el
 * aspecto --el frente y la fuente-- hay que meterlo en el estilo de omision del documento, que es
 * de donde heredan todos los demas.
 *
 * <p>Y solo si lo puso el aspecto: un color que puso el usuario a mano no se toca, que es la regla
 * de {@link UIResource}. Igual que en todos lados, pero acá la consecuencia de equivocarse es
 * peor: pisaria el color de un parrafo que el programa configuro a proposito.
 */
public class BasicTextPaneUI extends BasicEditorPaneUI {

    public BasicTextPaneUI() {
        super();
    }

    /** Uno nuevo por panel: un UI de texto guarda el componente. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicTextPaneUI();
    }

    protected String getPropertyPrefix() {
        return "TextPane";
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        actualizarFrente(c.getForeground());
        actualizarFuente(c.getFont());
    }

    /** Lleva al estilo de omision lo que cambio en el componente. */
    protected void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);
        String nombre = evt.getPropertyName();
        if ("foreground".equals(nombre)) {
            actualizarFrente((Color) evt.getNewValue());
        } else if ("font".equals(nombre)) {
            actualizarFuente((Font) evt.getNewValue());
        } else if ("document".equals(nombre)) {
            JComponent comp = (JComponent) evt.getSource();
            actualizarFrente(comp.getForeground());
            actualizarFuente(comp.getFont());
        }
    }

    /** El color al estilo de omision; ver la nota de la clase. */
    private void actualizarFrente(Color color) {
        StyledDocument doc = documento();
        if (doc == null) {
            return;
        }
        Style estilo = doc.getStyle(StyleContext.DEFAULT_STYLE);
        if (estilo == null) {
            return;
        }
        if (color == null) {
            estilo.removeAttribute(StyleConstants.Foreground);
            return;
        }
        if (color instanceof UIResource) {
            MutableAttributeSet a = new SimpleAttributeSet();
            StyleConstants.setForeground(a, color);
            estilo.addAttributes(a);
        }
    }

    /** La fuente al estilo de omision; ver la nota de la clase. */
    private void actualizarFuente(Font font) {
        StyledDocument doc = documento();
        if (doc == null) {
            return;
        }
        Style estilo = doc.getStyle(StyleContext.DEFAULT_STYLE);
        if (estilo == null) {
            return;
        }
        if (font == null) {
            estilo.removeAttribute(StyleConstants.FontFamily);
            estilo.removeAttribute(StyleConstants.FontSize);
            estilo.removeAttribute(StyleConstants.Bold);
            estilo.removeAttribute(StyleConstants.Italic);
            return;
        }
        if (font instanceof UIResource) {
            MutableAttributeSet a = new SimpleAttributeSet();
            StyleConstants.setFontFamily(a, font.getFamily());
            StyleConstants.setFontSize(a, font.getSize());
            StyleConstants.setBold(a, font.isBold());
            StyleConstants.setItalic(a, font.isItalic());
            estilo.addAttributes(a);
        }
    }

    private StyledDocument documento() {
        JTextComponent c = getComponent();
        if (c == null) {
            return null;
        }
        javax.swing.text.Document d = c.getDocument();
        return (d instanceof StyledDocument) ? (StyledDocument) d : null;
    }
}
