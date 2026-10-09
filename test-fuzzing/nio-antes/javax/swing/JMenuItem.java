package javax.swing;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.event.MenuDragMouseEvent;
import javax.swing.event.MenuDragMouseListener;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;
import javax.swing.plaf.MenuItemUI;

/**
 * A menu option.
 *
 * <h2>It is a button, and that resolves almost everything</h2>
 *
 * <p>It inherits from {@link AbstractButton}, so it already has text, icon, action, enabled and
 * the event on pressing it. What it adds is what a loose button does not need: the keyboard
 * shortcut and the handling of the menu's walk.
 *
 * <h2>Armed is not pressed</h2>
 *
 * <p>{@link #setArmed} marks the option the mouse is over while the menu is open. It is
 * different from pressed: pressed lasts as long as the click lasts, armed lasts while the mouse
 * passes over. A menu is walked without releasing the button, and that is the difference that
 * allows it.
 *
 * <h2>The events come with the path</h2>
 *
 * <p>{@link #processMouseEvent(MouseEvent, MenuElement[], MenuSelectionManager)} also receives
 * the path of elements from the menu above. An option cannot decide alone: if the mouse moves
 * to a submenu, who closes the previous one is whoever knows the whole path.
 */
public class JMenuItem extends AbstractButton implements Accessible, MenuElement {

    private static final String uiClassID = "MenuItemUI";

    private KeyStroke accelerator;
    private boolean isMouseDragged = false;

    /** An empty option. */
    public JMenuItem() {
        this(null, (Icon) null);
    }

    /** An option with only an icon. */
    public JMenuItem(Icon icon) {
        this(null, icon);
    }

    /** An option with that text. */
    public JMenuItem(String text) {
        this(text, (Icon) null);
    }

    /** An option that fires that action and takes its text and its icon from it. */
    public JMenuItem(Action a) {
        this();
        setAction(a);
    }

    /** An option with text and icon. */
    public JMenuItem(String text, Icon icon) {
        setModel(new DefaultButtonModel());
        init(text, icon);
        // Through the look and feel's path, not the user's: that way the look and feel that is
                // installed afterwards can switch it on again. With `setBorderPainted(false)` it
                // would be marked as the program's decision and no look and feel would ever touch
                // it again.
        javax.swing.LookAndFeel.installProperty(this, "borderPainted", Boolean.FALSE);
        setFocusPainted(false);
        setHorizontalTextPosition(JButton.TRAILING);
        setHorizontalAlignment(JButton.LEADING);
        updateUI();
    }

    /** An option with text and that letter underlined. */
    public JMenuItem(String text, int mnemonic) {
        setModel(new DefaultButtonModel());
        init(text, null);
        setMnemonic(mnemonic);
        // Through the look and feel's path, not the user's: that way the look and feel that is
                // installed afterwards can switch it on again. With `setBorderPainted(false)` it
                // would be marked as the program's decision and no look and feel would ever touch
                // it again.
        javax.swing.LookAndFeel.installProperty(this, "borderPainted", Boolean.FALSE);
        setFocusPainted(false);
        setHorizontalTextPosition(JButton.TRAILING);
        setHorizontalAlignment(JButton.LEADING);
        updateUI();
    }

    public void setModel(ButtonModel newModel) {
        super.setModel(newModel);
    }

    protected void init(String text, Icon icon) {
        if (text != null) {
            setText(text);
        }
        if (icon != null) {
            setIcon(icon);
        }
    }

