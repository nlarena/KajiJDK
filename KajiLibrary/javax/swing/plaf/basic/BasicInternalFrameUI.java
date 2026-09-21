package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;

import javax.swing.DefaultDesktopManager;
import javax.swing.DesktopManager;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.InternalFrameUI;
import javax.swing.plaf.UIResource;

/**
 * The basic look and feel of an internal frame.
 *
 * <h2>Four panes around the content, and only one exists</h2>
 *
 * <p>The frame has room for a pane on each side -- north, south, east, west -- and the basic
 * one only puts in the north one: the title bar. The other three are left {@code null} and are
 * there for the look and feel that wants a status bar at the bottom or a ruler at the side. It
 * is measured.
 *
 * <h2>Who moves the frame is not the frame</h2>
 *
 * <p>Moving, maximizing, iconifying and closing are done by the desktop's
 * {@link DesktopManager}, not by this look and feel: {@link #getDesktopManager} looks it up in
 * the {@link JDesktopPane} that contains it and, if there is none -- a loose internal frame --,
 * it builds one of its own. That is what makes two frames on the same desktop behave the same
 * even though they have different looks and feels.
 *
 * <h2>The JDK's stack overflow</h2>
 *
 * <p><strong>A loose internal frame's {@code getPreferredSize} overflows the stack in the
 * JDK.</strong> It is measured: {@code StackOverflowError}, not an exception. The layout asks
 * for the frame's preferred size, which asks the layout for it again. Here it is not copied
 * -- copying a stack overflow is of use to nobody --: the layout measures the root pane and the
 * title bar, which is what the arithmetic meant to say. The minimum and the maximum do agree.
 *
 * <p>For a component that is not its frame, all three answer fixed numbers: 100 x 100 the
 * preferred one, zero the minimum, infinite the maximum.
 *
 * <h2>What is left said</h2>
 *
 * <p>Dragging the border in order to resize needs the system cursor: the listener is there and
 * notes which side was pressed, but it does not change the size. And {@link #openMenuKey} is
 * left {@code null}, like the package's other key fields.
 */
public class BasicInternalFrameUI extends InternalFrameUI {

    protected JInternalFrame frame;
    protected MouseInputAdapter borderListener;
    protected PropertyChangeListener propertyChangeListener;
    protected LayoutManager internalFrameLayout;
    protected ComponentListener componentListener;
    protected MouseInputListener glassPaneDispatcher;

    protected JComponent northPane;
    protected JComponent southPane;
    protected JComponent westPane;
    protected JComponent eastPane;

    protected BasicInternalFrameTitlePane titlePane;

    /** Unused; see the class note. */
    protected KeyStroke openMenuKey;

    private boolean keyBindingRegistered;
    private boolean keyBindingActive;
    private DesktopManager sharedDesktopManager;

    private static final ColorUIResource BACKGROUND = new ColorUIResource(238, 238, 238);

    /** For that frame. */
    public BasicInternalFrameUI(JInternalFrame b) {
    }

    /** A new one per frame: it keeps its four panes and its bar. */
    public static ComponentUI createUI(JComponent b) {
        return new BasicInternalFrameUI((JInternalFrame) b);
    }

    public void installUI(JComponent c) {
        frame = (JInternalFrame) c;
        installDefaults();
        installListeners();
        installComponents();
        installKeyboardActions();
        frame.setOpaque(true);
    }

    public void uninstallUI(JComponent c) {
        if (c != frame) {
            throw new IllegalArgumentException(c + " is not this look and feel's frame");
        }
        uninstallKeyboardActions();
        uninstallComponents();
        uninstallListeners();
        uninstallDefaults();
        frame = null;
    }

    /** Colours and layout; the values are those of {@code InternalFrame.*} in Metal. */
    protected void installDefaults() {
        Color background = frame.getBackground();
        if (background == null || background instanceof UIResource) {
            frame.setBackground(BACKGROUND);
        }
        javax.swing.border.Border b = frame.getBorder();
        if (b == null || b instanceof UIResource) {
            frame.setBorder(BasicBorders.getInternalFrameBorder());
        }
        LookAndFeel.installProperty(frame, "opaque", Boolean.TRUE);
        internalFrameLayout = createLayoutManager();
        frame.setLayout(internalFrameLayout);
    }

