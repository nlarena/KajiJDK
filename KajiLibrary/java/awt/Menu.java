package java.awt;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * A menu: an option that, when chosen, unfolds others.
 *
 * <p>It inherits from {@link MenuItem} and that is what makes submenus come for free: a menu **is**
 * an option, so putting it inside another menu works with no special case at all. It is the same
 * composition idea that makes a container be a component.
 *
 * <p>A **tear-off** menu can be pulled off the bar and left floating as a little window. Almost no
 * modern desktop does it, but the flag is still in the API and is kept.
 *
 * <p>Its constructors declare {@link HeadlessException} like the JDK's and never throw it; see
 * {@link MenuComponent}.
 */
public class Menu extends MenuItem implements MenuContainer, Accessible {

    private static final long serialVersionUID = -8809584163345499784L;

    private final List<MenuItem> items = new ArrayList<MenuItem>();
    private final boolean tearOff;

    /** A menu without a label. */
    public Menu() throws HeadlessException {
        this("", false);
    }

    /** With that label. */
    public Menu(String label) throws HeadlessException {
        this(label, false);
    }

    /** With a label, and tear-off or not. */
    public Menu(String label, boolean tearOff) throws HeadlessException {
        super(label);
        this.tearOff = tearOff;
    }

    /** Notifies that it can be shown, and tells its children. */
    public void addNotify() {
        synchronized (this.getTreeLock()) {
            for (int i = 0; i < this.items.size(); i++) {
                this.items.get(i).addNotify();
            }
        }
    }

    /** Notifies that it can no longer be shown, and tells its children. */
    public void removeNotify() {
        synchronized (this.getTreeLock()) {
            for (int i = 0; i < this.items.size(); i++) {
                this.items.get(i).removeNotify();
            }
        }
        super.removeNotify();
    }

    /** Whether it can be pulled off the bar. */
    public boolean isTearOff() {
        return this.tearOff;
    }

    /** How many options it has. */
    public int getItemCount() {
        return this.items.size();
    }

    /**
     * How many options it has.
     *
     * @deprecated it is from the 1.0 model. Use {@link #getItemCount}.
     */
    @Deprecated
    public int countItems() {
        return this.getItemCount();
    }

    /**
     * The option at that position.
     *
     * @throws ArrayIndexOutOfBoundsException if there is no such option
     */
    public MenuItem getItem(int index) {
        return this.items.get(index);
    }

    /**
     * Adds an option at the end.
     *
     * <p>If it was already in another menu it is taken out of there first: an option hangs from a
     * single parent.
     *
     * @return the same option, so calls can be chained
     */
    public MenuItem add(MenuItem mi) {
        synchronized (this.getTreeLock()) {
            if (mi.getParent() != null) {
                ((MenuContainer) mi.getParent()).remove(mi);
            }
            this.items.add(mi);
            mi.setParent(this);
            return mi;
        }
    }

    /** Adds an option with that label. */
    public void add(String label) {
        this.add(new MenuItem(label));
    }

    /**
     * Inserts an option at that position.
     *
     * @throws IllegalArgumentException if the position is negative
     */
    public void insert(MenuItem menuitem, int index) {
        synchronized (this.getTreeLock()) {
            if (index < 0) {
                throw new IllegalArgumentException("index less than zero.");
            }
            if (menuitem.getParent() != null) {
                ((MenuContainer) menuitem.getParent()).remove(menuitem);
            }
            int n = this.items.size();
            this.items.add(index > n ? n : index, menuitem);
            menuitem.setParent(this);
        }
    }

    /**
     * Inserts an option with that label.
     *
     * @throws IllegalArgumentException if the position is negative
     */
    public void insert(String label, int index) {
        this.insert(new MenuItem(label), index);
    }

    /**
     * Adds a separating line.
     *
     * <p>A separator is an option with the label `"-"`: it is not a type apart, and that is why it
     * can be taken out with {@link #remove(int)} like any other.
     */
    public void addSeparator() {
        this.add("-");
    }

    /**
     * Inserts a separating line.
     *
     * @throws IllegalArgumentException if the position is negative
     */
    public void insertSeparator(int index) {
        this.insert("-", index);
    }

    /**
     * Takes out the option at that position.
     *
     * @throws ArrayIndexOutOfBoundsException if there is no such option
     */
    public void remove(int index) {
        synchronized (this.getTreeLock()) {
            MenuItem mi = this.items.remove(index);
            mi.setParent(null);
        }
    }

    /** Takes that option out; if it was not there, nothing happens. */
    public void remove(MenuComponent item) {
        synchronized (this.getTreeLock()) {
            int i = this.items.indexOf(item);
            if (i >= 0) {
                this.remove(i);
            }
        }
    }

    /** Takes them all out. */
    public void removeAll() {
        synchronized (this.getTreeLock()) {
            for (int i = this.items.size() - 1; i >= 0; i--) {
                this.remove(i);
            }
        }
    }

    public String paramString() {
        return super.paramString() + ",tearOff=" + this.tearOff + ",isHelpMenu=false";
    }

    /** The accessibility information of this menu. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTMenu();
        }
        return this.accessibleContext;
    }

    /** The accessibility of a menu. */
    protected class AccessibleAWTMenu extends AccessibleAWTMenuItem {

        /** For the subclasses. */
        protected AccessibleAWTMenu() {
        }

        /** It is a menu. */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.MENU;
        }

        /** How many options it has. */
        public int getAccessibleChildrenCount() {
            return Menu.this.getItemCount();
        }

        /**
         * The option at that position, if it is accessible.
         *
         * @return the option, or `null` if there is no such one
         */
        public Accessible getAccessibleChild(int i) {
            if (i < 0 || i >= Menu.this.getItemCount()) {
                return null;
            }
            MenuItem mi = Menu.this.getItem(i);
            if (mi instanceof Accessible) {
                return (Accessible) mi;
            }
            return null;
        }
    }
}
