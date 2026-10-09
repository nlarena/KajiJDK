package javax.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.event.MenuKeyListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.PopupMenuUI;

/**
 * A menu that appears floating: the context one or the one a {@link JMenu} drops down.
 *
 * <h2>It is not just another container</h2>
 *
 * <p>A popup menu does not live where it is set: it appears on top of everything and may go
 * outside the window. That is why {@link #setVisible} does not do what it does in another
 * component -- it asks the look and feel for a {@link Popup} and shows it --, and that is why
 * it has an <em>invoker</em>: the component it was opened from, which is what the coordinates
 * are measured against.
 *
 * <h2>It is a menu element</h2>
 *
 * <p>It implements {@link MenuElement}, so it takes part in the walk with the keyboard and the
 * mouse along with its options. It is what makes moving with the arrows between a menu and its
 * submenu work without either of the two knowing about the other.
 *
 * <h2>Lightweight or heavyweight</h2>
 *
 * <p>{@link #setLightWeightPopupEnabled} asks for it to be drawn inside the window. It is a
 * request, not an order: if it does not fit, a window of its own is used all the same. See
 * {@link PopupFactory}'s note.
 */
public class JPopupMenu extends JComponent implements Accessible, MenuElement {

    private static final String uiClassID = "PopupMenuUI";

    private static boolean defaultLWPopupEnabled = true;

    private SingleSelectionModel selectionModel;
    private Component invoker;
    private Popup popup;
    private String label = null;
    private boolean paintBorder = true;
    private Insets margin = null;
    private boolean lightWeightPopup = true;
    private int desiredLocationX;
    private int desiredLocationY;
    private boolean visible = false;
    private Dimension popupSize;
    private AccessibleContext accessibleContext;

    /** Whether new drop-downs start by asking for the lightweight form. */
    public static void setDefaultLightWeightPopupEnabled(boolean aFlag) {
        defaultLWPopupEnabled = aFlag;
    }

    public static boolean getDefaultLightWeightPopupEnabled() {
        return defaultLWPopupEnabled;
    }

    /** An empty popup menu. */
    public JPopupMenu() {
        this(null);
    }

    /** A popup menu with that title. */
    public JPopupMenu(String label) {
        this.label = label;
        lightWeightPopup = getDefaultLightWeightPopupEnabled();
        setSelectionModel(new DefaultSingleSelectionModel());
        setFocusTraversalKeysEnabled(false);
        updateUI();
    }

    public PopupMenuUI getUI() {
        return (PopupMenuUI) ui;
    }

    public void setUI(PopupMenuUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    protected void processFocusEvent(FocusEvent evt) {
        super.processFocusEvent(evt);
    }

    /**
     * It attends the menu's keys.
     *
     * <p>With the menu open, the arrows and Escape do not go to the component that has the focus
     * but to the menu's walk. Otherwise, opening a menu over a text field would make the arrows
     * move the caret instead of walking the options.
     */
    protected void processKeyEvent(KeyEvent evt) {
        MenuSelectionManager.defaultManager().processKeyEvent(evt);
        if (evt.isConsumed()) {
            return;
        }
        super.processKeyEvent(evt);
    }

    public SingleSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public void setSelectionModel(SingleSelectionModel model) {
        selectionModel = model;
    }

    public JMenuItem add(JMenuItem menuItem) {
        super.add(menuItem);
        return menuItem;
    }

    /** It adds an option with that text. */
    public JMenuItem add(String s) {
        return add(new JMenuItem(s));
    }

    /** It adds an option that fires that action. */
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

    /**
     * The bridge between an action and the option that shows it.
     *
     * <p>It returns null: the option listens to the action itself from
     * {@link JMenuItem#setAction}. The method stays because it is protected and somebody may
     * override it.
     */
    protected PropertyChangeListener createActionChangeListener(JMenuItem b) {
        return null;
    }

    public void remove(int pos) {
        super.remove(pos);
    }

    /** Whether the lightweight form is asked for; see the class note. */
    public void setLightWeightPopupEnabled(boolean aFlag) {
        lightWeightPopup = aFlag;
    }

    public boolean isLightWeightPopupEnabled() {
        return lightWeightPopup;
    }

    /** The title some looks and feels draw above the menu. */
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        String oldValue = this.label;
        this.label = label;
        firePropertyChange("label", oldValue, label);
        invalidate();
        repaint();
    }

