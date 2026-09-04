package java.awt;

import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.beans.PropertyChangeListener;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.Set;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

/**
 * Un componente que contiene otros componentes.
 *
 * <p>Que un contenedor **sea** un componente es lo que hace que la interfaz se pueda armar como un
 * árbol de profundidad arbitraria sin ningún caso especial: un panel adentro de otro panel adentro
 * de una ventana son todos lo mismo.
 *
 * <p>La ubicación de los hijos no la decide el contenedor sino su {@link LayoutManager}. Sin
 * distribución, los hijos quedan donde se los ponga a mano — que a veces es exactamente lo que se
 * quiere y casi siempre no, porque deja de funcionar en cuanto cambia el tamaño de la fuente.
 *
 * <p>El **orden Z** es el orden de los hijos en la lista, y decide dos cosas a la vez: cuál se
 * dibuja encima y cuál recibe primero un clic. El índice 0 es el de más arriba, que es al revés de
 * lo que la intuición dice.
 *
 * <p>El ciclo de foco es la otra jerarquía que vive acá. Un contenedor puede ser **raíz de ciclo**,
 * y entonces el tabulador da la vuelta adentro suyo en vez de salir. Es lo que hace que en un
 * diálogo el foco no se escape a la ventana de atrás.
 */
public class Container extends Component {

    private static final long serialVersionUID = 4613797578919906343L;

    private final List<Component> component = new ArrayList<Component>();
    private LayoutManager layoutMgr;
    private transient ContainerListener containerListener;
    private FocusTraversalPolicy focusTraversalPolicy;
    private boolean focusCycleRoot;
    private boolean focusTraversalPolicyProvider;

    /** Un contenedor vacío, sin distribución. */
    public Container() {
    }

    /** Cuántos hijos tiene. */
    public int getComponentCount() {
        return this.countComponents();
    }

    /**
     * Cuántos hijos tiene.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #getComponentCount}.
     */
    @Deprecated
    public int countComponents() {
        synchronized (this.getTreeLock()) {
            return this.component.size();
        }
    }

    /**
     * El hijo de esa posición.
     *
     * @throws ArrayIndexOutOfBoundsException si no existe
     */
    public Component getComponent(int n) {
        synchronized (this.getTreeLock()) {
            if (n < 0 || n >= this.component.size()) {
                throw new ArrayIndexOutOfBoundsException("No such child: " + n);
            }
            return this.component.get(n);
        }
    }

    /** Los hijos, en orden Z: el 0 es el de más arriba. */
    public Component[] getComponents() {
        synchronized (this.getTreeLock()) {
            return this.component.toArray(new Component[this.component.size()]);
        }
    }

    /**
     * Los márgenes que el contenedor se reserva para sí: bordes, barra de título.
     *
     * <p>Sin ventana no hay decoración que reservar, así que son cero. Una subclase con borde propio
     * lo redefine.
     */
    public Insets getInsets() {
        return this.insets();
    }

    /**
     * Los márgenes reservados.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #getInsets}.
     */
    @Deprecated
    public Insets insets() {
        return new Insets(0, 0, 0, 0);
    }

    /**
     * Agrega un hijo al final.
     *
     * @return el mismo componente, para poder encadenar
     * @throws NullPointerException si el componente es `null`
     * @throws IllegalArgumentException si se lo agrega a sí mismo o a un descendiente suyo
     */
    public Component add(Component comp) {
        this.addImpl(comp, null, -1);
        return comp;
    }

    /**
     * Agrega un hijo con ese nombre, para las distribuciones que los usan.
     *
     * @return el mismo componente
     * @throws NullPointerException si el componente es `null`
     */
    public Component add(String name, Component comp) {
        this.addImpl(comp, name, -1);
        return comp;
    }

    /**
     * Agrega un hijo en esa posición del orden Z.
     *
     * @param index dónde ponerlo, o -1 para el final
     * @return el mismo componente
     * @throws NullPointerException si el componente es `null`
     */
    public Component add(Component comp, int index) {
        this.addImpl(comp, null, index);
        return comp;
    }

