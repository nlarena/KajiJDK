package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.RootPaneContainer;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.ToolBarUI;
import javax.swing.plaf.UIResource;

/**
 * The basic look and feel of a tool bar.
 *
 * <h2>A bar that can be torn from its place</h2>
 *
 * <p>What distinguishes a tool bar from a row of buttons is that it can be grabbed by the edge
 * and taken somewhere else: to another side of the window, or outside, floating in a little
 * window of its own. That is almost everything this class does, and it is the reason for its
 * four colours -- two for the docked bar and two for the floating one -- and for the
 * {@code DragWindow}, the rectangle that is seen while dragging.
 *
 * <p>{@link #constraintBeforeFloating} keeps which side it was on before flying off, so as to
 * be able to put it back there. It starts at {@code "North"}, which is where a bar nobody
 * moved goes.
 *
 * <h2>Borders that appear when the mouse passes over</h2>
 *
 * <p>A bar's buttons carry no border until the mouse passes over them; that is what makes a bar
 * look like a row of icons and not like a row of buttons. {@link #setRolloverBorders} switches
 * between the two sets, and both borders are created once and shared
 * -- {@link #getRolloverBorder} always returns the same object --.
 *
 * <h2>What is left said</h2>
 *
 * <p>Taking the bar out to float needs a real window: {@link #createFloatingWindow} and
 * {@link #floatAt} are written and cannot be tested without a screen. What can be tested is the
 * state: the colours, the borders, and that {@link #isFloating} says no.
 *
 * <p>{@link #canDock} blows up with a null component, just like the JDK: it asks whether the
 * point falls inside without checking anything first.
 *
 * <p>The four protected keys are left null, as in {@link BasicSplitPaneUI}.
 */
public class BasicToolBarUI extends ToolBarUI implements SwingConstants {

    protected JToolBar toolBar;

    /** Which button had the focus before dragging; -1 if none. */
    protected int focusedCompIndex = -1;

    protected Color dockingColor;
    protected Color floatingColor;
    protected Color dockingBorderColor;
    protected Color floatingBorderColor;

    protected MouseInputListener dockingListener;
    protected PropertyChangeListener propertyListener;
    protected ContainerListener toolBarContListener;
    protected FocusListener toolBarFocusListener;

    /** Which side it was on before floating; see the class note. */
    protected String constraintBeforeFloating = NorthBorderLayout.NORTH;

    /** Unused; see the class note. */
    protected KeyStroke upKey;

    /** Unused; see the class note. */
    protected KeyStroke downKey;

    /** Unused; see the class note. */
    protected KeyStroke leftKey;

    /** Unused; see the class note. */
    protected KeyStroke rightKey;

    /** The rectangle that is seen when dragging; null until somebody drags. */
    protected DragWindow dragWindow;

    private boolean rolloverBorders = true;
    private boolean floating;
    private Border rolloverBorder;
    private Border nonRolloverBorder;

    private static final ColorUIResource BACKGROUND = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FOREGROUND = new ColorUIResource(51, 51, 51);
    private static final ColorUIResource DOCK_BORDER = new ColorUIResource(99, 130, 191);
    private static final ColorUIResource FLOATING_BORDER = new ColorUIResource(184, 207, 229);
    private static final FontUIResource FONT = new FontUIResource("Dialog", Font.BOLD, 12);

    public BasicToolBarUI() {
    }

    /** A new one per bar: it keeps the bar, its colours and the drag's state. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicToolBarUI();
    }

    public void installUI(JComponent c) {
        toolBar = (JToolBar) c;
        installDefaults();
        installComponents();
        installListeners();
        installKeyboardActions();
        floating = false;
    }

    public void uninstallUI(JComponent c) {
        uninstallDefaults();
        uninstallComponents();
        uninstallListeners();
        uninstallKeyboardActions();
        dragWindow = null;
        toolBar = null;
    }

    /** Colours, typeface and borders; the values are those of {@code ToolBar.*} in Metal. */
    protected void installDefaults() {
        Color background = toolBar.getBackground();
        if (background == null || background instanceof UIResource) {
            toolBar.setBackground(BACKGROUND);
        }
        Color foreground = toolBar.getForeground();
        if (foreground == null || foreground instanceof UIResource) {
            toolBar.setForeground(FOREGROUND);
        }
        Font font = toolBar.getFont();
        if (font == null || font instanceof UIResource) {
            toolBar.setFont(FONT);
        }
        LookAndFeel.installProperty(toolBar, "opaque", Boolean.TRUE);
        dockingColor = BACKGROUND;
        floatingColor = BACKGROUND;
        dockingBorderColor = DOCK_BORDER;
        floatingBorderColor = FLOATING_BORDER;
        rolloverBorders = true;
        setRolloverBorders(rolloverBorders);
    }

