package java.awt;

import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Quién tiene el foco del teclado y cómo se mueve.
 *
 * <p>El foco es de a uno en todo el programa: hay **un** componente que recibe las teclas, y saberlo
 * es responsabilidad de esta clase y no de los componentes. Por eso lo que guarda son cinco cosas
 * globales —el dueño del foco, el dueño permanente, la ventana con foco, la ventana activa y la raíz
 * del ciclo actual— y por eso los métodos que las escriben son `protected`: cambiarlas es cosa del
 * gestor, no de quien lo usa.
 *
 * <p>La diferencia entre el dueño **del foco** y el **permanente** confunde y es real: al abrir un
 * menú, el foco pasa al menú de forma temporal, pero el dueño permanente sigue siendo el campo de
 * texto que estaba escribiendo. Al cerrarse el menú, el foco vuelve solo. Lo mismo entre la ventana
 * **con foco** —la que recibe las teclas— y la **activa** —la que se ve resaltada, que puede ser la
 * dueña de un diálogo—.
 *
 * <p><strong>Sin sistema de ventanas nadie le avisa a este gestor que el foco se movió.</strong>
 * Todo lo que es estado y cálculo funciona: las teclas de recorrido, la política por omisión, los
 * oyentes de propiedad y de veto, la cadena de repartidores y posprocesadores, y las cinco
 * propiedades globales, que arrancan en `null` y se pueden fijar desde una subclase. Lo que no pasa
 * solo es que el foco se mueva, porque moverlo lo pide {@link Component#requestFocus} y eso, sin
 * pantalla, no hace nada —ni acá ni en el JDK—.
 */
public abstract class KeyboardFocusManager implements KeyEventDispatcher, KeyEventPostProcessor {

    /** Hacia adelante: Tab. */
    public static final int FORWARD_TRAVERSAL_KEYS = 0;

    /** Hacia atrás: Shift+Tab. */
    public static final int BACKWARD_TRAVERSAL_KEYS = 1;

    /** Un nivel de ciclo hacia arriba. */
    public static final int UP_CYCLE_TRAVERSAL_KEYS = 2;

    /** Un nivel de ciclo hacia abajo. */
    public static final int DOWN_CYCLE_TRAVERSAL_KEYS = 3;

    /** Cuántos sentidos de recorrido hay. */
    static final int TRAVERSAL_KEY_LENGTH = 4;

    /** El gestor en uso. */
    private static KeyboardFocusManager actual;

    /** El componente con el foco. */
    private Component focusOwner;

    /** El dueño permanente, que no cambia con un foco temporal. */
    private Component permanentFocusOwner;

    /** La ventana que recibe las teclas. */
    private Window focusedWindow;

    /** La ventana que se ve activa. */
    private Window activeWindow;

    /** La raíz del ciclo de foco que se está recorriendo. */
    private Container currentFocusCycleRoot;

    /** La política que usa un contenedor que no fijó la suya. */
    private FocusTraversalPolicy defaultPolicy = new DefaultFocusTraversalPolicy();

    /** Las teclas de recorrido de fábrica, por sentido. */
    private final Set<AWTKeyStroke>[] defaultKeys = crearTeclas();

    /**
     * Los repartidores, en orden, o `null` si nunca se registró ninguno.
     *
     * <p>La distinción entre `null` y lista vacía es visible desde afuera y es a propósito:
     * {@link #getKeyEventDispatchers} devuelve `null` mientras nadie haya registrado nunca nada, y
     * una lista vacía después de registrar y sacar. O sea que informa **si alguna vez hubo**
     * repartidores, no sólo si los hay ahora.
     */
    private ArrayList<KeyEventDispatcher> dispatchers;

    /** Los posprocesadores, con la misma distinción. */
    private ArrayList<KeyEventPostProcessor> postProcessors;

    /** Los oyentes de cambio de propiedad. */
    private final PropertyChangeSupport cambios = new PropertyChangeSupport(this);

    /** Los oyentes con derecho a vetar. */
    private final VetoableChangeSupport vetos = new VetoableChangeSupport(this);

    /** Un gestor con las teclas y la política de fábrica. */
    public KeyboardFocusManager() {
    }

    /** Las cuatro tablas de teclas de fábrica. */
    @SuppressWarnings("unchecked")
    private static Set<AWTKeyStroke>[] crearTeclas() {
        Set<AWTKeyStroke>[] t = new Set[TRAVERSAL_KEY_LENGTH];
        for (int i = 0; i < TRAVERSAL_KEY_LENGTH; i++) {
            t[i] = porOmision(i);
        }
        return t;
    }

    /**
     * Las teclas de fábrica de ese sentido.
     *
     * <p>Los dos ciclos —arriba y abajo— **no tienen ninguna** en AWT, y es a propósito: subir o
     * bajar de ciclo es cosa de Swing, que sí les pone Ctrl+Arriba y Ctrl+Abajo.
     */
    private static Set<AWTKeyStroke> porOmision(int id) {
        Set<AWTKeyStroke> s = new HashSet<AWTKeyStroke>();
        if (id == FORWARD_TRAVERSAL_KEYS) {
            s.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, 0));
            s.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB,
                    java.awt.event.InputEvent.CTRL_DOWN_MASK));
        } else if (id == BACKWARD_TRAVERSAL_KEYS) {
            s.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB,
                    java.awt.event.InputEvent.SHIFT_DOWN_MASK));
            s.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB,
                    java.awt.event.InputEvent.CTRL_DOWN_MASK
                            | java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        }
        return Collections.unmodifiableSet(s);
    }

    /**
     * El gestor en uso, armando uno si es el primer pedido.
     *
     * <p>Es único para todo el programa: dos gestores creerían cada uno tener el foco.
     */
    public static KeyboardFocusManager getCurrentKeyboardFocusManager() {
        synchronized (KeyboardFocusManager.class) {
            if (actual == null) {
                actual = new DefaultKeyboardFocusManager();
            }
            return actual;
        }
    }

    /**
     * Cambia el gestor.
     *
     * @param newManager el gestor, o `null` para volver al de fábrica en el próximo pedido
     */
    public static void setCurrentKeyboardFocusManager(KeyboardFocusManager newManager) {
        synchronized (KeyboardFocusManager.class) {
            actual = newManager;
        }
    }

    /**
     * El componente con el foco.
     *
     * @return el componente, o `null` si el foco no está en este programa
     */
    public Component getFocusOwner() {
        return this.getGlobalFocusOwner();
    }

    /** El dueño del foco, para las subclases. */
    protected Component getGlobalFocusOwner() {
        synchronized (this) {
            return this.focusOwner;
        }
    }

    /**
     * Fija el dueño del foco.
     *
     * <p>Un componente que no puede recibir el foco se ignora en silencio, que es lo que hace el
     * JDK: el pedido llega del sistema de ventanas y rechazarlo con una excepción cortaría el
     * reparto de eventos.
     */
    protected void setGlobalFocusOwner(Component focusOwner) {
        if (focusOwner != null && !focusOwner.isFocusable()) {
            return;
        }
        Component viejo;
        synchronized (this) {
            viejo = this.focusOwner;
            this.focusOwner = focusOwner;
        }
        this.firePropertyChange("focusOwner", viejo, focusOwner);
    }

    /** Suelta el foco si lo tiene este programa. */
    public void clearFocusOwner() {
        if (this.getFocusOwner() != null) {
            this.clearGlobalFocusOwner();
        }
    }

    /**
     * Suelta el foco.
     *
     * <p>Después de esto ningún componente lo tiene, hasta que el sistema de ventanas diga otra cosa.
     */
    public void clearGlobalFocusOwner() {
        this.setGlobalFocusOwner(null);
        this.setGlobalPermanentFocusOwner(null);
    }

    /**
     * El dueño **permanente** del foco.
     *
     * @return el componente, o `null`
     */
    public Component getPermanentFocusOwner() {
        return this.getGlobalPermanentFocusOwner();
    }

    /** El dueño permanente, para las subclases. */
    protected Component getGlobalPermanentFocusOwner() {
        synchronized (this) {
            return this.permanentFocusOwner;
        }
    }

    /** Fija el dueño permanente; también pasa a ser el dueño del foco. */
    protected void setGlobalPermanentFocusOwner(Component permanentFocusOwner) {
        if (permanentFocusOwner != null && !permanentFocusOwner.isFocusable()) {
            return;
        }
        Component viejo;
        synchronized (this) {
            viejo = this.permanentFocusOwner;
            this.permanentFocusOwner = permanentFocusOwner;
        }
        this.firePropertyChange("permanentFocusOwner", viejo, permanentFocusOwner);
        if (permanentFocusOwner != null) {
            this.setGlobalFocusOwner(permanentFocusOwner);
        }
    }

    /**
     * La ventana que recibe las teclas.
     *
     * @return la ventana, o `null`
     */
    public Window getFocusedWindow() {
        return this.getGlobalFocusedWindow();
    }

    /** La ventana con foco, para las subclases. */
    protected Window getGlobalFocusedWindow() {
        synchronized (this) {
            return this.focusedWindow;
        }
    }

    /** Fija la ventana con foco; una que no lo admite se ignora. */
    protected void setGlobalFocusedWindow(Window focusedWindow) {
        if (focusedWindow != null && !focusedWindow.isFocusableWindow()) {
            return;
        }
        Window vieja;
        synchronized (this) {
            vieja = this.focusedWindow;
            this.focusedWindow = focusedWindow;
        }
        this.firePropertyChange("focusedWindow", vieja, focusedWindow);
    }

    /**
     * La ventana activa.
     *
     * @return la ventana, o `null`
     */
    public Window getActiveWindow() {
        return this.getGlobalActiveWindow();
    }

    /** La ventana activa, para las subclases. */
    protected Window getGlobalActiveWindow() {
        synchronized (this) {
            return this.activeWindow;
        }
    }

    /** Fija la ventana activa. */
    protected void setGlobalActiveWindow(Window activeWindow) {
        Window vieja;
        synchronized (this) {
            vieja = this.activeWindow;
            this.activeWindow = activeWindow;
        }
        this.firePropertyChange("activeWindow", vieja, activeWindow);
    }

    /** La política que usa un contenedor que no fijó la suya. */
    public synchronized FocusTraversalPolicy getDefaultFocusTraversalPolicy() {
        return this.defaultPolicy;
    }

    /**
     * Cambia la política por omisión.
     *
     * <p>No toca a los contenedores que ya tienen la suya: la de omisión es la que se usa cuando no
     * hay ninguna, no una que pise a las demás.
     *
     * @throws IllegalArgumentException si la política es `null`
     */
    public void setDefaultFocusTraversalPolicy(FocusTraversalPolicy defaultPolicy) {
        if (defaultPolicy == null) {
            throw new IllegalArgumentException("default focus traversal policy cannot be null");
        }
        FocusTraversalPolicy vieja;
        synchronized (this) {
            vieja = this.defaultPolicy;
            this.defaultPolicy = defaultPolicy;
        }
        this.firePropertyChange("defaultFocusTraversalPolicy", vieja, defaultPolicy);
    }

    /**
     * Cambia las teclas de recorrido de fábrica de ese sentido.
     *
     * <p>El conjunto se copia y queda inmodificable: si se guardara el que pasaron, cambiarlo después
     * cambiaría el recorrido de todo el programa sin que nadie lo pida.
     *
     * @throws IllegalArgumentException si el sentido no es uno de los cuatro, si el conjunto es
     *     `null`, si trae un `null` adentro, si trae una tecla de tipo `KEY_TYPED` —que no distingue
     *     modificadores y haría el recorrido impredecible— o si una de sus teclas ya está en otro
     *     sentido
     */
    public void setDefaultFocusTraversalKeys(int id, Set<? extends AWTKeyStroke> keystrokes) {
        if (id < 0 || id >= TRAVERSAL_KEY_LENGTH) {
            throw new IllegalArgumentException("invalid focus traversal key identifier");
        }
        if (keystrokes == null) {
            throw new IllegalArgumentException("cannot set null Set of default focus traversal keys");
        }
        Set<AWTKeyStroke> copia = new HashSet<AWTKeyStroke>();
        java.util.Iterator<? extends AWTKeyStroke> it = keystrokes.iterator();
        while (it.hasNext()) {
            AWTKeyStroke k = it.next();
            if (k == null) {
                throw new IllegalArgumentException("cannot set null focus traversal key");
            }
            if (k.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
                throw new IllegalArgumentException("focus traversal keys cannot map to KEY_TYPED events");
            }
            for (int i = 0; i < TRAVERSAL_KEY_LENGTH; i++) {
                if (i != id && this.defaultKeys[i].contains(k)) {
                    throw new IllegalArgumentException("focus traversal keys must be unique for a Component");
                }
            }
            copia.add(k);
        }
        Set<AWTKeyStroke> vieja;
        synchronized (this) {
            vieja = this.defaultKeys[id];
            this.defaultKeys[id] = Collections.unmodifiableSet(copia);
        }
        this.firePropertyChange(nombreDeSentido(id), vieja, this.defaultKeys[id]);
    }

    /** Cómo se llama la propiedad de ese sentido. */
    private static String nombreDeSentido(int id) {
        if (id == FORWARD_TRAVERSAL_KEYS) {
            return "forwardDefaultFocusTraversalKeys";
        }
        if (id == BACKWARD_TRAVERSAL_KEYS) {
            return "backwardDefaultFocusTraversalKeys";
        }
        if (id == UP_CYCLE_TRAVERSAL_KEYS) {
            return "upCycleDefaultFocusTraversalKeys";
        }
        return "downCycleDefaultFocusTraversalKeys";
    }

    /**
     * Las teclas de recorrido de fábrica de ese sentido.
     *
     * @throws IllegalArgumentException si el sentido no es uno de los cuatro
     */
    public Set<AWTKeyStroke> getDefaultFocusTraversalKeys(int id) {
        if (id < 0 || id >= TRAVERSAL_KEY_LENGTH) {
            throw new IllegalArgumentException("invalid focus traversal key identifier");
        }
        synchronized (this) {
            return this.defaultKeys[id];
        }
    }

    /**
     * La raíz del ciclo de foco que se está recorriendo.
     *
     * @return el contenedor, o `null` si no se está recorriendo ninguno
     */
    public Container getCurrentFocusCycleRoot() {
        return this.getGlobalCurrentFocusCycleRoot();
    }

    /** La raíz del ciclo, para las subclases. */
    protected Container getGlobalCurrentFocusCycleRoot() {
        synchronized (this) {
            return this.currentFocusCycleRoot;
        }
    }

    /**
     * Fija la raíz del ciclo.
     *
     * <p>Es el único de los cinco escritores globales que es **público**, y la razón es concreta: el
     * recorrido hacia arriba y hacia abajo de ciclo lo hace quien recorre, no el gestor, así que
     * tiene que poder decir dónde quedó parado.
     */
    public void setGlobalCurrentFocusCycleRoot(Container newFocusCycleRoot) {
        Container vieja;
        synchronized (this) {
            vieja = this.currentFocusCycleRoot;
            this.currentFocusCycleRoot = newFocusCycleRoot;
        }
        this.firePropertyChange("currentFocusCycleRoot", vieja, newFocusCycleRoot);
    }

    /** Agrega un oyente de cambios; `null` no hace nada. */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (listener != null) {
            this.cambios.addPropertyChangeListener(listener);
        }
    }

    /** Saca un oyente de cambios. */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (listener != null) {
            this.cambios.removePropertyChangeListener(listener);
        }
    }

    /** Los oyentes de cambios. */
    public synchronized PropertyChangeListener[] getPropertyChangeListeners() {
        return this.cambios.getPropertyChangeListeners();
    }

    /** Agrega un oyente para una sola propiedad. */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (listener != null) {
            this.cambios.addPropertyChangeListener(propertyName, listener);
        }
    }

    /** Saca un oyente de una sola propiedad. */
    public void removePropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        if (listener != null) {
            this.cambios.removePropertyChangeListener(propertyName, listener);
        }
    }

    /** Los oyentes de esa propiedad. */
    public synchronized PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return this.cambios.getPropertyChangeListeners(propertyName);
    }

    /** Les avisa a los oyentes; si el valor no cambió no avisa nada. */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (oldValue == newValue) {
            return;
        }
        this.cambios.firePropertyChange(propertyName, oldValue, newValue);
    }

    /** Agrega un oyente con derecho a vetar. */
    public void addVetoableChangeListener(VetoableChangeListener listener) {
        if (listener != null) {
            this.vetos.addVetoableChangeListener(listener);
        }
    }

    /** Saca un oyente con derecho a vetar. */
    public void removeVetoableChangeListener(VetoableChangeListener listener) {
        if (listener != null) {
            this.vetos.removeVetoableChangeListener(listener);
        }
    }

    /** Los oyentes con derecho a vetar. */
    public synchronized VetoableChangeListener[] getVetoableChangeListeners() {
        return this.vetos.getVetoableChangeListeners();
    }

    /** Agrega un oyente con derecho a vetar una sola propiedad. */
    public void addVetoableChangeListener(String propertyName, VetoableChangeListener listener) {
        if (listener != null) {
            this.vetos.addVetoableChangeListener(propertyName, listener);
        }
    }

    /** Saca uno. */
    public void removeVetoableChangeListener(String propertyName,
            VetoableChangeListener listener) {
        if (listener != null) {
            this.vetos.removeVetoableChangeListener(propertyName, listener);
        }
    }

    /** Los oyentes con derecho a vetar esa propiedad. */
    public synchronized VetoableChangeListener[] getVetoableChangeListeners(String propertyName) {
        return this.vetos.getVetoableChangeListeners(propertyName);
    }

    /**
     * Les propone el cambio a los que pueden vetar.
     *
     * @throws PropertyVetoException si alguno lo veta; el cambio no se hace
     */
    protected void fireVetoableChange(String propertyName, Object oldValue, Object newValue)
            throws PropertyVetoException {
        if (oldValue == newValue) {
            return;
        }
        this.vetos.fireVetoableChange(propertyName, oldValue, newValue);
    }

    /**
     * Agrega un repartidor al final de la cadena.
     *
     * <p>El orden importa: el primero que devuelva `true` se queda el evento. `null` no hace nada.
     */
    public void addKeyEventDispatcher(KeyEventDispatcher dispatcher) {
        if (dispatcher == null) {
            return;
        }
        synchronized (this) {
            if (this.dispatchers == null) {
                this.dispatchers = new ArrayList<KeyEventDispatcher>();
            }
            this.dispatchers.add(dispatcher);
        }
    }

    /**
     * Saca un repartidor.
     *
     * <p>El gestor mismo es el último de la cadena y **no** se puede sacar así: para eso hay que
     * cambiar el gestor.
     */
    public void removeKeyEventDispatcher(KeyEventDispatcher dispatcher) {
        if (dispatcher == null) {
            return;
        }
        synchronized (this) {
            if (this.dispatchers != null) {
                this.dispatchers.remove(dispatcher);
            }
        }
    }

    /**
     * Los repartidores registrados.
     *
     * @return una copia, o `null` si **nunca** se registró ninguno. Sacar el último deja una lista
     *     vacía, no un `null`: lo que se contesta es si la cadena existe, no si tiene elementos.
     */
    protected synchronized java.util.List<KeyEventDispatcher> getKeyEventDispatchers() {
        if (this.dispatchers == null) {
            return null;
        }
        return new ArrayList<KeyEventDispatcher>(this.dispatchers);
    }

    /** Agrega un posprocesador al final de la cadena; `null` no hace nada. */
    public void addKeyEventPostProcessor(KeyEventPostProcessor processor) {
        if (processor == null) {
            return;
        }
        synchronized (this) {
            if (this.postProcessors == null) {
                this.postProcessors = new ArrayList<KeyEventPostProcessor>();
            }
            this.postProcessors.add(processor);
        }
    }

    /** Saca un posprocesador; el gestor mismo no se puede sacar así. */
    public void removeKeyEventPostProcessor(KeyEventPostProcessor processor) {
        if (processor == null) {
            return;
        }
        synchronized (this) {
            if (this.postProcessors != null) {
                this.postProcessors.remove(processor);
            }
        }
    }

    /**
     * Los posprocesadores registrados.
     *
     * @return una copia, o `null` si nunca se registró ninguno, con la misma distinción que
     *     {@link #getKeyEventDispatchers}
     */
    protected java.util.List<KeyEventPostProcessor> getKeyEventPostProcessors() {
        synchronized (this) {
            if (this.postProcessors == null) {
                return null;
            }
            return new ArrayList<KeyEventPostProcessor>(this.postProcessors);
        }
    }

    /** Reparte ese evento. */
    public abstract boolean dispatchEvent(AWTEvent e);

    /**
     * Le manda el evento al componente **sin** volver a pasar por la cadena de repartidores.
     *
     * <p>Es `final` y existe para que un {@link KeyEventDispatcher} pueda entregar el evento sin
     * armar un ciclo: si llamara a `dispatchEvent`, la cadena volvería a pasar por él.
     */
    public final void redispatchEvent(Component target, AWTEvent e) {
        target.dispatchEvent(e);
    }

    /** Reparte un evento de teclado. */
    public abstract boolean dispatchKeyEvent(KeyEvent e);

    /** Mira un evento de teclado que nadie consumió. */
    public abstract boolean postProcessKeyEvent(KeyEvent e);

    /** Atiende las teclas de recorrido de ese componente. */
    public abstract void processKeyEvent(Component focusedComponent, KeyEvent e);

    /**
     * Guarda los eventos de teclado que lleguen mientras el foco está en tránsito.
     *
     * <p>Sin esto, una tecla apretada justo cuando el foco cambia de componente le llegaría al
     * equivocado.
     */
    protected abstract void enqueueKeyEvents(long after, Component untilFocused);

    /** Suelta los eventos guardados: el foco ya llegó. */
    protected abstract void dequeueKeyEvents(long after, Component untilFocused);

    /** Tira los eventos guardados para ese componente: el foco ya no va a llegarle. */
    protected abstract void discardKeyEvents(Component comp);

    /** Le pasa el foco al siguiente del recorrido. */
    public abstract void focusNextComponent(Component aComponent);

    /** Se lo pasa al anterior. */
    public abstract void focusPreviousComponent(Component aComponent);

    /** Sube un nivel de ciclo de foco. */
    public abstract void upFocusCycle(Component aComponent);

    /** Baja un nivel, entrando en ese contenedor. */
    public abstract void downFocusCycle(Container aContainer);

    /** Le pasa el foco al siguiente del que lo tiene ahora. */
    public final void focusNextComponent() {
        Component c = this.getFocusOwner();
        if (c != null) {
            this.focusNextComponent(c);
        }
    }

    /** Se lo pasa al anterior del que lo tiene ahora. */
    public final void focusPreviousComponent() {
        Component c = this.getFocusOwner();
        if (c != null) {
            this.focusPreviousComponent(c);
        }
    }

    /** Sube un nivel desde el que tiene el foco. */
    public final void upFocusCycle() {
        Component c = this.getFocusOwner();
        if (c != null) {
            this.upFocusCycle(c);
        }
    }

    /**
     * Baja un nivel desde el que tiene el foco.
     *
     * <p>Sólo hace algo si el que tiene el foco es un contenedor: bajar de ciclo es entrar en uno.
     */
    public final void downFocusCycle() {
        Component c = this.getFocusOwner();
        if (c instanceof Container) {
            this.downFocusCycle((Container) c);
        }
    }
}