    /**
     * Agrega un hijo con restricciones para la distribución.
     *
     * @throws NullPointerException si el componente es `null`
     */
    public void add(Component comp, Object constraints) {
        this.addImpl(comp, constraints, -1);
    }

    /**
     * Como el anterior, en esa posición.
     *
     * @throws NullPointerException si el componente es `null`
     */
    public void add(Component comp, Object constraints, int index) {
        this.addImpl(comp, constraints, index);
    }

    /**
     * El único lugar por el que se agrega un hijo.
     *
     * <p>Los cinco {@code add} públicos pasan por acá, así que redefinirlo es la forma de
     * interceptar **todas** las incorporaciones sin tener que redefinir cinco métodos.
     *
     * @throws NullPointerException si el componente es `null`
     * @throws IllegalArgumentException si el componente es este mismo contenedor o un ancestro suyo,
     *     o si el índice no es válido
     */
    protected void addImpl(Component comp, Object constraints, int index) {
        synchronized (this.getTreeLock()) {
            if (comp == null) {
                throw new NullPointerException("component is null");
            }
            if (index > this.component.size() || index < -1) {
                throw new IllegalArgumentException(
                        "illegal component position");
            }
            // Agregar un contenedor adentro de si mismo o de uno de sus hijos armaria un ciclo, y el
            // arbol dejaria de ser un arbol: recorrerlo no terminaria nunca.
            if (comp instanceof Container) {
                if (comp == this || ((Container) comp).isAncestorOf(this)) {
                    throw new IllegalArgumentException("adding container's parent to itself");
                }
            }
            Container anterior = comp.getParent();
            if (anterior != null) {
                anterior.remove(comp);
            }
            if (index == -1) {
                this.component.add(comp);
            } else {
                this.component.add(index, comp);
            }
            comp.setParent(this);
            if (this.layoutMgr != null) {
                if (this.layoutMgr instanceof LayoutManager2) {
                    ((LayoutManager2) this.layoutMgr).addLayoutComponent(comp, constraints);
                } else if (constraints instanceof String) {
                    this.layoutMgr.addLayoutComponent((String) constraints, comp);
                } else {
                    this.layoutMgr.addLayoutComponent(null, comp);
                }
            }
            this.invalidate();
            if (this.containerListener != null
                    || (this.eventMask & AWTEvent.CONTAINER_EVENT_MASK) != 0) {
                this.processContainerEvent(
                        new ContainerEvent(this, ContainerEvent.COMPONENT_ADDED, comp));
            }
        }
    }

    /**
     * Saca el hijo de esa posición.
     *
     * @throws ArrayIndexOutOfBoundsException si no existe
     */
    public void remove(int index) {
        synchronized (this.getTreeLock()) {
            if (index < 0 || index >= this.component.size()) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            Component comp = this.component.remove(index);
            comp.setParent(null);
            if (this.layoutMgr != null) {
                this.layoutMgr.removeLayoutComponent(comp);
            }
            this.invalidate();
            if (this.containerListener != null
                    || (this.eventMask & AWTEvent.CONTAINER_EVENT_MASK) != 0) {
                this.processContainerEvent(
                        new ContainerEvent(this, ContainerEvent.COMPONENT_REMOVED, comp));
            }
        }
    }

    /** Saca ese hijo; si no estaba, no pasa nada. */
    public void remove(Component comp) {
        synchronized (this.getTreeLock()) {
            int i = this.component.indexOf(comp);
            if (i >= 0) {
                this.remove(i);
            }
        }
    }

    /** Saca todos los hijos. */
    public void removeAll() {
        synchronized (this.getTreeLock()) {
            while (!this.component.isEmpty()) {
                this.remove(this.component.size() - 1);
            }
        }
    }

    /**
     * Qué posición ocupa ese hijo en el orden Z.
     *
     * @return la posición, o -1 si no es hijo de este contenedor
     */
    public int getComponentZOrder(Component comp) {
        if (comp == null) {
            return -1;
        }
        synchronized (this.getTreeLock()) {
            if (comp.getParent() != this) {
                return -1;
            }
            return this.component.indexOf(comp);
        }
    }