    /** It removes the layout and the border this look and feel set. */
    protected void uninstallDefaults() {
        if (frame.getLayout() == internalFrameLayout) {
            frame.setLayout(null);
        }
        LookAndFeel.uninstallBorder(frame);
        internalFrameLayout = null;
    }

    /** It puts the title bar in as the north pane; see the class note. */
    protected void installComponents() {
        setNorthPane(createNorthPane(frame));
        setSouthPane(createSouthPane(frame));
        setEastPane(createEastPane(frame));
        setWestPane(createWestPane(frame));
    }

    protected void uninstallComponents() {
        setNorthPane(null);
        setSouthPane(null);
        setEastPane(null);
        setWestPane(null);
        titlePane = null;
    }

    protected void installListeners() {
        borderListener = createBorderListener(frame);
        propertyChangeListener = createPropertyChangeListener();
        frame.addPropertyChangeListener(propertyChangeListener);
        componentListener = createComponentListener();
        if (frame.getParent() != null) {
            frame.getParent().addComponentListener(componentListener);
        }
        installMouseHandlers(frame);
    }

    protected void uninstallListeners() {
        if (componentListener != null && frame.getParent() != null) {
            frame.getParent().removeComponentListener(componentListener);
        }
        deinstallMouseHandlers(frame);
        frame.removePropertyChangeListener(propertyChangeListener);
        propertyChangeListener = null;
        borderListener = null;
        componentListener = null;
        glassPaneDispatcher = null;
    }

    /** With no shortcuts of its own; see the class note about {@link #openMenuKey}. */
    protected void installKeyboardActions() {
        setupMenuOpenKey();
        setupMenuCloseKey();
    }

    protected void uninstallKeyboardActions() {
    }

    /**
     * A hook for the look and feel that wants to tie a key to the system menu; the basic one ties
     * none.
     */
    protected void setupMenuOpenKey() {
    }

    /** The same for closing it. */
    protected void setupMenuCloseKey() {
    }

    protected final boolean isKeyBindingRegistered() {
        return keyBindingRegistered;
    }

    protected final void setKeyBindingRegistered(boolean b) {
        keyBindingRegistered = b;
    }

    public final boolean isKeyBindingActive() {
        return keyBindingActive;
    }

    protected final void setKeyBindingActive(boolean b) {
        keyBindingActive = b;
    }

    protected LayoutManager createLayoutManager() {
        return new Handler(this);
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new Handler(this);
    }

    protected ComponentListener createComponentListener() {
        return new Handler(this);
    }

    protected MouseInputAdapter createBorderListener(JInternalFrame w) {
        return new BorderListener(this);
    }

    /**
     * The one that hands out the glass pane's events.
     *
     * <p>It returns one, but {@link #installComponents} does not keep it: the field
     * {@link #glassPaneDispatcher} is left {@code null} after installing. It is measured, and it
     * makes sense -- with no real window there is no glass to dispatch --.
     */
    protected MouseInputListener createGlassPaneDispatcher() {
        return new BorderListener(this);
    }

    /** A hook: the basic one builds no internal frame listener. */
    protected void createInternalFrameListener() {
    }

    protected void installMouseHandlers(JComponent c) {
        if (borderListener != null) {
            c.addMouseListener(borderListener);
            c.addMouseMotionListener(borderListener);
        }
    }

    protected void deinstallMouseHandlers(JComponent c) {
        if (borderListener != null) {
            c.removeMouseListener(borderListener);
            c.removeMouseMotionListener(borderListener);
        }
    }

    /** The title bar. */
    protected JComponent createNorthPane(JInternalFrame w) {
        titlePane = new BasicInternalFrameTitlePane(w);
        return titlePane;
    }

    /** None; see the class note. */
    protected JComponent createSouthPane(JInternalFrame w) {
        return null;
    }

    /** None. */
    protected JComponent createWestPane(JInternalFrame w) {
        return null;
    }

    /** None. */
    protected JComponent createEastPane(JInternalFrame w) {
        return null;
    }

    public JComponent getNorthPane() {
        return northPane;
    }

    public void setNorthPane(JComponent c) {
        if (northPane != null && northPane instanceof BasicInternalFrameTitlePane) {
            ((BasicInternalFrameTitlePane) northPane).uninstallListeners();
        }
        replacePane(northPane, c);
        northPane = c;
        if (c instanceof BasicInternalFrameTitlePane) {
            titlePane = (BasicInternalFrameTitlePane) c;
        }
    }