    /** It removes nothing; see {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    /** Nothing: the buttons are put in by the program. */
    protected void installComponents() {
    }

    protected void uninstallComponents() {
    }

    protected void installListeners() {
        dockingListener = createDockingListener();
        if (dockingListener != null) {
            toolBar.addMouseListener(dockingListener);
            toolBar.addMouseMotionListener(dockingListener);
        }
        propertyListener = createPropertyListener();
        if (propertyListener != null) {
            toolBar.addPropertyChangeListener(propertyListener);
        }
        toolBarContListener = createToolBarContListener();
        if (toolBarContListener != null) {
            toolBar.addContainerListener(toolBarContListener);
        }
        toolBarFocusListener = createToolBarFocusListener();
        if (toolBarFocusListener != null) {
            for (int i = 0; i < toolBar.getComponentCount(); i++) {
                toolBar.getComponent(i).addFocusListener(toolBarFocusListener);
            }
        }
    }

    protected void uninstallListeners() {
        if (dockingListener != null) {
            toolBar.removeMouseListener(dockingListener);
            toolBar.removeMouseMotionListener(dockingListener);
        }
        if (propertyListener != null) {
            toolBar.removePropertyChangeListener(propertyListener);
        }
        if (toolBarContListener != null) {
            toolBar.removeContainerListener(toolBarContListener);
        }
        if (toolBarFocusListener != null) {
            for (int i = 0; i < toolBar.getComponentCount(); i++) {
                toolBar.getComponent(i).removeFocusListener(toolBarFocusListener);
            }
        }
        dockingListener = null;
        propertyListener = null;
        toolBarContListener = null;
        toolBarFocusListener = null;
    }

    /** With no shortcuts of its own; see the class note about the four keys. */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    protected MouseInputListener createDockingListener() {
        return new Handler(this);
    }

    protected PropertyChangeListener createPropertyListener() {
        return new Handler(this);
    }

    protected ContainerListener createToolBarContListener() {
        return new Handler(this);
    }

    protected FocusListener createToolBarFocusListener() {
        return new Handler(this);
    }

    protected WindowListener createFrameListener() {
        return new Handler(this);
    }

    /** The border that is seen when the mouse passes over; see the class note. */
    protected Border createRolloverBorder() {
        return new CompoundBorder(
                new javax.swing.border.EtchedBorder(),
                new BasicBorders.MarginBorder());
    }

    /** And the one that is seen when it does not. */
    protected Border createNonRolloverBorder() {
        return new CompoundBorder(
                new javax.swing.border.EmptyBorder(2, 2, 2, 2),
                new BasicBorders.MarginBorder());
    }

    /** Always the same object; see the class note. */
    protected Border getRolloverBorder(AbstractButton b) {
        if (rolloverBorder == null) {
            rolloverBorder = createRolloverBorder();
        }
        return rolloverBorder;
    }

    /** The same. */
    protected Border getNonRolloverBorder(AbstractButton b) {
        if (nonRolloverBorder == null) {
            nonRolloverBorder = createNonRolloverBorder();
        }
        return nonRolloverBorder;
    }

    public boolean isRolloverBorders() {
        return rolloverBorders;
    }

    /** It switches the buttons from one set of borders to the other. */
    public void setRolloverBorders(boolean rollover) {
        rolloverBorders = rollover;
        if (toolBar == null) {
            return;
        }
        if (rolloverBorders) {
            installRolloverBorders(toolBar);
        } else {
            installNonRolloverBorders(toolBar);
        }
    }

