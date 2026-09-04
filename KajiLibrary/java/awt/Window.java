package java.awt;

import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.awt.im.InputContext;
import java.awt.image.BufferStrategy;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

/**
 * Una ventana sin borde ni barra de título: el contenedor de más arriba del árbol.
 *
 * <p>Es la raíz de todo lo que se muestra. Un {@link Frame} es una ventana con decoración y un
 * {@link Dialog} es una ventana subordinada; ésta, tal cual, es la que sirve para un menú emergente o
 * una pantalla de bienvenida — lo que tiene que aparecer sin marco.
 *
 * <p>La **propiedad** de una ventana sobre otra es lo que ata su suerte: una ventana con dueño se
 * minimiza, se cierra y se pone al frente con él. Es lo que hace que un diálogo no quede huérfano
 * flotando cuando se cierra la ventana que lo abrió.
 *
 * <p>Es raíz de ciclo de foco de manera irrevocable —{@link #setFocusCycleRoot} no hace nada— porque
 * el foco no puede salirse de una ventana con el tabulador: no hay a dónde ir.
 *
 * <p><strong>Nada de esto aparece en pantalla.</strong> Esta biblioteca no trae sistema de ventanas.
 * A diferencia del modo sin cabeza del JDK real, que **se niega a construir** una ventana, acá se
 * construye: negarse dejaría muerto todo el árbol de componentes, y así en cambio la jerarquía, el
 * maquetado, los oyentes y el reparto de eventos se pueden usar y probar. Lo que no pasa es la
 * aparición, y los métodos que dependen de ella lo dicen cada uno.
 */
public class Window extends Container implements Accessible {

    private static final long serialVersionUID = 4497834738069338734L;

    /** Para qué se usa una ventana; el escritorio la decora según esto. */
    public static enum Type {

        /** Una ventana común. */
        NORMAL,

        /** Una paleta de herramientas: barra de título más chica, no aparece en la barra de tareas. */
        UTILITY,

        /** Un menú emergente o una ayudita: sin decoración y efímera. */
        POPUP
    }

    private static final List<Window> todas = new ArrayList<Window>();

    private final Window owner;
    private final List<Window> ownedWindows = new ArrayList<Window>();
    private List<Image> icons = new ArrayList<Image>();
    private Dialog.ModalExclusionType modalExclusionType =
            Dialog.ModalExclusionType.NO_EXCLUDE;
    private Type type = Type.NORMAL;
    private boolean alwaysOnTop;
    private boolean focusableWindowState = true;
    private boolean autoRequestFocus = true;
    private boolean locationByPlatform;
    private float opacity = 1.0f;
    private Shape shape;
    private final GraphicsConfiguration graphicsConfig;

    private transient WindowListener windowListener;
    private transient WindowStateListener windowStateListener;
    private transient WindowFocusListener windowFocusListener;

    /** Con la ventana dueña, que puede ser `null`. */
    private Window(Window owner, GraphicsConfiguration gc, boolean marcaInterna) {
        this.owner = owner;
        this.graphicsConfig = gc;
        this.setFocusableWindowStateInterno();
        if (owner != null) {
            synchronized (owner.ownedWindows) {
                owner.ownedWindows.add(this);
            }
        }
        synchronized (todas) {
            todas.add(this);
        }
    }

    /** Deja la ventana como raíz de ciclo de foco, que es lo único que puede ser. */
    private void setFocusableWindowStateInterno() {
        super.setFocusCycleRoot(true);
    }

    /**
     * Una ventana que pertenece a ese marco.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public Window(Frame owner) {
        this(owner, owner == null ? null : owner.getGraphicsConfiguration(), true);
    }

    /**
     * Una ventana que pertenece a esa ventana.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public Window(Window owner) {
        this(owner, owner == null ? null : owner.getGraphicsConfiguration(), true);
    }

    /**
     * Como la anterior, con la configuración gráfica dada.
     *
     * @throws IllegalArgumentException si la configuración no es de una pantalla
     */
    public Window(Window owner, GraphicsConfiguration gc) {
        this(owner, gc, true);
    }

