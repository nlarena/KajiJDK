package javax.swing.plaf.metal;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JTextField;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicComboBoxEditor;

/**
 * El campo de texto de un desplegable editable, en Metal.
 *
 * <p>Todo lo que cambia del basico es el borde, y el borde tiene un detalle que se ve enseguida
 * cuando falta: los margenes son {@code (2,2,2,0)}. <strong>Cero a la derecha.</strong>
 *
 * <p>La razon es que a la derecha del campo esta la flecha, y las dos piezas comparten una sola
 * linea vertical. Si el campo dejara su pixel de aire, entre el texto y la flecha quedaria un
 * escalon y el desplegable dejaria de leerse como un control unico.
 *
 * <p>{@link #editorBorderInsets} es {@code protected static}, asi que un aspecto derivado puede
 * cambiarlo -- y se lo cambia a todos los desplegables a la vez, porque es un solo objeto
 * compartido--.
 */
public class MetalComboBoxEditor extends BasicComboBoxEditor {

    /** Cero a la derecha; ver la nota de la clase. */
    protected static Insets editorBorderInsets = new Insets(2, 2, 2, 0);

    public MetalComboBoxEditor() {
        super();
        editor.setBorder(new EditorBorder());
    }

    /** El marco del campo, sin el lado que da a la flecha. */
    private static class EditorBorder extends AbstractBorder {

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.setColor(MetalLookAndFeel.getControlDarkShadow());
            g.drawLine(x, y, x + w - 1, y);
            g.drawLine(x, y, x, y + h - 2);
            g.drawLine(x, y + h - 2, x + w - 1, y + h - 2);
            g.setColor(MetalLookAndFeel.getControlHighlight());
            g.drawLine(x + 1, y + 1, x + w - 1, y + 1);
            g.drawLine(x + 1, y + 1, x + 1, y + h - 3);
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(editorBorderInsets.top, editorBorderInsets.left,
                    editorBorderInsets.bottom, editorBorderInsets.right);
            return insets;
        }
    }

    /**
     * El mismo editor, marcado como puesto por el aspecto.
     *
     * <p>Existe solo para eso: un editor que es {@code UIResource} lo reemplaza el proximo aspecto,
     * y uno que el programa puso a mano se queda. Es la unica diferencia entre las dos clases.
     */
    public static class UIResource extends MetalComboBoxEditor
            implements javax.swing.plaf.UIResource {

        public UIResource() {
        }
    }
}