    /**
     * Mueve un hijo a esa posición del orden Z.
     *
     * <p>No dispara eventos de contenedor: el hijo no se agregó ni se sacó, sólo cambió de lugar en
     * la pila. Es la diferencia con sacarlo y volverlo a agregar, que sí los dispararía.
     *
     * @throws NullPointerException si el componente es `null`
     * @throws IllegalArgumentException si el componente es este contenedor o un ancestro, o si la
     *     posición no es válida
     */
    public void setComponentZOrder(Component comp, int index) {
        synchronized (this.getTreeLock()) {
            if (comp == null) {
                throw new NullPointerException("comp is null");
            }
            if (comp == this) {
                throw new IllegalArgumentException("component cannot be added to itself");
            }
            if (index < 0 || index > this.component.size()
                    || (comp.getParent() == this && index == this.component.size())) {
                throw new IllegalArgumentException("illegal component position");
            }
            if (comp.getParent() == this) {
                this.component.remove(comp);
                this.component.add(index, comp);
            } else {
                this.addImpl(comp, null, index);
            }
        }
    }

    /** Si ese componente cuelga de este contenedor, a cualquier profundidad. */
    public boolean isAncestorOf(Component c) {
        synchronized (this.getTreeLock()) {
            Container p = c == null ? null : c.getParent();
            while (p != null) {
                if (p == this) {
                    return true;
                }
                p = p.getParent();
            }
            return false;
        }
    }

    /** La distribución, o `null` si no tiene. */
    public LayoutManager getLayout() {
        return this.layoutMgr;
    }

    /** Le cambia la distribución e invalida, porque las posiciones dejan de valer. */
    public void setLayout(LayoutManager mgr) {
        this.layoutMgr = mgr;
        this.invalidate();
    }

    /** Le pide a la distribución que ubique a los hijos. */
    public void doLayout() {
        this.layout();
    }

    /**
     * Ubica a los hijos.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #doLayout}.
     */
    @Deprecated
    public void layout() {
        LayoutManager m = this.layoutMgr;
        if (m != null) {
            m.layoutContainer(this);
        }
    }

    /**
     * Si al validar hay que parar acá en vez de seguir subiendo.
     *
     * <p>Contesta `false`: un contenedor común propaga la validación hacia arriba. Los que tienen
     * tamaño propio —una ventana, un panel con barras— lo redefinen, y eso es lo que evita que
     * cambiar un botón revalide la aplicación entera.
     */
    public boolean isValidateRoot() {
        return false;
    }

    /**
     * Vuelve a maquetar el subárbol si hacía falta.
     *
     * <p>Es la operación cara del maquetado, y por eso no hace nada si el contenedor ya era válido.
     */
    public void validate() {
        synchronized (this.getTreeLock()) {
            // Sin pantalla no hay nada que maquetar, y marcarlo valido seria afirmar que se
            // maqueto. Es lo mismo que hace el JDK: `Container.validate` no llama a la version del
            // componente y no hace nada mientras no haya ventana detras.
            if (!this.isDisplayable()) {
                return;
            }
            if (!this.isValid()) {
                this.validateTree();
            }
            super.validate();
        }
    }

    /**
     * Recorre el subárbol maquetando de arriba hacia abajo.
     *
     * <p>El orden importa: un hijo no puede ubicarse antes de que su padre sepa cuánto espacio le
     * toca.
     */
    protected void validateTree() {
        synchronized (this.getTreeLock()) {
            this.doLayout();
            for (int i = 0; i < this.component.size(); i++) {
                Component c = this.component.get(i);
                if (c instanceof Container) {
                    ((Container) c).validateTree();
                } else {
                    c.validate();
                }
            }
        }
    }

    /** Marca que hay que volver a maquetar, y le avisa a la distribución. */
    public void invalidate() {
        synchronized (this.getTreeLock()) {
            LayoutManager m = this.layoutMgr;
            if (m instanceof LayoutManager2) {
                ((LayoutManager2) m).invalidateLayout(this);
            }
            super.invalidate();
        }
    }

