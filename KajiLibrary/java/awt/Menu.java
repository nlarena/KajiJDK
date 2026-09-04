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
 * Un menú: una opción que, al elegirse, despliega otras.
 *
 * <p>Hereda de {@link MenuItem} y eso es lo que hace que los submenús salgan gratis: un menú **es**
 * una opción, así que meterlo adentro de otro menú funciona sin ningún caso especial. Es la misma
 * idea de composición que hace que un contenedor sea un componente.
 *
 * <p>Un menú **desprendible** se puede arrancar de la barra y dejar flotando como una ventanita.
 * Casi ningún escritorio moderno lo hace, pero la bandera sigue en la API y se conserva.
 */
public class Menu extends MenuItem implements MenuContainer, Accessible {

    private static final long serialVersionUID = -8809584163345499784L;

    private final List<MenuItem> items = new ArrayList<MenuItem>();
    private final boolean tearOff;

    /**
     * Un menú sin etiqueta.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public Menu() throws HeadlessException {
        this("", false);
    }

    /**
     * Con esa etiqueta.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public Menu(String label) throws HeadlessException {
        this(label, false);
    }

    /**
     * Con etiqueta y desprendible o no.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public Menu(String label, boolean tearOff) throws HeadlessException {
        super(label);
        this.tearOff = tearOff;
    }

    /** Avisa que puede mostrarse, y se lo avisa a sus hijos. */
    public void addNotify() {
        synchronized (this.getTreeLock()) {
            for (int i = 0; i < this.items.size(); i++) {
                this.items.get(i).addNotify();
            }
        }
    }

    /** Avisa que dejó de poder mostrarse, y se lo avisa a sus hijos. */
    public void removeNotify() {
        synchronized (this.getTreeLock()) {
            for (int i = 0; i < this.items.size(); i++) {
                this.items.get(i).removeNotify();
            }
        }
        super.removeNotify();
    }

    /** Si se puede arrancar de la barra. */
    public boolean isTearOff() {
        return this.tearOff;
    }

    /** Cuántas opciones tiene. */
    public int getItemCount() {
        return this.items.size();
    }

    /**
     * Cuántas opciones tiene.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #getItemCount}.
     */
    @Deprecated
    public int countItems() {
        return this.getItemCount();
    }

    /**
     * La opción de esa posición.
     *
     * @throws ArrayIndexOutOfBoundsException si no existe
     */
    public MenuItem getItem(int index) {
        return this.items.get(index);
    }

    /**
     * Agrega una opción al final.
     *
     * <p>Si ya estaba en otro menú se la saca de ahí primero: una opción cuelga de un solo padre.
     *
     * @return la misma opción, para poder encadenar
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

    /** Agrega una opción con esa etiqueta. */
    public void add(String label) {
        this.add(new MenuItem(label));
    }

    /**
     * Inserta una opción en esa posición.
     *
     * @throws IllegalArgumentException si la posición es negativa
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
     * Inserta una opción con esa etiqueta.
     *
     * @throws IllegalArgumentException si la posición es negativa
     */
    public void insert(String label, int index) {
        this.insert(new MenuItem(label), index);
    }

    /**
     * Agrega una línea separadora.
     *
     * <p>Un separador es una opción con la etiqueta `"-"`: no es un tipo aparte, y por eso se puede
     * sacar con {@link #remove(int)} como cualquier otra.
     */
    public void addSeparator() {
        this.add("-");
    }

    /**
     * Inserta una línea separadora.
     *
     * @throws IllegalArgumentException si la posición es negativa
     */
    public void insertSeparator(int index) {
        this.insert("-", index);
    }

    /**
     * Saca la opción de esa posición.
     *
     * @throws ArrayIndexOutOfBoundsException si no existe
     */
    public void remove(int index) {
        synchronized (this.getTreeLock()) {
            MenuItem mi = this.items.remove(index);
            mi.setParent(null);
        }
    }

    /** Saca esa opción; si no estaba, no pasa nada. */
    public void remove(MenuComponent item) {
        synchronized (this.getTreeLock()) {
            int i = this.items.indexOf(item);
            if (i >= 0) {
                this.remove(i);
            }
        }
    }

    /** Saca todas. */
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

    /** La información de accesibilidad de este menú. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTMenu();
        }
        return this.accessibleContext;
    }

    /** La accesibilidad de un menú. */
    protected class AccessibleAWTMenu extends AccessibleAWTMenuItem {

        /** Para las subclases. */
        protected AccessibleAWTMenu() {
        }

        /** Es un menú. */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.MENU;
        }

        /** Cuántas opciones tiene. */
        public int getAccessibleChildrenCount() {
            return Menu.this.getItemCount();
        }

        /**
         * La opción de esa posición, si es accesible.
         *
         * @return la opción, o `null` si no existe
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