    /** Las imágenes que el escritorio usa como ícono, de varios tamaños. */
    public List<Image> getIconImages() {
        return new ArrayList<Image>(this.icons);
    }

    /**
     * Le pone íconos.
     *
     * <p>Se dan **varios tamaños** y el escritorio elige: uno chico para la barra de tareas, uno
     * grande para el conmutador de ventanas. Dar uno solo obliga a escalar y se ve mal.
     */
    public synchronized void setIconImages(List<? extends Image> icons) {
        List<Image> nuevos = new ArrayList<Image>();
        if (icons != null) {
            java.util.Iterator<? extends Image> it = icons.iterator();
            while (it.hasNext()) {
                Image i = it.next();
                if (i != null) {
                    nuevos.add(i);
                }
            }
        }
        this.icons = nuevos;
    }

    /** Le pone un solo ícono. */
    public void setIconImage(Image image) {
        List<Image> uno = new ArrayList<Image>();
        if (image != null) {
            uno.add(image);
        }
        this.setIconImages(uno);
    }

    /** Avisa que puede mostrarse. */
    public void addNotify() {
        super.addNotify();
    }

    /** Avisa que dejó de poder mostrarse. */
    public void removeNotify() {
        super.removeNotify();
    }

    /**
     * Ajusta la ventana al tamaño que sus hijos necesitan.
     *
     * <p>Es lo que evita tener que calcular a mano cuánto mide una interfaz: se arma el árbol, se
     * llama a esto, y la ventana queda del tamaño de su contenido.
     */
    public void pack() {
        Dimension d = this.getPreferredSize();
        Insets m = this.getInsets();
        this.setSize(d.width + m.left + m.right, d.height + m.top + m.bottom);
        this.validate();
    }

    /** Le fija la medida mínima. */
    public void setMinimumSize(Dimension minimumSize) {
        super.setMinimumSize(minimumSize);
    }

    /** La redimensiona. */
    public void setSize(Dimension d) {
        super.setSize(d);
    }

    /** La redimensiona. */
    public void setSize(int width, int height) {
        super.setSize(width, height);
    }

    /**
     * La mueve.
     *
     * <p>Mover una ventana a mano apaga {@link #setLocationByPlatform}: quien dice dónde va ya no
     * quiere que la ubique el escritorio.
     */
    public void setLocation(int x, int y) {
        this.locationByPlatform = false;
        super.setLocation(x, y);
    }

    /** La mueve. */
    public void setLocation(Point p) {
        this.setLocation(p.x, p.y);
    }

    /**
     * La mueve y la redimensiona.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #setBounds(int, int, int, int)}.
     */
    @Deprecated
    public void reshape(int x, int y, int width, int height) {
        this.locationByPlatform = false;
        super.reshape(x, y, width, height);
    }

    /** La mueve y la redimensiona. */
    public void setBounds(int x, int y, int width, int height) {
        this.reshape(x, y, width, height);
    }

    /** La mueve y la redimensiona. */
    public void setBounds(Rectangle r) {
        this.setBounds(r.x, r.y, r.width, r.height);
    }

    /**
     * La muestra o la esconde.
     *
     * <p>Mostrarla por primera vez dispara {@code WINDOW_OPENED}, y sólo la primera: es el aviso de
     * que la ventana nació, no de que se hizo visible.
     */
    public void setVisible(boolean b) {
        if (b) {
            this.show();
        } else {
            this.hide();
        }
    }

    private boolean seAbrioAlgunaVez;

    /**
     * La muestra.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #setVisible}.
     */
    @Deprecated
    public void show() {
        boolean primera = !this.seAbrioAlgunaVez;
        super.show();
        if (primera) {
            this.seAbrioAlgunaVez = true;
            this.dispararVentana(WindowEvent.WINDOW_OPENED);
        }
    }