    /** La medida preferida, según la distribución. */
    public Dimension getPreferredSize() {
        return this.preferredSize();
    }

    /**
     * La medida preferida.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #getPreferredSize}.
     */
    @Deprecated
    public Dimension preferredSize() {
        if (this.isPreferredSizeSet()) {
            return super.preferredSize();
        }
        synchronized (this.getTreeLock()) {
            LayoutManager m = this.layoutMgr;
            if (m != null) {
                return m.preferredLayoutSize(this);
            }
            // `super.preferredSize()` y no `super.getPreferredSize()`: lo segundo vuelve a
            // despachar a este mismo metodo y la llamada no termina nunca.
            return super.preferredSize();
        }
    }

    /** La medida mínima, según la distribución. */
    public Dimension getMinimumSize() {
        return this.minimumSize();
    }

    /**
     * La medida mínima.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #getMinimumSize}.
     */
    @Deprecated
    public Dimension minimumSize() {
        if (this.isMinimumSizeSet()) {
            return super.minimumSize();
        }
        synchronized (this.getTreeLock()) {
            LayoutManager m = this.layoutMgr;
            if (m != null) {
                return m.minimumLayoutSize(this);
            }
            return super.minimumSize();
        }
    }

    /**
     * La medida máxima, según la distribución.
     *
     * <p>Sólo una {@link LayoutManager2} sabe contestarla; con una distribución simple se cae en la
     * respuesta de {@link Component}, que es "sin tope".
     */
    public Dimension getMaximumSize() {
        if (this.isMaximumSizeSet()) {
            return super.getMaximumSize();
        }
        synchronized (this.getTreeLock()) {
            LayoutManager m = this.layoutMgr;
            if (m instanceof LayoutManager2) {
                return ((LayoutManager2) m).maximumLayoutSize(this);
            }
            return super.getMaximumSize();
        }
    }

    /** Cómo se alinea horizontalmente, según la distribución. */
    public float getAlignmentX() {
        LayoutManager m = this.layoutMgr;
        if (m instanceof LayoutManager2) {
            return ((LayoutManager2) m).getLayoutAlignmentX(this);
        }
        return super.getAlignmentX();
    }

    /** Cómo se alinea verticalmente, según la distribución. */
    public float getAlignmentY() {
        LayoutManager m = this.layoutMgr;
        if (m instanceof LayoutManager2) {
            return ((LayoutManager2) m).getLayoutAlignmentY(this);
        }
        return super.getAlignmentY();
    }

    /** Se dibuja y dibuja a sus hijos. */
    public void paint(Graphics g) {
        this.paintComponents(g);
    }

    /**
     * Borra el fondo, se dibuja y dibuja a sus hijos.
     *
     * <p>El borrado sólo si el contenedor es opaco: si no lo es, borrar taparía lo que hay debajo.
     */
    public void update(Graphics g) {
        if (this.isOpaque()) {
            g.setColor(this.getBackground());
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
            g.setColor(this.getForeground());
        }
        this.paint(g);
    }

    /** Se imprime e imprime a sus hijos. */
    public void print(Graphics g) {
        this.printComponents(g);
    }

    /**
     * Dibuja a los hijos, cada uno con su propio contexto recortado.
     *
     * <p>El recorte por hijo es lo que garantiza que un hijo no pueda pintar fuera de su rectángulo
     * por más que lo intente.
     */
    public void paintComponents(Graphics g) {
        synchronized (this.getTreeLock()) {
            for (int i = this.component.size() - 1; i >= 0; i--) {
                Component c = this.component.get(i);
                if (c.isVisible() && g != null) {
                    Graphics cg = g.create(c.getX(), c.getY(), c.getWidth(), c.getHeight());
                    if (cg != null) {
                        try {
                            c.paintAll(cg);
                        } finally {
                            cg.dispose();
                        }
                    }
                }
            }
        }
    }

