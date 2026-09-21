package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.TabbedPaneUI;
import javax.swing.plaf.UIResource;
import javax.swing.text.View;

/**
 * The basic look and feel of a tabbed pane.
 *
 * <h2>The tabs go in runs, not in a row</h2>
 *
 * <p>When the tabs do not fit in the pane's width, they neither shrink nor does a bar appear:
 * they are stacked in several rows -- "runs" --. {@link #tabRuns} keeps at which index each
 * one starts and {@link #runCount} how many there are. All the class's arithmetic turns around
 * that: which is the next tab ({@link #getNextTabIndex}), which is the next one <em>within the
 * run</em> ({@link #getNextTabIndexInRun}), and which run comes next
 * ({@link #getNextTabRun}).
 *
 * <p>And the runs are <em>rotated</em>: the chosen tab's run is always brought to the front,
 * stuck to the content. Without that, choosing a tab from the top row would leave two rows
 * between it and its content, and the line that joins them would look broken.
 *
 * <h2>The state is computed late</h2>
 *
 * <p>After installing, {@link #runCount} holds zero and so does {@link #maxTabHeight}: the
 * arithmetic is done by the layout, and it does not run until somebody asks. That is why
 * {@link #getTabRunCount} returns 2 in a pane of two tabs even though the field says zero
 * -- it forces the computation before answering --. It is measured, and it is easy to mistake
 * for a bug.
 *
 * <h2>A tabbed pane is not opaque</h2>
 *
 * <p>And its background colour is the tabs' shadow one, not a panel grey. Both are measured and
 * both are surprising; the reason is that what is seen behind the tabs is the content's border,
 * not a background.
 *
 * <h2>What is left said</h2>
 *
 * <p>The single-row mode with scroll buttons -- {@code SCROLL_TAB_LAYOUT} -- needs buttons and
 * a viewport; here every tab is laid out in runs. A pane put into that mode looks like a
 * wrapped one.
 *
 * <p>The four protected keys are left null, as in the rest of the package.
 */
public class BasicTabbedPaneUI extends TabbedPaneUI implements SwingConstants {

    protected JTabbedPane tabPane;

    protected Color highlight;
    protected Color lightHighlight;
    protected Color shadow;
    protected Color darkShadow;
    protected Color focus;

    protected int textIconGap;
    protected int tabRunOverlay;

    protected Insets tabInsets;
    protected Insets selectedTabPadInsets;
    protected Insets tabAreaInsets;
    protected Insets contentBorderInsets;

    /** Unused; see the class note. */
    protected KeyStroke upKey;

    /** Unused; see the class note. */
    protected KeyStroke downKey;

    /** Unused; see the class note. */
    protected KeyStroke leftKey;

    /** Unused; see the class note. */
    protected KeyStroke rightKey;

    /** Where each run starts; see the class note. */
    protected int[] tabRuns = new int[10];

    protected int runCount = 0;
    protected int selectedRun = -1;
    protected Rectangle[] rects = new Rectangle[0];
    protected int maxTabHeight;
    protected int maxTabWidth;

    protected ChangeListener tabChangeListener;
    protected PropertyChangeListener propertyChangeListener;
    protected MouseListener mouseListener;
    protected FocusListener focusListener;

    /** A working rectangle; it is reused so as not to create one for each piece of arithmetic. */
    protected transient Rectangle calcRect = new Rectangle();

    private Component visibleComponent;
    private int rolloverTabIndex = -1;
    private boolean layoutComputed;

    private static final ColorUIResource SHADOW = new ColorUIResource(184, 207, 229);
    private static final ColorUIResource DARK_SHADOW = new ColorUIResource(122, 138, 153);
    private static final ColorUIResource HIGHLIGHT = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource LIGHT_HIGHLIGHT = new ColorUIResource(255, 255, 255);
    private static final ColorUIResource FOCUS = new ColorUIResource(99, 130, 191);
    private static final ColorUIResource FOREGROUND = new ColorUIResource(51, 51, 51);
    private static final FontUIResource FONT = new FontUIResource("Dialog", Font.BOLD, 12);

    public BasicTabbedPaneUI() {
    }

    /** A new one per pane: it keeps the runs and the tabs' rectangles. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicTabbedPaneUI();
    }

    public void installUI(JComponent c) {
        this.tabPane = (JTabbedPane) c;
        calcRect = new Rectangle(0, 0, 0, 0);
        tabRuns = new int[10];
        installDefaults();
        installComponents();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        uninstallKeyboardActions();
        uninstallListeners();
        uninstallComponents();
        uninstallDefaults();
        tabPane = null;
    }

    /** Colours, insets and typeface; the values are those of {@code TabbedPane.*} in Metal. */
    protected void installDefaults() {
        Color background = tabPane.getBackground();
        if (background == null || background instanceof UIResource) {
            tabPane.setBackground(SHADOW);
        }
        Color foreground = tabPane.getForeground();
        if (foreground == null || foreground instanceof UIResource) {
            tabPane.setForeground(FOREGROUND);
        }
        Font font = tabPane.getFont();
        if (font == null || font instanceof UIResource) {
            tabPane.setFont(FONT);
        }
        highlight = HIGHLIGHT;
        lightHighlight = LIGHT_HIGHLIGHT;
        shadow = SHADOW;
        darkShadow = DARK_SHADOW;
        focus = FOCUS;

        textIconGap = 4;
        tabRunOverlay = 2;
        tabInsets = new InsetsUIResource(0, 9, 1, 9);
        selectedTabPadInsets = new InsetsUIResource(2, 2, 2, 1);
        tabAreaInsets = new Insets(2, 2, 0, 6);
        contentBorderInsets = new Insets(4, 2, 3, 3);

        LookAndFeel.installProperty(tabPane, "opaque", Boolean.FALSE);
    }

