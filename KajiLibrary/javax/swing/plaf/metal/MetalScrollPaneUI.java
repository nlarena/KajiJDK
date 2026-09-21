package javax.swing.plaf.metal;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollPaneUI;

/**
 * Metal's scroll pane.
 *
 * <p>The only thing it adds is a listener that watches for somebody <em>replacing</em> one of
 * the two bars. Metal sets on the pane's bars a property -- {@code "JScrollBar.isFreeStanding"}
 * at {@code false} -- that tells the bar's look and feel not to draw the outer border, because
 * the border is already put there by the pane. A bar put in afterwards would not have it and
 * would be drawn with an extra frame right against the pane's frame.
 *
 * <p>It is a two-pixel detail and it is the whole class. It is worth it because the symptom -- a
 * double line on a single side, and only if the program changed the bar -- is one of those
 * nobody finds by reading the code.
 */
public class MetalScrollPaneUI extends BasicScrollPaneUI {

    private PropertyChangeListener scrollBarChange;

    public MetalScrollPaneUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalScrollPaneUI();
    }

    public void installUI(JComponent c) {
        super.installUI(c);
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
    }

    public void installListeners(JScrollPane scrollPane) {
        super.installListeners(scrollPane);
        scrollBarChange = createScrollBarSwapListener();
        scrollPane.addPropertyChangeListener(scrollBarChange);
        free(scrollPane.getHorizontalScrollBar());
        free(scrollPane.getVerticalScrollBar());
    }

    public void uninstallListeners(JScrollPane scrollPane) {
        super.uninstallListeners((JComponent) scrollPane);
        if (scrollBarChange != null) {
            scrollPane.removePropertyChangeListener(scrollBarChange);
            scrollBarChange = null;
        }
    }

    protected void uninstallListeners(JComponent c) {
        uninstallListeners((JScrollPane) c);
    }

    /** It tells the bar that it goes attached to the pane and not free standing. */
    private static void free(JScrollBar bar) {
        if (bar != null) {
            bar.putClientProperty("JScrollBar.isFreeStanding", Boolean.FALSE);
        }
    }

    protected PropertyChangeListener createScrollBarSwapListener() {
        return new ScrollBarChange();
    }

    /**
     * Not static: it needs nothing of the pane, but the JDK makes it anonymous and it is the same.
     */
    private static class ScrollBarChange implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent e) {
            String name = e.getPropertyName();
            if ("verticalScrollBar".equals(name) || "horizontalScrollBar".equals(name)) {
                if (e.getOldValue() instanceof JScrollBar) {
                    ((JScrollBar) e.getOldValue())
                            .putClientProperty("JScrollBar.isFreeStanding", null);
                }
                if (e.getNewValue() instanceof JScrollBar) {
                    ((JScrollBar) e.getNewValue())
                            .putClientProperty("JScrollBar.isFreeStanding", Boolean.FALSE);
                }
            }
        }
    }
}
