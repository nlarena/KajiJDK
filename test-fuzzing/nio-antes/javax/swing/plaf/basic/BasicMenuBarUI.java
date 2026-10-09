package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.LookAndFeel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.MenuBarUI;
import javax.swing.plaf.UIResource;

/**
 * The basic look and feel of a menu bar.
 *
 * <h2>The only thing of its own is the layout</h2>
 *
 * <p>A menu bar draws nothing: what is seen are its menus. What it does do is give it
 * {@link DefaultMenuLayout} on the horizontal axis, and that is not a detail -- it is what makes
 * the menus end up stuck to the left one after the other and not spread out, which is what a
 * {@code FlowLayout} would do.
 *
 * <h2>The three sizes are {@code null}</h2>
 *
 * <p>Preferred, minimum and maximum: all three. The bar lets the layout answer, which is the
 * only one that knows how much the menus it holds measure. It is measured, and it is different
 * from almost every other look and feel, which at least answer the preferred one.
 */
public class BasicMenuBarUI extends MenuBarUI {

    protected JMenuBar menuBar = null;
    protected ContainerListener containerListener;
    protected ChangeListener changeListener;

    private static final ColorUIResource BACKGROUND = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FOREGROUND = new ColorUIResource(51, 51, 51);
    private static final FontUIResource FONT = new FontUIResource("Dialog", Font.BOLD, 12);

    public BasicMenuBarUI() {
    }

    /** A new one per bar: it keeps the component and its listeners. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicMenuBarUI();
    }

    public void installUI(JComponent c) {
        menuBar = (JMenuBar) c;
        installDefaults();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        uninstallDefaults();
        uninstallListeners();
        uninstallKeyboardActions();
        menuBar = null;
    }

    /** Colours, typeface and the layout; see the class note. */
    protected void installDefaults() {
        if (menuBar.getLayout() == null || menuBar.getLayout() instanceof UIResource) {
            menuBar.setLayout(new DefaultMenuLayout(menuBar, BoxLayout.LINE_AXIS));
        }
        Color background = menuBar.getBackground();
        if (background == null || background instanceof UIResource) {
            menuBar.setBackground(BACKGROUND);
        }
        Color foreground = menuBar.getForeground();
        if (foreground == null || foreground instanceof UIResource) {
            menuBar.setForeground(FOREGROUND);
        }
        Font font = menuBar.getFont();
        if (font == null || font instanceof UIResource) {
            menuBar.setFont(FONT);
        }
        if (menuBar.getBorder() == null || menuBar.getBorder() instanceof UIResource) {
            // Two pixels at the bottom and nothing else: the line that separates the bar from the
            // content.
            menuBar.setBorder(new BasicBorders.MenuBarBorder(FOREGROUND, BACKGROUND));
        }
        LookAndFeel.installProperty(menuBar, "opaque", Boolean.TRUE);
    }

    /** It removes nothing; see {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    protected void installListeners() {
        containerListener = createContainerListener();
        changeListener = createChangeListener();
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            javax.swing.JMenu menu = menuBar.getMenu(i);
            if (menu != null) {
                menu.getModel().addChangeListener(changeListener);
            }
        }
        menuBar.addContainerListener(containerListener);
    }

    protected void uninstallListeners() {
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            javax.swing.JMenu menu = menuBar.getMenu(i);
            if (menu != null) {
                menu.getModel().removeChangeListener(changeListener);
            }
        }
        menuBar.removeContainerListener(containerListener);
        containerListener = null;
        changeListener = null;
    }

    /** With no shortcuts of its own: each menu's underlined letter is handled by the menu. */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    /** The one that hooks and unhooks the change listener of the menus that come and go. */
    protected ContainerListener createContainerListener() {
        return new ContainerListenerImpl();
    }

    protected ChangeListener createChangeListener() {
        return new ChangeListenerImpl();
    }

    /** {@code null}; see the class note. */
    public Dimension getMinimumSize(JComponent c) {
        return null;
    }

    /** {@code null}; see the class note. */
    public Dimension getMaximumSize(JComponent c) {
        return null;
    }

    private class ContainerListenerImpl implements ContainerListener {

        public void componentAdded(ContainerEvent e) {
            java.awt.Component c = e.getChild();
            if (c instanceof javax.swing.JMenu) {
                ((javax.swing.JMenu) c).getModel().addChangeListener(changeListener);
            }
        }

        public void componentRemoved(ContainerEvent e) {
            java.awt.Component c = e.getChild();
            if (c instanceof javax.swing.JMenu) {
                ((javax.swing.JMenu) c).getModel().removeChangeListener(changeListener);
            }
        }
    }

    private class ChangeListenerImpl implements ChangeListener {

        public void stateChanged(ChangeEvent e) {
            if (menuBar != null) {
                menuBar.repaint();
            }
        }
    }
}
