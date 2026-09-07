package javax.swing.plaf.metal;

import java.beans.PropertyChangeEvent;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.JTextComponent;

/**
 * El campo de texto de Metal.
 *
 * <p>Lo unico que agrega es vigilar la propiedad {@code "editable"} y, cuando cambia, poner el
 * fondo que corresponde: el de un campo editable no es el mismo que el de uno de solo lectura, y
 * el basico no distingue.
 *
 * <p>Lo hace en {@code propertyChange} y no en {@code installDefaults} porque un campo se vuelve
 * de solo lectura en cualquier momento -- un formulario que se congela mientras guarda -- y el
 * color tiene que seguirlo.
 */
public class MetalTextFieldUI extends BasicTextFieldUI {

    public MetalTextFieldUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalTextFieldUI();
    }

    public void propertyChange(PropertyChangeEvent e) {
        if ("editable".equals(e.getPropertyName())) {
            JTextComponent c = getComponent();
            if (c != null && (c.getBackground() instanceof javax.swing.plaf.UIResource)) {
                c.setBackground(c.isEditable()
                        ? MetalLookAndFeel.getWindowBackground()
                        : MetalLookAndFeel.getControl());
            }
        }
        super.propertyChange(e);
    }
}