    protected void installRolloverBorders(JComponent c) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component comp = c.getComponent(i);
            if (comp instanceof JComponent) {
                installRolloverBorders((JComponent) comp);
                setBorderToRollover(comp);
            }
        }
    }

    protected void installNonRolloverBorders(JComponent c) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component comp = c.getComponent(i);
            if (comp instanceof JComponent) {
                installNonRolloverBorders((JComponent) comp);
                setBorderToNonRollover(comp);
            }
        }
    }

    /** It gives the buttons back the border they came with. */
    protected void installNormalBorders(JComponent c) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component comp = c.getComponent(i);
            if (comp instanceof JComponent) {
                installNormalBorders((JComponent) comp);
                setBorderToNormal(comp);
            }
        }
    }

    /**
     * It gives the button the mouse-over border.
     *
     * <p>Only if the one it has was set by a look and feel. That has a consequence that is
     * surprising and is measured: <strong>once changed, there is no going back</strong>. The
     * border this method sets is not a {@link UIResource}, so the next call -- to this one or to
     * {@link #setBorderToNonRollover} -- no longer touches it. Changing the set of borders on the
     * fly works only once per button.
     */
    protected void setBorderToRollover(Component c) {
        if (c instanceof AbstractButton) {
            AbstractButton b = (AbstractButton) c;
            if (b.getBorder() instanceof UIResource) {
                b.setBorder(getRolloverBorder(b));
            }
            b.setRolloverEnabled(true);
        }
    }

    /** The same the other way round; see {@link #setBorderToRollover}. */
    protected void setBorderToNonRollover(Component c) {
        if (c instanceof AbstractButton) {
            AbstractButton b = (AbstractButton) c;
            if (b.getBorder() instanceof UIResource) {
                b.setBorder(getNonRolloverBorder(b));
            }
            b.setRolloverEnabled(false);
        }
    }

    protected void setBorderToNormal(Component c) {
        if (c instanceof AbstractButton) {
            AbstractButton b = (AbstractButton) c;
            if (b.getBorder() == rolloverBorder || b.getBorder() == nonRolloverBorder) {
                b.setBorder(null);
            }
        }
    }

    public Color getDockingColor() {
        return dockingColor;
    }

    public void setDockingColor(Color c) {
        this.dockingColor = c;
    }

    public Color getFloatingColor() {
        return floatingColor;
    }

    public void setFloatingColor(Color c) {
        this.floatingColor = c;
    }

    public boolean isFloating() {
        return floating;
    }

    /** It takes the bar out to float or brings it back; see the class note. */
    public void setFloating(boolean b, Point p) {
        if (toolBar.isFloatable()) {
            floating = b;
        }
    }

    public void setFloatingLocation(int x, int y) {
    }

    /** It changes the bar's axis. */
    public void setOrientation(int orientation) {
        toolBar.setOrientation(orientation);
        if (dragWindow != null) {
            dragWindow.setOrientation(orientation);
        }
    }

    /**
     * Whether the bar can be docked in that component and at that point.
     *
     * @throws NullPointerException if the component is null; the JDK does not check it either
     */
    public boolean canDock(Component c, Point p) {
        return p != null && c.contains(p);
    }

    /** The little window the bar floats in; see the class note. */
    protected JFrame createFloatingFrame(JToolBar toolbar) {
        return new JFrame(toolbar.getName());
    }

    /** The same, when the look and feel prefers a dialog. */
    protected RootPaneContainer createFloatingWindow(JToolBar toolbar) {
        return createFloatingFrame(toolbar);
    }

    protected DragWindow createDragWindow(JToolBar toolbar) {
        return new DragWindow(this);
    }

    /** It moves the drag rectangle. */
    protected void dragTo(Point position, Point origin) {
        if (!toolBar.isFloatable()) {
            return;
        }
        if (dragWindow == null) {
            dragWindow = createDragWindow(toolBar);
        }
        dragWindow.setOrientation(toolBar.getOrientation());
        dragWindow.setBorderColor(dockingBorderColor);
    }

    /** It drops the bar wherever it is; see the class note. */
    protected void floatAt(Point position, Point origin) {
        if (toolBar.isFloatable()) {
            setFloating(true, position);
        }
    }

    /** The drag's rectangle. */
    protected void paintDragWindow(Graphics g) {
        g.setColor(dragWindow.getBorderColor());
        int w = dragWindow.getWidth();
        int h = dragWindow.getHeight();
        g.drawRect(0, 0, w - 1, h - 1);
    }

    /** It moves the focus to the next button or to the previous one. */
    protected void navigateFocusedComp(int direction) {
        int nComp = toolBar.getComponentCount();
        if (focusedCompIndex < 0 || focusedCompIndex >= nComp) {
            return;
        }
        int paso = (direction == EAST || direction == SOUTH) ? 1 : -1;
        for (int i = 1; i < nComp; i++) {
            int j = ((focusedCompIndex + paso * i) % nComp + nComp) % nComp;
            Component c = toolBar.getComponent(j);
            if (c != null && c.isFocusable() && c.isEnabled()) {
                c.requestFocus();
                return;
            }
        }
    }

    /**
     * The rectangle that is seen while the bar is dragged.
     *
     * <p>It is a window of its own in the JDK. Here it is a loose component: with no screen there
     * is no window to show, and the only thing that can be tested of it is its colour and its
     * orientation.
     */
    protected class DragWindow extends java.awt.Window {

        private Color borderColor;
        private int orientation;

        DragWindow(BasicToolBarUI ui) {
            super(new java.awt.Frame());
            this.orientation = SwingConstants.HORIZONTAL;
        }

        public void setOrientation(int o) {
            this.orientation = o;
        }

        public int getOrientation() {
            return orientation;
        }

        public Color getBorderColor() {
            return borderColor;
        }

        public void setBorderColor(Color c) {
            this.borderColor = c;
        }

        public void setOffset(Point p) {
        }

        public Point getOffset() {
            return new Point(0, 0);
        }
    }

    /** The four sides' names, without depending on {@code java.awt.BorderLayout}. */
    private static final class NorthBorderLayout {
        static final String NORTH = "North";

        private NorthBorderLayout() {
        }
    }

    /**
     * The one that listens to everything: the dragging, the bar's changes, and the buttons that
     * come and go.
     *
     * <p>Static and with the look and feel as a field, for the same reason as everywhere in the
     * package; see finding #518.
     */
    private static class Handler implements MouseInputListener, PropertyChangeListener,
            ContainerListener, FocusListener, WindowListener {

        private final BasicToolBarUI ui;
        private Point origin;

        Handler(BasicToolBarUI ui) {
            this.ui = ui;
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            if (!ui.toolBar.isEnabled() || !ui.toolBar.isFloatable()) {
                return;
            }
            origin = e.getPoint();
        }

        public void mouseReleased(MouseEvent e) {
            if (origin == null) {
                return;
            }
            ui.floatAt(e.getPoint(), origin);
            origin = null;
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseDragged(MouseEvent e) {
            if (origin != null) {
                ui.dragTo(e.getPoint(), origin);
            }
        }

        public void mouseMoved(MouseEvent e) {
        }

        public void propertyChange(PropertyChangeEvent e) {
            String name = e.getPropertyName();
            if ("rollover".equals(name)) {
                ui.setRolloverBorders(Boolean.TRUE.equals(e.getNewValue()));
            } else if ("orientation".equals(name) && ui.dragWindow != null) {
                ui.dragWindow.setOrientation(ui.toolBar.getOrientation());
            }
        }

        public void componentAdded(ContainerEvent e) {
            Component c = e.getChild();
            if (ui.toolBarFocusListener != null) {
                c.addFocusListener(ui.toolBarFocusListener);
            }
            if (ui.isRolloverBorders()) {
                ui.setBorderToRollover(c);
            } else {
                ui.setBorderToNonRollover(c);
            }
        }

        public void componentRemoved(ContainerEvent e) {
            Component c = e.getChild();
            if (ui.toolBarFocusListener != null) {
                c.removeFocusListener(ui.toolBarFocusListener);
            }
            ui.setBorderToNormal(c);
        }

        public void focusGained(FocusEvent e) {
            Component c = e.getComponent();
            for (int i = 0; i < ui.toolBar.getComponentCount(); i++) {
                if (ui.toolBar.getComponent(i) == c) {
                    ui.focusedCompIndex = i;
                    return;
                }
            }
        }

        public void focusLost(FocusEvent e) {
        }

        public void windowOpened(WindowEvent e) {
        }

        public void windowClosing(WindowEvent e) {
            ui.setFloating(false, null);
        }

        public void windowClosed(WindowEvent e) {
        }

        public void windowIconified(WindowEvent e) {
        }

        public void windowDeiconified(WindowEvent e) {
        }

        public void windowActivated(WindowEvent e) {
        }

        public void windowDeactivated(WindowEvent e) {
        }
    }
}
