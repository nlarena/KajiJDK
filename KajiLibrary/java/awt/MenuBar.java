package java.awt;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * La barra de menús de un marco.
 *
 * <p>Es un {@link MenuComponent} y no un {@link Component}: la barra no vive en el espacio de la
 * ventana sino que la dibuja el escritorio, arriba de todo. Por eso no tiene posición ni tamaño.
 *
 * <p>El **menú de ayuda** tiene lugar propio porque las plataformas lo tratan distinto: en algunas va
 * pegado a la derecha, separado del resto. Declararlo permite que cada escritorio lo ubique como
 * corresponde en vez de dejarlo como uno más.
 */
public class MenuBar extends MenuComponent implements MenuContainer, Accessible {

    private static final long serialVersionUID = -4930327919388951260L;

    private final List<Menu> menus = new ArrayList<Menu>();
    private Menu helpMenu;

    /**
     * Una barra vacía.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public MenuBar() throws HeadlessException {
    }

    /** Avisa que puede mostrarse, y se lo avisa a sus menús. */
    public void addNotify() {
        synchronized (this.getTreeLock()) {
            for (int i = 0; i < this.menus.size(); i++) {
                this.menus.get(i).addNotify();
            }
        }
    }

    /** Avisa que dejó de poder mostrarse. */
    public void removeNotify() {
        synchronized (this.getTreeLock()) {
            for (int i = 0; i < this.menus.size(); i++) {
                this.menus.get(i).removeNotify();
            }
        }
        super.removeNotify();
    }

    /**
     * El menú de ayuda.
     *
     * @return el menú, o `null` si no hay
     */
    public Menu getHelpMenu() {
        return this.helpMenu;
    }

    /**
     * Declara cuál es el menú de ayuda.
     *
     * <p>Si estaba en otra barra se lo saca de ahí: un menú cuelga de un solo padre.
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
     * Agrega un menú al final.
     *
     * @return el mismo menú, para poder encadenar
     * @throws NullPointerException si el menú es `null`
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
     * Saca el menú de esa posición.
     *
     * @throws ArrayIndexOutOfBoundsException si no existe
     */
    public void remove(int index) {
        synchronized (this.getTreeLock()) {
            Menu m = this.menus.remove(index);
            m.setParent(null);
        }
    }

    /** Saca ese menú; si no estaba, no pasa nada. */
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

    /** Cuántos menús tiene, sin contar el de ayuda. */
    public int getMenuCount() {
        return this.countMenus();
    }

    /**
     * Cuántos menús tiene.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #getMenuCount}.
     */
    @Deprecated
    public int countMenus() {
        synchronized (this.getTreeLock()) {
            return this.menus.size();
        }
    }

    /**
     * El menú de esa posición.
     *
     * @throws ArrayIndexOutOfBoundsException si no existe
     */
    public Menu getMenu(int i) {
        synchronized (this.getTreeLock()) {
            return this.menus.get(i);
        }
    }

    /** Todos los atajos de teclado de la barra. */
    public synchronized Enumeration<MenuShortcut> shortcuts() {
        Vector<MenuShortcut> v = new Vector<MenuShortcut>();
        this.juntarAtajos(this.menus, v);
        return v.elements();
    }

    /** Recorre los menús juntando los atajos de sus opciones. */
    private void juntarAtajos(List<Menu> desde, Vector<MenuShortcut> v) {
        for (int i = 0; i < desde.size(); i++) {
            Menu m = desde.get(i);
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
     * Qué opción tiene ese atajo.
     *
     * @return la opción, o `null` si ninguna
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

    /** Le saca ese atajo a la opción que lo tenga. */
    public void deleteShortcut(MenuShortcut s) {
        MenuItem mi = this.getShortcutMenuItem(s);
        if (mi != null) {
            mi.deleteShortcut();
        }
    }

    /** La información de accesibilidad de esta barra. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTMenuBar();
        }
        return this.accessibleContext;
    }

    /** La accesibilidad de una barra de menús. */
    protected class AccessibleAWTMenuBar extends AccessibleAWTMenuComponent {

        /** Para las subclases. */
        protected AccessibleAWTMenuBar() {
        }

        /** Es una barra de menús. */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.MENU_BAR;
        }

        /** Cuántos menús tiene. */
        public int getAccessibleChildrenCount() {
            return MenuBar.this.getMenuCount();
        }

        /**
         * El menú de esa posición.
         *
         * @return el menú, o `null` si no existe
         */
        public Accessible getAccessibleChild(int i) {
            if (i < 0 || i >= MenuBar.this.getMenuCount()) {
                return null;
            }
            return MenuBar.this.getMenu(i);
        }
    }
}
