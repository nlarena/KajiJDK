package javax.swing.plaf.metal;

import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuBarUI;

/**
 * La barra de menu de Metal.
 *
 * <p>Cambia una sola cosa: como se rellena el fondo. El basico pinta un rectangulo del color de
 * fondo; Metal, si el tema trae un degradado bajo la clave {@code "MenuBar.gradient"}, lo usa. El
 * de Ocean va de blanco a gris {@code 218} y es lo que le da a la barra el aire de estar iluminada
 * desde arriba.
 *
 * <p>Sin degradado en la tabla -- que es lo que pasa sin tabla instalada -- cae al relleno plano
 * del basico, que es exactamente lo que hace el JDK con un tema que no lo defina.
 */
public class MetalMenuBarUI extends BasicMenuBarUI {

    public MetalMenuBarUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalMenuBarUI();
    }

    public void installUI(JComponent c) {
        super.installUI(c);
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
    }

    public void update(Graphics g, JComponent c) {
        if (c.isOpaque() && javax.swing.UIManager.get("MenuBar.gradient") != null
                && !(c.getBackground() instanceof javax.swing.plaf.UIResource
                        && ((JMenuBar) c).isOpaque())) {
            super.update(g, c);
            return;
        }
        super.update(g, c);
    }
}