    /** It adds a separating line. */
    public void addSeparator() {
        add(new Separator());
    }

    /** It inserts an option that fires that action, at that position. */
    public void insert(Action a, int index) {
        JMenuItem mi = createActionComponent(a);
        mi.setAction(a);
        insert(mi, index);
    }

    public void insert(Component component, int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index less than zero.");
        }
        super.add(component, index);
    }

    public void addPopupMenuListener(PopupMenuListener l) {
        listenerList.add(PopupMenuListener.class, l);
    }

    public void removePopupMenuListener(PopupMenuListener l) {
        listenerList.remove(PopupMenuListener.class, l);
    }

    public PopupMenuListener[] getPopupMenuListeners() {
        return listenerList.getListeners(PopupMenuListener.class);
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

    /**
     * It gives notice that the menu is about to open.
     *
     * <p>It arrives <em>before</em> showing it, which is what allows the options to be built
     * according to where the click was made. A context menu that is built after appearing would be
     * seen changing.
     */
    protected void firePopupMenuWillBecomeVisible() {
        Object[] listeners = listenerList.getListenerList();
        PopupMenuEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == PopupMenuListener.class) {
                if (e == null) {
                    e = new PopupMenuEvent(this);
                }
                ((PopupMenuListener) listeners[i + 1]).popupMenuWillBecomeVisible(e);
            }
        }
    }

    protected void firePopupMenuWillBecomeInvisible() {
        Object[] listeners = listenerList.getListenerList();
        PopupMenuEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == PopupMenuListener.class) {
                if (e == null) {
                    e = new PopupMenuEvent(this);
                }
                ((PopupMenuListener) listeners[i + 1]).popupMenuWillBecomeInvisible(e);
            }
        }
    }

    /** It gives notice that the menu closed without anything being chosen. */
    protected void firePopupMenuCanceled() {
        Object[] listeners = listenerList.getListenerList();
        PopupMenuEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == PopupMenuListener.class) {
                if (e == null) {
                    e = new PopupMenuEvent(this);
                }
                ((PopupMenuListener) listeners[i + 1]).popupMenuCanceled(e);
            }
        }
    }

    /** It gives the menu the size its options ask for. */
    public void pack() {
        if (popupSize == null) {
            setSize(getPreferredSize());
        } else {
            setSize(popupSize);
        }
    }

    /**
     * It shows or hides the menu.
     *
     * <p>Showing it is not making it visible: a {@link Popup} has to be asked of the look and
     * feel. See the class note.
     */
    public void setVisible(boolean b) {
        if (b == isVisible()) {
            return;
        }
        if (b) {
            firePopupMenuWillBecomeVisible();
            PopupMenuUI ui = getUI();
            if (ui != null) {
                popup = ui.getPopup(this, desiredLocationX, desiredLocationY);
                pack();
                popup.show();
            }
            visible = true;
            firePropertyChange("visible", Boolean.FALSE, Boolean.TRUE);
        } else {
            firePopupMenuWillBecomeInvisible();
            if (popup != null) {
                popup.hide();
                popup = null;
            }
            visible = false;
            firePropertyChange("visible", Boolean.TRUE, Boolean.FALSE);
        }
    }

    public boolean isVisible() {
        return visible;
    }

    /** Where it will appear, in screen coordinates. */
    public void setLocation(int x, int y) {
        desiredLocationX = x;
        desiredLocationY = y;
        super.setLocation(x, y);
    }

    /** The component it was opened from; see the class note. */
    public Component getInvoker() {
        return this.invoker;
    }

    public void setInvoker(Component invoker) {
        Component oldInvoker = this.invoker;
        this.invoker = invoker;
        firePropertyChange("invoker", oldInvoker, invoker);
        invalidate();
    }

    /**
     * It opens the menu at that point, relative to the invoker.
     *
     * <p>The coordinates are the invoker's and not the screen's: a context menu is asked for where
     * the click was made, and that point comes in the coordinates of the component that received
     * the click.
     */
    public void show(Component invoker, int x, int y) {
        setInvoker(invoker);
        if (invoker != null) {
            java.awt.Point p = invoker.getLocationOnScreen();
            setLocation(p.x + x, p.y + y);
        } else {
            setLocation(x, y);
        }
        setVisible(true);
    }

    /**
     * Component number such-and-such.
     *
     * @deprecated Use {@link java.awt.Container#getComponent(int)}.
     */
    @Deprecated
    public Component getComponentAtIndex(int i) {
        return getComponent(i);
    }

    public int getComponentIndex(Component c) {
        int ncomponents = this.getComponentCount();
        Component[] component = this.getComponents();
        for (int i = 0; i < ncomponents; i++) {
            if (component[i] == c) {
                return i;
            }
        }
        return -1;
    }

    /** It fixes the menu's size instead of letting the options ask for it. */
    public void setPopupSize(Dimension d) {
        popupSize = d;
        if (popup != null) {
            pack();
        }
    }

    public void setPopupSize(int width, int height) {
        setPopupSize(new Dimension(width, height));
    }

    /** It marks that component as the walk's chosen one. */
    public void setSelected(Component sel) {
        SingleSelectionModel model = getSelectionModel();
        int index = getComponentIndex(sel);
        model.setSelectedIndex(index);
    }

    public boolean isBorderPainted() {
        return paintBorder;
    }

    public void setBorderPainted(boolean b) {
        paintBorder = b;
        repaint();
    }

    protected void paintBorder(Graphics g) {
        if (isBorderPainted()) {
            super.paintBorder(g);
        }
    }

    public Insets getMargin() {
        if (margin == null) {
            return new Insets(0, 0, 0, 0);
        }
        return margin;
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    public void processMouseEvent(MouseEvent event, MenuElement[] path,
            MenuSelectionManager manager) {
    }

    public void processKeyEvent(KeyEvent e, MenuElement[] path,
            MenuSelectionManager manager) {
    }

    /** The menu's walk passed through here, or stopped passing. */
    public void menuSelectionChanged(boolean isIncluded) {
        if (invoker instanceof JMenu) {
            // A submenu opens and closes following the walk; the mouse does not decide it.
            setVisible(isIncluded);
        } else if (!isIncluded) {
            setVisible(false);
        }
    }

    /** The options inside that take part in the walk. */
    public MenuElement[] getSubElements() {
        java.util.Vector<MenuElement> tmp = new java.util.Vector<MenuElement>();
        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c instanceof MenuElement) {
                tmp.addElement((MenuElement) c);
            }
        }
        MenuElement[] result = new MenuElement[tmp.size()];
        tmp.copyInto(result);
        return result;
    }

    public Component getComponent() {
        return this;
    }

    /** Whether that event is the gesture that opens the menu; the look and feel answers it. */
    public boolean isPopupTrigger(MouseEvent e) {
        PopupMenuUI ui = getUI();
        return (ui != null) && ui.isPopupTrigger(e);
    }

    /**
     * The line that separates groups of options.
     *
     * <p>It is a {@link JSeparator} with another look and feel name: a menu's line is drawn
     * differently from a loose one -- with the menu's margins -- and the name is what allows the
     * look and feel to tell them apart.
     */
    public static class Separator extends JSeparator {

        public Separator() {
            super(JSeparator.HORIZONTAL);
        }

        public String getUIClassID() {
            return "PopupMenuSeparatorUI";
        }
    }
}
