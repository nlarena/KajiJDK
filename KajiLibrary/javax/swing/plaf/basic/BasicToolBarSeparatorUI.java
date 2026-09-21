package javax.swing.plaf.basic;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;

/**
 * A tool bar's separator: a gap, and nothing drawn.
 *
 * <p>It is the only separator that paints no line. In a tool bar what separates two groups of
 * buttons is the air, not a groove, so {@link #paint} is empty on purpose and the only thing
 * that matters is the size: {@code ToolBar.separatorSize}, measured in Metal (JDK 25) at
 * 10 x 10.
 *
 * <p>{@link #getPreferredSize} reads the component's size instead of answering the constant,
 * because it may be the one the user set. If the separator does not have one it returns
 * {@code null}, just like {@link BasicSeparatorUI#getMinimumSize}.
 */
public class BasicToolBarSeparatorUI extends BasicSeparatorUI {

    /**
     * A bare {@link Dimension}, not a {@code DimensionUIResource}.
     *
     * <p>It looks like an oversight and it is measured: the look and feel's table keeps an unmarked
     * size there. The consequence is that after the first install the separator no longer has a
     * size "of the look and feel's", so a second install does not overwrite it. Marking it would
     * change that and it would also show: {@code getSeparatorSize().toString()} says the class's
     * name.
     */
    private static final Dimension DEFAULT_SIZE = new Dimension(10, 10);

    public BasicToolBarSeparatorUI() {
    }

    /** A new one each time, like the one of the class it comes from. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicToolBarSeparatorUI();
    }

    /** The gap's size, only if the separator does not have one of its own. */
    protected void installDefaults(JSeparator s) {
        Dimension size = ((JToolBar.Separator) s).getSeparatorSize();
        if (size == null || size instanceof UIResource) {
            ((JToolBar.Separator) s).setSeparatorSize(DEFAULT_SIZE);
        }
    }

    /** Nothing; see the class note. */
    public void paint(Graphics g, JComponent c) {
    }

    /** Whatever size the separator has set, or {@code null} if it has none. */
    public Dimension getPreferredSize(JComponent c) {
        Dimension size = ((JToolBar.Separator) c).getSeparatorSize();
        if (size != null) {
            return size.getSize();
        }
        return null;
    }
}
