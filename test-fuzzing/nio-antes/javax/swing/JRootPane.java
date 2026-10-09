package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.LayoutManager2;
import java.io.Serializable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.RootPaneUI;

/**
 * The pane every Swing window has inside.
 *
 * <h2>Four stacked pieces</h2>
 *
 * <p>From the bottom up: the <em>layered pane</em>, which holds everything; inside it, the
 * <em>menu bar</em> and the <em>content pane</em>, one beside the other; and on top of
 * everything, the <em>glass</em>, transparent and catching the mouse.
 *
 * <p>It is the reason components are not added to a Swing window directly but to its
 * {@code getContentPane()}. Adding them to the window would put them beside the root pane, not
 * inside it.
 *
 * <h2>Why the glass</h2>
 *
 * <p>A component on top of everything, normally invisible, serves to cover the window while it
 * loads, to draw on top without touching anything, or to catch the mouse during a drag.
 * Without it, each of those things would force every component below to be touched.
 *
 * <h2>The default button</h2>
 *
 * <p>{@link #setDefaultButton} is the one that answers Enter. It lives here and not in the
 * window because it is a property of the content: changing pane changes which the main button
 * is.
 */
public class JRootPane extends JComponent implements Accessible {

    private static final String uiClassID = "RootPaneUI";

    /** With no decoration of its own: the system draws it. */
    public static final int NONE = 0;

    /** An ordinary window's decoration. */
    public static final int FRAME = 1;

    /** A dialog's decoration. */
    public static final int PLAIN_DIALOG = 2;

    /** An information dialog's decoration. */
    public static final int INFORMATION_DIALOG = 3;

    /** An error dialog's decoration. */
    public static final int ERROR_DIALOG = 4;

    /** The colour chooser's decoration. */
    public static final int COLOR_CHOOSER_DIALOG = 5;

    /** The file chooser's decoration. */
    public static final int FILE_CHOOSER_DIALOG = 6;

    /** A question dialog's decoration. */
    public static final int QUESTION_DIALOG = 7;

    /** A warning dialog's decoration. */
    public static final int WARNING_DIALOG = 8;

    /** The menu bar. */
    protected JMenuBar menuBar;

    /** Where what the program adds goes. */
    protected Container contentPane;

    /** The one that holds all the others. */
    protected JLayeredPane layeredPane;

    /** The one on top of everything; see the class note. */
    protected Component glassPane;

    /** The button that answers Enter. */
    protected JButton defaultButton;

    private int windowDecorationStyle = NONE;
    private AccessibleContext accessibleContext;

    /** A root pane with its four pieces built. */
    public JRootPane() {
        setGlassPane(createGlassPane());
        setLayeredPane(createLayeredPane());
        setContentPane(createContentPane());
        setLayout(createRootLayout());
        setDoubleBuffered(true);
        updateUI();
    }

    public void setDoubleBuffered(boolean aFlag) {
        super.setDoubleBuffered(aFlag);
    }

    public int getWindowDecorationStyle() {
        return windowDecorationStyle;
    }

    /**
     * Whether Swing draws the window's frame instead of the system.
     *
     * @throws IllegalArgumentException if the value is not one of the nine.
     */
    public void setWindowDecorationStyle(int windowDecorationStyle) {
        if (windowDecorationStyle < 0 || windowDecorationStyle > WARNING_DIALOG) {
            throw new IllegalArgumentException("Invalid decoration style");
        }
        int oldWindowDecorationStyle = getWindowDecorationStyle();
        this.windowDecorationStyle = windowDecorationStyle;
        firePropertyChange("windowDecorationStyle", oldWindowDecorationStyle,
                windowDecorationStyle);
    }

    public RootPaneUI getUI() {
        return (RootPaneUI) ui;
    }

