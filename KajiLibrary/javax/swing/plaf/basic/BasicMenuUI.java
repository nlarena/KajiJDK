package javax.swing.plaf.basic;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.MenuSelectionManager;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuDragMouseListener;
import javax.swing.event.MenuKeyListener;
import javax.swing.event.MenuListener;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ComponentUI;

/**
 * The basic look and feel of a menu.
 *
 * <p>A {@link JMenu} <em>is</em> a menu item -- it inherits from {@code JMenuItem} --, so
 * almost everything comes from {@link BasicMenuItemUI}. What is its own are three things.
 *
 * <h2>A bar menu does not stretch</h2>
 *
 * <p>{@link #getMaximumSize} returns the preferred width and an infinite height, but
 * <em>only</em> for a menu that hangs from the bar. Without that, the bar's layout would give
 * all the leftover width to the first menu and "File" would take up half the screen. A menu
 * inside returns {@code null} like any item, because there it is handed out by
 * {@code DefaultMenuLayout}.
 *
 * <h2>The delay before opening</h2>
 *
 * <p>{@link #setupPostTimer} sets the timer that opens the submenu when the mouse stays still
 * over it. The delay comes from {@code Menu.delay}, and it is at 200 milliseconds. The delay
 * exists so that passing the mouse over it on the way somewhere else does not open three
 * submenus.
 *
 * <h2>Telling that it opened</h2>
 *
 * <p>The {@link MenuListener} is what makes {@code menuSelected} arrive before the submenu is
 * shown: it is the moment when a program can build the menu's content according to the state,
 * instead of having to keep it up to date all the time.
 */
public class BasicMenuUI extends BasicMenuItemUI {

    protected ChangeListener changeListener;
    protected MenuListener menuListener;

    public BasicMenuUI() {
    }

    /** A new one per menu: it keeps the component and its listeners. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicMenuUI();
    }

    protected String getPropertyPrefix() {
        return "Menu";
    }

    protected void installDefaults() {
        super.installDefaults();
        // The menu has an arrow of its own: the item's points to a submenu that hangs at the side,
                // and so does the menu's, but a look and feel may want to draw them differently.
        arrowIcon = BasicIconFactory.getMenuArrowIcon();
        ((JMenu) menuItem).setDelay(200);
    }

    protected void uninstallDefaults() {
        super.uninstallDefaults();
    }

    protected void installListeners() {
        super.installListeners();
        changeListener = createChangeListener(menuItem);
        if (changeListener != null) {
            menuItem.addChangeListener(changeListener);
        }
        menuListener = createMenuListener(menuItem);
        if (menuListener != null) {
            ((JMenu) menuItem).addMenuListener(menuListener);
        }
    }

    protected void uninstallListeners() {
        super.uninstallListeners();
        if (changeListener != null) {
            menuItem.removeChangeListener(changeListener);
        }
        if (menuListener != null) {
            ((JMenu) menuItem).removeMenuListener(menuListener);
        }
        changeListener = null;
        menuListener = null;
    }

    /** With no shortcuts of its own; the underlined letter is handled by the bar. */
    protected void installKeyboardActions() {
        super.installKeyboardActions();
    }

    protected void uninstallKeyboardActions() {
        super.uninstallKeyboardActions();
    }

    protected MouseInputListener createMouseInputListener(JComponent c) {
        return super.createMouseInputListener(c);
    }

    protected MenuDragMouseListener createMenuDragMouseListener(JComponent c) {
        return super.createMenuDragMouseListener(c);
    }

    /** None, as in {@link BasicMenuItemUI}. */
    protected MenuKeyListener createMenuKeyListener(JComponent c) {
        return null;
    }

    protected PropertyChangeListener createPropertyChangeListener(JComponent c) {
        return super.createPropertyChangeListener(c);
    }

    /**
     * None.
     *
     * <p>Both -- this one and {@link #createMenuListener} -- return {@code null}, and both fields
     * are left null after installing. It is not an oversight: who follows the menu's state is the
     * same object that follows the mouse, and hooking it twice would make it react twice. It is
     * measured.
     */
    protected ChangeListener createChangeListener(JComponent c) {
        return null;
    }

    /** None; see {@link #createChangeListener}. */
    protected MenuListener createMenuListener(JComponent c) {
        return null;
    }

    /** Infinite only for a bar menu; see the class note. */
    public Dimension getMaximumSize(JComponent c) {
        if (((JMenu) menuItem).isTopLevelMenu()) {
            Dimension d = c.getPreferredSize();
            return new Dimension(d.width, Short.MAX_VALUE);
        }
        return null;
    }

    /** {@code null}, as in any item. */
    public Dimension getMinimumSize(JComponent c) {
        return null;
    }

    /**
     * It sets the timer that opens the submenu; see the class note.
     *
     * <p>A single shot: the timer opens the menu and switches off.
     */
    protected void setupPostTimer(JMenu menu) {
        Timer timer = new Timer(menu.getDelay(), new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                MenuSelectionManager.defaultManager().setSelectedPath(getPath());
            }
        });
        timer.setRepeats(false);
        timer.start();
    }
}