    /**
     * La esconde.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #setVisible}.
     */
    @Deprecated
    public void hide() {
        synchronized (this.ownedWindows) {
            for (int i = 0; i < this.ownedWindows.size(); i++) {
                this.ownedWindows.get(i).hide();
            }
        }
        super.hide();
    }

    /**
     * Suelta los recursos de la ventana y de las que le pertenecen.
     *
     * <p>Una ventana desechada se puede volver a mostrar: {@code dispose} suelta los recursos del
     * sistema, no destruye el objeto. Es la diferencia con cerrar.
     */
    public void dispose() {
        synchronized (this.ownedWindows) {
            for (int i = this.ownedWindows.size() - 1; i >= 0; i--) {
                this.ownedWindows.get(i).dispose();
            }
        }
        this.hide();
        this.removeNotify();
        this.dispararVentana(WindowEvent.WINDOW_CLOSED);
    }

    /** Dispara un evento de ventana si alguien lo pidió. */
    private void dispararVentana(int id) {
        if (this.windowListener != null || this.windowStateListener != null
                || this.windowFocusListener != null
                || (this.eventMask & AWTEvent.WINDOW_EVENT_MASK) != 0) {
            this.processEvent(new WindowEvent(this, id));
        }
    }

    /** La pone adelante de las demás; sin escritorio, no hay orden que cambiar. */
    public void toFront() {
    }

    /** La manda atrás. */
    public void toBack() {
    }

    /** El juego de herramientas de la plataforma. */
    public Toolkit getToolkit() {
        return Toolkit.getDefaultToolkit();
    }

    /**
     * El aviso que el sistema dibuja sobre una ventana de código no confiable.
     *
     * @return `null`: esta ventana no es de código no confiable
     */
    public final String getWarningString() {
        return null;
    }

    /** El idioma; el de la máquina si no tiene propio, porque una ventana no tiene padre. */
    public Locale getLocale() {
        Locale l = null;
        try {
            l = super.getLocale();
        } catch (IllegalComponentStateException e) {
            // Una ventana no tiene padre del que heredarlo: se cae en el de la maquina.
            l = null;
        }
        if (l != null) {
            return l;
        }
        return Locale.getDefault();
    }

    /** El estado de escritura de esta ventana. */
    public InputContext getInputContext() {
        return InputContext.getInstance();
    }

    /** Le pone cursor. */
    public void setCursor(Cursor cursor) {
        super.setCursor(cursor);
    }

    /** La ventana a la que pertenece, o `null` si no pertenece a ninguna. */
    public Window getOwner() {
        return this.owner;
    }

    /** Las ventanas que le pertenecen. */
    public Window[] getOwnedWindows() {
        synchronized (this.ownedWindows) {
            return this.ownedWindows.toArray(new Window[this.ownedWindows.size()]);
        }
    }

    /** Todas las ventanas de esta aplicación. */
    public static Window[] getWindows() {
        synchronized (todas) {
            return todas.toArray(new Window[todas.size()]);
        }
    }

    /** Las que no pertenecen a ninguna otra. */
    public static Window[] getOwnerlessWindows() {
        synchronized (todas) {
            List<Window> out = new ArrayList<Window>();
            for (int i = 0; i < todas.size(); i++) {
                if (todas.get(i).getOwner() == null) {
                    out.add(todas.get(i));
                }
            }
            return out.toArray(new Window[out.size()]);
        }
    }

    /**
     * Declara que esta ventana no se bloquee con los diálogos modales.
     *
     * @throws NullPointerException si el tipo es `null`
     */
    public void setModalExclusionType(Dialog.ModalExclusionType exclusionType) {
        if (exclusionType == null) {
            this.modalExclusionType = Dialog.ModalExclusionType.NO_EXCLUDE;
        } else {
            this.modalExclusionType = exclusionType;
        }
    }

    /** De qué modales queda excluida. */
    public Dialog.ModalExclusionType getModalExclusionType() {
        return this.modalExclusionType;
    }

