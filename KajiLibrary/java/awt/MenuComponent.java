package java.awt;

import java.io.Serializable;
import java.util.Locale;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleStateSet;

/**
 * La raíz de todo lo que vive en un menú.
 *
 * <p>Es el equivalente de {@link Component} para los menús, y que sean dos jerarquías separadas no
 * es un descuido de diseño sino una consecuencia: los menús los dibuja el sistema operativo, no el
 * programa. Un elemento de menú no tiene posición ni tamaño en el espacio de la ventana porque no
 * está en la ventana.
 *
 * <p>De ahí que esta clase sea tan chica comparada con `Component`: nombre, fuente, padre, y el
 * reparto de eventos. Todo lo geométrico no existe.
 */
public abstract class MenuComponent implements Serializable {

    private static final long serialVersionUID = -4536902356436156350L;

    private String name;
    private boolean nameExplicitlySet;
    private Font font;
    private MenuContainer parent;

    /** El contexto de accesibilidad, armado a demanda. */
    protected AccessibleContext accessibleContext;

    private static int nameCounter;

    /**
     * Un componente de menú.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public MenuComponent() throws HeadlessException {
    }

    /** El nombre por omisión, distinto para cada uno. */
    String constructComponentName() {
        synchronized (MenuComponent.class) {
            String n = this.getClass().getName() + nameCounter;
            nameCounter = nameCounter + 1;
            return n;
        }
    }

    /**
     * Cómo se llama.
     *
     * <p>Si nadie le puso nombre, se le arma uno: sirve para depurar, y devolver `null` obligaría a
     * comprobarlo en cada traza.
     */
    public String getName() {
        if (this.name == null && !this.nameExplicitlySet) {
            synchronized (this) {
                if (this.name == null && !this.nameExplicitlySet) {
                    this.name = this.constructComponentName();
                }
            }
        }
        return this.name;
    }

    /** Le pone nombre. */
    public void setName(String name) {
        synchronized (this) {
            this.name = name;
            this.nameExplicitlySet = true;
        }
    }

    /** De qué menú o barra cuelga, o `null`. */
    public MenuContainer getParent() {
        return this.parent;
    }

    /** Lo usa el contenedor al agregarlo o sacarlo. */
    void setParent(MenuContainer p) {
        this.parent = p;
    }

    /**
     * La fuente con la que se dibuja.
     *
     * <p>Si no tiene una propia, se hereda la del padre. Devolver `null` cuando no hay ninguna en
     * toda la cadena es correcto: significa que la decide el sistema.
     */
    public Font getFont() {
        Font f = this.font;
        if (f != null) {
            return f;
        }
        MenuContainer p = this.parent;
        if (p != null) {
            return p.getFont();
        }
        return null;
    }

    /** Le pone fuente propia. */
    public void setFont(Font f) {
        synchronized (this) {
            this.font = f;
        }
    }

    /** Avisa que dejó de poder mostrarse. */
    public void removeNotify() {
    }

    /**
     * Le manda un evento del modelo viejo.
     *
     * @deprecated es del modelo de eventos de 1.0. Usar {@link #dispatchEvent}.
     */
    @Deprecated
    public boolean postEvent(Event evt) {
        MenuContainer p = this.parent;
        if (p != null) {
            return p.postEvent(evt);
        }
        return false;
    }

    /**
     * Le entrega un evento.
     *
     * <p>Es `final` y delega en {@link #processEvent}: el reparto no se redefine, lo que se
     * redefine es qué se hace con el evento.
     */
    public final void dispatchEvent(AWTEvent e) {
        this.processEvent(e);
    }

    /** Atiende el evento; las subclases lo redefinen. */
    protected void processEvent(AWTEvent e) {
    }

    /** La descripción del componente, sin el nombre de la clase. */
    protected String paramString() {
        return "name=" + this.getName();
    }

    public String toString() {
        return this.getClass().getName() + "[" + this.paramString() + "]";
    }

    /**
     * El candado con el que se sincroniza el árbol de menús.
     *
     * <p>Es el mismo objeto para todo AWT, y ésa es la idea: un solo candado global evita que
     * bloquear dos ramas del árbol en distinto orden termine en un abrazo mortal.
     */
    protected final Object getTreeLock() {
        return Component.LOCK;
    }

    /** La información de accesibilidad de este componente de menú. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTMenuComponent();
        }
        return this.accessibleContext;
    }

    /**
     * La accesibilidad de un componente de menú.
     *
     * <p>Es lo mínimo honesto: nombre, rol y padre. Sin sistema de ventanas no hay estados de
     * pantalla que informar, así que el conjunto de estados sale vacío en vez de inventado.
     */
    protected class AccessibleAWTMenuComponent extends AccessibleContext {

        /** Para las subclases. */
        protected AccessibleAWTMenuComponent() {
        }

        /** Desconocido; las subclases concretas lo afinan. */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.AWT_COMPONENT;
        }

        /** Vacío: no hay pantalla de la que sacar estados. */
        public AccessibleStateSet getAccessibleStateSet() {
            return new AccessibleStateSet();
        }

        /** El nombre del componente de menú. */
        public String getAccessibleName() {
            return MenuComponent.this.getName();
        }

        /** Cero: un componente de menú simple no tiene hijos. */
        public int getAccessibleChildrenCount() {
            return 0;
        }

        /** Siempre `null`. */
        public javax.accessibility.Accessible getAccessibleChild(int i) {
            return null;
        }

        /** Su posición dentro del padre, o -1 si no se sabe. */
        public int getAccessibleIndexInParent() {
            return -1;
        }

        /** El idioma por omisión: un menú no tiene uno propio. */
        public Locale getLocale() {
            return Locale.getDefault();
        }
    }
}
