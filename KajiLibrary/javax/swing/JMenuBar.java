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
 * La barra de menu de una ventana.
 *
 * <h2>Es un contenedor y un elemento de menu</h2>
 *
 * <p>Como contenedor tiene a los {@link JMenu} de arriba; como {@link MenuElement} participa del
 * recorrido con el teclado y el mouse. Lo segundo es lo que hace que moverse con las flechas entre
 * dos menus de la barra funcione: el recorrido pasa por la barra, no salta de un menu al otro.
 *
 * <h2>El menu de ayuda</h2>
 *
 * <p>{@link #setHelpMenu} existe porque en algunos sistemas ese menu va pegado a la derecha. No
 * esta implementado -- ni en el JDK -- y lanza {@code Error}: es preferible a guardarlo y no
 * hacerle nada, que dejaria al programa creyendo que lo puso.
 */
public class JMenuBar extends JComponent implements Accessible, MenuElement {

    private static final String uiClassID = "MenuBarUI";

    private transient SingleSelectionModel selectionModel;
    private boolean paintBorder = true;
    private Insets margin = null;
    private AccessibleContext accessibleContext;

    /** Una barra vacia. */
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

    /** Cual menu de la barra esta abierto. */
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
     * El menu numero tal.
     *
     * <p>Devuelve nulo si en esa posicion hay algo que no es un menu; la barra puede tener otros
     * componentes.
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
     * El menu de ayuda.
     *
     * @throws Error siempre; ver la nota de la clase.
     */
    public void setHelpMenu(JMenu menu) {
        throw new Error("setHelpMenu() not yet implemented.");
    }

    /**
     * El menu de ayuda.
     *
     * @throws Error siempre.
     */
    public JMenu getHelpMenu() {
        throw new Error("getHelpMenu() not yet implemented.");
    }

    /**
     * El componente numero tal.
     *
     * @deprecated Usar {@link java.awt.Container#getComponent(int)}.
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

    /** Abre ese menu de la barra. */
    public void setSelected(Component sel) {
        SingleSelectionModel model = getSelectionModel();
        int index = getComponentIndex(sel);
        model.setSelectedIndex(index);
    }

    /** Si algun menu de la barra esta abierto. */
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

    /** El recorrido dejo la barra: se cierra lo que hubiera abierto. */
    public void menuSelectionChanged(boolean isIncluded) {
        if (!isIncluded) {
            getSelectionModel().clearSelection();
        }
    }

    /** Los menus de la barra que participan del recorrido. */
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