    /** Suma un oyente de ventana; un `null` se ignora. */
    public synchronized void addWindowListener(WindowListener l) {
        if (l == null) {
            return;
        }
        this.windowListener = AWTEventMulticaster.add(this.windowListener, l);
        this.enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    }

    /** Suma un oyente de estado; un `null` se ignora. */
    public synchronized void addWindowStateListener(WindowStateListener l) {
        if (l == null) {
            return;
        }
        this.windowStateListener = AWTEventMulticaster.add(this.windowStateListener, l);
        this.enableEvents(AWTEvent.WINDOW_STATE_EVENT_MASK);
    }

    /** Suma un oyente de foco de ventana; un `null` se ignora. */
    public synchronized void addWindowFocusListener(WindowFocusListener l) {
        if (l == null) {
            return;
        }
        this.windowFocusListener = AWTEventMulticaster.add(this.windowFocusListener, l);
        this.enableEvents(AWTEvent.WINDOW_FOCUS_EVENT_MASK);
    }

    /** Saca a ese oyente. */
    public synchronized void removeWindowListener(WindowListener l) {
        if (l == null) {
            return;
        }
        this.windowListener = AWTEventMulticaster.remove(this.windowListener, l);
    }

    /** Saca a ese oyente. */
    public synchronized void removeWindowStateListener(WindowStateListener l) {
        if (l == null) {
            return;
        }
        this.windowStateListener = AWTEventMulticaster.remove(this.windowStateListener, l);
    }

    /** Saca a ese oyente. */
    public synchronized void removeWindowFocusListener(WindowFocusListener l) {
        if (l == null) {
            return;
        }
        this.windowFocusListener = AWTEventMulticaster.remove(this.windowFocusListener, l);
    }

    /** Los oyentes de ventana. */
    public synchronized WindowListener[] getWindowListeners() {
        return AWTEventMulticaster.getListeners(this.windowListener, WindowListener.class);
    }

    /** Los oyentes de foco de ventana. */
    public synchronized WindowFocusListener[] getWindowFocusListeners() {
        return AWTEventMulticaster.getListeners(this.windowFocusListener,
                WindowFocusListener.class);
    }

    /** Los oyentes de estado. */
    public synchronized WindowStateListener[] getWindowStateListeners() {
        return AWTEventMulticaster.getListeners(this.windowStateListener,
                WindowStateListener.class);
    }

    /**
     * Los oyentes de esa clase.
     *
     * @throws ClassCastException si la clase no es de oyente
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (listenerType == WindowListener.class) {
            return AWTEventMulticaster.getListeners(this.windowListener, listenerType);
        }
        if (listenerType == WindowStateListener.class) {
            return AWTEventMulticaster.getListeners(this.windowStateListener, listenerType);
        }
        if (listenerType == WindowFocusListener.class) {
            return AWTEventMulticaster.getListeners(this.windowFocusListener, listenerType);
        }
        return super.getListeners(listenerType);
    }

    /**
     * Clasifica el evento.
     *
     * <p>Los tres tipos de evento de ventana comparten la clase {@link WindowEvent} y se distinguen
     * por el identificador; por eso hay que repartirlos acá y no por el tipo.
     */
    protected void processEvent(AWTEvent e) {
        if (e instanceof WindowEvent) {
            int id = e.getID();
            if (id == WindowEvent.WINDOW_GAINED_FOCUS || id == WindowEvent.WINDOW_LOST_FOCUS) {
                this.processWindowFocusEvent((WindowEvent) e);
            } else if (id == WindowEvent.WINDOW_STATE_CHANGED) {
                this.processWindowStateEvent((WindowEvent) e);
            } else {
                this.processWindowEvent((WindowEvent) e);
            }
            return;
        }
        super.processEvent(e);
    }