    /** Lo mismo, para imprimir. */
    public void printComponents(Graphics g) {
        synchronized (this.getTreeLock()) {
            for (int i = this.component.size() - 1; i >= 0; i--) {
                Component c = this.component.get(i);
                if (c.isVisible() && g != null) {
                    Graphics cg = g.create(c.getX(), c.getY(), c.getWidth(), c.getHeight());
                    if (cg != null) {
                        try {
                            c.printAll(cg);
                        } finally {
                            cg.dispose();
                        }
                    }
                }
            }
        }
    }

    /** Suma un oyente de contenedor; un `null` se ignora. */
    public synchronized void addContainerListener(ContainerListener l) {
        if (l == null) {
            return;
        }
        this.containerListener = AWTEventMulticaster.add(this.containerListener, l);
        this.enableEvents(AWTEvent.CONTAINER_EVENT_MASK);
    }

    /** Saca a ese oyente. */
    public synchronized void removeContainerListener(ContainerListener l) {
        if (l == null) {
            return;
        }
        this.containerListener = AWTEventMulticaster.remove(this.containerListener, l);
    }

    /** Los oyentes de contenedor. */
    public synchronized ContainerListener[] getContainerListeners() {
        return AWTEventMulticaster.getListeners(this.containerListener, ContainerListener.class);
    }

    /**
     * Los oyentes de esa clase.
     *
     * @throws ClassCastException si la clase no es de oyente
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (listenerType == ContainerListener.class) {
            return AWTEventMulticaster.getListeners(this.containerListener, listenerType);
        }
        return super.getListeners(listenerType);
    }

    /** Clasifica el evento; los de contenedor los atiende él, el resto sube. */
    protected void processEvent(AWTEvent e) {
        if (e instanceof ContainerEvent) {
            this.processContainerEvent((ContainerEvent) e);
        } else {
            super.processEvent(e);
        }
    }

    /** Les avisa a los oyentes de contenedor. */
    protected void processContainerEvent(ContainerEvent e) {
        ContainerListener l = this.containerListener;
        if (l == null) {
            return;
        }
        if (e.getID() == ContainerEvent.COMPONENT_ADDED) {
            l.componentAdded(e);
        } else if (e.getID() == ContainerEvent.COMPONENT_REMOVED) {
            l.componentRemoved(e);
        }
    }

    /**
     * Le manda un evento del modelo viejo.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #dispatchEvent}.
     */
    @Deprecated
    public void deliverEvent(Event e) {
        Component comp = this.getComponentAt(e.x, e.y);
        if (comp != null && comp != this) {
            comp.deliverEvent(e);
        } else {
            this.postEvent(e);
        }
    }

    /**
     * Qué hijo hay en ese punto.
     *
     * <p>Recorre en orden Z, así que devuelve el de **más arriba**: es el que recibiría el clic.
     */
    public Component getComponentAt(int x, int y) {
        return this.locate(x, y);
    }

    /**
     * Qué hijo hay en ese punto.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #getComponentAt(int, int)}.
     */
    @Deprecated
    public Component locate(int x, int y) {
        if (!this.contains(x, y)) {
            return null;
        }
        synchronized (this.getTreeLock()) {
            for (int i = 0; i < this.component.size(); i++) {
                Component c = this.component.get(i);
                if (c.isVisible() && c.contains(x - c.getX(), y - c.getY())) {
                    return c;
                }
            }
        }
        return this;
    }

    /**
     * Qué hijo hay en ese punto.
     *
     * @throws NullPointerException si el punto es `null`
     */
    public Component getComponentAt(Point p) {
        return this.getComponentAt(p.x, p.y);
    }

    /**
     * Qué componente hay en ese punto, **bajando** por el árbol.
     *
     * <p>Es la diferencia con {@link #getComponentAt}: aquél mira sólo a los hijos directos, éste
     * llega hasta la hoja. Es lo que hace falta para saber a quién entregarle un clic.
     *
     * @return el componente más profundo, o `null` si el punto cae afuera
     */
    public Component findComponentAt(int x, int y) {
        synchronized (this.getTreeLock()) {
            if (!this.contains(x, y) || !this.isVisible() || !this.isEnabled()) {
                return null;
            }
            for (int i = 0; i < this.component.size(); i++) {
                Component c = this.component.get(i);
                int cx = x - c.getX();
                int cy = y - c.getY();
                if (!c.isVisible() || !c.contains(cx, cy)) {
                    continue;
                }
                if (c instanceof Container) {
                    Component hondo = ((Container) c).findComponentAt(cx, cy);
                    if (hondo != null) {
                        return hondo;
                    }
                } else {
                    return c;
                }
            }
            return this;
        }
    }