    public void setUI(MenuItemUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** It marks the option the mouse is over; see the class note. */
    public void setArmed(boolean b) {
        ButtonModel model = getModel();
        if (model.isArmed() != b) {
            model.setArmed(b);
        }
    }

    public boolean isArmed() {
        ButtonModel model = getModel();
        return (model != null) && model.isArmed();
    }

    /**
     * It enables or disables the option.
     *
     * <p>Disabling it disarms it: a switched-off option cannot stay highlighted, because on being
     * enabled again it would appear marked without the mouse being over it.
     */
    public void setEnabled(boolean b) {
        if (!b) {
            setArmed(false);
        }
        super.setEnabled(b);
    }

    /**
     * The shortcut that fires the option without opening the menu.
     *
     * <p>It belongs to the menu, not to the component: it works with the window in focus even
     * though the menu is closed. That is why it is drawn beside the text, so that it can be
     * learned.
     */
    public void setAccelerator(KeyStroke keyStroke) {
        KeyStroke oldAccelerator = accelerator;
        this.accelerator = keyStroke;
        firePropertyChange("accelerator", oldAccelerator, accelerator);
        repaint();
        revalidate();
    }

    public KeyStroke getAccelerator() {
        return this.accelerator;
    }

    protected void configurePropertiesFromAction(Action a) {
        super.configurePropertiesFromAction(a);
        if (a != null) {
            Object o = a.getValue(Action.ACCELERATOR_KEY);
            if (o instanceof KeyStroke) {
                setAccelerator((KeyStroke) o);
            }
        }
    }

    protected void actionPropertyChanged(Action action, String propertyName) {
        if (Action.ACCELERATOR_KEY.equals(propertyName)) {
            Object o = action.getValue(Action.ACCELERATOR_KEY);
            setAccelerator(o instanceof KeyStroke ? (KeyStroke) o : null);
        } else {
            super.actionPropertyChanged(action, propertyName);
        }
    }

    /** A mouse event with the menu's path; see the class note. */
    public void processMouseEvent(MouseEvent e, MenuElement[] path,
            MenuSelectionManager manager) {
        processMenuDragMouseEvent(new MenuDragMouseEvent(e.getComponent(), e.getID(),
                e.getWhen(), e.getModifiersEx(), e.getX(), e.getY(), e.getClickCount(),
                e.isPopupTrigger(), path, manager));
    }

    public void processKeyEvent(KeyEvent e, MenuElement[] path,
            MenuSelectionManager manager) {
        MenuKeyEvent mke = new MenuKeyEvent(e.getComponent(), e.getID(), e.getWhen(),
                e.getModifiersEx(), e.getKeyCode(), e.getKeyChar(), path, manager);
        processMenuKeyEvent(mke);
        if (mke.isConsumed()) {
            e.consume();
        }
    }

    /** It hands the drag event out to whoever it belongs to. */
    public void processMenuDragMouseEvent(MenuDragMouseEvent e) {
        int id = e.getID();
        if (id == MouseEvent.MOUSE_ENTERED) {
            isMouseDragged = false;
            fireMenuDragMouseEntered(e);
        } else if (id == MouseEvent.MOUSE_EXITED) {
            isMouseDragged = false;
            fireMenuDragMouseExited(e);
        } else if (id == MouseEvent.MOUSE_DRAGGED) {
            isMouseDragged = true;
            fireMenuDragMouseDragged(e);
        } else if (id == MouseEvent.MOUSE_RELEASED) {
            if (isMouseDragged) {
                fireMenuDragMouseReleased(e);
            }
        }
    }

    public void processMenuKeyEvent(MenuKeyEvent e) {
        int id = e.getID();
        if (id == KeyEvent.KEY_PRESSED) {
            fireMenuKeyPressed(e);
        } else if (id == KeyEvent.KEY_RELEASED) {
            fireMenuKeyReleased(e);
        } else if (id == KeyEvent.KEY_TYPED) {
            fireMenuKeyTyped(e);
        }
    }

    protected void fireMenuDragMouseEntered(MenuDragMouseEvent event) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == MenuDragMouseListener.class) {
                ((MenuDragMouseListener) listeners[i + 1]).menuDragMouseEntered(event);
            }
        }
    }

    protected void fireMenuDragMouseExited(MenuDragMouseEvent event) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == MenuDragMouseListener.class) {
                ((MenuDragMouseListener) listeners[i + 1]).menuDragMouseExited(event);
            }
        }
    }

    protected void fireMenuDragMouseDragged(MenuDragMouseEvent event) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == MenuDragMouseListener.class) {
                ((MenuDragMouseListener) listeners[i + 1]).menuDragMouseDragged(event);
            }
        }
    }

    protected void fireMenuDragMouseReleased(MenuDragMouseEvent event) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == MenuDragMouseListener.class) {
                ((MenuDragMouseListener) listeners[i + 1]).menuDragMouseReleased(event);
            }
        }
    }

    protected void fireMenuKeyPressed(MenuKeyEvent event) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == MenuKeyListener.class) {
                ((MenuKeyListener) listeners[i + 1]).menuKeyPressed(event);
            }
        }
    }

    protected void fireMenuKeyReleased(MenuKeyEvent event) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == MenuKeyListener.class) {
                ((MenuKeyListener) listeners[i + 1]).menuKeyReleased(event);
            }
        }
    }

    protected void fireMenuKeyTyped(MenuKeyEvent event) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == MenuKeyListener.class) {
                ((MenuKeyListener) listeners[i + 1]).menuKeyTyped(event);
            }
        }
    }

    /** The menu's walk passed through here, or stopped passing. */
    public void menuSelectionChanged(boolean isIncluded) {
        setArmed(isIncluded);
    }

    /**
     * The elements inside; none.
     *
     * <p>An option is a leaf. {@link JMenu}, which does have a submenu, overrides it.
     */
    public MenuElement[] getSubElements() {
        return new MenuElement[0];
    }

    public Component getComponent() {
        return this;
    }

    public void addMenuDragMouseListener(MenuDragMouseListener l) {
        listenerList.add(MenuDragMouseListener.class, l);
    }

    public void removeMenuDragMouseListener(MenuDragMouseListener l) {
        listenerList.remove(MenuDragMouseListener.class, l);
    }

    public MenuDragMouseListener[] getMenuDragMouseListeners() {
        return listenerList.getListeners(MenuDragMouseListener.class);
    }

    public void addMenuKeyListener(MenuKeyListener l) {
        listenerList.add(MenuKeyListener.class, l);
    }

    public void removeMenuKeyListener(MenuKeyListener l) {
        listenerList.remove(MenuKeyListener.class, l);
    }

    public MenuKeyListener[] getMenuKeyListeners() {
        return listenerList.getListeners(MenuKeyListener.class);
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
