package javax.swing.plaf.metal;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/**
 * Metal's split pane.
 *
 * <p>A single useful line of code: it swaps the divider for a Metal one. Everything else -- the
 * layout, the dragging, the two one-touch buttons -- is the basic one's.
 *
 * <p>It is a good example of what it is for that {@code createDefaultDivider} is a method and not
 * a {@code new} inside {@code installUI}: a whole look and feel changing the look of a whole
 * component, in three lines.
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