    public JComponent getSouthPane() {
        return southPane;
    }

    public void setSouthPane(JComponent c) {
        replacePane(southPane, c);
        southPane = c;
    }

    public JComponent getWestPane() {
        return westPane;
    }

    public void setWestPane(JComponent c) {
        replacePane(westPane, c);
        westPane = c;
    }

    public JComponent getEastPane() {
        return eastPane;
    }

    public void setEastPane(JComponent c) {
        replacePane(eastPane, c);
        eastPane = c;
    }

    /**
     * It takes the old one out of the container and puts the new one in, with its mouse listeners.
     */
    protected void replacePane(JComponent currentPane, JComponent newPane) {
        if (currentPane != null) {
            deinstallMouseHandlers(currentPane);
            frame.remove(currentPane);
        }
        if (newPane != null) {
            frame.add(newPane);
            installMouseHandlers(newPane);
        }
    }

    /** The one of the desktop that contains it, or one of its own; see the class note. */
    protected DesktopManager getDesktopManager() {
        JDesktopPane pane = frame.getDesktopPane();
        if (pane != null && pane.getDesktopManager() != null) {
            return pane.getDesktopManager();
        }
        if (sharedDesktopManager == null) {
            sharedDesktopManager = createDesktopManager();
        }
        return sharedDesktopManager;
    }

    protected DesktopManager createDesktopManager() {
        return new DefaultDesktopManager();
    }

    /** The six operations, all delegated to the manager. */
    protected void closeFrame(JInternalFrame f) {
        getDesktopManager().closeFrame(f);
    }

    protected void maximizeFrame(JInternalFrame f) {
        getDesktopManager().maximizeFrame(f);
    }

    protected void minimizeFrame(JInternalFrame f) {
        getDesktopManager().minimizeFrame(f);
    }

    protected void iconifyFrame(JInternalFrame f) {
        getDesktopManager().iconifyFrame(f);
    }

    protected void deiconifyFrame(JInternalFrame f) {
        getDesktopManager().deiconifyFrame(f);
    }

    protected void activateFrame(JInternalFrame f) {
        getDesktopManager().activateFrame(f);
    }

    protected void deactivateFrame(JInternalFrame f) {
        getDesktopManager().deactivateFrame(f);
    }

    /** The layout's for its frame, and 100 x 100 for any other component. */
    public Dimension getPreferredSize(JComponent x) {
        if (frame == x && internalFrameLayout != null) {
            return internalFrameLayout.preferredLayoutSize(x);
        }
        return new Dimension(100, 100);
    }

    /** The layout's for its frame, and zero for any other. */
    public Dimension getMinimumSize(JComponent x) {
        if (frame == x && internalFrameLayout != null) {
            return internalFrameLayout.minimumLayoutSize(x);
        }
        return new Dimension(0, 0);
    }