    /**
     * Lo mismo, con un punto.
     *
     * @throws NullPointerException si el punto es `null`
     */
    public Component findComponentAt(Point p) {
        return this.findComponentAt(p.x, p.y);
    }

    /**
     * Dónde está el ratón sobre este contenedor.
     *
     * @param allowChildren si cuenta cuando el ratón está sobre un hijo
     * @return `null` siempre: el contenedor no está en pantalla
     * @throws HeadlessException si no hay pantalla
     */
    public Point getMousePosition(boolean allowChildren) throws HeadlessException {
        return null;
    }

    /** Avisa que puede mostrarse, y se lo avisa a sus hijos. */
    public void addNotify() {
        synchronized (this.getTreeLock()) {
            super.addNotify();
            for (int i = 0; i < this.component.size(); i++) {
                this.component.get(i).addNotify();
            }
        }
    }

    /** Avisa que dejó de poder mostrarse, y se lo avisa a sus hijos. */
    public void removeNotify() {
        synchronized (this.getTreeLock()) {
            for (int i = this.component.size() - 1; i >= 0; i--) {
                this.component.get(i).removeNotify();
            }
            super.removeNotify();
        }
    }

    /**
     * Le cambia la fuente a él y con eso a los hijos que no tengan propia.
     *
     * <p>Se redefine para invalidar el subárbol: cambiar la fuente cambia cuánto mide el texto de
     * todos los descendientes que la heredan.
     */
    public void setFont(Font f) {
        super.setFont(f);
        this.invalidate();
    }

    /** Le pone esa orientación a él y a todos sus descendientes. */
    public void applyComponentOrientation(ComponentOrientation o) {
        super.applyComponentOrientation(o);
        synchronized (this.getTreeLock()) {
            for (int i = 0; i < this.component.size(); i++) {
                this.component.get(i).applyComponentOrientation(o);
            }
        }
    }