    public void setUI(RootPaneUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** The usual layered pane, with the content in the lowest layer. */
    protected JLayeredPane createLayeredPane() {
        JLayeredPane p = new JLayeredPane();
        p.setName(this.getName() + ".layeredPane");
        return p;
    }

    /** The usual content pane: opaque and with a border layout. */
    protected Container createContentPane() {
        JComponent c = new JPanel();
        c.setName(this.getName() + ".contentPane");
        c.setLayout(new java.awt.BorderLayout());
        return c;
    }

    /** The usual glass: transparent and invisible. */
    protected Component createGlassPane() {
        JComponent c = new JPanel();
        c.setName(this.getName() + ".glassPane");
        c.setVisible(false);
        ((JPanel) c).setOpaque(false);
        return c;
    }

    /** The layout that stacks the four pieces; see the class note. */
    protected LayoutManager createRootLayout() {
        return new RootLayout(this);
    }

    /** The menu bar; it goes inside the layered pane, above the content. */
    public void setJMenuBar(JMenuBar menu) {
        if (menuBar != null && menuBar.getParent() == layeredPane) {
            layeredPane.remove(menuBar);
        }
        menuBar = menu;
        if (menuBar != null) {
            layeredPane.add(menuBar, JLayeredPane.FRAME_CONTENT_LAYER);
        }
    }

    /**
     * The menu bar.
     *
     * @deprecated Use {@link #setJMenuBar}.
     */
    @Deprecated
    public void setMenuBar(JMenuBar menu) {
        setJMenuBar(menu);
    }

    public JMenuBar getJMenuBar() {
        return menuBar;
    }

    /**
     * The menu bar.
     *
     * @deprecated Use {@link #getJMenuBar}.
     */
    @Deprecated
    public JMenuBar getMenuBar() {
        return menuBar;
    }

    /**
     * Where what the program adds goes.
     *
     * @throws IllegalComponentStateException if it is null.
     */
    public void setContentPane(Container content) {
        if (content == null) {
            throw new java.awt.IllegalComponentStateException(
                    "contentPane cannot be set to null.");
        }
        if (contentPane != null && contentPane.getParent() == layeredPane) {
            layeredPane.remove(contentPane);
        }
        contentPane = content;
        layeredPane.add(contentPane, JLayeredPane.FRAME_CONTENT_LAYER);
    }

    public Container getContentPane() {
        return contentPane;
    }

    /**
     * The layered pane.
     *
     * @throws IllegalComponentStateException if it is null.
     */
    public void setLayeredPane(JLayeredPane layered) {
        if (layered == null) {
            throw new java.awt.IllegalComponentStateException(
                    "layeredPane cannot be set to null.");
        }
        if (layeredPane != null && layeredPane.getParent() == this) {
            this.remove(layeredPane);
        }
        layeredPane = layered;
        this.add(layeredPane, -1);
    }

    public JLayeredPane getLayeredPane() {
        return layeredPane;
    }

    /**
     * The glass on top.
     *
     * <p>It is kept if it was visible: replacing it while it covers the window should not uncover
     * what is below.
     *
     * @throws NullPointerException if it is null.
     */
    public void setGlassPane(Component glass) {
        if (glass == null) {
            throw new NullPointerException("glassPane cannot be set to null.");
        }
        boolean visible = false;
        if (glassPane != null && glassPane.getParent() == this) {
            this.remove(glassPane);
            visible = glassPane.isVisible();
        }
        glass.setVisible(visible);
        glassPane = glass;
        this.add(glassPane, 0);
    }

    public Component getGlassPane() {
        return glassPane;
    }

    /**
     * Always true.
     *
     * <p>It is the point where the laying out stops going up: whatever happens inside a window
     * does not change the window's size. Without this cut, typing a letter in a field would lay
     * everything out as far as the root.
     */
    public boolean isValidateRoot() {
        return true;
    }

    /** False: the glass is on top of the content, by definition they overlap. */
    public boolean isOptimizedDrawingEnabled() {
        return !glassPane.isVisible();
    }

    public void addNotify() {
        super.addNotify();
    }

    public void removeNotify() {
        super.removeNotify();
    }

    /** The button that answers Enter; see the class note. */
    public void setDefaultButton(JButton defaultButton) {
        JButton oldDefault = this.defaultButton;
        if (oldDefault != defaultButton) {
            this.defaultButton = defaultButton;
            firePropertyChange("defaultButton", oldDefault, defaultButton);
        }
    }

    public JButton getDefaultButton() {
        return defaultButton;
    }

    /**
     * It adds a child; the glass always stays first.
     *
     * <p>First in the list is on top on the screen. Without this rule, putting the layered pane
     * after the glass would cover it.
     */
    protected void addImpl(Component comp, Object constraints, int index) {
        super.addImpl(comp, constraints, index);
        if (glassPane != null && glassPane.getParent() == this
                && getComponent(0) != glassPane) {
            add(glassPane, 0);
        }
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    /**
     * It stacks the glass, the layered pane, the bar and the content.
     *
     * <p>It is not an ordinary layout: the four do not go one beside the other but some inside
     * others and with the bar above the content. No usual layout does that.
     */
    static class RootLayout implements LayoutManager2, Serializable {

        private final JRootPane root;

        RootLayout(JRootPane root) {
            this.root = root;
        }

        public Dimension preferredLayoutSize(Container parent) {
            Dimension rd;
            Dimension mbd;
            Insets i = root.getInsets();
            if (root.contentPane != null) {
                rd = root.contentPane.getPreferredSize();
            } else {
                rd = parent.getSize();
            }
            if (root.menuBar != null && root.menuBar.isVisible()) {
                mbd = root.menuBar.getPreferredSize();
            } else {
                mbd = new Dimension(0, 0);
            }
            return new Dimension(Math.max(rd.width, mbd.width) + i.left + i.right,
                    rd.height + mbd.height + i.top + i.bottom);
        }

        public Dimension minimumLayoutSize(Container parent) {
            Dimension rd;
            Dimension mbd;
            Insets i = root.getInsets();
            if (root.contentPane != null) {
                rd = root.contentPane.getMinimumSize();
            } else {
                rd = parent.getSize();
            }
            if (root.menuBar != null && root.menuBar.isVisible()) {
                mbd = root.menuBar.getMinimumSize();
            } else {
                mbd = new Dimension(0, 0);
            }
            return new Dimension(Math.max(rd.width, mbd.width) + i.left + i.right,
                    rd.height + mbd.height + i.top + i.bottom);
        }

        public Dimension maximumLayoutSize(Container target) {
            Dimension rd;
            Dimension mbd;
            Insets i = root.getInsets();
            if (root.menuBar != null && root.menuBar.isVisible()) {
                mbd = root.menuBar.getMaximumSize();
            } else {
                mbd = new Dimension(0, 0);
            }
            if (root.contentPane != null) {
                rd = root.contentPane.getMaximumSize();
            } else {
                rd = new Dimension(Integer.MAX_VALUE,
                        Integer.MAX_VALUE - i.top - i.bottom - mbd.height - 1);
            }
            return new Dimension(Math.min(rd.width, mbd.width) + i.left + i.right,
                    rd.height + mbd.height + i.top + i.bottom);
        }

        public void layoutContainer(Container parent) {
            Insets i = root.getInsets();
            int w = parent.getWidth() - i.right - i.left;
            int h = parent.getHeight() - i.top - i.bottom;

            if (root.layeredPane != null) {
                root.layeredPane.setBounds(i.left, i.top, w, h);
            }
            if (root.glassPane != null) {
                root.glassPane.setBounds(i.left, i.top, w, h);
            }
            // The bar and the content go inside the layered pane, in its own coordinate system:
                        // that is why they start at zero and not at the margin.
            int contentY = 0;
            if (root.menuBar != null && root.menuBar.isVisible()) {
                Dimension mbd = root.menuBar.getPreferredSize();
                root.menuBar.setBounds(0, 0, w, mbd.height);
                contentY = mbd.height;
            }
            if (root.contentPane != null) {
                root.contentPane.setBounds(0, contentY, w, h - contentY);
            }
        }

        public void addLayoutComponent(String name, Component comp) {
        }

        public void removeLayoutComponent(Component comp) {
        }

        public void addLayoutComponent(Component comp, Object constraints) {
        }

        public float getLayoutAlignmentX(Container target) {
            return 0.0f;
        }

        public float getLayoutAlignmentY(Container target) {
            return 0.0f;
        }

        public void invalidateLayout(Container target) {
        }
    }
}