    /** No cap, always. */
    public Dimension getMaximumSize(JComponent x) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * The one that lays the five places out and listens to the frame's changes.
     *
     * <p>The content goes in the centre and the four panes around it. It is a {@code BorderLayout}
     * written by hand, and it is written because the real one does not know that the frame's root
     * pane is the centre whatever happens.
     */
    private static class Handler implements LayoutManager, PropertyChangeListener,
            ComponentListener {

        private final BasicInternalFrameUI ui;

        Handler(BasicInternalFrameUI ui) {
            this.ui = ui;
        }

        public void addLayoutComponent(String name, Component c) {
        }

        public void removeLayoutComponent(Component c) {
        }

        /** See the class note about the JDK's stack overflow. */
        public Dimension preferredLayoutSize(Container c) {
            return measure(c, false);
        }

        public Dimension minimumLayoutSize(Container c) {
            return measure(c, true);
        }

        private Dimension measure(Container c, boolean min) {
            JInternalFrame frame = ui.frame;
            if (frame == null) {
                return new Dimension(0, 0);
            }
            Dimension d = new Dimension(0, 0);
            javax.swing.JRootPane root = frame.getRootPane();
            if (root != null) {
                Dimension r = min ? root.getMinimumSize() : root.getPreferredSize();
                d.width = r.width;
                d.height = r.height;
            }
            JComponent[] verticals = {ui.northPane, ui.southPane};
            for (int i = 0; i < verticals.length; i++) {
                if (verticals[i] != null) {
                    Dimension p = min ? verticals[i].getMinimumSize()
                            : verticals[i].getPreferredSize();
                    d.width = Math.max(d.width, p.width);
                    d.height += p.height;
                }
            }
            JComponent[] horizontals = {ui.westPane, ui.eastPane};
            for (int i = 0; i < horizontals.length; i++) {
                if (horizontals[i] != null) {
                    Dimension p = min ? horizontals[i].getMinimumSize()
                            : horizontals[i].getPreferredSize();
                    d.width += p.width;
                    d.height = Math.max(d.height, p.height);
                }
            }
            Insets in = frame.getInsets();
            d.width += in.left + in.right;
            d.height += in.top + in.bottom;
            return d;
        }

        public void layoutContainer(Container c) {
            JInternalFrame frame = ui.frame;
            if (frame == null) {
                return;
            }
            Insets in = frame.getInsets();
            int cx = in.left;
            int cy = in.top;
            int cw = frame.getWidth() - in.left - in.right;
            int ch = frame.getHeight() - in.top - in.bottom;

            if (ui.northPane != null) {
                Dimension d = ui.northPane.getPreferredSize();
                ui.northPane.setBounds(cx, cy, cw, d.height);
                cy += d.height;
                ch -= d.height;
            }
            if (ui.southPane != null) {
                Dimension d = ui.southPane.getPreferredSize();
                ui.southPane.setBounds(cx, cy + ch - d.height, cw, d.height);
                ch -= d.height;
            }
            if (ui.westPane != null) {
                Dimension d = ui.westPane.getPreferredSize();
                ui.westPane.setBounds(cx, cy, d.width, ch);
                cx += d.width;
                cw -= d.width;
            }
            if (ui.eastPane != null) {
                Dimension d = ui.eastPane.getPreferredSize();
                ui.eastPane.setBounds(cx + cw - d.width, cy, d.width, ch);
                cw -= d.width;
            }
            javax.swing.JRootPane root = frame.getRootPane();
            if (root != null) {
                root.setBounds(cx, cy, cw, ch);
            }
        }

        public void propertyChange(PropertyChangeEvent e) {
            JInternalFrame frame = ui.frame;
            if (frame == null) {
                return;
            }
            String prop = e.getPropertyName();
            if (JInternalFrame.IS_CLOSED_PROPERTY.equals(prop)) {
                if (Boolean.TRUE.equals(e.getNewValue())) {
                    ui.closeFrame(frame);
                }
            } else if (JInternalFrame.IS_MAXIMUM_PROPERTY.equals(prop)) {
                if (Boolean.TRUE.equals(e.getNewValue())) {
                    ui.maximizeFrame(frame);
                } else {
                    ui.minimizeFrame(frame);
                }
            } else if (JInternalFrame.IS_ICON_PROPERTY.equals(prop)) {
                if (Boolean.TRUE.equals(e.getNewValue())) {
                    ui.iconifyFrame(frame);
                } else {
                    ui.deiconifyFrame(frame);
                }
            } else if (JInternalFrame.IS_SELECTED_PROPERTY.equals(prop)) {
                if (Boolean.TRUE.equals(e.getNewValue())) {
                    ui.activateFrame(frame);
                } else {
                    ui.deactivateFrame(frame);
                }
            }
        }

        public void componentResized(ComponentEvent e) {
        }

        public void componentMoved(ComponentEvent e) {
        }

        public void componentShown(ComponentEvent e) {
        }

        public void componentHidden(ComponentEvent e) {
        }
    }

    /**
     * The one that notes which side of the border was pressed.
     *
     * <p>Really resizing needs the system cursor; see the class note.
     */
    private static class BorderListener extends MouseInputAdapter implements MouseInputListener {

        private final BasicInternalFrameUI ui;

        BorderListener(BasicInternalFrameUI ui) {
            this.ui = ui;
        }

        public void mousePressed(MouseEvent e) {
            JInternalFrame frame = ui.frame;
            if (frame == null || !frame.isSelected()) {
                if (frame != null) {
                    try {
                        frame.setSelected(true);
                    } catch (PropertyVetoException ex) {
                        // Somebody said no. It is a valid answer.
                    }
                }
            }
        }
    }
}
