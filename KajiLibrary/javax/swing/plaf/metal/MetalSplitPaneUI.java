package javax.swing.plaf.metal;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/**
 * El panel dividido de Metal.
 *
 * <p>Una sola linea de codigo util: cambia el divisor por uno de Metal. Todo lo demas -- la
 * distribucion, el arrastre, los dos botones de un toque -- es del basico.
 *
 * <p>Es un buen ejemplo de para que sirve que {@code createDefaultDivider} sea un metodo y no un
 * {@code new} adentro de {@code installUI}: un aspecto entero que cambia el aspecto de un
 * componente entero, en tres lineas.
 */
public class MetalSplitPaneUI extends BasicSplitPaneUI {

    public MetalSplitPaneUI() {
    }

    public static ComponentUI createUI(JComponent x) {
        return new MetalSplitPaneUI();
    }

    public BasicSplitPaneDivider createDefaultDivider() {
        return new MetalSplitPaneDivider(this);
    }
}