    /** Les avisa a los oyentes de ventana. */
    protected void processWindowEvent(WindowEvent e) {
        WindowListener l = this.windowListener;
        if (l == null) {
            return;
        }
        int id = e.getID();
        if (id == WindowEvent.WINDOW_OPENED) {
            l.windowOpened(e);
        } else if (id == WindowEvent.WINDOW_CLOSING) {
            l.windowClosing(e);
        } else if (id == WindowEvent.WINDOW_CLOSED) {
            l.windowClosed(e);
        } else if (id == WindowEvent.WINDOW_ICONIFIED) {
            l.windowIconified(e);
        } else if (id == WindowEvent.WINDOW_DEICONIFIED) {
            l.windowDeiconified(e);
        } else if (id == WindowEvent.WINDOW_ACTIVATED) {
            l.windowActivated(e);
        } else if (id == WindowEvent.WINDOW_DEACTIVATED) {
            l.windowDeactivated(e);
        }
    }

    /** Les avisa a los oyentes de foco de ventana. */
    protected void processWindowFocusEvent(WindowEvent e) {
        WindowFocusListener l = this.windowFocusListener;
        if (l == null) {
            return;
        }
        if (e.getID() == WindowEvent.WINDOW_GAINED_FOCUS) {
            l.windowGainedFocus(e);
        } else if (e.getID() == WindowEvent.WINDOW_LOST_FOCUS) {
            l.windowLostFocus(e);
        }
    }

    /** Les avisa a los oyentes de estado. */
    protected void processWindowStateEvent(WindowEvent e) {
        WindowStateListener l = this.windowStateListener;
        if (l != null && e.getID() == WindowEvent.WINDOW_STATE_CHANGED) {
            l.windowStateChanged(e);
        }
    }

    /** Declara que la ventana quede siempre por encima de las demás. */
    public final void setAlwaysOnTop(boolean alwaysOnTop) {
        boolean viejo;
        synchronized (this) {
            viejo = this.alwaysOnTop;
            this.alwaysOnTop = alwaysOnTop;
        }
        this.firePropertyChange("alwaysOnTop", viejo, alwaysOnTop);
    }

    /**
     * Si el escritorio admite ventanas siempre arriba.
     *
     * @return `false`: no hay escritorio
     */
    public boolean isAlwaysOnTopSupported() {
        return false;
    }

    /** Si se pidió que quede siempre arriba. */
    public final boolean isAlwaysOnTop() {
        return this.alwaysOnTop;
    }

    /**
     * Qué componente de esta ventana tiene el foco.
     *
     * @return `null`: no hay gestor de foco que se lo haya dado a nadie
     */
    public Component getFocusOwner() {
        return null;
    }

    /**
     * Quién tenía el foco la última vez que la ventana estuvo activa.
     *
     * @return `null` por el mismo motivo
     */
    public Component getMostRecentFocusOwner() {
        return null;
    }

    /**
     * Si es la ventana activa.
     *
     * @return `false`: sin escritorio ninguna ventana está activa
     */
    public boolean isActive() {
        return false;
    }

