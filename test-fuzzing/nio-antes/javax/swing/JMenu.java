package javax.swing;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 * A menu option that opens a submenu.
 *
 * <h2>It is an option, and it also has a drop-down</h2>
 *
 * <p>It inherits from {@link JMenuItem}, so in the bar or in a menu above it behaves like any
 * option. What it adds is a {@link JPopupMenu} of its own: what is added to it with
 * {@link #add} does not go to the menu but to that drop-down.
 *
 * <p>That explains why {@link #getMenuComponentCount} and {@link #getComponentCount} exist
 * separately: the first counts the submenu's options, the second the component's children,
 * which are another thing.
 *
 * <h2>Chosen is not dropped down</h2>
 *
 * <p>{@link #setSelected} marks the menu as the one the walk is touching;
 * {@link #setPopupMenuVisible} opens the drop-down. Almost always they go together, and not
 * always: walking the bar with the keyboard marks menus without opening them until the down
 * arrow is pressed.
 *
 * <h2>The delay</h2>
 *
 * <p>{@link #setDelay} is how long it waits before opening the submenu when the mouse passes
 * over. It exists because without it, crossing a menu on the way to another would open every
 * one on the way.
 */
public class JMenu extends JMenuItem implements Accessible, MenuElement {

    private static final String uiClassID = "MenuUI";

    private JPopupMenu popupMenu;
    private int delay;
    private Point customMenuLocation = null;

    /** It listens to the drop-down's window in order to close it when it closes. */
    protected WinListener popupListener;

    /** An empty menu with no text. */
    public JMenu() {
        this("");
    }

    /** A menu with that text. */
    public JMenu(String s) {
        super(s);
    }

    /** A menu that takes its text and its icon from that action. */
    public JMenu(Action a) {
        this();
        setAction(a);
    }

    /**
     * A menu with that text.
     *
     * @param b whether the submenu can be torn off and left floating; it is not implemented.
     */
    public JMenu(String s, boolean b) {
        this(s);
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    public void setModel(ButtonModel newModel) {
        super.setModel(newModel);
    }

    /** Whether the menu's walk is touching this one; see the class note. */
    public boolean isSelected() {
        return getModel().isSelected();
    }

    public void setSelected(boolean b) {
        ButtonModel model = getModel();
        if (b != model.isSelected() || !isEnabled()) {
            model.setSelected(b);
        }
    }

    public boolean isPopupMenuVisible() {
        ensurePopupMenuCreated();
        return popupMenu.isVisible();
    }

    /**
     * It opens or closes the submenu.
     *
     * <p>A disabled menu opens nothing even though it is asked to: if it opened, the user could
     * choose from a menu that is switched off.
     */
    public void setPopupMenuVisible(boolean b) {
        boolean isVisible = isPopupMenuVisible();
        if (b != isVisible && (isEnabled() || !b)) {
            ensurePopupMenuCreated();
            if (b) {
                Point p = getPopupMenuOrigin();
                getPopupMenu().show(this, p.x, p.y);
            } else {
                getPopupMenu().setVisible(false);
            }
        }
    }

    /**
     * Where to open the submenu, relative to this menu.
     *
     * <p>Below if the menu is in the bar, at the side if it is inside another menu. It is what
     * keeps a submenu from covering its parent.
     */
    protected Point getPopupMenuOrigin() {
        if (isTopLevelMenu()) {
            return new Point(0, getHeight());
        }
        return new Point(getWidth(), 0);
    }

    public int getDelay() {
        return delay;
    }

    /**
     * How long it waits before opening the submenu; see the class note.
     *
     * @throws IllegalArgumentException if it is negative.
     */
    public void setDelay(int d) {
        if (d < 0) {
            throw new IllegalArgumentException("Delay must be a positive integer");
        }
        delay = d;
    }

    /** It forces where the submenu opens, instead of computing it. */
    public void setMenuLocation(int x, int y) {
        customMenuLocation = new Point(x, y);
        if (popupMenu != null) {
            popupMenu.setLocation(x, y);
        }
    }

    public JMenuItem add(JMenuItem menuItem) {
        ensurePopupMenuCreated();
        return popupMenu.add(menuItem);
    }

    public Component add(Component c) {
        ensurePopupMenuCreated();
        popupMenu.add(c);
        return c;
    }

    public Component add(Component c, int index) {
        ensurePopupMenuCreated();
        popupMenu.add(c, index);
        return c;
    }

    /** It adds an option with that text. */
    public JMenuItem add(String s) {
        return add(new JMenuItem(s));
    }

    public JMenuItem add(Action a) {
        JMenuItem mi = createActionComponent(a);
        mi.setAction(a);
        add(mi);
        return mi;
    }

    protected JMenuItem createActionComponent(Action a) {
        JMenuItem mi = new JMenuItem();
        mi.setHorizontalTextPosition(JButton.TRAILING);
        mi.setVerticalTextPosition(JButton.CENTER);
        return mi;
    }

    /** It returns null; the option listens to the action itself. See {@link JPopupMenu}. */
    protected PropertyChangeListener createActionChangeListener(JMenuItem b) {
        return null;
    }

    public void addSeparator() {
        ensurePopupMenuCreated();
        popupMenu.addSeparator();
    }

    public void insert(String s, int pos) {
        if (pos < 0) {
            throw new IllegalArgumentException("index less than zero.");
        }
        ensurePopupMenuCreated();
        popupMenu.insert(new JMenuItem(s), pos);
    }

    public JMenuItem insert(JMenuItem mi, int pos) {
        if (pos < 0) {
            throw new IllegalArgumentException("index less than zero.");
        }
        ensurePopupMenuCreated();
        popupMenu.insert(mi, pos);
        return mi;
    }

    public JMenuItem insert(Action a, int pos) {
        if (pos < 0) {
            throw new IllegalArgumentException("index less than zero.");
        }
        ensurePopupMenuCreated();
        JMenuItem mi = createActionComponent(a);
        mi.setAction(a);
        popupMenu.insert(mi, pos);
        return mi;
    }

    public void insertSeparator(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index less than zero.");
        }
        ensurePopupMenuCreated();
        popupMenu.insert(new JPopupMenu.Separator(), index);
    }

    /**
     * Option number such-and-such of the submenu.
     *
     * <p>It returns null if at that position there is something that is not an option -- a
     * separator, for instance --. It does not skip: the position is the component's.
     */
    public JMenuItem getItem(int pos) {
        if (pos < 0) {
            throw new IllegalArgumentException("index less than zero.");
        }
        Component c = getMenuComponent(pos);
        if (c instanceof JMenuItem) {
            return (JMenuItem) c;
        }
        return null;
    }

    /** How many things there are in the submenu, counting separators. */
    public int getItemCount() {
        return getMenuComponentCount();
    }

    /**
     * Whether the submenu can be torn off and left floating.
     *
     * @throws Error always: it is not implemented, not in the JDK either.
     */
    public boolean isTearOff() {
        throw new Error("boolean isTearOff() {} not yet implemented");
    }

    public void remove(JMenuItem item) {
        if (popupMenu != null) {
            popupMenu.remove(item);
        }
    }

    public void remove(int pos) {
        if (pos < 0) {
            throw new IllegalArgumentException("index less than zero.");
        }
        if (pos > getItemCount()) {
            throw new IllegalArgumentException("index greater than the number of items.");
        }
        if (popupMenu != null) {
            popupMenu.remove(pos);
        }
    }

    public void remove(Component c) {
        if (popupMenu != null) {
            popupMenu.remove(c);
        }
    }

    public void removeAll() {
        if (popupMenu != null) {
            popupMenu.removeAll();
        }
    }

    /** How many components there are in the submenu; see the class note. */
    public int getMenuComponentCount() {
        int componentCount = 0;
        if (popupMenu != null) {
            componentCount = popupMenu.getComponentCount();
        }
        return componentCount;
    }

    public Component getMenuComponent(int n) {
        if (popupMenu != null) {
            return popupMenu.getComponent(n);
        }
        return null;
    }

    public Component[] getMenuComponents() {
        if (popupMenu != null) {
            return popupMenu.getComponents();
        }
        return new Component[0];
    }

    /** Whether it is directly in the menu bar and not inside another menu. */
    public boolean isTopLevelMenu() {
        return getParent() instanceof JMenuBar;
    }

    public boolean isMenuComponent(Component c) {
        if (c == this) {
            return true;
        }
        if (c instanceof JPopupMenu) {
            return false;
        }
        if (popupMenu != null) {
            int ncomponents = popupMenu.getComponentCount();
            for (int i = 0; i < ncomponents; i++) {
                Component comp = popupMenu.getComponent(i);
                if (comp == c) {
                    return true;
                }
                if (comp instanceof JMenu && ((JMenu) comp).isMenuComponent(c)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The drop-down; it is built the first time it is needed. */
    public JPopupMenu getPopupMenu() {
        ensurePopupMenuCreated();
        return popupMenu;
    }

    private void ensurePopupMenuCreated() {
        if (popupMenu == null) {
            popupMenu = new JPopupMenu();
            popupMenu.setInvoker(this);
            popupListener = createWinListener(popupMenu);
        }
    }

    public void addMenuListener(MenuListener l) {
        listenerList.add(MenuListener.class, l);
    }

    public void removeMenuListener(MenuListener l) {
        listenerList.remove(MenuListener.class, l);
    }

    public MenuListener[] getMenuListeners() {
        return listenerList.getListeners(MenuListener.class);
    }

    /**
     * It gives notice that the menu was chosen.
     *
     * <p>It is the hook for building the menu right before opening it: a menu of recent files is
     * filled here and not when the window is built.
     */
    protected void fireMenuSelected() {
        Object[] listeners = listenerList.getListenerList();
        MenuEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == MenuListener.class) {
                if (e == null) {
                    e = new MenuEvent(this);
                }
                ((MenuListener) listeners[i + 1]).menuSelected(e);
            }
        }
    }

    protected void fireMenuDeselected() {
        Object[] listeners = listenerList.getListenerList();
        MenuEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == MenuListener.class) {
                if (e == null) {
                    e = new MenuEvent(this);
                }
                ((MenuListener) listeners[i + 1]).menuDeselected(e);
            }
        }
    }

    protected void fireMenuCanceled() {
        Object[] listeners = listenerList.getListenerList();
        MenuEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == MenuListener.class) {
                if (e == null) {
                    e = new MenuEvent(this);
                }
                ((MenuListener) listeners[i + 1]).menuCanceled(e);
            }
        }
    }

    protected WinListener createWinListener(JPopupMenu p) {
        return new WinListener(this, p);
    }

    /**
     * It closes the menu when the drop-down's window closes.
     *
     * <p>It is only needed with a heavyweight drop-down, which lives in a window of its own.
     * Without this, closing it from the system would leave the menu believing it is still open.
     */
    public static class WinListener extends WindowAdapter implements Serializable {

        private final JMenu menu;
        JPopupMenu popupMenu;

        public WinListener(JMenu menu, JPopupMenu p) {
            this.menu = menu;
            this.popupMenu = p;
        }

        public void windowClosing(WindowEvent e) {
            menu.setSelected(false);
        }
    }

    /** The menu's walk passed through here: the submenu opens or closes. */
    public void menuSelectionChanged(boolean isIncluded) {
        setSelected(isIncluded);
    }

    /** The submenu's options, with the drop-down in front. */
    public MenuElement[] getSubElements() {
        if (popupMenu == null) {
            return new MenuElement[0];
        }
        MenuElement[] result = new MenuElement[1];
        result[0] = popupMenu;
        return result;
    }

    public Component getComponent() {
        return this;
    }

    public void applyComponentOrientation(ComponentOrientation o) {
        super.applyComponentOrientation(o);
        if (popupMenu != null) {
            popupMenu.applyComponentOrientation(o);
        }
    }

    public void setComponentOrientation(ComponentOrientation o) {
        super.setComponentOrientation(o);
        if (popupMenu != null) {
            popupMenu.setComponentOrientation(o);
        }
    }

    /**
     * A menu carries no shortcut.
     *
     * <p>A shortcut fires an action; a menu does nothing, it only opens. Setting one would have
     * nothing to do, and that is why it is rejected instead of being kept.
     *
     * @throws Error always.
     */
    public void setAccelerator(KeyStroke keyStroke) {
        throw new Error("setAccelerator() is not defined for JMenu.  Use setMnemonic() instead.");
    }

    protected void processKeyEvent(KeyEvent evt) {
        MenuSelectionManager.defaultManager().processKeyEvent(evt);
        if (evt.isConsumed()) {
            return;
        }
        super.processKeyEvent(evt);
    }

    /** Open the submenu, which is what a menu does on being pressed. */
    public void doClick(int pressTime) {
        MenuElement[] me = buildMenuElementArray(this);
        MenuSelectionManager.defaultManager().setSelectedPath(me);
    }

    /** The path from the menu above down to this one. */
    private static MenuElement[] buildMenuElementArray(JMenu leaf) {
        java.util.Vector<MenuElement> elements = new java.util.Vector<MenuElement>();
        Component current = leaf.getPopupMenu();
        while (true) {
            if (current instanceof JPopupMenu) {
                JPopupMenu pop = (JPopupMenu) current;
                elements.insertElementAt(pop, 0);
                current = pop.getInvoker();
            } else if (current instanceof JMenu) {
                JMenu menu = (JMenu) current;
                elements.insertElementAt(menu, 0);
                current = menu.getParent();
            } else if (current instanceof JMenuBar) {
                JMenuBar bar = (JMenuBar) current;
                elements.insertElementAt(bar, 0);
                break;
            } else {
                break;
            }
        }
        MenuElement[] me = new MenuElement[elements.size()];
        elements.copyInto(me);
        return me;
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return super.getAccessibleContext();
    }
}
