package java.awt;

import java.awt.datatransfer.Clipboard;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.event.AWTEventListener;
import java.awt.event.AWTEventListenerProxy;
import java.awt.im.InputMethodHighlight;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * El puente entre AWT y la plataforma.
 *
 * <p>Todo lo que depende del sistema operativo pasa por acá: el tamaño de la pantalla, el
 * portapapeles, las fuentes instaladas, la cola de eventos, el pitido. Es la clase que hace que el
 * resto de AWT no tenga que saber en qué sistema corre.
 *
 * <p>Es abstracta y hay **una** instancia, la que devuelve {@link #getDefaultToolkit}. El JDK carga
 * la de la plataforma por propiedad del sistema.
 *
 * <p><strong>Acá la instancia es un juego de herramientas sin pantalla.</strong> Es lo mismo que hace
 * el modo sin cabeza del JDK real, y la regla es la misma: lo que se puede contestar sin pantalla se
 * contesta, y lo que necesita una pantalla tira {@link HeadlessException}. No hay valores inventados
 * — un tamaño de pantalla que no existe no se puede aproximar.
 *
 * <p>Lo que **sí** funciona resulta ser bastante: el modelo de color, la cola de eventos con su hilo
 * de despacho, {@code createImage} a partir de un productor de píxeles, {@code prepareImage} y
 * {@code checkImage}, las propiedades del escritorio, los oyentes globales de eventos y el
 * portapapeles del sistema — que acá es uno privado, porque no hay uno del sistema con el que
 * compartir.
 */
public abstract class Toolkit {

    private static Toolkit toolkit;

    /** A quién avisarle de los cambios en las propiedades del escritorio. */
    protected final PropertyChangeSupport desktopPropsSupport = new PropertyChangeSupport(this);

    /** Las propiedades del escritorio, por nombre. */
    protected final Map<String, Object> desktopProperties = new HashMap<String, Object>();

    private final List<AWTEventListenerProxy> eventListeners =
            new ArrayList<AWTEventListenerProxy>();

    private boolean dynamicLayout = true;

    /** Para las subclases. */
    protected Toolkit() {
    }

    /**
     * La instancia de la plataforma.
     *
     * <p>Acá siempre es la misma: un juego de herramientas sin pantalla.
     */
    public static synchronized Toolkit getDefaultToolkit() {
        if (toolkit == null) {
            toolkit = new HeadlessToolkit();
        }
        return toolkit;
    }

    /** El tamaño de la pantalla. */
    public abstract Dimension getScreenSize() throws HeadlessException;

    /** Cuántos puntos por pulgada tiene la pantalla. */
    public abstract int getScreenResolution() throws HeadlessException;

    /**
     * Qué parte de la pantalla está tapada por barras del escritorio.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public Insets getScreenInsets(GraphicsConfiguration gc) throws HeadlessException {
        if (gc == null) {
            throw new NullPointerException("gc");
        }
        throw new HeadlessException();
    }

    /** El formato de píxel de la pantalla. */
    public abstract ColorModel getColorModel() throws HeadlessException;

    /**
     * Los nombres de las fuentes instaladas.
     *
     * @deprecated devuelve sólo las familias lógicas. Usar
     *     {@code GraphicsEnvironment.getAvailableFontFamilyNames}.
     */
    @Deprecated
    public abstract String[] getFontList();

    /**
     * Las medidas de esa fuente.
     *
     * @deprecated depende de la pantalla en la que se dibuje. Usar
     *     {@code Font.getLineMetrics(String, FontRenderContext)}.
     */
    @Deprecated
    public abstract FontMetrics getFontMetrics(Font font);

    /** Manda a la pantalla todo lo que estuviera pendiente de dibujar. */
    public abstract void sync();

    /** Una imagen leída de ese archivo. */
    public abstract Image getImage(String filename);

    /** Una imagen leída de esa dirección. */
    public abstract Image getImage(URL url);

    /** Una imagen leída de ese archivo, sin usar la caché. */
    public abstract Image createImage(String filename);

    /** Una imagen leída de esa dirección, sin usar la caché. */
    public abstract Image createImage(URL url);

    /** Una imagen a partir de un productor de píxeles. */
    public abstract Image createImage(ImageProducer producer);

    /** Una imagen decodificada de esos bytes. */
    public abstract Image createImage(byte[] imagedata, int imageoffset, int imagelength);

    /** Lo mismo, con el arreglo entero. */
    public Image createImage(byte[] imagedata) {
        return this.createImage(imagedata, 0, imagedata.length);
    }

    /**
     * Empieza a preparar una imagen para ese tamaño.
     *
     * @return si ya está lista
     */
    public abstract boolean prepareImage(Image image, int width, int height,
            ImageObserver observer);

    /** Cuánto se preparó, como banderas de {@link ImageObserver}. */
    public abstract int checkImage(Image image, int width, int height, ImageObserver observer);

    /**
     * Un trabajo de impresión.
     *
     * @return el trabajo, o `null` si el usuario lo canceló
     */
    public abstract PrintJob getPrintJob(Frame frame, String jobtitle, Properties props);

    /**
     * Un trabajo de impresión con atributos.
     *
     * @return el trabajo, o `null` si el usuario lo canceló
     * @throws NullPointerException si el marco es `null` y no se dan atributos de trabajo
     */
    public PrintJob getPrintJob(Frame frame, String jobtitle, JobAttributes jobAttributes,
            PageAttributes pageAttributes) {
        if (frame == null && (jobAttributes == null
                || jobAttributes.getDialog() == JobAttributes.DialogType.NATIVE)) {
            throw new NullPointerException("frame must not be null");
        }
        return this.getPrintJob(frame, jobtitle, null);
    }

    /** Hace sonar el pitido del sistema. */
    public abstract void beep();

    /** El portapapeles del sistema. */
    public abstract Clipboard getSystemClipboard() throws HeadlessException;

    /**
     * El portapapeles de selección, el que en X11 se llena al seleccionar texto.
     *
     * @return `null` si la plataforma no tiene uno
     * @throws HeadlessException si no hay pantalla
     */
    public Clipboard getSystemSelection() throws HeadlessException {
        return null;
    }

    /**
     * Qué tecla es el modificador de menú de la plataforma.
     *
     * @deprecated devuelve una máscara de la codificación vieja. Usar
     *     {@link #getMenuShortcutKeyMaskEx}.
     * @throws HeadlessException si no hay pantalla
     */
    @Deprecated
    public int getMenuShortcutKeyMask() throws HeadlessException {
        return java.awt.event.InputEvent.CTRL_MASK;
    }

    /**
     * Qué tecla es el modificador de menú, en la codificación nueva.
     *
     * <p>Control en casi todos lados, Meta en macOS. Preguntarlo es lo que evita escribir esa
     * diferencia en cada aplicación.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public int getMenuShortcutKeyMaskEx() throws HeadlessException {
        return java.awt.event.InputEvent.CTRL_DOWN_MASK;
    }

    /**
     * Si esa tecla de bloqueo está activada.
     *
     * @throws IllegalArgumentException si la tecla no es una de bloqueo
     * @throws UnsupportedOperationException si la plataforma no lo puede decir
     */
    public boolean getLockingKeyState(int keyCode) throws UnsupportedOperationException {
        if (keyCode != java.awt.event.KeyEvent.VK_CAPS_LOCK
                && keyCode != java.awt.event.KeyEvent.VK_NUM_LOCK
                && keyCode != java.awt.event.KeyEvent.VK_SCROLL_LOCK
                && keyCode != java.awt.event.KeyEvent.VK_KANA_LOCK) {
            throw new IllegalArgumentException("invalid key for Toolkit.getLockingKeyState");
        }
        throw new UnsupportedOperationException("no hay teclado del sistema al que preguntarle");
    }

    /**
     * Activa o desactiva una tecla de bloqueo.
     *
     * @throws IllegalArgumentException si la tecla no es una de bloqueo
     * @throws UnsupportedOperationException si la plataforma no lo admite
     */
    public void setLockingKeyState(int keyCode, boolean on) throws UnsupportedOperationException {
        if (keyCode != java.awt.event.KeyEvent.VK_CAPS_LOCK
                && keyCode != java.awt.event.KeyEvent.VK_NUM_LOCK
                && keyCode != java.awt.event.KeyEvent.VK_SCROLL_LOCK
                && keyCode != java.awt.event.KeyEvent.VK_KANA_LOCK) {
            throw new IllegalArgumentException("invalid key for Toolkit.setLockingKeyState");
        }
        throw new UnsupportedOperationException("no hay teclado del sistema al que pedírselo");
    }

    /**
     * Un cursor hecho a partir de una imagen.
     *
     * @throws IndexOutOfBoundsException si el punto caliente cae fuera de la imagen
     * @throws HeadlessException si no hay pantalla
     */
    public Cursor createCustomCursor(Image cursor, Point hotSpot, String name)
            throws IndexOutOfBoundsException, HeadlessException {
        throw new HeadlessException();
    }

    /**
     * El tamaño de cursor que la plataforma admite más cercano al pedido.
     *
     * @return el tamaño, o (0,0) si no admite cursores propios
     * @throws HeadlessException si no hay pantalla
     */
    public Dimension getBestCursorSize(int preferredWidth, int preferredHeight)
            throws HeadlessException {
        throw new HeadlessException();
    }

    /**
     * Cuántos colores admite un cursor propio.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public int getMaximumCursorColors() throws HeadlessException {
        throw new HeadlessException();
    }

    /**
     * Si la plataforma admite ese estado de ventana.
     *
     * @return `false`: no hay gestor de ventanas que los aplique
     * @throws HeadlessException si no hay pantalla
     */
    public boolean isFrameStateSupported(int state) throws HeadlessException {
        return state == Frame.NORMAL;
    }

    /**
     * Si la plataforma admite ventanas siempre arriba.
     *
     * @return `false`: no hay gestor de ventanas
     */
    public boolean isAlwaysOnTopSupported() {
        return false;
    }

    /** Si admite ese alcance de modalidad. */
    public abstract boolean isModalityTypeSupported(Dialog.ModalityType modalityType);

    /** Si admite ese tipo de exclusión de modalidad. */
    public abstract boolean isModalExclusionTypeSupported(
            Dialog.ModalExclusionType modalExclusionType);

    /**
     * Declara si las ventanas se remaquetan mientras se las arrastra.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public void setDynamicLayout(boolean dynamic) throws HeadlessException {
        this.dynamicLayout = dynamic;
    }

    /** Si se pidió el remaquetado continuo. */
    protected boolean isDynamicLayoutSet() throws HeadlessException {
        return this.dynamicLayout;
    }

    /**
     * Si el remaquetado continuo está efectivamente activo.
     *
     * @return `false`: no hay gestor de ventanas que arrastre nada
     * @throws HeadlessException si no hay pantalla
     */
    public boolean isDynamicLayoutActive() throws HeadlessException {
        return false;
    }

    /**
     * Si se distinguen los botones del ratón más allá del tercero.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public boolean areExtraMouseButtonsEnabled() throws HeadlessException {
        return true;
    }

    /** La cola de eventos del sistema. */
    public final EventQueue getSystemEventQueue() {
        return this.getSystemEventQueueImpl();
    }

    /** De dónde sale la cola; lo escribe cada juego de herramientas. */
    protected abstract EventQueue getSystemEventQueueImpl();

    /**
     * Cómo se dibuja un tramo de texto que el método de entrada está componiendo.
     *
     * @return el estilo, o `null` para que lo decida el componente
     * @throws HeadlessException si no hay pantalla
     */
    public abstract Map<java.awt.font.TextAttribute, ?> mapInputMethodHighlight(
            InputMethodHighlight highlight) throws HeadlessException;

    /**
     * Una propiedad del escritorio.
     *
     * @return el valor, o `null` si no está definida
     */
    public final synchronized Object getDesktopProperty(String propertyName) {
        if (this.desktopProperties.isEmpty()) {
            this.initializeDesktopProperties();
        }
        Object v = this.desktopProperties.get(propertyName);
        if (v == null) {
            v = this.lazilyLoadDesktopProperty(propertyName);
            if (v != null) {
                this.setDesktopProperty(propertyName, v);
            }
        }
        return v;
    }

    /** Guarda una propiedad del escritorio y avisa del cambio. */
    protected final void setDesktopProperty(String name, Object newValue) {
        Object viejo;
        synchronized (this) {
            viejo = this.desktopProperties.get(name);
            this.desktopProperties.put(name, newValue);
        }
        this.desktopPropsSupport.firePropertyChange(name, viejo, newValue);
    }

    /**
     * Carga una propiedad recién cuando se la pide.
     *
     * @return el valor, o `null` si no existe
     */
    protected Object lazilyLoadDesktopProperty(String name) {
        return null;
    }

    /** Llena las propiedades del escritorio; sin escritorio no hay ninguna. */
    protected void initializeDesktopProperties() {
    }

    /** Suma alguien a quien avisarle de los cambios de esa propiedad. */
    public void addPropertyChangeListener(String name, PropertyChangeListener pcl) {
        this.desktopPropsSupport.addPropertyChangeListener(name, pcl);
    }

    /** Saca a ese oyente. */
    public void removePropertyChangeListener(String name, PropertyChangeListener pcl) {
        this.desktopPropsSupport.removePropertyChangeListener(name, pcl);
    }

    /** Todos los oyentes de propiedades. */
    public PropertyChangeListener[] getPropertyChangeListeners() {
        return this.desktopPropsSupport.getPropertyChangeListeners();
    }

    /** Los oyentes de esa propiedad. */
    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return this.desktopPropsSupport.getPropertyChangeListeners(propertyName);
    }

    /**
     * Suma un oyente que ve **todos** los eventos de esas familias.
     *
     * <p>Es la puerta de atrás del despacho: se registra en el juego de herramientas y no en un
     * componente. Un `null` se ignora.
     */
    public void addAWTEventListener(AWTEventListener listener, long eventMask) {
        if (listener == null) {
            return;
        }
        synchronized (this) {
            this.eventListeners.add(new AWTEventListenerProxy(eventMask, listener));
        }
    }

    /** Saca a ese oyente global. */
    public void removeAWTEventListener(AWTEventListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (this) {
            for (int i = this.eventListeners.size() - 1; i >= 0; i--) {
                if (this.eventListeners.get(i).getListener() == listener) {
                    this.eventListeners.remove(i);
                }
            }
        }
    }

    /** Todos los oyentes globales, cada uno con su máscara. */
    public AWTEventListener[] getAWTEventListeners() {
        synchronized (this) {
            return this.eventListeners.toArray(new AWTEventListener[this.eventListeners.size()]);
        }
    }

    /** Los oyentes globales que cubren todas esas familias. */
    public AWTEventListener[] getAWTEventListeners(long eventMask) {
        synchronized (this) {
            List<AWTEventListener> out = new ArrayList<AWTEventListener>();
            for (int i = 0; i < this.eventListeners.size(); i++) {
                AWTEventListenerProxy p = this.eventListeners.get(i);
                if ((p.getEventMask() & eventMask) == eventMask) {
                    out.add(p);
                }
            }
            return out.toArray(new AWTEventListener[out.size()]);
        }
    }

    /**
     * Un reconocedor de gesto de arrastre de la clase pedida.
     *
     * @return `null`: el reconocedor concreto lo aporta el sistema de ventanas
     */
    public <T extends DragGestureRecognizer> T createDragGestureRecognizer(
            Class<T> abstractRecognizerClass, DragSource ds, Component c, int srcActions,
            DragGestureListener dgl) {
        return null;
    }

    /**
     * Una propiedad del sistema, con valor por omisión.
     *
     * @deprecated es un envoltorio de {@code System.getProperty} que no agrega nada.
     */
    @Deprecated
    public static String getProperty(String key, String defaultValue) {
        String v = System.getProperty(key);
        return v == null ? defaultValue : v;
    }

    /**
     * El contenedor nativo de un componente.
     *
     * @return el ancestro pesado más cercano, o `null` si no hay
     */
    protected static Container getNativeContainer(Component c) {
        Container p = c == null ? null : c.getParent();
        while (p != null && p.isLightweight()) {
            p = p.getParent();
        }
        return p;
    }

    /**
     * Llena ese arreglo con los colores del sistema.
     *
     * <p>No hace nada: sin escritorio no hay paleta que leer, y {@link SystemColor} ya trae valores
     * razonables por omisión.
     */
    protected void loadSystemColors(int[] systemColors) throws HeadlessException {
    }
}