    /** Suma alguien a quien avisarle de los cambios de propiedad. */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        super.addPropertyChangeListener(listener);
    }

    /** Suma un oyente para una propiedad concreta. */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        super.addPropertyChangeListener(propertyName, listener);
    }

    /** En qué orden el tabulador recorre a los hijos. */
    public FocusTraversalPolicy getFocusTraversalPolicy() {
        if (!this.isFocusTraversalPolicyProvider() && !this.isFocusCycleRoot()) {
            return null;
        }
        FocusTraversalPolicy p = this.focusTraversalPolicy;
        if (p != null) {
            return p;
        }
        Container padre = this.getParent();
        if (padre != null) {
            return padre.getFocusTraversalPolicy();
        }
        return null;
    }

    /** Le cambia el orden de recorrido; con `null` vuelve a heredarlo. */
    public void setFocusTraversalPolicy(FocusTraversalPolicy policy) {
        FocusTraversalPolicy viejo;
        synchronized (this) {
            viejo = this.focusTraversalPolicy;
            this.focusTraversalPolicy = policy;
        }
        this.firePropertyChange("focusTraversalPolicy", viejo, policy);
    }

    /** Si tiene orden de recorrido propio. */
    public boolean isFocusTraversalPolicySet() {
        return this.focusTraversalPolicy != null;
    }

    /**
     * Si el tabulador da la vuelta adentro suyo en vez de salir.
     *
     * <p>Es lo que encierra el foco en un diálogo.
     */
    public boolean isFocusCycleRoot() {
        return this.focusCycleRoot;
    }

    /** Declara si el foco da la vuelta adentro suyo. */
    public void setFocusCycleRoot(boolean focusCycleRoot) {
        boolean viejo;
        synchronized (this) {
            viejo = this.focusCycleRoot;
            this.focusCycleRoot = focusCycleRoot;
        }
        this.firePropertyChange("focusCycleRoot", viejo, focusCycleRoot);
    }

    /**
     * Si ese contenedor es la raíz del ciclo de foco de éste.
     *
     * <p>Un contenedor que sea raíz de ciclo es la raíz **de sí mismo**, que es la diferencia con la
     * versión de {@link Component}.
     */
    public boolean isFocusCycleRoot(Container container) {
        if (this.isFocusCycleRoot() && container == this) {
            return true;
        }
        return super.isFocusCycleRoot(container);
    }

    /**
     * Si aporta orden de recorrido a sus hijos sin ser raíz de ciclo.
     *
     * <p>Es el punto medio entre las dos cosas: ordena a los suyos, pero el tabulador puede salirse.
     */
    public final boolean isFocusTraversalPolicyProvider() {
        return this.focusTraversalPolicyProvider;
    }

    /** Declara si aporta orden de recorrido. */
    public final void setFocusTraversalPolicyProvider(boolean provider) {
        boolean viejo;
        synchronized (this) {
            viejo = this.focusTraversalPolicyProvider;
            this.focusTraversalPolicyProvider = provider;
        }
        this.firePropertyChange("focusTraversalPolicyProvider", viejo, provider);
    }

    /** Baja el foco a este contenedor; no hace nada sin gestor de foco. */
    public void transferFocusDownCycle() {
    }

    /**
     * Las teclas de recorrido en ese sentido.
     *
     * @throws IllegalArgumentException si el sentido no es uno de los cuatro
     */
    public Set<AWTKeyStroke> getFocusTraversalKeys(int id) {
        return super.getFocusTraversalKeys(id);
    }

    /**
     * Cambia las teclas de recorrido.
     *
     * @throws IllegalArgumentException si el sentido o alguna tecla no son válidos
     */
    public void setFocusTraversalKeys(int id, Set<? extends AWTKeyStroke> keystrokes) {
        super.setFocusTraversalKeys(id, keystrokes);
    }

    /**
     * Si se le fijaron teclas propias en ese sentido.
     *
     * @throws IllegalArgumentException si el sentido no es uno de los cuatro
     */
    public boolean areFocusTraversalKeysSet(int id) {
        return super.areFocusTraversalKeysSet(id);
    }

    /** Escribe este contenedor y su subárbol, con sangría creciente. */
    public void list(PrintStream out, int indent) {
        super.list(out, indent);
        synchronized (this.getTreeLock()) {
            for (int i = 0; i < this.component.size(); i++) {
                this.component.get(i).list(out, indent + 1);
            }
        }
    }

    /** Lo mismo, en un escritor. */
    public void list(PrintWriter out, int indent) {
        super.list(out, indent);
        synchronized (this.getTreeLock()) {
            for (int i = 0; i < this.component.size(); i++) {
                this.component.get(i).list(out, indent + 1);
            }
        }
    }

    protected String paramString() {
        String s = super.paramString();
        LayoutManager m = this.layoutMgr;
        if (m != null) {
            s = s + ",layout=" + m.getClass().getName();
        }
        return s;
    }

    /**
     * La accesibilidad de un contenedor.
     *
     * <p>Lo único que agrega respecto de un componente son los hijos, y es todo lo que hace falta:
     * el árbol de accesibilidad sigue al de componentes.
     */
    protected class AccessibleAWTContainer extends AccessibleAWTComponent {

        /** Para las subclases. */
        protected AccessibleAWTContainer() {
        }

        /** Cuántos hijos tiene. */
        public int getAccessibleChildrenCount() {
            return Container.this.getComponentCount();
        }

        /**
         * El hijo de esa posición, si es accesible.
         *
         * @return el hijo, o `null` si no existe o no es accesible
         */
        public Accessible getAccessibleChild(int i) {
            if (i < 0 || i >= Container.this.getComponentCount()) {
                return null;
            }
            Component c = Container.this.getComponent(i);
            if (c instanceof Accessible) {
                return (Accessible) c;
            }
            return null;
        }
    }
}
