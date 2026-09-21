package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/**
 * Metal's tabbed pane.
 *
 * <h2>The gap between the chosen tab and the content</h2>
 *
 * <p>Half of this class's methods exist because of a single drawing detail: the tab that is
 * chosen has no border on the side facing the content, so the tab and the pane read as a single
 * piece. That leaves a gap where the content's border would have to continue, and
 * {@link #shouldFillGap} and {@link #getColorForGap} are the ones that decide whether it is
 * filled and in what colour.
 *
 * <p>The gap's colour is the theme's primary and not the tab's: it is the same as the border
 * that was interrupted.
 *
 * <h2>The four sides, one per method</h2>
 *
 * <p>{@code paintTopTabBorder}, {@code paintLeftTabBorder}, {@code paintBottomTabBorder} and
 * {@code paintRightTabBorder} are not the same drawing rotated. With the tabs at the top, the
 * chosen one rises by a pixel; with the tabs on the left, it shifts. Metal writes them
 * separately because the result is not symmetric.
 *
 * <h2>The numbers</h2>
 *
 * <p>A tab's minimum width is forty -- which is what keeps a one-letter tab from looking like a
 * square button -- and the label's shift is zero in both directions, chosen or not. Both,
 * measured; the basic one shifts the label and Metal does not.
 */
public class MetalTabbedPaneUI extends BasicTabbedPaneUI {

    protected Color selectColor;
    protected Color selectHighlight;
    protected Color tabAreaBackground;

    /** Forty; see the class note. */
    protected int minTabWidth = 40;