    /** It removes nothing; see {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
        highlight = null;
        lightHighlight = null;
        shadow = null;
        darkShadow = null;
        focus = null;
        tabInsets = null;
        selectedTabPadInsets = null;
        tabAreaInsets = null;
        contentBorderInsets = null;
    }

    /** It sets the runs' layout. */
    protected void installComponents() {
        tabPane.setLayout(createLayoutManager());
    }

    protected void uninstallComponents() {
        tabPane.setLayout(null);
    }

    protected LayoutManager createLayoutManager() {
        return new TabbedPaneLayout(this);
    }

    protected void installListeners() {
        propertyChangeListener = createPropertyChangeListener();
        tabPane.addPropertyChangeListener(propertyChangeListener);
        tabChangeListener = createChangeListener();
        tabPane.addChangeListener(tabChangeListener);
        mouseListener = createMouseListener();
        tabPane.addMouseListener(mouseListener);
        focusListener = createFocusListener();
        tabPane.addFocusListener(focusListener);
    }

    protected void uninstallListeners() {
        tabPane.removePropertyChangeListener(propertyChangeListener);
        tabPane.removeChangeListener(tabChangeListener);
        tabPane.removeMouseListener(mouseListener);
        tabPane.removeFocusListener(focusListener);
        propertyChangeListener = null;
        tabChangeListener = null;
        mouseListener = null;
        focusListener = null;
    }