    /**
     * Si tiene el foco del teclado.
     *
     * @return `false` por el mismo motivo
     */
    public boolean isFocused() {
        return false;
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
     * No hace nada.
     *
     * <p>Una ventana **siempre** es raíz de ciclo de foco: el tabulador no tiene a dónde salir.
     */
    public final void setFocusCycleRoot(boolean focusCycleRoot) {
    }

    /** Siempre `true`. */
    public final boolean isFocusCycleRoot() {
        return true;
    }

    /**
     * La raíz del ciclo que la contiene.
     *
     * @return `null`: una ventana es la raíz, no está adentro de otra
     */
    public final Container getFocusCycleRootAncestor() {
        return null;
    }

    /**
     * Si puede recibir el foco.
     *
     * <p>No alcanza con quererlo: una ventana sin dueño y sin nada que enfocar adentro tampoco
     * puede.
     */
    public final boolean isFocusableWindow() {
        if (!this.getFocusableWindowState()) {
            return false;
        }
        return true;
    }

    /** Si se declaró que puede recibir el foco. */
    public boolean getFocusableWindowState() {
        return this.focusableWindowState;
    }

    /**
     * Declara si puede recibir el foco.
     *
     * <p>Apagarlo es lo que hace una barra de herramientas flotante: se puede clickear sin que la
     * ventana de trabajo pierda el foco.
     */
    public void setFocusableWindowState(boolean focusableWindowState) {
        boolean viejo;
        synchronized (this) {
            viejo = this.focusableWindowState;
            this.focusableWindowState = focusableWindowState;
        }
        this.firePropertyChange("focusableWindowState", viejo, focusableWindowState);
    }

    /** Declara si la ventana pide el foco sola al mostrarse. */
    public void setAutoRequestFocus(boolean autoRequestFocus) {
        this.autoRequestFocus = autoRequestFocus;
    }

    /** Si pide el foco sola al mostrarse. */
    public boolean isAutoRequestFocus() {
        return this.autoRequestFocus;
    }

    /** Suma alguien a quien avisarle de los cambios de propiedad. */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        super.addPropertyChangeListener(listener);
    }

    /** Suma un oyente para una propiedad concreta. */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        super.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Si al validar hay que parar acá.
     *
     * <p>Siempre `true`: una ventana tiene tamaño propio, así que revalidar hacia arriba no tiene
     * sentido. Es lo que evita que tocar un botón revalide la aplicación entera.
     */
    public boolean isValidateRoot() {
        return true;
    }

    /**
     * Le manda un evento del modelo viejo.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #dispatchEvent}.
     */
    @Deprecated
    public boolean postEvent(Event e) {
        if (this.handleEvent(e)) {
            e.consume();
            return true;
        }
        return false;
    }

    /**
     * Si se ve de verdad.
     *
     * @return `false`: la ventana nunca llega a la pantalla
     */
    public boolean isShowing() {
        return this.isVisible() && this.isDisplayable();
    }

    /**
     * Le aplica los textos de un catálogo de recursos.
     *
     * @deprecated no funcionaba bien con los contenedores anidados y se dejó sin reemplazo.
     */
    @Deprecated
    public void applyResourceBundle(ResourceBundle rb) {
    }

    /**
     * Lo mismo, buscando el catálogo por nombre.
     *
     * @deprecated por el mismo motivo.
     */
    @Deprecated
    public void applyResourceBundle(String rbName) {
        this.applyResourceBundle(ResourceBundle.getBundle(rbName));
    }

    /**
     * Declara para qué se usa la ventana.
     *
     * <p>Sólo tiene efecto **antes** de mostrarla: la decoración la elige el escritorio al crearla.
     *
     * @throws NullPointerException si el tipo es `null`
     */
    public void setType(Type type) {
        if (type == null) {
            throw new NullPointerException("type should not be null.");
        }
        this.type = type;
    }

    /** Para qué se usa. */
    public Type getType() {
        return this.type;
    }

    /**
     * La centra respecto de ese componente.
     *
     * <p>Con `null`, o con un componente que no esté en pantalla, la centra en el origen: es lo que
     * corresponde cuando no hay pantalla respecto de la cual centrar.
     */
    public void setLocationRelativeTo(Component c) {
        if (c == null || !c.isShowing()) {
            this.setLocation(0, 0);
            return;
        }
        Rectangle r = c.getBounds();
        this.setLocation(r.x + (r.width - this.getWidth()) / 2,
                r.y + (r.height - this.getHeight()) / 2);
    }

    /**
     * Arma una estrategia de buffers para dibujar sin parpadeo.
     *
     * @throws IllegalArgumentException si se piden menos de dos buffers
     * @throws IllegalStateException siempre: una estrategia de buffers necesita una superficie del
     *     sistema, y esta ventana no tiene ninguna
     */
    public void createBufferStrategy(int numBuffers) {
        if (numBuffers < 1) {
            throw new IllegalArgumentException("Number of buffers must be at least 1");
        }
        throw new IllegalStateException("la ventana no tiene superficie del sistema: esta "
                + "biblioteca no trae sistema de ventanas");
    }

