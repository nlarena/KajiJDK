package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.PopupMenuUI;
import javax.swing.plaf.UIResource;

/**
 * The basic look and feel of a popup menu.
 *
 * <p>Like the bar, it draws nothing of its own: it sets the layout -- {@link DefaultMenuLayout}
 * on the vertical axis, so that the items end up one below the other -- and the colours, and
 * steps aside.
 *
 * <h2>What counts as "the right button"</h2>
 *
 * <p>{@link #isPopupTrigger} asks the event and nothing else. Which button and which moment
 * -- pressing or releasing -- opens a context menu is decided by the operating system, not by
 * Swing: on Windows it is releasing the right button and on X11 it is pressing it. Answering yes
 * to any right button would break that difference and the menu would open twice on one of the
 * two systems.
 *
 * <p>{@link #installDefaults} is public here and protected in almost every other look and feel.
 * It is an inconsistency of the JDK, and it is copied: a subclass from another package could be
 * calling it.
 */
public class BasicPopupMenuUI extends PopupMenuUI {

    protected JPopupMenu popupMenu = null;

    private static final ColorUIResource BACKGROUND = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FOREGROUND = new ColorUIResource(51, 51, 51);
    private static final FontUIResource FONT = new FontUIResource("Dialog", Font.BOLD, 12);

    public BasicPopupMenuUI() {
    }

    /** A new one per menu: it keeps the component. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicPopupMenuUI();
    }

    public void installUI(JComponent c) {
        popupMenu = (JPopupMenu) c;
        installDefaults();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        uninstallDefaults();
        uninstallListeners();
        uninstallKeyboardActions();
        popupMenu = null;
    }

    /** Colours, typeface and the vertical layout; see the class note. */
    public void installDefaults() {
        if (popupMenu.getLayout() == null || popupMenu.getLayout() instanceof UIResource) {
            popupMenu.setLayout(new DefaultMenuLayout(popupMenu, BoxLayout.Y_AXIS));
        }
        LookAndFeel.installProperty(popupMenu, "opaque", Boolean.TRUE);
        Color background = popupMenu.getBackground();
        if (background == null || background instanceof UIResource) {
            popupMenu.setBackground(BACKGROUND);
        }
        Color foreground = popupMenu.getForeground();
        if (foreground == null || foreground instanceof UIResource) {
            popupMenu.setForeground(FOREGROUND);
        }
        Font font = popupMenu.getFont();
        if (font == null || font instanceof UIResource) {
            popupMenu.setFont(FONT);
        }
    }

    /** It removes nothing; see {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    /** It listens to nothing: who opens and closes the menu is {@code MenuSelectionManager}. */
    protected void installListeners() {
    }

    protected void uninstallListeners() {
    }

    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    /** Whatever the event says; see the class note. */
    public boolean isPopupTrigger(MouseEvent e) {
        return e.isPopupTrigger();
    }
}
