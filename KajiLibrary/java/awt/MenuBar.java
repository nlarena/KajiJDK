package java.awt;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * The menu bar of a frame.
 *
 * <p>It is a {@link MenuComponent} and not a {@link Component}: the bar does not live in the
 * window's space but is drawn by the desktop, right at the top. That is why it has no position or
 * size.
 *
 * <p>The **help menu** has a place of its own because the platforms treat it differently: on some
 * it goes stuck to the right, apart from the rest. Declaring it lets each desktop put it where it
 * belongs instead of leaving it as one more.
 *
 * <p>Its constructor declares {@link HeadlessException} like the JDK's and never throws it; see
 * {@link MenuComponent}.
 */
public class MenuBar extends MenuComponent implements MenuContainer, Accessible {

    private static final long serialVersionUID = -4930327919388951260L;

    private final List<Menu> menus = new ArrayList<Menu>();
    private Menu helpMenu;

    /** An empty bar. */
    public MenuBar() throws HeadlessException {
    }

    /** Notifies that it can be shown, and tells its menus. */
    public void addNotify() {
        synchronized (this.getTreeLock()) {
            for (int i = 0; i < this.menus.size(); i++) {
                this.menus.get(i).addNotify();
            }
        }
    }

    /** Notifies that it can no longer be shown. */
    public void removeNotify() {
        synchronized (this.getTreeLock()) {
            for (int i = 0; i < this.menus.size(); i++) {
                this.menus.get(i).removeNotify();
            }
        }
        super.removeNotify();
    }

    /**
     * The help menu.
     *
     * @return the menu, or `null` if there is none
     */
    public Menu getHelpMenu() {
        return this.helpMenu;
    }

    /**
     * Declares which is the help menu.
     *
     * <p>If it was in another bar it is taken out of there: a menu hangs from a single parent.
     */
    public void setHelpMenu(Menu m) {
        synchronized (this.getTreeLock()) {
            if (this.helpMenu == m) {
                return;
            }
            if (this.helpMenu != null) {
                this.remove(this.helpMenu);
            }
            this.helpMenu = m;
            if (m != null) {
                if (m.getParent() != null) {
                    ((MenuContainer) m.getParent()).remove(m);
                }
                m.setParent(this);
            }
        }
    }

    /**
     * Adds a menu at the end.
     *
     * @return the same menu, so calls can be chained
     * @throws NullPointerException if the menu is `null`
     */
    public Menu add(Menu m) {
        synchronized (this.getTreeLock()) {
            if (m.getParent() != null) {
                ((MenuContainer) m.getParent()).remove(m);
            }
            this.menus.add(m);
            m.setParent(this);
            return m;
        }
    }

    /**
     * Takes out the menu at that position.
     *
     * @throws ArrayIndexOutOfBoundsException if there is no such menu
     */
    public void remove(int index) {
        synchronized (this.getTreeLock()) {
            Menu m = this.menus.remove(index);
            m.setParent(null);
        }
    }

    /** Takes that menu out; if it was not there, nothing happens. */
    public void remove(MenuComponent m) {
        synchronized (this.getTreeLock()) {
            int i = this.menus.indexOf(m);
            if (i >= 0) {
                this.remove(i);
            } else if (this.helpMenu == m) {
                this.helpMenu = null;
                m.setParent(null);
            }
        }
    }

    /** How many menus it has, not counting the help one. */
    public int getMenuCount() {
        return this.countMenus();
    }

    /**
     * How many menus it has.
     *
     * @deprecated it is from the 1.0 model. Use {@link #getMenuCount}.
     */
    @Deprecated
    public int countMenus() {
        synchronized (this.getTreeLock()) {
            return this.menus.size();
        }
    }

    /**
     * The menu at that position.
     *
     * @throws ArrayIndexOutOfBoundsException if there is no such menu
     */
    public Menu getMenu(int i) {
        synchronized (this.getTreeLock()) {
            return this.menus.get(i);
        }
    }

    /**
     * The keyboard shortcuts of the bar's own menus.
     *
     * <p>One level only: it does not go into the submenus, nor into the help menu, which is not one
     * of the bar's menus. The JDK walks the whole tree, because there each menu knows how to list
     * its own shortcuts recursively.
     */
    public synchronized Enumeration<MenuShortcut> shortcuts() {
        Vector<MenuShortcut> v = new Vector<MenuShortcut>();
        this.collectShortcuts(this.menus, v);
        return v.elements();
    }

    /** Walks the menus collecting the shortcuts of their options, without going into submenus. */
    private void collectShortcuts(List<Menu> source, Vector<MenuShortcut> v) {
        for (int i = 0; i < source.size(); i++) {
            Menu m = source.get(i);
            for (int j = 0; j < m.getItemCount(); j++) {
                MenuItem mi = m.getItem(j);
                MenuShortcut s = mi.getShortcut();
                if (s != null) {
                    v.addElement(s);
                }
            }
        }
    }

    /**
     * Which option has that shortcut.
     *
     * <p>It looks at the options of the bar's menus, one level deep, with the same limit as
     * {@link #shortcuts}.
     *
     * @return the option, or `null` if there is none
     */
    public MenuItem getShortcutMenuItem(MenuShortcut s) {
        synchronized (this.getTreeLock()) {
            for (int i = 0; i < this.menus.size(); i++) {
                Menu m = this.menus.get(i);
                for (int j = 0; j < m.getItemCount(); j++) {
                    MenuItem mi = m.getItem(j);
                    if (s.equals(mi.getShortcut())) {
                        return mi;
                    }
                }
            }
            return null;
        }
    }

    /** Takes that shortcut away from whichever option has it. */
    public void deleteShortcut(MenuShortcut s) {
        MenuItem mi = this.getShortcutMenuItem(s);
        if (mi != null) {
            mi.deleteShortcut();
        }
    }

    /** The accessibility information of this bar. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTMenuBar();
        }
        return this.accessibleContext;
    }

    /** The accessibility of a menu bar. */
    protected class AccessibleAWTMenuBar extends AccessibleAWTMenuComponent {

        /** For the subclasses. */
        protected AccessibleAWTMenuBar() {
        }

        /** It is a menu bar. */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.MENU_BAR;
        }

        /** How many menus it has. */
        public int getAccessibleChildrenCount() {
            return MenuBar.this.getMenuCount();
        }

        /**
         * The menu at that position.
         *
         * @return the menu, or `null` if there is no such one
         */
        public Accessible getAccessibleChild(int i) {
            if (i < 0 || i >= MenuBar.this.getMenuCount()) {
                return null;
            }
            return MenuBar.this.getMenu(i);
        }
    }
}
