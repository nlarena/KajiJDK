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
 * Una opcion de menu que abre un submenu.
 *
 * <h2>Es una opcion, y ademas tiene un desplegable</h2>
 *
 * <p>Hereda de {@link JMenuItem}, asi que en la barra o en un menu de arriba se comporta como
 * cualquier opcion. Lo que agrega es un {@link JPopupMenu} propio: lo que se le agrega con
 * {@link #add} no va al menu sino a ese desplegable.
 *
 * <p>Eso explica que existan {@link #getMenuComponentCount} y {@link #getComponentCount} por
 * separado: el primero cuenta las opciones del submenu, el segundo los hijos del componente, que
 * son otra cosa.
 *
 * <h2>Elegido no es desplegado</h2>
 *
 * <p>{@link #setSelected} marca el menu como el que el recorrido esta tocando;
 * {@link #setPopupMenuVisible} abre el desplegable. Casi siempre van juntos, y no siempre: recorrer
 * la barra con el teclado marca menus sin abrirlos hasta que se aprieta la flecha abajo.
 *
 * <h2>El retardo</h2>
 *
 * <p>{@link #setDelay} es cuanto espera antes de abrir el submenu al pasar el mouse. Existe porque
 * sin el, cruzar un menu camino a otro abriria todos los del camino.
 */
public class JMenu extends JMenuItem implements Accessible, MenuElement {

    private static final String uiClassID = "MenuUI";

    private JPopupMenu popupMenu;
    private int delay;
    private Point customMenuLocation = null;

    /** Escucha la ventana del desplegable para cerrarlo cuando se cierra. */
    protected WinListener popupListener;

    /** Un menu vacio sin texto. */
    public JMenu() {
        this("");
    }

    /** Un menu con ese texto. */
    public JMenu(String s) {
        super(s);
    }

    /** Un menu que toma su texto y su icono de esa accion. */
    public JMenu(Action a) {
        this();
        setAction(a);
    }

    /**
     * Un menu con ese texto.
     *
     * @param b si el submenu se puede arrancar y dejar flotando; no esta implementado.
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

    /** Si el recorrido del menu esta tocando este; ver la nota de la clase. */
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
     * Abre o cierra el submenu.
     *
     * <p>Un menu deshabilitado no abre nada aunque se lo pidan: si abriera, el usuario podria
     * elegir de un menu que esta apagado.
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
     * Donde abrir el submenu, relativo a este menu.
     *
     * <p>Abajo si el menu esta en la barra, al costado si esta adentro de otro menu. Es lo que hace
     * que un submenu no tape a su padre.
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
     * Cuanto espera antes de abrir el submenu; ver la nota de la clase.
     *
     * @throws IllegalArgumentException si es negativo.
     */
    public void setDelay(int d) {
        if (d < 0) {
            throw new IllegalArgumentException("Delay must be a positive integer");
        }
        delay = d;
    }

    /** Fuerza donde se abre el submenu, en lugar de calcularlo. */
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

    /** Agrega una opcion con ese texto. */
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

    /** Devuelve nulo; la opcion escucha a la accion ella misma. Ver {@link JPopupMenu}. */
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
     * La opcion numero tal del submenu.
     *
     * <p>Devuelve nulo si en esa posicion hay algo que no es una opcion -- un separador, por
     * ejemplo --. No saltea: la posicion es la del componente.
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

    /** Cuantas cosas hay en el submenu, contando separadores. */
    public int getItemCount() {
        return getMenuComponentCount();
    }

    /**
     * Si el submenu se puede arrancar y dejar flotando.
     *
     * @throws Error siempre: no esta implementado, ni en el JDK.
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

    /** Cuantos componentes hay en el submenu; ver la nota de la clase. */
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

    /** Si esta directamente en la barra de menu y no adentro de otro menu. */
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

    /** El desplegable; se arma la primera vez que hace falta. */
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
     * Avisa que el menu quedo elegido.
     *
     * <p>Es el gancho para armar el menu justo antes de abrirlo: un menu de archivos recientes se
     * llena aca y no al armar la ventana.
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
     * Cierra el menu cuando se cierra la ventana del desplegable.
     *
     * <p>Solo hace falta con un desplegable pesado, que vive en su propia ventana. Sin esto,
     * cerrarla desde el sistema dejaria al menu creyendo que sigue abierto.
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

    /** El recorrido del menu paso por aca: se abre o se cierra el submenu. */
    public void menuSelectionChanged(boolean isIncluded) {
        setSelected(isIncluded);
    }

    /** Las opciones del submenu, con el desplegable adelante. */
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
     * Un menu no lleva atajo.
     *
     * <p>Un atajo dispara una accion; un menu no hace nada, solo abre. Poner uno no tendria que
     * hacer, y por eso se rechaza en lugar de guardarse.
     *
     * @throws Error siempre.
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

    /** Abrir el submenu, que es lo que un menu hace al ser apretado. */
    public void doClick(int pressTime) {
        MenuElement[] me = buildMenuElementArray(this);
        MenuSelectionManager.defaultManager().setSelectedPath(me);
    }

    /** El camino desde el menu de arriba hasta este. */
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
