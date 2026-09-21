package javax.swing;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * Who keeps track of which menu is open.
 *
 * <h2>Why a manager is needed and each menu is not enough</h2>
 *
 * <p>Because an open menu <strong>captures</strong> the whole application's mouse and keyboard:
 * clicking anywhere has to close it, and moving the mouse from one submenu to the one beside it
 * has to close the first and open the second. Neither of the two menus can decide that alone --
 * each one sees nothing but its own events.
 *
 * <p>The state that resolves all that is <em>one</em>: the selected path, from the bar down to
 * the deepest item. Opening, closing and navigating are changes of that path, and that is why
 * the manager is a singleton per application.
 *
 * <h2>What this VM does not do</h2>
 *
 * <p>{@link #processMouseEvent} and {@link #processKeyEvent} hand out to the path, which is
 * their whole job. {@link #componentForPoint} needs to know where each component is on the
 * screen and that asks for a windowing system this VM does not have: it returns {@code null},
 * and it says so.
 */
public class MenuSelectionManager {

    private static MenuSelectionManager shared;

    /** The event that is reused; see {@link #fireStateChanged}. */
    protected transient ChangeEvent changeEvent = null;

    /** The listeners. */
    protected EventListenerList listenerList = new EventListenerList();

    private MenuElement[] selection = new MenuElement[0];

    /** A new manager. The usual thing is to ask for {@link #defaultManager}'s. */
    public MenuSelectionManager() {
    }

    /** The application's manager. */
    public static MenuSelectionManager defaultManager() {
        if (shared == null) {
            shared = new MenuSelectionManager();
        }
        return shared;
    }

    /**
     * It changes the selected path.
     *
     * <p>It gives notice to the elements that <strong>came in</strong> and to those that
     * <strong>went out</strong>, and for that it compares with the previous path from the root down
     * to where the two agree. Giving notice to everybody on each change would make a submenu close
     * and reopen when the mouse moves a pixel.
     */
    public void setSelectedPath(MenuElement[] path) {
        MenuElement[] newValue = path == null ? new MenuElement[0] : path;
        int comun = 0;
        while (comun < this.selection.length && comun < newValue.length
                && this.selection[comun] == newValue[comun]) {
            comun = comun + 1;
        }
        for (int i = this.selection.length - 1; i >= comun; i--) {
            this.selection[i].menuSelectionChanged(false);
        }
        MenuElement[] copy = new MenuElement[newValue.length];
        for (int i = 0; i < newValue.length; i++) {
            copy[i] = newValue[i];
        }
        this.selection = copy;
        for (int i = comun; i < this.selection.length; i++) {
            this.selection[i].menuSelectionChanged(true);
        }
        fireStateChanged();
    }

    /** The selected path, in a new array. */
    public MenuElement[] getSelectedPath() {
        MenuElement[] copy = new MenuElement[this.selection.length];
        for (int i = 0; i < this.selection.length; i++) {
            copy[i] = this.selection[i];
        }
        return copy;
    }

    /** It closes everything. */
    public void clearSelectedPath() {
        if (this.selection.length > 0) {
            setSelectedPath(null);
        }
    }

    /** It adds a listener for changes of the path. */
    public void addChangeListener(ChangeListener l) {
        this.listenerList.add(ChangeListener.class, l);
    }

    /** It removes a listener. */
    public void removeChangeListener(ChangeListener l) {
        this.listenerList.remove(ChangeListener.class, l);
    }

    /** The change listeners. */
    public ChangeListener[] getChangeListeners() {
        return this.listenerList.getListeners(ChangeListener.class);
    }

    /**
     * It gives notice that the path changed.
     *
     * <p>The {@link ChangeEvent} is created once and reused: it carries no datum but its source,
     * which is always this object, so allocating a new one per notice would be pure rubbish. It is
     * the convention of the whole of Swing.
     */
    protected void fireStateChanged() {
        Object[] listeners = this.listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ChangeListener.class) {
                if (this.changeEvent == null) {
                    this.changeEvent = new ChangeEvent(this);
                }
                ChangeListener l = (ChangeListener) listeners[i + 1];
                l.stateChanged(this.changeEvent);
            }
        }
    }

    /** It hands a mouse event out to the whole path, from the deepest to the shallowest. */
    public void processMouseEvent(MouseEvent event) {
        MenuElement[] path = getSelectedPath();
        for (int i = path.length - 1; i >= 0; i--) {
            path[i].processMouseEvent(event, path, this);
            if (event.isConsumed()) {
                return;
            }
        }
    }

    /** It hands a keyboard event out to the whole path. */
    public void processKeyEvent(KeyEvent event) {
        MenuElement[] path = getSelectedPath();
        for (int i = path.length - 1; i >= 0; i--) {
            path[i].processKeyEvent(event, path, this);
            if (event.isConsumed()) {
                return;
            }
        }
    }

    /**
     * Which component of the menu is under that point.
     *
     * @return {@code null} always on this VM: the position of each component on the screen is
     *     needed, which the windowing system gives. See the class note.
     */
    public Component componentForPoint(Component source, Point sourcePoint) {
        return null;
    }

    /** Whether {@code c} is part of the open menu. */
    public boolean isComponentPartOfCurrentMenu(Component c) {
        if (this.selection.length == 0) {
            return false;
        }
        return isPart(this.selection[0], c);
    }

    private boolean isPart(MenuElement root, Component c) {
        if (root == null) {
            return false;
        }
        if (root.getComponent() == c) {
            return true;
        }
        MenuElement[] children = root.getSubElements();
        for (int i = 0; i < children.length; i++) {
            if (isPart(children[i], c)) {
                return true;
            }
        }
        return false;
    }
}