    /**
     * Como la anterior, con las capacidades pedidas.
     *
     * @throws IllegalArgumentException si se piden menos de dos buffers o faltan las capacidades
     * @throws AWTException si las capacidades no se pueden cumplir
     * @throws IllegalStateException siempre, por el mismo motivo
     */
    public void createBufferStrategy(int numBuffers, BufferCapabilities caps)
            throws AWTException {
        if (numBuffers < 1) {
            throw new IllegalArgumentException("Number of buffers must be at least 1");
        }
        if (caps == null) {
            throw new IllegalArgumentException("No capabilities specified");
        }
        throw new IllegalStateException("la ventana no tiene superficie del sistema: esta "
                + "biblioteca no trae sistema de ventanas");
    }

    /**
     * La estrategia de buffers.
     *
     * @return `null`: nunca se pudo crear ninguna
     */
    public BufferStrategy getBufferStrategy() {
        return null;
    }

    /** Declara que la ubique el escritorio en vez de ponerla en una posición fija. */
    public void setLocationByPlatform(boolean locationByPlatform) {
        this.locationByPlatform = locationByPlatform;
    }

    /** Si se pidió que la ubique el escritorio. */
    public boolean isLocationByPlatform() {
        return this.locationByPlatform;
    }

    /** Cuán opaca es, de 0 a 1. */
    public float getOpacity() {
        return this.opacity;
    }

    /**
     * Le cambia la opacidad.
     *
     * @throws IllegalArgumentException si el valor no está entre 0 y 1
     * @throws IllegalComponentStateException si la ventana está decorada y se pide translucidez
     */
    public void setOpacity(float opacity) {
        if (opacity < 0.0f || opacity > 1.0f) {
            throw new IllegalArgumentException(
                    "The value of opacity should be in the range [0.0f .. 1.0f].");
        }
        this.opacity = opacity;
    }

    /** La forma recortada de la ventana, o `null` si es rectangular. */
    public Shape getShape() {
        return this.shape;
    }

    /**
     * Le recorta la forma.
     *
     * <p>Con `null` vuelve a ser rectangular. Es lo que permite una ventana redonda o con un agujero.
     */
    public void setShape(Shape shape) {
        this.shape = shape;
    }

    /** El color de fondo. */
    public Color getBackground() {
        return super.getBackground();
    }

    /**
     * Le cambia el color de fondo.
     *
     * <p>Un fondo con alfa menor que 255 pide transparencia por píxel, que el escritorio puede no
     * admitir.
     */
    public void setBackground(Color bgColor) {
        super.setBackground(bgColor);
    }

    /** Si pinta todos sus píxeles: sólo si su fondo es opaco. */
    public boolean isOpaque() {
        Color c = this.getBackground();
        if (c == null) {
            return true;
        }
        return c.getAlpha() == 255;
    }

    /** Se dibuja y dibuja a sus hijos. */
    public void paint(Graphics g) {
        super.paint(g);
    }

    /** La configuración gráfica con la que se creó, o `null`. */
    public GraphicsConfiguration getGraphicsConfiguration() {
        if (this.graphicsConfig != null) {
            return this.graphicsConfig;
        }
        return super.getGraphicsConfiguration();
    }

    /** La información de accesibilidad de esta ventana. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTWindow();
        }
        return this.accessibleContext;
    }

    /** La accesibilidad de una ventana. */
    protected class AccessibleAWTWindow extends AccessibleAWTContainer {

        /** Para las subclases. */
        protected AccessibleAWTWindow() {
        }

        /** Es una ventana. */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.WINDOW;
        }

        /** Los de un contenedor, más si está activa. */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet s = super.getAccessibleStateSet();
            if (Window.this.isActive()) {
                s.add(AccessibleState.ACTIVE);
            }
            return s;
        }
    }
}