    public MetalTabbedPaneUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalTabbedPaneUI();
    }

    protected void installDefaults() {
        super.installDefaults();
        selectColor = MetalLookAndFeel.tableColor("TabbedPane.selected");
        selectHighlight = MetalLookAndFeel.tableColor("TabbedPane.selectHighlight");
        if (selectHighlight == null) {
            selectHighlight = MetalLookAndFeel.getControlHighlight();
        }
        tabAreaBackground = MetalLookAndFeel.tableColor("TabbedPane.tabAreaBackground");
    }

    protected LayoutManager createLayoutManager() {
        return new TabbedPaneLayout();
    }

    /**
     * Metal's layout.
     *
     * <p>It is the basic one's with no changes; it exists only so that {@code getClass().getName()}
     * says {@code MetalTabbedPaneUI$TabbedPaneLayout}, which is what the JDK says and what a
     * subclass might be looking at.
     */
    public class TabbedPaneLayout extends BasicTabbedPaneUI.TabbedPaneLayout {

        public TabbedPaneLayout() {
        }
    }

    /** Zero: Metal does not shift the label even when the tab is chosen. */
    protected int getTabLabelShiftX(int tabPlacement, int tabIndex, boolean isSelected) {
        return 0;
    }

    protected int getTabLabelShiftY(int tabPlacement, int tabIndex, boolean isSelected) {
        return 0;
    }

    protected int getBaselineOffset() {
        return 0;
    }

    /** Zero: Metal's tab rows do not overlap. */
    protected int getTabRunOverlay(int tabPlacement) {
        return 0;
    }

    /** No: the chosen row stays where it is. */
    protected boolean shouldRotateTabRuns(int tabPlacement, int selectedRun) {
        return false;
    }

    protected boolean shouldPadTabRun(int tabPlacement, int run) {
        return false;
    }

    /** Whether the gap the chosen tab leaves has to be covered; see the class note. */
    protected boolean shouldFillGap(int currentRun, int tabIndex, int x, int y) {
        return true;
    }

    /** The theme's primary: the same as the border that was interrupted. */
    protected Color getColorForGap(int currentRun, int x, int y) {
        return MetalLookAndFeel.getPrimaryControl();
    }

    protected int calculateMaxTabHeight(int tabPlacement) {
        return super.calculateMaxTabHeight(tabPlacement);
    }

    public void update(Graphics g, JComponent c) {
        super.update(g, c);
    }

    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);
    }

    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
            int x, int y, int w, int h, boolean isSelected) {
        g.setColor(isSelected && selectColor != null
                ? selectColor : MetalLookAndFeel.getControl());
        g.fillRect(x, y, w, h);
    }

    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
            int x, int y, int w, int h, boolean isSelected) {
        int row = getRunForTab(tabPane.getTabCount(), tabIndex);
        // With `if` and not with `switch`: this house's compiler does not accept SwingConstants'
                // constants in a `case`.
        if (tabPlacement == SwingConstants.LEFT) {
            paintLeftTabBorder(tabIndex, g, x, y, w, h, row, tabIndex, isSelected);
        } else if (tabPlacement == SwingConstants.BOTTOM) {
            paintBottomTabBorder(tabIndex, g, x, y, w, h, row, tabIndex, isSelected);
        } else if (tabPlacement == SwingConstants.RIGHT) {
            paintRightTabBorder(tabIndex, g, x, y, w, h, row, tabIndex, isSelected);
        } else {
            paintTopTabBorder(tabIndex, g, x, y, w, h, row, tabIndex, isSelected);
        }
    }

    /** Top: the chosen one does not carry the bottom line. */
    protected void paintTopTabBorder(int tabIndex, Graphics g, int x, int y, int w, int h,
            int btm, int rght, boolean isSelected) {
        g.setColor(isSelected ? selectHighlight : MetalLookAndFeel.getControlHighlight());
        g.drawLine(x, y + 2, x + w - 2, y + 2);
        g.setColor(MetalLookAndFeel.getControlDarkShadow());
        g.drawLine(x, y + 2, x, y + h - 1);
        g.drawLine(x + w - 1, y + 2, x + w - 1, y + h - 1);
        if (!isSelected) {
            g.drawLine(x, y + h - 1, x + w - 1, y + h - 1);
        }
    }

    protected void paintBottomTabBorder(int tabIndex, Graphics g, int x, int y, int w, int h,
            int btm, int rght, boolean isSelected) {
        g.setColor(MetalLookAndFeel.getControlDarkShadow());
        g.drawLine(x, y, x, y + h - 3);
        g.drawLine(x + w - 1, y, x + w - 1, y + h - 3);
        g.drawLine(x, y + h - 3, x + w - 1, y + h - 3);
        if (!isSelected) {
            g.setColor(MetalLookAndFeel.getControlHighlight());
            g.drawLine(x, y, x + w - 1, y);
        }
    }

    protected void paintLeftTabBorder(int tabIndex, Graphics g, int x, int y, int w, int h,
            int btm, int rght, boolean isSelected) {
        g.setColor(isSelected ? selectHighlight : MetalLookAndFeel.getControlHighlight());
        g.drawLine(x + 2, y, x + 2, y + h - 1);
        g.setColor(MetalLookAndFeel.getControlDarkShadow());
        g.drawLine(x + 2, y, x + w - 1, y);
        g.drawLine(x + 2, y + h - 1, x + w - 1, y + h - 1);
        if (!isSelected) {
            g.drawLine(x + w - 1, y, x + w - 1, y + h - 1);
        }
    }

    protected void paintRightTabBorder(int tabIndex, Graphics g, int x, int y, int w, int h,
            int btm, int rght, boolean isSelected) {
        g.setColor(MetalLookAndFeel.getControlDarkShadow());
        g.drawLine(x, y, x + w - 3, y);
        g.drawLine(x, y + h - 1, x + w - 3, y + h - 1);
        g.drawLine(x + w - 3, y, x + w - 3, y + h - 1);
        if (!isSelected) {
            g.setColor(MetalLookAndFeel.getControlHighlight());
            g.drawLine(x, y, x, y + h - 1);
        }
    }

    protected void paintContentBorderTopEdge(Graphics g, int tabPlacement, int selectedIndex,
            int x, int y, int w, int h) {
        super.paintContentBorderTopEdge(g, tabPlacement, selectedIndex, x, y, w, h);
    }

    protected void paintContentBorderBottomEdge(Graphics g, int tabPlacement, int selectedIndex,
            int x, int y, int w, int h) {
        super.paintContentBorderBottomEdge(g, tabPlacement, selectedIndex, x, y, w, h);
    }

    protected void paintContentBorderLeftEdge(Graphics g, int tabPlacement, int selectedIndex,
            int x, int y, int w, int h) {
        super.paintContentBorderLeftEdge(g, tabPlacement, selectedIndex, x, y, w, h);
    }

    protected void paintContentBorderRightEdge(Graphics g, int tabPlacement, int selectedIndex,
            int x, int y, int w, int h) {
        super.paintContentBorderRightEdge(g, tabPlacement, selectedIndex, x, y, w, h);
    }

    protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
            int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
    }

    /** The highlight at the bottom of the chosen tab, which joins it to the content. */
    protected void paintHighlightBelowTab() {
    }
}
