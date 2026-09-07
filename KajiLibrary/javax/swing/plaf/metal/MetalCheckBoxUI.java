package javax.swing.plaf.metal;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * La casilla de Metal.
 *
 * <p>Todo el dibujo lo hace {@link MetalRadioButtonUI}, de la que hereda. Lo unico que esta clase
 * cambia es {@link #getPropertyPrefix}, que pasa a ser {@code "CheckBox."}: con eso, los mismos
 * cuatro metodos de la clase de arriba leen {@code "CheckBox.focus"} en vez de
 * {@code "RadioButton.focus"} y la casilla puede tener colores propios sin una linea de dibujo
 * repetida.
 *
 * <p>Es la clase mas corta del paquete y la que mejor muestra para que sirve el prefijo.
 */
public class MetalCheckBoxUI extends MetalRadioButtonUI {

    private static final MetalCheckBoxUI UNICO = new MetalCheckBoxUI();

    public MetalCheckBoxUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return UNICO;
    }

    public String getPropertyPrefix() {
        return "CheckBox.";
    }

    public void installDefaults(AbstractButton b) {
        super.installDefaults(b);
    }

    protected void uninstallDefaults(AbstractButton b) {
        super.uninstallDefaults(b);
    }
}
