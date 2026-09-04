package java.awt;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * Un contenedor de **un solo hijo** que muestra una parte de él y deja desplazarse por el resto.
 *
 * <p>Lo de "un solo hijo" es literal: agregar un segundo saca al primero. Cuando hay que desplazar
 * varias cosas, se mete un {@link Panel} adentro y los componentes van en él.
 *
 * <p>Tiene su distribución propia y {@link #setLayout} es `final`: cambiarla rompería la única cosa
 * que el panel hace.
 */
public class ScrollPane extends Container implements Accessible {

    private static final long serialVersionUID = 7956609840827222915L;

    private static int scrollPaneCounter = 0;

    /** Muestra las barras sólo cuando el hijo no entra. */
    public static final int SCROLLBARS_AS_NEEDED = 0;

    /** Las muestra siempre. */
    public static final int SCROLLBARS_ALWAYS = 1;

    /** No las muestra nunca; el desplazamiento queda sólo por programa. */
    public static final int SCROLLBARS_NEVER = 2;

    /** Cuál de las tres políticas se pidió. */
    private final int scrollbarDisplayPolicy;

    /** La barra vertical. */
    private final ScrollPaneAdjustable vAdjustable;

    /** La horizontal. */
    private final ScrollPaneAdjustable hAdjustable;

    /** Si la rueda del mouse lo desplaza. */
    private boolean wheelScrollingEnabled = true;

    /** Un panel que muestra las barras cuando hacen falta. */
    public ScrollPane() throws HeadlessException {
        this(SCROLLBARS_AS_NEEDED);
    }

    /**
     * Un panel con esa política de barras.
     *
     * @throws IllegalArgumentException si la política no es una de las tres
     */
    public ScrollPane(int scrollbarDisplayPolicy) throws HeadlessException {
        if (scrollbarDisplayPolicy != SCROLLBARS_AS_NEEDED
                && scrollbarDisplayPolicy != SCROLLBARS_ALWAYS
                && scrollbarDisplayPolicy != SCROLLBARS_NEVER) {
            throw new IllegalArgumentException("illegal scrollbar display policy");
        }
        this.scrollbarDisplayPolicy = scrollbarDisplayPolicy;
        this.vAdjustable = new ScrollPaneAdjustable(this, null, Adjustable.VERTICAL);
        this.hAdjustable = new ScrollPaneAdjustable(this, null, Adjustable.HORIZONTAL);
        super.setLayout(null);
    }

    String constructComponentName() {
        synchronized (ScrollPane.class) {
            String n = "scrollpane" + scrollPaneCounter;
            scrollPaneCounter = scrollPaneCounter + 1;
            return n;
        }
    }

    /**
     * Agrega el hijo, sacando al que hubiera.
     *
     * <p>Es `final` y hace desaparecer al anterior a propósito: un panel de desplazamiento con dos
     * hijos no tiene sentido, y dejar que se agreguen para después ignorar a uno sería peor.
     */
    protected final void addImpl(Component comp, Object constraints, int index) {
        if (this.getComponentCount() > 0) {
            this.remove(0);
        }
        super.addImpl(comp, constraints, 0);
    }

    /** Qué política de barras se pidió. */
    public int getScrollbarDisplayPolicy() {
        return this.scrollbarDisplayPolicy;
    }

    /**
     * Cuánto se ve del hijo.
     *
     * <p>Como las barras no ocupan lugar sin pantalla, es el tamaño del panel menos sus márgenes.
     */
    public Dimension getViewportSize() {
        Insets i = this.getInsets();
        return new Dimension(this.getWidth() - i.left - i.right,
                this.getHeight() - i.top - i.bottom);
    }

    /**
     * Cuánto alto se lleva la barra horizontal.
     *
     * @return 0: sin pantalla no hay barra dibujada que ocupe lugar
     */
    public int getHScrollbarHeight() {
        return 0;
    }

    /**
     * Cuánto ancho se lleva la vertical.
     *
     * @return 0, por lo mismo
     */
    public int getVScrollbarWidth() {
        return 0;
    }

    /** La barra vertical. */
    public Adjustable getVAdjustable() {
        return this.vAdjustable;
    }

    /** La horizontal. */
    public Adjustable getHAdjustable() {
        return this.hAdjustable;
    }

    /**
     * Desplaza a esa posición del hijo.
     *
     * <p>Se recorta a lo que se puede desplazar de verdad, que es el tamaño del hijo menos el de la
     * ventanilla. Desplazar más allá dejaría un vacío abajo del contenido.
     *
     * @throws NullPointerException si el panel no tiene hijo
     */
    public void setScrollPosition(int x, int y) {
        synchronized (this.getTreeLock()) {
            if (this.getComponentCount() == 0) {
                throw new NullPointerException("Child does not exist");
            }
            Component hijo = this.getComponent(0);
            Dimension v = this.getViewportSize();
            int maxX = Math.max(0, hijo.getWidth() - v.width);
            int maxY = Math.max(0, hijo.getHeight() - v.height);
            int nx = Math.max(0, Math.min(x, maxX));
            int ny = Math.max(0, Math.min(y, maxY));
            Insets i = this.getInsets();
            hijo.setLocation(i.left - nx, i.top - ny);
            this.ajustarBarras();
        }
    }

    /**
     * Desplaza a esa posición.
     *
     * @throws NullPointerException si el punto es `null` o el panel no tiene hijo
     */
    public void setScrollPosition(Point p) {
        this.setScrollPosition(p.x, p.y);
    }

    /**
     * Por dónde va el desplazamiento.
     *
     * @throws NullPointerException si el panel no tiene hijo
     */
    public Point getScrollPosition() {
        synchronized (this.getTreeLock()) {
            if (this.getComponentCount() == 0) {
                throw new NullPointerException("Child does not exist");
            }
            Component hijo = this.getComponent(0);
            Insets i = this.getInsets();
            return new Point(i.left - hijo.getX(), i.top - hijo.getY());
        }
    }

    /**
     * No se puede cambiar la distribución.
     *
     * @throws AWTError siempre: el panel tiene la suya y es lo único que hace
     */
    public final void setLayout(LayoutManager mgr) {
        throw new AWTError("ScrollPane controls layout");
    }

    /**
     * Acomoda al hijo.
     *
     * <p>Le da su tamaño preferido, o el de la ventanilla si el preferido es más chico: un hijo más
     * chico que la ventanilla la llena en vez de dejar un borde sin usar.
     */
    public void doLayout() {
        this.layout();
    }

    /** Cuánto tiene que medir el hijo. */
    Dimension calculateChildSize() {
        Component hijo = this.getComponent(0);
        Dimension p = hijo.getPreferredSize();
        Dimension v = this.getViewportSize();
        return new Dimension(Math.max(p.width, v.width), Math.max(p.height, v.height));
    }

    /**
     * Acomoda al hijo.
     *
     * @deprecated es del nombrado de 1.1. Usar {@link #doLayout}.
     */
    @Deprecated
    public void layout() {
        synchronized (this.getTreeLock()) {
            if (this.getComponentCount() == 0) {
                return;
            }
            Component hijo = this.getComponent(0);
            Dimension d = this.calculateChildSize();
            hijo.setSize(d.width, d.height);
            this.ajustarBarras();
        }
    }

    /** Pone el rango de las dos barras a partir del tamaño del hijo y de la ventanilla. */
    private void ajustarBarras() {
        if (this.getComponentCount() == 0) {
            return;
        }
        Component hijo = this.getComponent(0);
        Dimension v = this.getViewportSize();
        this.hAdjustable.setSpan(0, hijo.getWidth(), v.width);
        this.vAdjustable.setSpan(0, hijo.getHeight(), v.height);
    }

    /**
     * Imprime a los hijos.
     *
     * <p>No hace nada: imprimir necesita un {@link Graphics} de verdad y esta implementación no
     * tiene rasterizador.
     */
    public void printComponents(Graphics g) {
    }

    /** Lo declara mostrable. */
    public void addNotify() {
        super.addNotify();
    }

    public String paramString() {
        String politica = "as-needed";
        if (this.scrollbarDisplayPolicy == SCROLLBARS_ALWAYS) {
            politica = "always";
        } else if (this.scrollbarDisplayPolicy == SCROLLBARS_NEVER) {
            politica = "never";
        }
        String pos = "";
        if (this.getComponentCount() > 0) {
            Point p = this.getScrollPosition();
            pos = ",ScrollPosition=(" + p.x + "," + p.y + ")";
        }
        return super.paramString() + pos + ",Insets=" + this.getInsets()
                + ",ScrollbarDisplayPolicy=" + politica + ",wheelScrollingEnabled="
                + this.wheelScrollingEnabled;
    }

    /** Desplaza según la rueda, si está habilitada. */
    void autoProcessMouseWheel(MouseWheelEvent e) {
        this.processMouseWheelEvent(e);
    }

    /**
     * Atiende la rueda del mouse.
     *
     * <p>Desplaza vertical por unidades, que es lo que hace el JDK cuando la rueda pide
     * {@link MouseWheelEvent#WHEEL_UNIT_SCROLL}.
     */
    protected void processMouseWheelEvent(MouseWheelEvent e) {
        if (this.wheelScrollingEnabled && !e.isConsumed() && this.getComponentCount() > 0) {
            int cuanto = e.getUnitsToScroll() * this.vAdjustable.getUnitIncrement();
            this.vAdjustable.setValue(this.vAdjustable.getValue() + cuanto);
            e.consume();
        }
        super.processMouseWheelEvent(e);
    }

    /** Si le interesa esa familia de eventos; la rueda le interesa siempre que esté habilitada. */
    protected boolean eventTypeEnabled(int type) {
        if (type == MouseEvent.MOUSE_WHEEL) {
            return this.isWheelScrollingEnabled();
        }
        return false;
    }

    /** Prende o apaga el desplazamiento con la rueda. */
    public void setWheelScrollingEnabled(boolean handleWheel) {
        this.wheelScrollingEnabled = handleWheel;
    }

    /** Si la rueda lo desplaza. */
    public boolean isWheelScrollingEnabled() {
        return this.wheelScrollingEnabled;
    }

    /** La accesibilidad del panel. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTScrollPane();
        }
        return this.accessibleContext;
    }

    /** Un panel de desplazamiento, para la accesibilidad, es un panel de desplazamiento. */
    protected class AccessibleAWTScrollPane extends AccessibleAWTContainer {

        /** Para las subclases. */
        protected AccessibleAWTScrollPane() {
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.SCROLL_PANE;
        }
    }
}