    /** With no shortcuts of its own; see the class note. */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new Handler(this);
    }

    protected ChangeListener createChangeListener() {
        return new Handler(this);
    }

    protected MouseListener createMouseListener() {
        return new Handler(this);
    }

    protected FocusListener createFocusListener() {
        return new Handler(this);
    }

    protected FontMetrics getFontMetrics() {
        Font font = tabPane.getFont();
        return tabPane.getFontMetrics(font);
    }

    protected Icon getIconForTab(int tabIndex) {
        return (!tabPane.isEnabled() || !tabPane.isEnabledAt(tabIndex))
                ? tabPane.getDisabledIconAt(tabIndex) : tabPane.getIconAt(tabIndex);
    }

    /** The title's HTML view, if it is one; see {@link BasicHTML}. */
    protected View getTextViewForTab(int tabIndex) {
        return null;
    }

    protected Insets getTabInsets(int tabPlacement, int tabIndex) {
        return tabInsets;
    }

    protected Insets getSelectedTabPadInsets(int tabPlacement) {
        rotateInsets(selectedTabPadInsets, calcRect2, tabPlacement);
        return new Insets(calcRect2.top, calcRect2.left, calcRect2.bottom, calcRect2.right);
    }

    private final Insets calcRect2 = new Insets(0, 0, 0, 0);

    protected Insets getTabAreaInsets(int tabPlacement) {
        rotateInsets(tabAreaInsets, calcRect2, tabPlacement);
        return new Insets(calcRect2.top, calcRect2.left, calcRect2.bottom, calcRect2.right);
    }

    protected Insets getContentBorderInsets(int tabPlacement) {
        rotateInsets(contentBorderInsets, calcRect2, tabPlacement);
        return new Insets(calcRect2.top, calcRect2.left, calcRect2.bottom, calcRect2.right);
    }

    /**
     * It turns some insets around according to which side the tabs go on.
     *
     * <p>The numbers are written for tabs on top; with the tabs on the left, those insets' "top"
     * becomes the "left". Turning them around is cheaper -- and much less error-prone -- than
     * having four sets of numbers.
     */
    protected static void rotateInsets(Insets topInsets, Insets targetInsets, int targetPlacement) {
        if (targetPlacement == LEFT) {
            targetInsets.top = topInsets.left;
            targetInsets.left = topInsets.top;
            targetInsets.bottom = topInsets.right;
            targetInsets.right = topInsets.bottom;
        } else if (targetPlacement == BOTTOM) {
            targetInsets.top = topInsets.bottom;
            targetInsets.left = topInsets.left;
            targetInsets.bottom = topInsets.top;
            targetInsets.right = topInsets.right;
        } else if (targetPlacement == RIGHT) {
            targetInsets.top = topInsets.left;
            targetInsets.left = topInsets.bottom;
            targetInsets.bottom = topInsets.right;
            targetInsets.right = topInsets.top;
        } else {
            targetInsets.top = topInsets.top;
            targetInsets.left = topInsets.left;
            targetInsets.bottom = topInsets.bottom;
            targetInsets.right = topInsets.right;
        }
    }

    protected int getTabRunOverlay(int tabPlacement) {
        return tabRunOverlay;
    }

    protected int getTabRunIndent(int tabPlacement, int run) {
        return 0;
    }

    /** Whether the run stretches to fill the width; the last one does not. */
    protected boolean shouldPadTabRun(int tabPlacement, int run) {
        return runCount > 1;
    }

    /** Whether the chosen run is brought to the front; see the class note. */
    protected boolean shouldRotateTabRuns(int tabPlacement) {
        return true;
    }

    /** A tab's height: the text's plus its insets plus two. */
    protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
        int height = 0;
        Component c = tabPane.getTabComponentAt(tabIndex);
        if (c != null) {
            height = c.getPreferredSize().height;
        } else {
            View v = getTextViewForTab(tabIndex);
            if (v != null) {
                height += (int) v.getPreferredSpan(View.Y_AXIS);
            } else {
                height += fontHeight;
            }
            Icon icon = getIconForTab(tabIndex);
            if (icon != null) {
                height = Math.max(height, icon.getIconHeight());
            }
        }
        Insets insets = getTabInsets(tabPlacement, tabIndex);
        height += insets.top + insets.bottom + 2;
        return height;
    }

    protected int calculateMaxTabHeight(int tabPlacement) {
        FontMetrics metrics = getFontMetrics();
        int tabCount = tabPane.getTabCount();
        int result = 0;
        int fontHeight = metrics.getHeight();
        for (int i = 0; i < tabCount; i++) {
            result = Math.max(calculateTabHeight(tabPlacement, i, fontHeight), result);
        }
        return result;
    }

    /** A tab's width: the text's plus its insets plus three. */
    protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
        Insets insets = getTabInsets(tabPlacement, tabIndex);
        int width = insets.left + insets.right + 3;
        Component tabComponent = tabPane.getTabComponentAt(tabIndex);
        if (tabComponent != null) {
            width += tabComponent.getPreferredSize().width;
            return width;
        }
        Icon icon = getIconForTab(tabIndex);
        if (icon != null) {
            width += icon.getIconWidth() + textIconGap;
        }
        View v = getTextViewForTab(tabIndex);
        if (v != null) {
            width += (int) v.getPreferredSpan(View.X_AXIS);
        } else {
            String title = tabPane.getTitleAt(tabIndex);
            width += metrics.stringWidth(title);
        }
        return width;
    }

    protected int calculateMaxTabWidth(int tabPlacement) {
        FontMetrics metrics = getFontMetrics();
        int tabCount = tabPane.getTabCount();
        int result = 0;
        for (int i = 0; i < tabCount; i++) {
            result = Math.max(calculateTabWidth(tabPlacement, i, metrics), result);
        }
        return result;
    }

    /** The tab area's height: the runs minus the overlap, plus the insets. */
    protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) {
        Insets insets = getTabAreaInsets(tabPlacement);
        int overlay = getTabRunOverlay(tabPlacement);
        return (horizRunCount > 0)
                ? horizRunCount * (maxTabHeight - overlay) + overlay
                        + insets.top + insets.bottom
                : 0;
    }

    protected int calculateTabAreaWidth(int tabPlacement, int vertRunCount, int maxTabWidth) {
        Insets insets = getTabAreaInsets(tabPlacement);
        int overlay = getTabRunOverlay(tabPlacement);
        return (vertRunCount > 0)
                ? vertRunCount * (maxTabWidth - overlay) + overlay
                        + insets.left + insets.right
                : 0;
    }

    /** It enlarges the table of runs when no more fit. */
    protected void expandTabRunsArray() {
        int[] mas = new int[tabRuns.length * 2];
        System.arraycopy(tabRuns, 0, mas, 0, tabRuns.length);
        tabRuns = mas;
    }

    /** It makes sure there is a rectangle per tab. */
    protected void assureRectsCreated(int tabCount) {
        if (rects == null || rects.length < tabCount) {
            Rectangle[] added = new Rectangle[tabCount];
            int old = (rects == null) ? 0 : rects.length;
            for (int i = 0; i < old && i < tabCount; i++) {
                added[i] = rects[i];
            }
            for (int i = old; i < tabCount; i++) {
                added[i] = new Rectangle();
            }
            rects = added;
        }
    }

    /** Which tab has the focus; the chosen one. */
    protected int getFocusIndex() {
        return tabPane.getSelectedIndex();
    }

    /**
     * A scroll button of the tab area.
     *
     * <p>Only the single-row mode uses it, which this library does not lay out; see the class
     * note. The button is built all the same, so that a subclass that wants it has it.
     */
    protected javax.swing.JButton createScrollButton(int direction) {
        if (direction != SOUTH && direction != NORTH && direction != EAST
                && direction != WEST) {
            throw new IllegalArgumentException("Direction must be one of: "
                    + "SOUTH, NORTH, EAST or WEST");
        }
        return new BasicArrowButton(direction);
    }

    /** It forces the computation if it went stale; see the class note. */
    private void ensureCurrentLayout() {
        if (!layoutComputed) {
            LayoutManager lm = tabPane.getLayout();
            if (lm instanceof TabbedPaneLayout) {
                ((TabbedPaneLayout) lm).calculateLayoutInfo();
            }
        }
    }

    public int getTabRunCount(JTabbedPane pane) {
        ensureCurrentLayout();
        return runCount;
    }

    /** That tab's rectangle; see the class note about the late computation. */
    public Rectangle getTabBounds(JTabbedPane pane, int i) {
        ensureCurrentLayout();
        return getTabBounds(i, new Rectangle());
    }

    protected Rectangle getTabBounds(int tabIndex, Rectangle dest) {
        if (rects == null || tabIndex < 0 || tabIndex >= rects.length
                || rects[tabIndex] == null) {
            dest.setBounds(0, 0, 0, 0);
            return dest;
        }
        dest.setBounds(rects[tabIndex]);
        return dest;
    }

    /** The run that tab is in. */
    protected int getRunForTab(int tabCount, int tabIndex) {
        for (int i = 0; i < runCount; i++) {
            int first = tabRuns[i];
            int last = lastTabInRun(tabCount, i);
            if (tabIndex >= first && tabIndex <= last) {
                return i;
            }
        }
        return 0;
    }

    /** That run's last tab. */
    protected int lastTabInRun(int tabCount, int run) {
        if (runCount == 1) {
            return tabCount - 1;
        }
        int nextRun = (run == runCount - 1) ? 0 : run + 1;
        if (tabRuns[nextRun] == 0) {
            return tabCount - 1;
        }
        return tabRuns[nextRun] - 1;
    }

    protected int getNextTabRun(int baseRun) {
        return (baseRun + 1) % runCount;
    }

    protected int getPreviousTabRun(int baseRun) {
        return ((baseRun - 1) >= 0) ? (baseRun - 1) : (runCount - 1);
    }

    protected int getNextTabIndex(int base) {
        return (base + 1) % tabPane.getTabCount();
    }

    protected int getPreviousTabIndex(int base) {
        int i = base - 1;
        return (i < 0) ? (tabPane.getTabCount() - 1) : i;
    }

    protected int getNextTabIndexInRun(int tabCount, int base) {
        if (runCount < 2) {
            return getNextTabIndex(base);
        }
        int currentRun = getRunForTab(tabCount, base);
        int next = getNextTabIndex(base);
        if (next == tabRuns[getNextTabRun(currentRun)]) {
            return tabRuns[currentRun];
        }
        return next;
    }

    protected int getPreviousTabIndexInRun(int tabCount, int base) {
        if (runCount < 2) {
            return getPreviousTabIndex(base);
        }
        int currentRun = getRunForTab(tabCount, base);
        if (base == tabRuns[currentRun]) {
            int previousRun = getPreviousTabRun(currentRun);
            return lastTabInRun(tabCount, previousRun);
        }
        return getPreviousTabIndex(base);
    }

    /** How much that run shifts; the basic one shifts none. */
    protected int getTabRunOffset(int tabPlacement, int tabCount, int tabIndex,
            boolean forward) {
        return 0;
    }

    /** How much the chosen tab's text shifts; the basic one does not shift it. */
    protected int getTabLabelShiftX(int tabPlacement, int tabIndex, boolean isSelected) {
        return 0;
    }

    protected int getTabLabelShiftY(int tabPlacement, int tabIndex, boolean isSelected) {
        return 0;
    }

    protected Component getVisibleComponent() {
        return visibleComponent;
    }

    /** It shows the chosen tab's content and hides the previous one. */
    protected void setVisibleComponent(Component component) {
        if (visibleComponent != null && visibleComponent != component
                && visibleComponent.getParent() == tabPane
                && visibleComponent.isVisible()) {
            visibleComponent.setVisible(false);
        }
        if (component != null && !component.isVisible()) {
            component.setVisible(true);
        }
        visibleComponent = component;
    }

    /** The tab with the mouse over it; a look and feel may draw it differently. */
    protected void setRolloverTab(int index) {
        rolloverTabIndex = index;
    }

    protected int getRolloverTab() {
        return rolloverTabIndex;
    }

    /** It chooses the next tab, the previous one, or the one in the run beside it. */
    protected void navigateSelectedTab(int direction) {
        int tabCount = tabPane.getTabCount();
        if (tabCount <= 0) {
            return;
        }
        int current = tabPane.getSelectedIndex();
        if (direction == NORTH || direction == WEST) {
            selectPreviousTab(current);
        } else {
            selectNextTab(current);
        }
    }

    protected void selectNextTab(int current) {
        int tabIndex = getNextTabIndex(current);
        while (tabIndex != current && !tabPane.isEnabledAt(tabIndex)) {
            tabIndex = getNextTabIndex(tabIndex);
        }
        tabPane.setSelectedIndex(tabIndex);
    }

    protected void selectPreviousTab(int current) {
        int tabIndex = getPreviousTabIndex(current);
        while (tabIndex != current && !tabPane.isEnabledAt(tabIndex)) {
            tabIndex = getPreviousTabIndex(tabIndex);
        }
        tabPane.setSelectedIndex(tabIndex);
    }

    protected void selectNextTabInRun(int current) {
        int tabCount = tabPane.getTabCount();
        int tabIndex = getNextTabIndexInRun(tabCount, current);
        while (tabIndex != current && !tabPane.isEnabledAt(tabIndex)) {
            tabIndex = getNextTabIndexInRun(tabCount, tabIndex);
        }
        tabPane.setSelectedIndex(tabIndex);
    }

    protected void selectPreviousTabInRun(int current) {
        int tabCount = tabPane.getTabCount();
        int tabIndex = getPreviousTabIndexInRun(tabCount, current);
        while (tabIndex != current && !tabPane.isEnabledAt(tabIndex)) {
            tabIndex = getPreviousTabIndexInRun(tabCount, tabIndex);
        }
        tabPane.setSelectedIndex(tabIndex);
    }

    /** It chooses the most similar tab of the run beside it. */
    protected void selectAdjacentRunTab(int tabPlacement, int tabIndex, int offset) {
        if (runCount < 2) {
            return;
        }
        int newIndex = tabIndex + offset;
        int tabCount = tabPane.getTabCount();
        if (newIndex < 0) {
            newIndex = tabCount - 1;
        } else if (newIndex >= tabCount) {
            newIndex = 0;
        }
        tabPane.setSelectedIndex(newIndex);
    }

    /**
     * Where that tab's text rests.
     *
     * <p>It is measured with the tab, not with the pane: two tabs of different heights have
     * different baselines, and whoever asks wants the one of the tab that is seen.
     */
    protected int getBaseline(int tab) {
        if (tabPane.getTabComponentAt(tab) != null) {
            int offset = getBaselineOffset();
            if (offset != 0) {
                return -1;
            }
            Component c = tabPane.getTabComponentAt(tab);
            Dimension pref = c.getPreferredSize();
            Insets insets = getTabInsets(tabPane.getTabPlacement(), tab);
            int loc = getTabLabelShiftY(tabPane.getTabPlacement(), tab, false);
            return c.getBaseline(pref.width, pref.height) + insets.top + loc;
        }
        View view = getTextViewForTab(tab);
        if (view != null) {
            return -1;
        }
        FontMetrics metrics = getFontMetrics();
        int height = calculateTabHeight(tabPane.getTabPlacement(), tab, metrics.getHeight());
        return (height - metrics.getHeight()) / 2 + metrics.getAscent() + getBaselineOffset();
    }

    /**
     * How much a tab's baseline shifts.
     *
     * <p>One pixel, and of a different sign according to the side and to whether there is one tab
     * or several. It is an adjustment by eye of the JDK's -- with a single tab the drawing ends up
     * one pixel off with respect to two -- and it is measured: without it, the baseline of a pane
     * of two tabs on top gives 14 instead of 15.
     */
    protected int getBaselineOffset() {
        int tabPlacement = tabPane.getTabPlacement();
        boolean varias = tabPane.getTabCount() > 1;
        if (tabPlacement == TOP) {
            return varias ? 1 : -1;
        }
        if (tabPlacement == BOTTOM) {
            return varias ? -1 : 1;
        }
        return varias ? 1 : 0;
    }

    /**
     * The pane's: the first tab's plus where the area starts.
     *
     * @throws NullPointerException if the component is null
     * @throws IllegalArgumentException if the width or the height are negative
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        if (tabPane.getTabCount() <= 0) {
            return -1;
        }
        int baseline = getBaseline(0);
        if (baseline < 0) {
            return -1;
        }
        Insets insets = tabPane.getInsets();
        Insets areaInsets = getTabAreaInsets(tabPane.getTabPlacement());
        return baseline + insets.top + areaInsets.top;
    }

    /**
     * {@code CONSTANT_ASCENT}: the tabs are always at the very top.
     *
     * @throws NullPointerException if the component is null
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        return Component.BaselineResizeBehavior.CONSTANT_ASCENT;
    }

    /** {@code null}; the layout answers. */
    public Dimension getMinimumSize(JComponent c) {
        return null;
    }

    /** {@code null}; the layout answers. */
    public Dimension getMaximumSize(JComponent c) {
        return null;
    }

    public void paint(Graphics g, JComponent c) {
        int selectedIndex = tabPane.getSelectedIndex();
        int tabPlacement = tabPane.getTabPlacement();
        ensureCurrentLayout();
        paintTabArea(g, tabPlacement, selectedIndex);
        paintContentBorder(g, tabPlacement, selectedIndex);
    }

    /** Every tab, run by run, leaving the chosen one for the end. */
    protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
        int tabCount = tabPane.getTabCount();
        Rectangle iconRect = new Rectangle();
        Rectangle textRect = new Rectangle();
        for (int i = runCount - 1; i >= 0; i--) {
            int start = tabRuns[i];
            int next = tabRuns[(i == runCount - 1) ? 0 : i + 1];
            int end = (next != 0 ? next - 1 : tabCount - 1);
            for (int j = start; j <= end && j < tabCount; j++) {
                if (j != selectedIndex) {
                    paintTab(g, tabPlacement, rects, j, iconRect, textRect);
                }
            }
        }
        if (selectedIndex >= 0 && selectedIndex < tabCount) {
            paintTab(g, tabPlacement, rects, selectedIndex, iconRect, textRect);
        }
    }

    /** One tab: background, border, icon, text and focus mark. */
    protected void paintTab(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex,
            Rectangle iconRect, Rectangle textRect) {
        if (rects == null || tabIndex >= rects.length || rects[tabIndex] == null) {
            return;
        }
        Rectangle tabRect = rects[tabIndex];
        int selectedIndex = tabPane.getSelectedIndex();
        boolean isSelected = selectedIndex == tabIndex;

        paintTabBackground(g, tabPlacement, tabIndex, tabRect.x, tabRect.y,
                tabRect.width, tabRect.height, isSelected);
        paintTabBorder(g, tabPlacement, tabIndex, tabRect.x, tabRect.y,
                tabRect.width, tabRect.height, isSelected);

        String title = tabPane.getTitleAt(tabIndex);
        Font font = tabPane.getFont();
        FontMetrics metrics = tabPane.getFontMetrics(font);
        Icon icon = getIconForTab(tabIndex);
        layoutLabel(tabPlacement, metrics, tabIndex, title, icon, tabRect, iconRect,
                textRect, isSelected);
        paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected);
        paintIcon(g, tabPlacement, tabIndex, icon, iconRect, isSelected);
        paintFocusIndicator(g, tabPlacement, rects, tabIndex, iconRect, textRect, isSelected);
    }

    /** It places the icon and the text inside the tab. */
    protected void layoutLabel(int tabPlacement, FontMetrics metrics, int tabIndex, String title,
            Icon icon, Rectangle tabRect, Rectangle iconRect, Rectangle textRect,
            boolean isSelected) {
        textRect.x = 0;
        textRect.y = 0;
        textRect.width = 0;
        textRect.height = 0;
        iconRect.x = 0;
        iconRect.y = 0;
        iconRect.width = 0;
        iconRect.height = 0;
        SwingUtilities.layoutCompoundLabel(tabPane, metrics, title, icon,
                SwingConstants.CENTER, SwingConstants.CENTER,
                SwingConstants.CENTER, SwingConstants.TRAILING,
                tabRect, iconRect, textRect, textIconGap);
        int xNudge = getTabLabelShiftX(tabPlacement, tabIndex, isSelected);
        int yNudge = getTabLabelShiftY(tabPlacement, tabIndex, isSelected);
        iconRect.x += xNudge;
        iconRect.y += yNudge;
        textRect.x += xNudge;
        textRect.y += yNudge;
    }

    protected void paintIcon(Graphics g, int tabPlacement, int tabIndex, Icon icon,
            Rectangle iconRect, boolean isSelected) {
        if (icon != null) {
            icon.paintIcon(tabPane, g, iconRect.x, iconRect.y);
        }
    }

    protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics,
            int tabIndex, String title, Rectangle textRect, boolean isSelected) {
        g.setFont(font);
        View v = getTextViewForTab(tabIndex);
        if (v != null) {
            v.paint(g, textRect);
            return;
        }
        if (tabPane.isEnabled() && tabPane.isEnabledAt(tabIndex)) {
            Color fg = tabPane.getForegroundAt(tabIndex);
            g.setColor(fg);
        } else {
            g.setColor(darkShadow);
        }
        g.drawString(title, textRect.x, textRect.y + metrics.getAscent());
    }

    /** A tab's background: whatever colour it has set, or the shadow's. */
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y,
            int w, int h, boolean isSelected) {
        g.setColor(!isSelected || tabPane.getBackgroundAt(tabIndex) != null
                ? tabPane.getBackgroundAt(tabIndex) : lightHighlight);
        g.fillRect(x, y, w, h);
    }

    /** A tab's border: two lines that join it to the content. */
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y,
            int w, int h, boolean isSelected) {
        g.setColor(lightHighlight);
        g.drawLine(x, y + h - 1, x, y + 2);
        g.drawLine(x, y + 2, x + 2, y);
        g.drawLine(x + 2, y, x + w - 3, y);
        g.setColor(shadow);
        g.drawLine(x + w - 2, y + 2, x + w - 2, y + h - 1);
        g.setColor(darkShadow);
        g.drawLine(x + w - 1, y + 2, x + w - 1, y + h - 1);
    }

    /** The mark that the tab has the focus: a dotted rectangle. */
    protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
            int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
        if (!tabPane.hasFocus() || !isSelected) {
            return;
        }
        g.setColor(focus);
        g.drawRect(textRect.x - 1, textRect.y, textRect.width + 1, textRect.height - 1);
    }

    /** The frame around the content, with the chosen tab's gap. */
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
        int width = tabPane.getWidth();
        int height = tabPane.getHeight();
        Insets insets = tabPane.getInsets();
        Insets borderInsets = getContentBorderInsets(tabPlacement);

        int x = insets.left;
        int y = insets.top;
        int w = width - insets.right - insets.left;
        int h = height - insets.top - insets.bottom;

        if (tabPlacement == TOP) {
            int tabAreaHeight = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
            y += tabAreaHeight;
            h -= tabAreaHeight;
        } else if (tabPlacement == BOTTOM) {
            h -= calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
        } else if (tabPlacement == LEFT) {
            int tabAreaWidth = calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
            x += tabAreaWidth;
            w -= tabAreaWidth;
        } else {
            w -= calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
        }
        paintContentBorderTopEdge(g, tabPlacement, selectedIndex, x, y, w, h);
        paintContentBorderLeftEdge(g, tabPlacement, selectedIndex, x, y, w, h);
        paintContentBorderBottomEdge(g, tabPlacement, selectedIndex, x, y, w, h);
        paintContentBorderRightEdge(g, tabPlacement, selectedIndex, x, y, w, h);
        // `borderInsets` is left for the subclasses that draw thicker.
        if (borderInsets == null) {
            return;
        }
    }

    protected void paintContentBorderTopEdge(Graphics g, int tabPlacement, int selectedIndex,
            int x, int y, int w, int h) {
        g.setColor(lightHighlight);
        g.drawLine(x, y, x + w - 2, y);
    }

    protected void paintContentBorderLeftEdge(Graphics g, int tabPlacement, int selectedIndex,
            int x, int y, int w, int h) {
        g.setColor(lightHighlight);
        g.drawLine(x, y, x, y + h - 2);
    }

    protected void paintContentBorderBottomEdge(Graphics g, int tabPlacement, int selectedIndex,
            int x, int y, int w, int h) {
        g.setColor(darkShadow);
        g.drawLine(x, y + h - 1, x + w - 1, y + h - 1);
    }

    protected void paintContentBorderRightEdge(Graphics g, int tabPlacement, int selectedIndex,
            int x, int y, int w, int h) {
        g.setColor(darkShadow);
        g.drawLine(x + w - 1, y, x + w - 1, y + h - 1);
    }

    /** Which tab falls at that point; -1 if none. */
    public int tabForCoordinate(JTabbedPane pane, int x, int y) {
        ensureCurrentLayout();
        for (int i = 0; i < rects.length; i++) {
            if (rects[i] != null && rects[i].contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The runs' layout; see the class note.
     *
     * <p>Static and with the look and feel as the first parameter, which is the signature the JDK
     * generates for an inner class; see finding #518.
     */
    public static class TabbedPaneLayout implements LayoutManager {

        private final BasicTabbedPaneUI ui;

        public TabbedPaneLayout(BasicTabbedPaneUI ui) {
            this.ui = ui;
        }

        public void addLayoutComponent(String name, Component comp) {
        }

        public void removeLayoutComponent(Component comp) {
        }

        public Dimension preferredLayoutSize(Container parent) {
            return calculateSize(false);
        }

        public Dimension minimumLayoutSize(Container parent) {
            return calculateSize(true);
        }

        /** The content's plus the tab area. */
        protected Dimension calculateSize(boolean minimum) {
            JTabbedPane tabPane = ui.tabPane;
            int tabPlacement = tabPane.getTabPlacement();
            Insets insets = tabPane.getInsets();
            Insets contentInsets = ui.getContentBorderInsets(tabPlacement);
            int width = 0;
            int height = 0;
            for (int i = 0; i < tabPane.getTabCount(); i++) {
                Component component = tabPane.getComponentAt(i);
                if (component == null) {
                    continue;
                }
                Dimension size = minimum ? component.getMinimumSize()
                        : component.getPreferredSize();
                if (size != null) {
                    height = Math.max(height, size.height);
                    width = Math.max(width, size.width);
                }
            }
            width += contentInsets.left + contentInsets.right;
            height += contentInsets.top + contentInsets.bottom;
            if (tabPlacement == LEFT || tabPlacement == RIGHT) {
                width += preferredTabAreaWidth(tabPlacement, height - contentInsets.top
                        - contentInsets.bottom);
            } else {
                height += preferredTabAreaHeight(tabPlacement, width - contentInsets.left
                        - contentInsets.right);
            }
            return new Dimension(width + insets.left + insets.right,
                    height + insets.top + insets.bottom);
        }

        protected int preferredTabAreaHeight(int tabPlacement, int width) {
            JTabbedPane tabPane = ui.tabPane;
            FontMetrics metrics = ui.getFontMetrics();
            int tabCount = tabPane.getTabCount();
            int total = 0;
            if (tabCount > 0) {
                int rows = 1;
                int x = 0;
                int maxTabHeight = ui.calculateMaxTabHeight(tabPlacement);
                for (int i = 0; i < tabCount; i++) {
                    int tabWidth = ui.calculateTabWidth(tabPlacement, i, metrics);
                    if (x != 0 && x + tabWidth > width) {
                        rows++;
                        x = 0;
                    }
                    x += tabWidth;
                }
                total = ui.calculateTabAreaHeight(tabPlacement, rows, maxTabHeight);
            }
            return total;
        }

        protected int preferredTabAreaWidth(int tabPlacement, int height) {
            JTabbedPane tabPane = ui.tabPane;
            FontMetrics metrics = ui.getFontMetrics();
            int tabCount = tabPane.getTabCount();
            int total = 0;
            if (tabCount > 0) {
                int columns = 1;
                int y = 0;
                int fontHeight = metrics.getHeight();
                int maxTabWidth = ui.calculateMaxTabWidth(tabPlacement);
                for (int i = 0; i < tabCount; i++) {
                    int tabHeight = ui.calculateTabHeight(tabPlacement, i, fontHeight);
                    if (y != 0 && y + tabHeight > height) {
                        columns++;
                        y = 0;
                    }
                    y += tabHeight;
                }
                total = ui.calculateTabAreaWidth(tabPlacement, columns, maxTabWidth);
            }
            return total;
        }

        /** It rebuilds runs, rectangles and maximums. */
        public void calculateLayoutInfo() {
            JTabbedPane tabPane = ui.tabPane;
            int tabCount = tabPane.getTabCount();
            ui.assureRectsCreated(tabCount);
            calculateTabRects(tabPane.getTabPlacement(), tabCount);
            ui.layoutComputed = true;
        }

        /** It shares the tabs out into runs; see the class note. */
        protected void calculateTabRects(int tabPlacement, int tabCount) {
            JTabbedPane tabPane = ui.tabPane;
            FontMetrics metrics = ui.getFontMetrics();
            Dimension size = tabPane.getSize();
            Insets insets = tabPane.getInsets();
            Insets tabAreaInsets = ui.getTabAreaInsets(tabPlacement);
            int fontHeight = metrics.getHeight();
            int selectedIndex = tabPane.getSelectedIndex();

            ui.maxTabHeight = ui.calculateMaxTabHeight(tabPlacement);
            ui.maxTabWidth = ui.calculateMaxTabWidth(tabPlacement);
            ui.runCount = 0;
            ui.selectedRun = -1;
            if (tabCount == 0) {
                return;
            }

            boolean vertical = (tabPlacement == LEFT || tabPlacement == RIGHT);
            int available = vertical
                    ? size.height - insets.top - insets.bottom
                            - tabAreaInsets.top - tabAreaInsets.bottom
                    : size.width - insets.left - insets.right
                            - tabAreaInsets.left - tabAreaInsets.right;
            int x = vertical ? insets.left + tabAreaInsets.left
                    : insets.left + tabAreaInsets.left;
            int y = vertical ? insets.top + tabAreaInsets.top
                    : insets.top + tabAreaInsets.top;
            int shifted = 0;
            ui.tabRuns[0] = 0;
            ui.runCount = 1;
            int pos = 0;

            for (int i = 0; i < tabCount; i++) {
                Rectangle rect = ui.rects[i];
                if (vertical) {
                    int height = ui.calculateTabHeight(tabPlacement, i, fontHeight);
                    if (pos != 0 && pos + height > available) {
                        shifted++;
                        if (shifted >= ui.tabRuns.length) {
                            ui.expandTabRunsArray();
                        }
                        ui.tabRuns[shifted] = i;
                        ui.runCount = shifted + 1;
                        pos = 0;
                    }
                    rect.x = x + shifted * (ui.maxTabWidth - ui.getTabRunOverlay(tabPlacement));
                    rect.y = y + pos;
                    rect.width = ui.maxTabWidth;
                    rect.height = height;
                    pos += height;
                } else {
                    int width = ui.calculateTabWidth(tabPlacement, i, metrics);
                    if (pos != 0 && pos + width > available) {
                        shifted++;
                        if (shifted >= ui.tabRuns.length) {
                            ui.expandTabRunsArray();
                        }
                        ui.tabRuns[shifted] = i;
                        ui.runCount = shifted + 1;
                        pos = 0;
                    }
                    rect.x = x + pos;
                    rect.y = y + shifted * (ui.maxTabHeight - ui.getTabRunOverlay(tabPlacement));
                    rect.width = width;
                    rect.height = ui.maxTabHeight;
                    pos += width;
                }
                if (i == selectedIndex) {
                    ui.selectedRun = shifted;
                }
            }
            if (ui.shouldRotateTabRuns(tabPlacement)) {
                rotateTabRuns(tabPlacement, ui.selectedRun);
            }
        }

        /** It brings the chosen run to the front; see the class note. */
        protected void rotateTabRuns(int tabPlacement, int selectedRun) {
            if (selectedRun < 1 || ui.runCount < 2) {
                return;
            }
            for (int i = 0; i < selectedRun; i++) {
                int first = ui.tabRuns[0];
                for (int j = 1; j < ui.runCount; j++) {
                    ui.tabRuns[j - 1] = ui.tabRuns[j];
                }
                ui.tabRuns[ui.runCount - 1] = first;
            }
        }

        /** It stretches a run's tabs so that they fill the width. */
        protected void padTabRun(int tabPlacement, int start, int end, int max) {
        }

        /** It enlarges the chosen tab a little, so that it is seen at the front. */
        protected void padSelectedTab(int tabPlacement, int selectedIndex) {
            if (selectedIndex < 0 || ui.rects == null || selectedIndex >= ui.rects.length) {
                return;
            }
            Rectangle selRect = ui.rects[selectedIndex];
            Insets padInsets = ui.getSelectedTabPadInsets(tabPlacement);
            selRect.x -= padInsets.left;
            selRect.width += (padInsets.left + padInsets.right);
            selRect.y -= padInsets.top;
            selRect.height += (padInsets.top + padInsets.bottom);
        }

        /** It hands the leftover out between the runs so that they come out even. */
        protected void normalizeTabRuns(int tabPlacement, int tabCount, int start, int max) {
        }

        public void layoutContainer(Container parent) {
            JTabbedPane tabPane = ui.tabPane;
            calculateLayoutInfo();
            int tabPlacement = tabPane.getTabPlacement();
            Insets insets = tabPane.getInsets();
            int selectedIndex = tabPane.getSelectedIndex();
            Component visible = (selectedIndex < 0) ? null
                    : tabPane.getComponentAt(selectedIndex);
            ui.setVisibleComponent(visible);
            if (visible == null) {
                return;
            }
            int cx = insets.left;
            int cy = insets.top;
            int cw = tabPane.getWidth() - insets.left - insets.right;
            int ch = tabPane.getHeight() - insets.top - insets.bottom;
            if (tabPlacement == LEFT || tabPlacement == RIGHT) {
                int width = ui.calculateTabAreaWidth(tabPlacement, ui.runCount, ui.maxTabWidth);
                if (tabPlacement == LEFT) {
                    cx += width;
                }
                cw -= width;
            } else {
                int height = ui.calculateTabAreaHeight(tabPlacement, ui.runCount, ui.maxTabHeight);
                if (tabPlacement == TOP) {
                    cy += height;
                }
                ch -= height;
            }
            Insets contentInsets = ui.getContentBorderInsets(tabPlacement);
            visible.setBounds(cx + contentInsets.left, cy + contentInsets.top,
                    cw - contentInsets.left - contentInsets.right,
                    ch - contentInsets.top - contentInsets.bottom);
        }
    }

    /**
     * The one that listens to the mouse, the focus, the model and the properties.
     *
     * <p>Static and with the look and feel as a field, for the same reason as everywhere in the
     * package.
     */
    private static class Handler extends MouseAdapter implements MouseListener, FocusListener,
            ChangeListener, PropertyChangeListener {

        private final BasicTabbedPaneUI ui;

        Handler(BasicTabbedPaneUI ui) {
            this.ui = ui;
        }

        public void mousePressed(MouseEvent e) {
            JTabbedPane tabPane = ui.tabPane;
            if (!tabPane.isEnabled()) {
                return;
            }
            int tabIndex = ui.tabForCoordinate(tabPane, e.getX(), e.getY());
            if (tabIndex >= 0 && tabPane.isEnabledAt(tabIndex)) {
                if (tabIndex != tabPane.getSelectedIndex()) {
                    tabPane.setSelectedIndex(tabIndex);
                } else if (tabPane.isRequestFocusEnabled()) {
                    tabPane.requestFocus();
                }
            }
        }

        public void focusGained(FocusEvent e) {
            ui.tabPane.repaint();
        }

        public void focusLost(FocusEvent e) {
            ui.tabPane.repaint();
        }

        public void stateChanged(ChangeEvent e) {
            JTabbedPane tabPane = ui.tabPane;
            if (tabPane == null) {
                return;
            }
            tabPane.revalidate();
            tabPane.repaint();
        }

        public void propertyChange(PropertyChangeEvent e) {
            JTabbedPane pane = ui.tabPane;
            if (pane == null) {
                return;
            }
            String name = e.getPropertyName();
            if ("tabPlacement".equals(name) || "font".equals(name)
                    || "indexForTabComponent".equals(name) || "tabLayoutPolicy".equals(name)) {
                ui.layoutComputed = false;
                pane.revalidate();
                pane.repaint();
            }
        }
    }
}
