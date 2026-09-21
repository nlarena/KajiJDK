package javax.swing;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.MenuBarUI;

/**
 * A window's menu bar.
 *
 * <h2>It is a container and a menu element</h2>
 *
 * <p>As a container it holds the top {@link JMenu}s; as a {@link MenuElement} it takes part in
 * the walk with the keyboard and the mouse. The second is what makes moving with the arrows
 * between two menus of the bar work: the walk goes through the bar, it does not jump from one
 * menu to the other.
 *
 * <h2>The help menu</h2>
 *
 * <p>{@link #setHelpMenu} exists because on some systems that menu goes stuck to the right. It
 * is not implemented -- not in the JDK either -- and throws {@code Error}: it is preferable to
 * keeping it and doing nothing with it, which would leave the program believing it had set
 * it.
 */
public class JMenuBar extends JComponent implements Accessible, MenuElement {

    private static final String uiClassID = "MenuBarUI";

    private transient SingleSelectionModel selectionModel;
    private boolean paintBorder = true;
    private Insets margin = null;
    private AccessibleContext accessibleContext;

    /** An empty bar. */
    public JMenuBar() {
        super();
        setSelectionModel(new DefaultSingleSelectionModel());
        setFocusTraversalKeysEnabled(false);
        updateUI();
    }

    public MenuBarUI getUI() {
        return (MenuBarUI) ui;
    }

    public void setUI(MenuBarUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** Which menu of the bar is open. */
    public SingleSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public void setSelectionModel(SingleSelectionModel model) {
        SingleSelectionModel oldValue = selectionModel;
        this.selectionModel = model;
        firePropertyChange("selectionModel", oldValue, model);
    }

    public JMenu add(JMenu c) {
        super.add(c);
        return c;
    }

    /**
     * Menu number such-and-such.
     *
     * <p>It returns null if at that position there is something that is not a menu; the bar may
     * have other components.
     */
    public JMenu getMenu(int index) {
        Component c = getComponentAtIndex(index);
        if (c instanceof JMenu) {
            return (JMenu) c;
        }
        return null;
    }

    public int getMenuCount() {
        return getComponentCount();
    }

    /**
     * The help menu.
     *
     * @throws Error always; see the class note.
     */
    public void setHelpMenu(JMenu menu) {
        throw new Error("setHelpMenu() not yet implemented.");
    }

    /**
     * The help menu.
     *
     * @throws Error always.
     */
    public JMenu getHelpMenu() {
        throw new Error("getHelpMenu() not yet implemented.");
    }

    /**
     * Component number such-and-such.
     *
     * @deprecated Use {@link java.awt.Container#getComponent(int)}.
     */
    @Deprecated
    public Component getComponentAtIndex(int i) {
        if (i < 0 || i >= getComponentCount()) {
            return null;
        }
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

    /** It opens that menu of the bar. */
    public void setSelected(Component sel) {
        SingleSelectionModel model = getSelectionModel();
        int index = getComponentIndex(sel);
        model.setSelectedIndex(index);
    }

    /** Whether some menu of the bar is open. */
    public boolean isSelected() {
        return selectionModel.isSelected();
    }

    public boolean isBorderPainted() {
        return paintBorder;
    }

    public void setBorderPainted(boolean b) {
        boolean oldValue = paintBorder;
        paintBorder = b;
        firePropertyChange("borderPainted", oldValue, paintBorder);
        if (b != oldValue) {
            revalidate();
            repaint();
        }
    }

    protected void paintBorder(Graphics g) {
        if (isBorderPainted()) {
            super.paintBorder(g);
        }
    }

    public void setMargin(Insets m) {
        Insets old = margin;
        this.margin = m;
        firePropertyChange("margin", old, m);
        if (old == null || !old.equals(m)) {
            revalidate();
            repaint();
        }
    }

    public Insets getMargin() {
        if (margin == null) {
            return new Insets(0, 0, 0, 0);
        }
        return margin;
    }

    public void processMouseEvent(MouseEvent event, MenuElement[] path,
            MenuSelectionManager manager) {
    }

    public void processKeyEvent(KeyEvent e, MenuElement[] path,
            MenuSelectionManager manager) {
    }

    /** The walk left the bar: whatever was open is closed. */
    public void menuSelectionChanged(boolean isIncluded) {
        if (!isIncluded) {
            getSelectionModel().clearSelection();
        }
    }

    /** The bar's menus that take part in the walk. */
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

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition,
            boolean pressed) {
        return super.processKeyBinding(ks, e, condition, pressed);
    }

    public void addNotify() {
        super.addNotify();
    }

    public void removeNotify() {
        super.removeNotify();
    }
}
