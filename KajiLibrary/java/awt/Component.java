package java.awt;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.im.InputContext;
import java.awt.im.InputMethodRequests;
import java.awt.image.BufferStrategy;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.VolatileImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

/**
 * Cualquier cosa que ocupa lugar en pantalla y puede recibir entrada del usuario.
 *
 * <p>Es la clase más grande de AWT y la raíz de todo lo visible. Junta cinco responsabilidades que
 * en un diseño de hoy estarían separadas, y conviene verlas por separado para entenderla:
 *
 * <ul>
 *   <li><strong>geometría</strong>: dónde está y cuánto mide, más las tres medidas sugeridas
 *       —mínima, preferida y máxima— que las distribuciones usan para repartir el espacio;
 *   <li><strong>apariencia</strong>: color de frente y de fondo, fuente, cursor, si se ve;
 *   <li><strong>eventos</strong>: registrar oyentes y repartirles lo que llega;
 *   <li><strong>foco</strong>: si puede recibirlo, con qué teclas se recorre;
 *   <li><strong>pintado</strong>: {@code paint}, {@code update} y {@code repaint}.
 * </ul>
 *
 * <p>Las tres medidas tienen una regla que se olvida: {@link #getPreferredSize} devuelve lo que se
 * le haya fijado con {@link #setPreferredSize}, y sólo si no se le fijó nada la calcula. Por eso
 * {@link #isPreferredSizeSet} existe — es la única forma de distinguir "me dijeron que mida esto" de
 * "yo creo que debería medir esto".
 *
 * <p>El reparto de eventos es de tres pasos y cada uno se puede interceptar:
 * {@link #dispatchEvent} recibe, {@link #processEvent} clasifica, y los {@code processXEvent}
 * avisan a los oyentes. Redefinir el del medio permite ver todo; redefinir uno de los últimos,
 * cambiar el tratamiento de una familia sin tocar el resto.
 *
 * <p><strong>Este componente nunca es mostrable.</strong> Todo lo que necesita una ventana del
 * sistema —{@link #isDisplayable}, {@link #getGraphics}, {@link #getLocationOnScreen},
 * {@link #createImage(int, int)}, el foco de verdad— contesta lo que corresponde a un componente que
 * no está en pantalla: `false`, `null` o la excepción que el método declara para ese caso. No son
 * rellenos: son las respuestas ciertas. Todo lo demás —la geometría, los colores, los oyentes, el
 * reparto de eventos, la jerarquía, la accesibilidad— funciona de verdad y se puede usar y probar.
 */
public abstract class Component implements ImageObserver, MenuContainer, Serializable {

    private static final long serialVersionUID = -7644114512714619750L;

    /** Alineado con el borde superior. */
    public static final float TOP_ALIGNMENT = 0.0f;

    /** Centrado. */
    public static final float CENTER_ALIGNMENT = 0.5f;

    /** Alineado con el borde inferior. */
    public static final float BOTTOM_ALIGNMENT = 1.0f;

    /** Alineado con el borde izquierdo. */
    public static final float LEFT_ALIGNMENT = 0.0f;

    /** Alineado con el borde derecho. */
    public static final float RIGHT_ALIGNMENT = 1.0f;

    /**
     * El candado con el que se sincroniza el árbol de componentes.
     *
     * <p>Es **uno solo** para todo AWT. Un candado por componente parecería mejor, pero recorrer el
     * árbol tomándolos en distinto orden terminaría en un abrazo mortal; con uno global eso no puede
     * pasar.
     */
    static final Object LOCK = new Object();

    private static int nameCounter;

    /** Cómo se estira la línea de base cuando el componente cambia de alto. */
    public static enum BaselineResizeBehavior {

        /** La distancia desde arriba no cambia. */
        CONSTANT_ASCENT,

        /** La distancia desde abajo no cambia. */
        CONSTANT_DESCENT,

        /** La línea de base se mantiene a la misma distancia del centro. */
        CENTER_OFFSET,

        /** Ninguna de las tres: hay que volver a preguntar en cada tamaño. */
        OTHER
    }

    private int x;
    private int y;
    private int width;
    private int height;
    private boolean visible = true;
    private boolean enabled = true;
    private boolean valid;
    private boolean focusable = true;
    private boolean focusTraversalKeysEnabled = true;
    private boolean ignoreRepaint;
    private Color foreground;
    private Color background;
    private Font font;
    private Cursor cursor;
    private String name;
    private boolean nameExplicitlySet;
    private Locale locale;
    private ComponentOrientation componentOrientation = ComponentOrientation.UNKNOWN;
    private Dimension minSize;
    private Dimension prefSize;
    private Dimension maxSize;
    private boolean minSizeSet;
    private boolean prefSizeSet;
    private boolean maxSizeSet;
    private java.awt.dnd.DropTarget dropTarget;
    private final Set<AWTKeyStroke>[] focusTraversalKeys = crearJuegoDeTeclas();
    private Container parent;
    private final List<PopupMenu> popups = new ArrayList<PopupMenu>();
    private PropertyChangeSupport changeSupport;

    /** Qué familias de eventos pidió recibir. */
    long eventMask;

    private transient ComponentListener componentListener;
    private transient FocusListener focusListener;
    private transient HierarchyListener hierarchyListener;
    private transient HierarchyBoundsListener hierarchyBoundsListener;
    private transient KeyListener keyListener;
    private transient MouseListener mouseListener;
    private transient MouseMotionListener mouseMotionListener;
    private transient MouseWheelListener mouseWheelListener;
    private transient InputMethodListener inputMethodListener;

    /** La información de accesibilidad, armada a demanda. */
    protected AccessibleContext accessibleContext;

    /** Un arreglo de cuatro conjuntos de teclas, uno por sentido de recorrido. */
    @SuppressWarnings("unchecked")
    private static Set<AWTKeyStroke>[] crearJuegoDeTeclas() {
        return (Set<AWTKeyStroke>[]) new Set<?>[4];
    }

    /**
     * Las teclas de recorrido de fábrica.
     *
     * <p>Tabulador hacia adelante, tabulador con mayúsculas hacia atrás, y nada para subir y bajar
     * de ciclo. Son las mismas de cualquier escritorio, y estar acá y no en un gestor de foco es lo
     * que permite que un componente suelto conteste bien sin que haya un gestor instalado.
     */
    private static Set<AWTKeyStroke> tecladoPorOmision(int id) {
        Set<AWTKeyStroke> s = new HashSet<AWTKeyStroke>();
        if (id == 0) {
            s.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, 0));
            s.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB,
                    java.awt.event.InputEvent.CTRL_DOWN_MASK));
        } else if (id == 1) {
            s.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB,
                    java.awt.event.InputEvent.SHIFT_DOWN_MASK));
            s.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB,
                    java.awt.event.InputEvent.CTRL_DOWN_MASK
                            | java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        }
        return Collections.unmodifiableSet(s);
    }

    /** Para las subclases. */
    protected Component() {
    }

    /** El nombre por omisión, distinto para cada uno. */
    String constructComponentName() {
        synchronized (Component.class) {
            String n = this.getClass().getName() + nameCounter;
            nameCounter = nameCounter + 1;
            return n;
        }
    }

    /** Cómo se llama; si nadie le puso nombre, se le arma uno para poder depurar. */
    public String getName() {
        if (this.name == null && !this.nameExplicitlySet) {
            synchronized (this.getObjectLock()) {
                if (this.name == null && !this.nameExplicitlySet) {
                    this.name = this.constructComponentName();
                }
            }
        }
        return this.name;
    }

    /** Le pone nombre y avisa del cambio. */
    public void setName(String name) {
        String viejo;
        synchronized (this.getObjectLock()) {
            viejo = this.name;
            this.name = name;
            this.nameExplicitlySet = true;
        }
        this.firePropertyChange("name", viejo, name);
    }

    /** El candado de este objeto; separado del del árbol para no serializarlo. */
    private Object getObjectLock() {
        return this;
    }

    /** De qué contenedor cuelga, o `null`. */
    public Container getParent() {
        return this.parent;
    }

    /** Lo usa el contenedor al agregarlo o sacarlo. */
    void setParent(Container p) {
        this.parent = p;
    }

    /**
     * El candado del árbol de componentes.
     *
     * <p>Es `final` y es el mismo para todos: ver {@link #LOCK}.
     */
    public final Object getTreeLock() {
        return LOCK;
    }

    /** El juego de herramientas de la plataforma. */
    public Toolkit getToolkit() {
        return Toolkit.getDefaultToolkit();
    }

    /**
     * Si el componente tiene una ventana del sistema detrás.
     *
     * <p>Contesta `false` siempre: esta biblioteca no trae sistema de ventanas, así que ningún
     * componente llega a tener una. De acá salen casi todas las demás respuestas negativas de la
     * clase, y todas son ciertas.
     */
    public boolean isDisplayable() {
        return false;
    }

    /** Si está declarado visible. */
    public boolean isVisible() {
        return this.visible;
    }

    /**
     * Si se ve de verdad.
     *
     * <p>No alcanza con estar declarado visible: hay que estarlo, tener padre, y que el padre
     * también se vea. Contesta `false` siempre porque ningún componente llega a estar en pantalla.
     */
    public boolean isShowing() {
        if (this.visible && this.isDisplayable()) {
            Container p = this.parent;
            return p == null || p.isShowing();
        }
        return false;
    }

    /** Si responde a la entrada del usuario. */
    public boolean isEnabled() {
        return this.enabled;
    }

    /** Lo habilita o lo deshabilita. */
    public void setEnabled(boolean b) {
        boolean viejo;
        synchronized (this.getTreeLock()) {
            viejo = this.enabled;
            this.enabled = b;
        }
        this.firePropertyChange("enabled", viejo, b);
    }

    /**
     * Lo habilita.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #setEnabled}.
     */
    @Deprecated
    public void enable() {
        this.setEnabled(true);
    }

    /**
     * Lo habilita o lo deshabilita.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #setEnabled}.
     */
    @Deprecated
    public void enable(boolean b) {
        this.setEnabled(b);
    }

    /**
     * Lo deshabilita.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #setEnabled}.
     */
    @Deprecated
    public void disable() {
        this.setEnabled(false);
    }

    /** Si dibuja en dos pasos para evitar el parpadeo. */
    public boolean isDoubleBuffered() {
        return false;
    }

    /** Prende o apaga el método de entrada para este componente. */
    public void enableInputMethods(boolean enable) {
    }

    /** Lo muestra o lo oculta, y avisa. */
    public void setVisible(boolean b) {
        this.show(b);
    }

    /**
     * Lo muestra.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #setVisible}.
     */
    @Deprecated
    public void show() {
        boolean viejo;
        synchronized (this.getTreeLock()) {
            viejo = this.visible;
            this.visible = true;
        }
        if (!viejo) {
            this.firePropertyChange("visible", false, true);
            this.dispararComponente(ComponentEvent.COMPONENT_SHOWN);
        }
    }

    /**
     * Lo muestra o lo oculta.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #setVisible}.
     */
    @Deprecated
    public void show(boolean b) {
        if (b) {
            this.show();
        } else {
            this.hide();
        }
    }

    /**
     * Lo oculta.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #setVisible}.
     */
    @Deprecated
    public void hide() {
        boolean viejo;
        synchronized (this.getTreeLock()) {
            viejo = this.visible;
            this.visible = false;
        }
        if (viejo) {
            this.firePropertyChange("visible", true, false);
            this.dispararComponente(ComponentEvent.COMPONENT_HIDDEN);
        }
    }

    /**
     * Dispara un evento de componente si alguien lo pidió **y** el componente está en pantalla.
     *
     * <p>La segunda condición es la que sorprende y es la del JDK: estos eventos los genera el
     * sistema de ventanas al mover o redimensionar de verdad, no el modelo al cambiar un número.
     * Un componente que nunca llega a la pantalla no genera ninguno por su cuenta — lo que no impide
     * entregárselos a mano con {@link #dispatchEvent}, que es lo que hace un armador de pruebas.
     */
    private void dispararComponente(int id) {
        if (!this.isDisplayable()) {
            return;
        }
        if (this.componentListener != null
                || (this.eventMask & AWTEvent.COMPONENT_EVENT_MASK) != 0) {
            this.processComponentEvent(new ComponentEvent(this, id));
        }
    }

    /** El color con el que se dibuja; se hereda del padre si no tiene propio. */
    public Color getForeground() {
        Color c = this.foreground;
        if (c != null) {
            return c;
        }
        Container p = this.parent;
        if (p != null) {
            return p.getForeground();
        }
        return null;
    }

    /** Le pone color propio. */
    public void setForeground(Color c) {
        Color viejo = this.foreground;
        this.foreground = c;
        this.firePropertyChange("foreground", viejo, c);
    }

    /** Si tiene color propio, sin contar el heredado. */
    public boolean isForegroundSet() {
        return this.foreground != null;
    }

    /** El color de fondo; se hereda del padre si no tiene propio. */
    public Color getBackground() {
        Color c = this.background;
        if (c != null) {
            return c;
        }
        Container p = this.parent;
        if (p != null) {
            return p.getBackground();
        }
        return null;
    }

    /** Le pone color de fondo propio. */
    public void setBackground(Color c) {
        Color viejo = this.background;
        this.background = c;
        this.firePropertyChange("background", viejo, c);
    }

    /** Si tiene color de fondo propio. */
    public boolean isBackgroundSet() {
        return this.background != null;
    }

    /** La fuente; se hereda del padre si no tiene propia. */
    public Font getFont() {
        Font f = this.font;
        if (f != null) {
            return f;
        }
        Container p = this.parent;
        if (p != null) {
            return p.getFont();
        }
        return null;
    }

    /**
     * Le pone fuente propia.
     *
     * <p>Invalida el componente: cambiar la fuente cambia cuánto mide el texto, y con eso la medida
     * preferida.
     */
    public void setFont(Font f) {
        Font viejo;
        synchronized (this.getTreeLock()) {
            viejo = this.font;
            this.font = f;
        }
        this.firePropertyChange("font", viejo, f);
        this.invalidate();
    }

    /** Si tiene fuente propia. */
    public boolean isFontSet() {
        return this.font != null;
    }

    /** El idioma; se hereda del padre si no tiene propio. */
    public Locale getLocale() {
        Locale l = this.locale;
        if (l != null) {
            return l;
        }
        Container p = this.parent;
        if (p == null) {
            throw new IllegalComponentStateException(
                    "This component must have a parent in order to determine its locale");
        }
        return p.getLocale();
    }

    /** Le pone idioma propio. */
    public void setLocale(Locale l) {
        Locale viejo = this.locale;
        this.locale = l;
        this.firePropertyChange("locale", viejo, l);
        this.invalidate();
    }

    /**
     * El formato de color en el que dibuja.
     *
     * <p>Sin ventana propia, el del juego de herramientas.
     */
    public ColorModel getColorModel() {
        return this.getToolkit().getColorModel();
    }

    /** Dónde está, relativo a su padre. */
    public Point getLocation() {
        return this.location();
    }

    /**
     * Dónde está en la pantalla.
     *
     * @throws IllegalComponentStateException siempre: el componente no está en pantalla, así que no
     *     tiene posición en ella. Es la excepción que el método declara para este caso.
     */
    public Point getLocationOnScreen() {
        throw new IllegalComponentStateException("component must be showing on the screen to "
                + "determine its location");
    }

    /**
     * Dónde está.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #getLocation}.
     */
    @Deprecated
    public Point location() {
        return new Point(this.x, this.y);
    }

    /** Lo mueve. */
    public void setLocation(int x, int y) {
        this.move(x, y);
    }

    /**
     * Lo mueve.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #setLocation}.
     */
    @Deprecated
    public void move(int x, int y) {
        synchronized (this.getTreeLock()) {
            this.setBoundsOp(x, y, this.width, this.height);
        }
    }

    /**
     * Lo mueve.
     *
     * @throws NullPointerException si el punto es `null`
     */
    public void setLocation(Point p) {
        this.setLocation(p.x, p.y);
    }

    /** Cuánto mide. */
    public Dimension getSize() {
        return this.size();
    }

    /**
     * Cuánto mide.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #getSize}.
     */
    @Deprecated
    public Dimension size() {
        return new Dimension(this.width, this.height);
    }

    /** Lo redimensiona. */
    public void setSize(int width, int height) {
        this.resize(width, height);
    }

    /**
     * Lo redimensiona.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #setSize}.
     */
    @Deprecated
    public void resize(int width, int height) {
        synchronized (this.getTreeLock()) {
            this.setBoundsOp(this.x, this.y, width, height);
        }
    }

    /**
     * Lo redimensiona.
     *
     * @throws NullPointerException si la dimensión es `null`
     */
    public void setSize(Dimension d) {
        this.setSize(d.width, d.height);
    }

    /**
     * Lo redimensiona.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #setSize}.
     */
    @Deprecated
    public void resize(Dimension d) {
        this.setSize(d.width, d.height);
    }

    /** Dónde está y cuánto mide. */
    public Rectangle getBounds() {
        return this.bounds();
    }

    /**
     * Dónde está y cuánto mide.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #getBounds}.
     */
    @Deprecated
    public Rectangle bounds() {
        return new Rectangle(this.x, this.y, this.width, this.height);
    }

    /** Lo mueve y lo redimensiona de una vez. */
    public void setBounds(int x, int y, int width, int height) {
        this.reshape(x, y, width, height);
    }

    /**
     * Lo mueve y lo redimensiona.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #setBounds}.
     */
    @Deprecated
    public void reshape(int x, int y, int width, int height) {
        synchronized (this.getTreeLock()) {
            this.setBoundsOp(x, y, width, height);
        }
    }

    /**
     * Cambia el rectángulo y dispara los eventos que correspondan.
     *
     * <p>Moverse y redimensionarse son dos eventos distintos, y una operación que haga las dos cosas
     * tiene que disparar los dos: hay código que escucha sólo uno.
     */
    private void setBoundsOp(int x, int y, int width, int height) {
        boolean seMovio = this.x != x || this.y != y;
        boolean cambioTamano = this.width != width || this.height != height;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        if (cambioTamano) {
            this.invalidate();
        }
        if (seMovio) {
            this.dispararComponente(ComponentEvent.COMPONENT_MOVED);
        }
        if (cambioTamano) {
            this.dispararComponente(ComponentEvent.COMPONENT_RESIZED);
        }
    }

    /**
     * Lo mueve y lo redimensiona.
     *
     * @throws NullPointerException si el rectángulo es `null`
     */
    public void setBounds(Rectangle r) {
        this.setBounds(r.x, r.y, r.width, r.height);
    }

    /** La X, relativa al padre. */
    public int getX() {
        return this.x;
    }

    /** La Y, relativa al padre. */
    public int getY() {
        return this.y;
    }

    /** El ancho. */
    public int getWidth() {
        return this.width;
    }

    /** El alto. */
    public int getHeight() {
        return this.height;
    }

    /**
     * Su rectángulo, escrito en el que se pasa.
     *
     * <p>Existe para no crear un objeto por consulta en un bucle de maquetado.
     */
    public Rectangle getBounds(Rectangle rv) {
        if (rv == null) {
            return new Rectangle(this.x, this.y, this.width, this.height);
        }
        rv.setBounds(this.x, this.y, this.width, this.height);
        return rv;
    }

    /** Su tamaño, escrito en el que se pasa. */
    public Dimension getSize(Dimension rv) {
        if (rv == null) {
            return new Dimension(this.width, this.height);
        }
        rv.setSize(this.width, this.height);
        return rv;
    }

    /** Su posición, escrita en el que se pasa. */
    public Point getLocation(Point rv) {
        if (rv == null) {
            return new Point(this.x, this.y);
        }
        rv.setLocation(this.x, this.y);
        return rv;
    }

    /**
     * Si pinta todos sus píxeles.
     *
     * <p>Contesta `false` cuando no tiene color de fondo: sin fondo, lo de abajo se ve.
     */
    public boolean isOpaque() {
        return false;
    }

    /**
     * Si no tiene ventana propia del sistema.
     *
     * <p>Contesta `true` siempre acá: ningún componente de esta biblioteca llega a tener una.
     */
    public boolean isLightweight() {
        return true;
    }

    /** Le fija la medida preferida; con `null` vuelve a calcularla. */
    public void setPreferredSize(Dimension preferredSize) {
        Dimension viejo = this.prefSize;
        this.prefSize = preferredSize;
        this.prefSizeSet = preferredSize != null;
        this.firePropertyChange("preferredSize", viejo, preferredSize);
    }

    /** Si alguien le fijó la medida preferida. */
    public boolean isPreferredSizeSet() {
        return this.prefSizeSet;
    }

    /** La medida preferida: la fijada, o la calculada si no hay. */
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
        if (this.prefSizeSet && this.prefSize != null) {
            return new Dimension(this.prefSize);
        }
        return this.getMinimumSize();
    }

    /** Le fija la medida mínima; con `null` vuelve a calcularla. */
    public void setMinimumSize(Dimension minimumSize) {
        Dimension viejo = this.minSize;
        this.minSize = minimumSize;
        this.minSizeSet = minimumSize != null;
        this.firePropertyChange("minimumSize", viejo, minimumSize);
    }

    /** Si alguien le fijó la medida mínima. */
    public boolean isMinimumSizeSet() {
        return this.minSizeSet;
    }

    /** La medida mínima: la fijada, o el tamaño actual si no hay. */
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
        if (this.minSizeSet && this.minSize != null) {
            return new Dimension(this.minSize);
        }
        return new Dimension(this.width, this.height);
    }

    /** Le fija la medida máxima; con `null` vuelve a calcularla. */
    public void setMaximumSize(Dimension maximumSize) {
        Dimension viejo = this.maxSize;
        this.maxSize = maximumSize;
        this.maxSizeSet = maximumSize != null;
        this.firePropertyChange("maximumSize", viejo, maximumSize);
    }

    /** Si alguien le fijó la medida máxima. */
    public boolean isMaximumSizeSet() {
        return this.maxSizeSet;
    }

    /**
     * La medida máxima.
     *
     * <p>Sin fijar, es el máximo entero en las dos direcciones: significa "no tengo tope", que es
     * distinto de "quiero ser enorme".
     */
    public Dimension getMaximumSize() {
        if (this.maxSizeSet && this.maxSize != null) {
            return new Dimension(this.maxSize);
        }
        return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
    }

    /** Cómo se alinea horizontalmente dentro de su contenedor. */
    public float getAlignmentX() {
        return CENTER_ALIGNMENT;
    }

    /** Cómo se alinea verticalmente. */
    public float getAlignmentY() {
        return CENTER_ALIGNMENT;
    }

    /**
     * A qué altura tiene la línea de base para ese tamaño.
     *
     * @return -1: un componente genérico no tiene línea de base, y decir que la tiene en cualquier
     *     lado desalinearía el texto de toda una fila
     * @throws IllegalArgumentException si alguna medida es negativa
     */
    public int getBaseline(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Width and height must be >= 0");
        }
        return -1;
    }

    /** Cómo se mueve la línea de base al cambiar el alto. */
    public BaselineResizeBehavior getBaselineResizeBehavior() {
        return BaselineResizeBehavior.OTHER;
    }

    /** Reordena a sus hijos; un componente sin hijos no hace nada. */
    public void doLayout() {
        this.layout();
    }

    /**
     * Reordena a sus hijos.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #doLayout}.
     */
    @Deprecated
    public void layout() {
    }

    /**
     * Vuelve a maquetar si hacía falta.
     *
     * <p>Marca el componente como válido de abajo hacia arriba; lo hace un contenedor de verdad al
     * redefinirlo.
     */
    public void validate() {
        synchronized (this.getTreeLock()) {
            this.valid = true;
        }
    }

    /**
     * Marca que hay que volver a maquetar.
     *
     * <p>Se propaga **hacia arriba**: si un hijo cambió de tamaño preferido, el padre tiene que
     * recalcular el suyo. De ahí que invalidar sea barato y validar caro.
     *
     * <p>Ahora bien, sube **sólo si el padre estaba válido**. Si ya estaba inválido, alguien lo
     * invalidó antes y la rama de arriba ya se enteró: seguir subiendo sería recorrer el árbol de
     * nuevo para no cambiar nada. Y no es sólo eficiencia: una distribución que está ubicando a sus
     * hijos hace un `setBounds` por cada uno, y cada uno la invalidaría **a ella en el medio del
     * trabajo**, tirando lo que acababa de calcular. Con la guarda, el padre inválido —que es lo
     * que es un contenedor mientras se lo maqueta— no se entera de nada.
     */
    public void invalidate() {
        synchronized (this.getTreeLock()) {
            this.valid = false;
            this.prefSize = this.prefSizeSet ? this.prefSize : null;
            this.minSize = this.minSizeSet ? this.minSize : null;
            this.maxSize = this.maxSizeSet ? this.maxSize : null;
            this.invalidateParent();
        }
    }

    /** Invalida al padre, si lo hay y si estaba válido. */
    void invalidateParent() {
        Container p = this.parent;
        if (p != null) {
            p.invalidateIfValid();
        }
    }

    /** Se invalida sólo si estaba válido; si ya estaba inválido no hay nada que avisar. */
    void invalidateIfValid() {
        if (this.isValid()) {
            this.invalidate();
        }
    }

    /** Invalida y pide revalidar la rama. */
    public void revalidate() {
        this.invalidate();
        Container p = this.parent;
        if (p != null) {
            p.validate();
        }
    }

    /** Si no hace falta volver a maquetarlo. */
    public boolean isValid() {
        return this.valid;
    }

    /**
     * Un contexto para dibujar sobre este componente.
     *
     * @return `null` siempre: el componente no está en pantalla, y es lo que el JDK devuelve en ese
     *     caso. Para dibujar sobre píxeles está {@link java.awt.image.BufferedImage}.
     */
    public Graphics getGraphics() {
        return null;
    }

    /**
     * Las medidas de esa fuente.
     *
     * @throws NullPointerException si la fuente es `null`
     */
    public FontMetrics getFontMetrics(Font font) {
        return this.getToolkit().getFontMetrics(font);
    }

    /** El cursor; se hereda del padre si no tiene propio. */
    public Cursor getCursor() {
        Cursor c = this.cursor;
        if (c != null) {
            return c;
        }
        Container p = this.parent;
        if (p != null) {
            return p.getCursor();
        }
        return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    }

    /** Le pone cursor propio. */
    public void setCursor(Cursor cursor) {
        this.cursor = cursor;
    }

    /** Si tiene cursor propio. */
    public boolean isCursorSet() {
        return this.cursor != null;
    }

    /**
     * Dibuja el componente.
     *
     * <p>No hace nada: un componente genérico no tiene nada que dibujar. Las subclases lo redefinen.
     */
    public void paint(Graphics g) {
    }

    /**
     * Borra el fondo y dibuja.
     *
     * <p>Es lo que se llama al repintar un componente que ya estaba dibujado. Separarlo de
     * {@link #paint} permite que un componente que sabe que va a cubrir todo se saltee el borrado.
     */
    public void update(Graphics g) {
        if (this.isOpaque()) {
            g.setColor(this.getBackground());
            g.fillRect(0, 0, this.width, this.height);
            g.setColor(this.getForeground());
        }
        this.paint(g);
    }

    /** Dibuja este componente y todos sus hijos. */
    public void paintAll(Graphics g) {
        if (this.isShowing()) {
            this.paint(g);
        }
    }

    /**
     * Dibuja el componente para imprimirlo.
     *
     * <p>Por omisión es lo mismo que pintarlo. Se separa para que un componente pueda imprimirse
     * distinto de como se ve — sin fondo oscuro, por ejemplo.
     */
    public void print(Graphics g) {
        this.paint(g);
    }

    /** Imprime este componente y todos sus hijos. */
    public void printAll(Graphics g) {
        this.print(g);
    }

    /**
     * Pide que se lo vuelva a dibujar.
     *
     * <p>No hace nada: repintar es encolar un pedido en la cola de eventos para que el sistema lo
     * atienda, y sin ventana no hay nada en pantalla que actualizar. No es un descarte silencioso de
     * trabajo — es que el trabajo no existe.
     */
    public void repaint() {
        this.repaint(0, 0, 0, this.width, this.height);
    }

    /** Como el anterior, con un plazo máximo. */
    public void repaint(long tm) {
        this.repaint(tm, 0, 0, this.width, this.height);
    }

    /** Como el anterior, sólo de ese rectángulo. */
    public void repaint(int x, int y, int width, int height) {
        this.repaint(0, x, y, width, height);
    }

    /** Como el anterior, con plazo y rectángulo. */
    public void repaint(long tm, int x, int y, int width, int height) {
    }

    /** Si hay que ignorar los pedidos de repintado del sistema. */
    public boolean getIgnoreRepaint() {
        return this.ignoreRepaint;
    }

    /**
     * Declara si hay que ignorarlos.
     *
     * <p>Sirve para las aplicaciones que dibujan cada cuadro por su cuenta: el repintado del sistema
     * sólo les agregaría trabajo y parpadeo.
     */
    public void setIgnoreRepaint(boolean ignoreRepaint) {
        this.ignoreRepaint = ignoreRepaint;
    }

    /** Si ese punto, relativo al componente, cae adentro. */
    public boolean contains(int x, int y) {
        return this.inside(x, y);
    }

    /**
     * Si ese punto cae adentro.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #contains(int, int)}.
     */
    @Deprecated
    public boolean inside(int x, int y) {
        return x >= 0 && x < this.width && y >= 0 && y < this.height;
    }

    /**
     * Si ese punto cae adentro.
     *
     * @throws NullPointerException si el punto es `null`
     */
    public boolean contains(Point p) {
        return this.contains(p.x, p.y);
    }

    /** Qué componente hay en ese punto: él mismo, o `null` si el punto cae afuera. */
    public Component getComponentAt(int x, int y) {
        return this.locate(x, y);
    }

    /**
     * Qué componente hay en ese punto.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #getComponentAt(int, int)}.
     */
    @Deprecated
    public Component locate(int x, int y) {
        return this.contains(x, y) ? this : null;
    }

    /**
     * Qué componente hay en ese punto.
     *
     * @throws NullPointerException si el punto es `null`
     */
    public Component getComponentAt(Point p) {
        return this.getComponentAt(p.x, p.y);
    }

    /**
     * Dónde está el ratón sobre este componente.
     *
     * @return `null` siempre: el componente no está en pantalla, así que el ratón no puede estar
     *     sobre él
     * @throws HeadlessException si no hay pantalla
     */
    public Point getMousePosition() throws HeadlessException {
        return null;
    }

    /**
     * Le manda un evento del modelo viejo.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #dispatchEvent}.
     */
    @Deprecated
    public void deliverEvent(Event e) {
        this.postEvent(e);
    }

    /**
     * Le manda un evento del modelo viejo al padre.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #dispatchEvent}.
     */
    @Deprecated
    public boolean postEvent(Event e) {
        Container p = this.parent;
        if (p != null) {
            return p.postEvent(e);
        }
        return false;
    }

    /**
     * Le entrega un evento a este componente.
     *
     * <p>Es `final`: el punto de extensión es {@link #processEvent}, no éste. Que sea así permite
     * que el sistema haga lo suyo —marcar el evento, consumirlo si hace falta— antes de que el
     * componente lo vea.
     */
    public final void dispatchEvent(AWTEvent e) {
        this.processEvent(e);
    }

    /**
     * Clasifica el evento y se lo pasa al método de su familia.
     *
     * <p>Es el punto donde interceptar **todo** lo que le llega al componente. Una subclase que lo
     * redefina tiene que llamar a `super` o los oyentes dejan de recibir.
     */
    protected void processEvent(AWTEvent e) {
        if (e instanceof FocusEvent) {
            this.processFocusEvent((FocusEvent) e);
        } else if (e instanceof MouseWheelEvent) {
            this.processMouseWheelEvent((MouseWheelEvent) e);
        } else if (e instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) e;
            int id = me.getID();
            if (id == MouseEvent.MOUSE_MOVED || id == MouseEvent.MOUSE_DRAGGED) {
                this.processMouseMotionEvent(me);
            } else {
                this.processMouseEvent(me);
            }
        } else if (e instanceof KeyEvent) {
            this.processKeyEvent((KeyEvent) e);
        } else if (e instanceof ComponentEvent) {
            this.processComponentEvent((ComponentEvent) e);
        } else if (e instanceof InputMethodEvent) {
            this.processInputMethodEvent((InputMethodEvent) e);
        } else if (e instanceof HierarchyEvent) {
            HierarchyEvent he = (HierarchyEvent) e;
            if (he.getID() == HierarchyEvent.HIERARCHY_CHANGED) {
                this.processHierarchyEvent(he);
            } else {
                this.processHierarchyBoundsEvent(he);
            }
        }
    }

    /** Les avisa a los oyentes de componente. */
    protected void processComponentEvent(ComponentEvent e) {
        ComponentListener l = this.componentListener;
        if (l == null) {
            return;
        }
        int id = e.getID();
        if (id == ComponentEvent.COMPONENT_RESIZED) {
            l.componentResized(e);
        } else if (id == ComponentEvent.COMPONENT_MOVED) {
            l.componentMoved(e);
        } else if (id == ComponentEvent.COMPONENT_SHOWN) {
            l.componentShown(e);
        } else if (id == ComponentEvent.COMPONENT_HIDDEN) {
            l.componentHidden(e);
        }
    }

    /** Les avisa a los oyentes de foco. */
    protected void processFocusEvent(FocusEvent e) {
        FocusListener l = this.focusListener;
        if (l == null) {
            return;
        }
        if (e.getID() == FocusEvent.FOCUS_GAINED) {
            l.focusGained(e);
        } else if (e.getID() == FocusEvent.FOCUS_LOST) {
            l.focusLost(e);
        }
    }

    /** Les avisa a los oyentes de teclado. */
    protected void processKeyEvent(KeyEvent e) {
        KeyListener l = this.keyListener;
        if (l == null) {
            return;
        }
        int id = e.getID();
        if (id == KeyEvent.KEY_TYPED) {
            l.keyTyped(e);
        } else if (id == KeyEvent.KEY_PRESSED) {
            l.keyPressed(e);
        } else if (id == KeyEvent.KEY_RELEASED) {
            l.keyReleased(e);
        }
    }

    /** Les avisa a los oyentes de botones del ratón. */
    protected void processMouseEvent(MouseEvent e) {
        MouseListener l = this.mouseListener;
        if (l == null) {
            return;
        }
        int id = e.getID();
        if (id == MouseEvent.MOUSE_PRESSED) {
            l.mousePressed(e);
        } else if (id == MouseEvent.MOUSE_RELEASED) {
            l.mouseReleased(e);
        } else if (id == MouseEvent.MOUSE_CLICKED) {
            l.mouseClicked(e);
        } else if (id == MouseEvent.MOUSE_ENTERED) {
            l.mouseEntered(e);
        } else if (id == MouseEvent.MOUSE_EXITED) {
            l.mouseExited(e);
        }
    }

    /** Les avisa a los oyentes de movimiento del ratón. */
    protected void processMouseMotionEvent(MouseEvent e) {
        MouseMotionListener l = this.mouseMotionListener;
        if (l == null) {
            return;
        }
        if (e.getID() == MouseEvent.MOUSE_MOVED) {
            l.mouseMoved(e);
        } else if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
            l.mouseDragged(e);
        }
    }

    /** Les avisa a los oyentes de la rueda. */
    protected void processMouseWheelEvent(MouseWheelEvent e) {
        MouseWheelListener l = this.mouseWheelListener;
        if (l != null && e.getID() == MouseEvent.MOUSE_WHEEL) {
            l.mouseWheelMoved(e);
        }
    }

    /** Les avisa a los oyentes del método de entrada. */
    protected void processInputMethodEvent(InputMethodEvent e) {
        InputMethodListener l = this.inputMethodListener;
        if (l == null) {
            return;
        }
        if (e.getID() == InputMethodEvent.INPUT_METHOD_TEXT_CHANGED) {
            l.inputMethodTextChanged(e);
        } else if (e.getID() == InputMethodEvent.CARET_POSITION_CHANGED) {
            l.caretPositionChanged(e);
        }
    }

    /** Les avisa a los oyentes del árbol. */
    protected void processHierarchyEvent(HierarchyEvent e) {
        HierarchyListener l = this.hierarchyListener;
        if (l != null && e.getID() == HierarchyEvent.HIERARCHY_CHANGED) {
            l.hierarchyChanged(e);
        }
    }

    /** Les avisa a los oyentes de cambios de tamaño de los ancestros. */
    protected void processHierarchyBoundsEvent(HierarchyEvent e) {
        HierarchyBoundsListener l = this.hierarchyBoundsListener;
        if (l == null) {
            return;
        }
        if (e.getID() == HierarchyEvent.ANCESTOR_MOVED) {
            l.ancestorMoved(e);
        } else if (e.getID() == HierarchyEvent.ANCESTOR_RESIZED) {
            l.ancestorResized(e);
        }
    }

    /**
     * Pide recibir esas familias de eventos.
     *
     * <p>Registrar un oyente ya la prende sola; esto sirve para recibir una familia **sin** oyente,
     * que es lo que hace una subclase que atiende los eventos redefiniendo `processXEvent`.
     */
    protected final void enableEvents(long eventsToEnable) {
        this.eventMask = this.eventMask | eventsToEnable;
    }

    /** Deja de recibirlas. */
    protected final void disableEvents(long eventsToDisable) {
        this.eventMask = this.eventMask & ~eventsToDisable;
    }

    /**
     * Junta dos eventos de la misma familia en uno.
     *
     * @return `null` siempre: juntar eventos es una optimización de la cola, y sin cola no hay
     *     ninguno que juntar
     */
    protected AWTEvent coalesceEvents(AWTEvent existingEvent, AWTEvent newEvent) {
        return null;
    }

    /** Suma un oyente de componente; un `null` se ignora. */
    public synchronized void addComponentListener(ComponentListener l) {
        if (l == null) {
            return;
        }
        this.componentListener = AWTEventMulticaster.add(this.componentListener, l);
        this.enableEvents(AWTEvent.COMPONENT_EVENT_MASK);
    }

    /** Saca a ese oyente. */
    public synchronized void removeComponentListener(ComponentListener l) {
        if (l == null) {
            return;
        }
        this.componentListener = AWTEventMulticaster.remove(this.componentListener, l);
    }

    /** Los oyentes de componente. */
    public synchronized ComponentListener[] getComponentListeners() {
        return AWTEventMulticaster.getListeners(this.componentListener, ComponentListener.class);
    }

    /** Suma un oyente de foco; un `null` se ignora. */
    public synchronized void addFocusListener(FocusListener l) {
        if (l == null) {
            return;
        }
        this.focusListener = AWTEventMulticaster.add(this.focusListener, l);
        this.enableEvents(AWTEvent.FOCUS_EVENT_MASK);
    }

    /** Saca a ese oyente. */
    public synchronized void removeFocusListener(FocusListener l) {
        if (l == null) {
            return;
        }
        this.focusListener = AWTEventMulticaster.remove(this.focusListener, l);
    }

    /** Los oyentes de foco. */
    public synchronized FocusListener[] getFocusListeners() {
        return AWTEventMulticaster.getListeners(this.focusListener, FocusListener.class);
    }

    /** Suma un oyente del árbol; un `null` se ignora. */
    public void addHierarchyListener(HierarchyListener l) {
        if (l == null) {
            return;
        }
        synchronized (this) {
            this.hierarchyListener = AWTEventMulticaster.add(this.hierarchyListener, l);
            this.enableEvents(AWTEvent.HIERARCHY_EVENT_MASK);
        }
    }

    /** Saca a ese oyente. */
    public void removeHierarchyListener(HierarchyListener l) {
        if (l == null) {
            return;
        }
        synchronized (this) {
            this.hierarchyListener = AWTEventMulticaster.remove(this.hierarchyListener, l);
        }
    }

    /** Los oyentes del árbol. */
    public synchronized HierarchyListener[] getHierarchyListeners() {
        return AWTEventMulticaster.getListeners(this.hierarchyListener, HierarchyListener.class);
    }

    /** Suma un oyente de cambios de tamaño de ancestros; un `null` se ignora. */
    public void addHierarchyBoundsListener(HierarchyBoundsListener l) {
        if (l == null) {
            return;
        }
        synchronized (this) {
            this.hierarchyBoundsListener =
                    AWTEventMulticaster.add(this.hierarchyBoundsListener, l);
            this.enableEvents(AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK);
        }
    }

    /** Saca a ese oyente. */
    public void removeHierarchyBoundsListener(HierarchyBoundsListener l) {
        if (l == null) {
            return;
        }
        synchronized (this) {
            this.hierarchyBoundsListener =
                    AWTEventMulticaster.remove(this.hierarchyBoundsListener, l);
        }
    }

    /** Los oyentes de cambios de tamaño de ancestros. */
    public synchronized HierarchyBoundsListener[] getHierarchyBoundsListeners() {
        return AWTEventMulticaster.getListeners(this.hierarchyBoundsListener,
                HierarchyBoundsListener.class);
    }

    /** Suma un oyente de teclado; un `null` se ignora. */
    public synchronized void addKeyListener(KeyListener l) {
        if (l == null) {
            return;
        }
        this.keyListener = AWTEventMulticaster.add(this.keyListener, l);
        this.enableEvents(AWTEvent.KEY_EVENT_MASK);
    }

    /** Saca a ese oyente. */
    public synchronized void removeKeyListener(KeyListener l) {
        if (l == null) {
            return;
        }
        this.keyListener = AWTEventMulticaster.remove(this.keyListener, l);
    }

    /** Los oyentes de teclado. */
    public synchronized KeyListener[] getKeyListeners() {
        return AWTEventMulticaster.getListeners(this.keyListener, KeyListener.class);
    }

    /** Suma un oyente de botones del ratón; un `null` se ignora. */
    public synchronized void addMouseListener(MouseListener l) {
        if (l == null) {
            return;
        }
        this.mouseListener = AWTEventMulticaster.add(this.mouseListener, l);
        this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
    }

    /** Saca a ese oyente. */
    public synchronized void removeMouseListener(MouseListener l) {
        if (l == null) {
            return;
        }
        this.mouseListener = AWTEventMulticaster.remove(this.mouseListener, l);
    }

    /** Los oyentes de botones del ratón. */
    public synchronized MouseListener[] getMouseListeners() {
        return AWTEventMulticaster.getListeners(this.mouseListener, MouseListener.class);
    }

    /** Suma un oyente de movimiento del ratón; un `null` se ignora. */
    public synchronized void addMouseMotionListener(MouseMotionListener l) {
        if (l == null) {
            return;
        }
        this.mouseMotionListener = AWTEventMulticaster.add(this.mouseMotionListener, l);
        this.enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    /** Saca a ese oyente. */
    public synchronized void removeMouseMotionListener(MouseMotionListener l) {
        if (l == null) {
            return;
        }
        this.mouseMotionListener = AWTEventMulticaster.remove(this.mouseMotionListener, l);
    }

    /** Los oyentes de movimiento del ratón. */
    public synchronized MouseMotionListener[] getMouseMotionListeners() {
        return AWTEventMulticaster.getListeners(this.mouseMotionListener,
                MouseMotionListener.class);
    }

    /** Suma un oyente de la rueda; un `null` se ignora. */
    public synchronized void addMouseWheelListener(MouseWheelListener l) {
        if (l == null) {
            return;
        }
        this.mouseWheelListener = AWTEventMulticaster.add(this.mouseWheelListener, l);
        this.enableEvents(AWTEvent.MOUSE_WHEEL_EVENT_MASK);
    }

    /** Saca a ese oyente. */
    public synchronized void removeMouseWheelListener(MouseWheelListener l) {
        if (l == null) {
            return;
        }
        this.mouseWheelListener = AWTEventMulticaster.remove(this.mouseWheelListener, l);
    }

    /** Los oyentes de la rueda. */
    public synchronized MouseWheelListener[] getMouseWheelListeners() {
        return AWTEventMulticaster.getListeners(this.mouseWheelListener,
                MouseWheelListener.class);
    }

    /** Suma un oyente del método de entrada; un `null` se ignora. */
    public synchronized void addInputMethodListener(InputMethodListener l) {
        if (l == null) {
            return;
        }
        this.inputMethodListener = AWTEventMulticaster.add(this.inputMethodListener, l);
        this.enableEvents(AWTEvent.INPUT_METHOD_EVENT_MASK);
    }

    /** Saca a ese oyente. */
    public synchronized void removeInputMethodListener(InputMethodListener l) {
        if (l == null) {
            return;
        }
        this.inputMethodListener = AWTEventMulticaster.remove(this.inputMethodListener, l);
    }

    /** Los oyentes del método de entrada. */
    public synchronized InputMethodListener[] getInputMethodListeners() {
        return AWTEventMulticaster.getListeners(this.inputMethodListener,
                InputMethodListener.class);
    }

    /**
     * Los oyentes de esa clase.
     *
     * @throws ClassCastException si la clase no es de oyente
     * @throws NullPointerException si la clase es `null`
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        EventListener l = null;
        if (listenerType == ComponentListener.class) {
            l = this.componentListener;
        } else if (listenerType == FocusListener.class) {
            l = this.focusListener;
        } else if (listenerType == HierarchyListener.class) {
            l = this.hierarchyListener;
        } else if (listenerType == HierarchyBoundsListener.class) {
            l = this.hierarchyBoundsListener;
        } else if (listenerType == KeyListener.class) {
            l = this.keyListener;
        } else if (listenerType == MouseListener.class) {
            l = this.mouseListener;
        } else if (listenerType == MouseMotionListener.class) {
            l = this.mouseMotionListener;
        } else if (listenerType == MouseWheelListener.class) {
            l = this.mouseWheelListener;
        } else if (listenerType == InputMethodListener.class) {
            l = this.inputMethodListener;
        }
        return AWTEventMulticaster.getListeners(l, listenerType);
    }

    /**
     * El estado de escritura de la ventana que lo contiene.
     *
     * @return `null` si no cuelga de ninguna ventana
     */
    public InputContext getInputContext() {
        Container p = this.parent;
        if (p == null) {
            return null;
        }
        return p.getInputContext();
    }

    /**
     * Lo que el método de entrada necesita preguntarle a este componente.
     *
     * @return `null`: un componente genérico no muestra texto en composición
     */
    public InputMethodRequests getInputMethodRequests() {
        return null;
    }

    /** El destino de arrastre enganchado, o `null`. */
    public synchronized java.awt.dnd.DropTarget getDropTarget() {
        return this.dropTarget;
    }

    /**
     * Le engancha un destino de arrastre.
     *
     * <p>Desengancha el anterior y le avisa al nuevo cuál es su componente: los dos lados de la
     * relación se mantienen consistentes desde acá.
     */
    public synchronized void setDropTarget(java.awt.dnd.DropTarget dt) {
        if (dt == this.dropTarget) {
            return;
        }
        java.awt.dnd.DropTarget anterior = this.dropTarget;
        this.dropTarget = dt;
        if (anterior != null && anterior.getComponent() == this) {
            anterior.setComponent(null);
        }
        if (dt != null && dt.getComponent() != this) {
            dt.setComponent(this);
        }
    }

    /**
     * Le engancha un menú emergente.
     *
     * @throws NullPointerException si el menú es `null`
     */
    public void add(PopupMenu popup) {
        synchronized (this.getTreeLock()) {
            if (popup.getParent() != null) {
                ((MenuContainer) popup.getParent()).remove(popup);
            }
            this.popups.add(popup);
            popup.setParent(this);
        }
    }

    /** Le saca un menú emergente. */
    public void remove(MenuComponent popup) {
        synchronized (this.getTreeLock()) {
            if (this.popups.remove(popup)) {
                popup.setParent(null);
            }
        }
    }

    /** Avisa que puede mostrarse. */
    public void addNotify() {
    }

    /** Avisa que dejó de poder mostrarse. */
    public void removeNotify() {
    }

    /** Si puede recibir el foco. */
    public boolean isFocusable() {
        return this.focusable;
    }

    /** Declara si puede recibir el foco. */
    public void setFocusable(boolean focusable) {
        boolean viejo;
        synchronized (this) {
            viejo = this.focusable;
            this.focusable = focusable;
        }
        this.firePropertyChange("focusable", viejo, focusable);
    }

    /**
     * Si puede recibir el foco.
     *
     * @deprecated es del modelo de 1.1. Usar {@link #isFocusable}.
     */
    @Deprecated
    public boolean isFocusTraversable() {
        return this.focusable;
    }

    /**
     * Las teclas que recorren el foco en ese sentido.
     *
     * <p>Si no se le fijaron, se heredan del padre; si no hay padre, son las de fábrica.
     *
     * @throws IllegalArgumentException si el sentido no es uno de los cuatro
     */
    public Set<AWTKeyStroke> getFocusTraversalKeys(int id) {
        this.comprobarSentido(id);
        Set<AWTKeyStroke> s = this.focusTraversalKeys[id];
        if (s != null) {
            return s;
        }
        Container p = this.parent;
        if (p != null) {
            return p.getFocusTraversalKeys(id);
        }
        return tecladoPorOmision(id);
    }

    /**
     * Cambia las teclas de recorrido en ese sentido.
     *
     * <p>Con `null` se vuelve a heredar del padre.
     *
     * @throws IllegalArgumentException si el sentido no es uno de los cuatro, si el conjunto trae un
     *     `null`, o si trae un atajo de tecla soltada
     */
    public void setFocusTraversalKeys(int id, Set<? extends AWTKeyStroke> keystrokes) {
        this.comprobarSentido(id);
        Set<AWTKeyStroke> nuevo = null;
        if (keystrokes != null) {
            Set<AWTKeyStroke> copia = new HashSet<AWTKeyStroke>();
            java.util.Iterator<? extends AWTKeyStroke> it = keystrokes.iterator();
            while (it.hasNext()) {
                AWTKeyStroke k = it.next();
                if (k == null) {
                    throw new IllegalArgumentException(
                            "cannot set null focus traversal key");
                }
                // Un atajo al soltar no sirve para recorrer: para cuando la tecla se suelta, el foco
                // ya se movio con el apretón y el recorrido saltaría dos veces.
                if (k.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
                    throw new IllegalArgumentException(
                            "focus traversal keys cannot map to KEY_TYPED events");
                }
                copia.add(k);
            }
            nuevo = Collections.unmodifiableSet(copia);
        }
        Set<AWTKeyStroke> viejo = this.focusTraversalKeys[id];
        this.focusTraversalKeys[id] = nuevo;
        this.firePropertyChange(nombreDeSentido(id), viejo, nuevo);
    }

    /** Si a este componente se le fijaron teclas propias en ese sentido. */
    public boolean areFocusTraversalKeysSet(int id) {
        this.comprobarSentido(id);
        return this.focusTraversalKeys[id] != null;
    }

    /**
     * Comprueba que el sentido sea uno de los cuatro.
     *
     * @throws IllegalArgumentException si no lo es
     */
    private void comprobarSentido(int id) {
        if (id < 0 || id > 3) {
            throw new IllegalArgumentException("invalid focus traversal key identifier");
        }
    }

    /** El nombre de propiedad que corresponde a ese sentido. */
    private static String nombreDeSentido(int id) {
        if (id == 0) {
            return "forwardFocusTraversalKeys";
        }
        if (id == 1) {
            return "backwardFocusTraversalKeys";
        }
        if (id == 2) {
            return "upCycleFocusTraversalKeys";
        }
        return "downCycleFocusTraversalKeys";
    }

    /** Si el componente atiende las teclas de recorrido en vez de recibirlas como teclado. */
    public boolean getFocusTraversalKeysEnabled() {
        return this.focusTraversalKeysEnabled;
    }

    /**
     * Declara si las atiende.
     *
     * <p>Apagarlo es lo que hace que un editor de texto pueda recibir el tabulador como carácter en
     * vez de perder el foco.
     */
    public void setFocusTraversalKeysEnabled(boolean focusTraversalKeysEnabled) {
        boolean viejo;
        synchronized (this) {
            viejo = this.focusTraversalKeysEnabled;
            this.focusTraversalKeysEnabled = focusTraversalKeysEnabled;
        }
        this.firePropertyChange("focusTraversalKeysEnabled", viejo, focusTraversalKeysEnabled);
    }

    /**
     * Le pide el foco.
     *
     * <p>No hace nada: mover el foco lo decide el gestor de foco a partir de lo que el sistema de
     * ventanas informe, y no hay ninguno de los dos. El método no devuelve nada, así que no afirma
     * haberlo conseguido.
     */
    public void requestFocus() {
    }

    /** Como el anterior, declarando por qué se pide. */
    public void requestFocus(FocusEvent.Cause cause) {
    }

    /**
     * Le pide el foco, diciendo si el cambio es temporal.
     *
     * @return `false` siempre: el foco no se pudo mover porque no hay gestor de foco
     */
    protected boolean requestFocus(boolean temporary) {
        return false;
    }

    /**
     * Como el anterior, declarando por qué.
     *
     * @return `false` siempre
     */
    protected boolean requestFocus(boolean temporary, FocusEvent.Cause cause) {
        return false;
    }

    /**
     * Le pide el foco sólo si su ventana ya lo tiene.
     *
     * @return `false` siempre
     */
    public boolean requestFocusInWindow() {
        return false;
    }

    /**
     * Como el anterior, declarando por qué.
     *
     * @return `false` siempre
     */
    public boolean requestFocusInWindow(FocusEvent.Cause cause) {
        return false;
    }

    /**
     * Como el anterior, diciendo si el cambio es temporal.
     *
     * @return `false` siempre
     */
    protected boolean requestFocusInWindow(boolean temporary) {
        return false;
    }

    /** Si tiene el foco del teclado. */
    public boolean hasFocus() {
        return false;
    }

    /** Si es el componente con el foco. */
    public boolean isFocusOwner() {
        return this.hasFocus();
    }

    /** Le pasa el foco al siguiente del recorrido; no hace nada sin gestor de foco. */
    public void transferFocus() {
    }

    /** Le pasa el foco al anterior. */
    public void transferFocusBackward() {
    }

    /** Sube un nivel de ciclo de foco. */
    public void transferFocusUpCycle() {
    }

    /**
     * Le pasa el foco al siguiente.
     *
     * @deprecated es del modelo de 1.1. Usar {@link #transferFocus}.
     */
    @Deprecated
    public void nextFocus() {
        this.transferFocus();
    }

    /** La raíz del ciclo de foco al que pertenece, o `null`. */
    public Container getFocusCycleRootAncestor() {
        Container p = this.parent;
        while (p != null) {
            if (p.isFocusCycleRoot()) {
                return p;
            }
            p = p.getParent();
        }
        return null;
    }

    /** Si ese contenedor es la raíz del ciclo de foco de este componente. */
    public boolean isFocusCycleRoot(Container container) {
        return this.getFocusCycleRootAncestor() == container;
    }

    /** La configuración gráfica; `null` sin ventana. */
    public GraphicsConfiguration getGraphicsConfiguration() {
        Container p = this.parent;
        if (p != null) {
            return p.getGraphicsConfiguration();
        }
        return null;
    }

    /**
     * Una imagen para dibujar fuera de pantalla.
     *
     * @return `null` siempre: sin ventana no hay formato de destino al que ajustarla. Para dibujar
     *     sobre píxeles está {@link java.awt.image.BufferedImage}.
     */
    public Image createImage(int width, int height) {
        return null;
    }

    /**
     * Una imagen a partir de un productor de píxeles.
     *
     * <p>Esta sí funciona: no necesita ventana, sólo el productor.
     *
     * @throws NullPointerException si el productor es `null`
     */
    public Image createImage(ImageProducer producer) {
        return this.getToolkit().createImage(producer);
    }

    /**
     * Una imagen volátil.
     *
     * @return `null` siempre: una imagen volátil vive en la memoria del dispositivo de video
     */
    public VolatileImage createVolatileImage(int width, int height) {
        return null;
    }

    /**
     * Una imagen volátil con las capacidades pedidas.
     *
     * @return `null` siempre, por el mismo motivo
     * @throws AWTException si las capacidades no se pueden cumplir
     */
    public VolatileImage createVolatileImage(int width, int height, ImageCapabilities caps)
            throws AWTException {
        return null;
    }

    /**
     * Empieza a cargar una imagen a ese tamaño.
     *
     * @return si ya está lista
     */
    public boolean prepareImage(Image image, ImageObserver observer) {
        return this.prepareImage(image, -1, -1, observer);
    }

    /**
     * Como el anterior, a un tamaño concreto.
     *
     * @return si ya está lista
     */
    public boolean prepareImage(Image image, int width, int height, ImageObserver observer) {
        return this.getToolkit().prepareImage(image, width, height, observer);
    }

    /** Cuánto se cargó de una imagen, como banderas de {@link ImageObserver}. */
    public int checkImage(Image image, ImageObserver observer) {
        return this.checkImage(image, -1, -1, observer);
    }

    /** Como el anterior, a un tamaño concreto. */
    public int checkImage(Image image, int width, int height, ImageObserver observer) {
        return this.getToolkit().checkImage(image, width, height, observer);
    }

    /**
     * Le avisan que una imagen avanzó.
     *
     * <p>Repinta cuando llegan píxeles nuevos y deja de escuchar cuando la imagen está completa o
     * falló — que es lo que el valor de retorno significa.
     */
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) {
        if ((infoflags & (ALLBITS | FRAMEBITS)) != 0) {
            this.repaint();
            return false;
        }
        if ((infoflags & (ERROR | ABORT)) != 0) {
            return false;
        }
        if ((infoflags & SOMEBITS) != 0) {
            this.repaint();
        }
        return true;
    }

    /** De qué lado se lee el contenido. */
    public ComponentOrientation getComponentOrientation() {
        return this.componentOrientation;
    }

    /**
     * Declara de qué lado se lee.
     *
     * @throws NullPointerException si la orientación es `null`
     */
    public void setComponentOrientation(ComponentOrientation o) {
        ComponentOrientation viejo = this.componentOrientation;
        this.componentOrientation = o;
        this.firePropertyChange("componentOrientation", viejo, o);
        this.invalidate();
    }

    /**
     * Le pone esa orientación a él y a todos sus descendientes.
     *
     * @throws NullPointerException si la orientación es `null`
     */
    public void applyComponentOrientation(ComponentOrientation orientation) {
        if (orientation == null) {
            throw new NullPointerException();
        }
        this.setComponentOrientation(orientation);
    }

    /** Le da forma al recorte que usa la mezcla de componentes pesados y livianos. */
    public void setMixingCutoutShape(Shape shape) {
    }

    /** Suma alguien a quien avisarle de los cambios de propiedad. */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (this.getObjectLock()) {
            if (this.changeSupport == null) {
                this.changeSupport = new PropertyChangeSupport(this);
            }
            this.changeSupport.addPropertyChangeListener(listener);
        }
    }

    /** Saca a ese oyente. */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (listener == null || this.changeSupport == null) {
            return;
        }
        synchronized (this.getObjectLock()) {
            this.changeSupport.removePropertyChangeListener(listener);
        }
    }

    /** Los oyentes de cambios de propiedad. */
    public PropertyChangeListener[] getPropertyChangeListeners() {
        synchronized (this.getObjectLock()) {
            if (this.changeSupport == null) {
                return new PropertyChangeListener[0];
            }
            return this.changeSupport.getPropertyChangeListeners();
        }
    }

    /** Suma un oyente para una propiedad concreta. */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (this.getObjectLock()) {
            if (this.changeSupport == null) {
                this.changeSupport = new PropertyChangeSupport(this);
            }
            this.changeSupport.addPropertyChangeListener(propertyName, listener);
        }
    }

    /** Saca a ese oyente de esa propiedad. */
    public void removePropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        if (listener == null || this.changeSupport == null) {
            return;
        }
        synchronized (this.getObjectLock()) {
            this.changeSupport.removePropertyChangeListener(propertyName, listener);
        }
    }

    /** Los oyentes de esa propiedad. */
    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        synchronized (this.getObjectLock()) {
            if (this.changeSupport == null) {
                return new PropertyChangeListener[0];
            }
            return this.changeSupport.getPropertyChangeListeners(propertyName);
        }
    }

    /** Avisa que cambió una propiedad. */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        PropertyChangeSupport s = this.changeSupport;
        if (s != null) {
            s.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /** Avisa que cambió una propiedad booleana. */
    protected void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        PropertyChangeSupport s = this.changeSupport;
        if (s != null) {
            s.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /** Avisa que cambió una propiedad entera. */
    protected void firePropertyChange(String propertyName, int oldValue, int newValue) {
        PropertyChangeSupport s = this.changeSupport;
        if (s != null) {
            s.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /** Avisa que cambió una propiedad de tipo `byte`. */
    public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {
        this.firePropertyChange(propertyName, Byte.valueOf(oldValue), Byte.valueOf(newValue));
    }

    /** Avisa que cambió una propiedad de tipo `char`. */
    public void firePropertyChange(String propertyName, char oldValue, char newValue) {
        this.firePropertyChange(propertyName, Character.valueOf(oldValue),
                Character.valueOf(newValue));
    }

    /** Avisa que cambió una propiedad de tipo `short`. */
    public void firePropertyChange(String propertyName, short oldValue, short newValue) {
        this.firePropertyChange(propertyName, Short.valueOf(oldValue), Short.valueOf(newValue));
    }

    /** Avisa que cambió una propiedad de tipo `long`. */
    public void firePropertyChange(String propertyName, long oldValue, long newValue) {
        this.firePropertyChange(propertyName, Long.valueOf(oldValue), Long.valueOf(newValue));
    }

    /** Avisa que cambió una propiedad de tipo `float`. */
    public void firePropertyChange(String propertyName, float oldValue, float newValue) {
        this.firePropertyChange(propertyName, Float.valueOf(oldValue), Float.valueOf(newValue));
    }

    /** Avisa que cambió una propiedad de tipo `double`. */
    public void firePropertyChange(String propertyName, double oldValue, double newValue) {
        this.firePropertyChange(propertyName, Double.valueOf(oldValue), Double.valueOf(newValue));
    }

    /**
     * Atiende un evento del modelo viejo.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #processEvent}.
     */
    @Deprecated
    public boolean handleEvent(Event evt) {
        return false;
    }

    /**
     * Se apretó el ratón.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #processMouseEvent}.
     */
    @Deprecated
    public boolean mouseDown(Event evt, int x, int y) {
        return false;
    }

    /**
     * Se arrastró el ratón.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #processMouseMotionEvent}.
     */
    @Deprecated
    public boolean mouseDrag(Event evt, int x, int y) {
        return false;
    }

    /**
     * Se soltó el ratón.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #processMouseEvent}.
     */
    @Deprecated
    public boolean mouseUp(Event evt, int x, int y) {
        return false;
    }

    /**
     * Se movió el ratón.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #processMouseMotionEvent}.
     */
    @Deprecated
    public boolean mouseMove(Event evt, int x, int y) {
        return false;
    }

    /**
     * El ratón entró.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #processMouseEvent}.
     */
    @Deprecated
    public boolean mouseEnter(Event evt, int x, int y) {
        return false;
    }

    /**
     * El ratón salió.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #processMouseEvent}.
     */
    @Deprecated
    public boolean mouseExit(Event evt, int x, int y) {
        return false;
    }

    /**
     * Se apretó una tecla.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #processKeyEvent}.
     */
    @Deprecated
    public boolean keyDown(Event evt, int key) {
        return false;
    }

    /**
     * Se soltó una tecla.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #processKeyEvent}.
     */
    @Deprecated
    public boolean keyUp(Event evt, int key) {
        return false;
    }

    /**
     * Se ejecutó una acción.
     *
     * @deprecated es del modelo de 1.0. Usar un {@code ActionListener}.
     */
    @Deprecated
    public boolean action(Event evt, Object what) {
        return false;
    }

    /**
     * Ganó el foco.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #processFocusEvent}.
     */
    @Deprecated
    public boolean gotFocus(Event evt, Object what) {
        return false;
    }

    /**
     * Perdió el foco.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #processFocusEvent}.
     */
    @Deprecated
    public boolean lostFocus(Event evt, Object what) {
        return false;
    }

    /** Escribe el árbol de componentes en la salida estándar. */
    public void list() {
        this.list(System.out, 0);
    }

    /** Lo escribe en ese flujo. */
    public void list(PrintStream out) {
        this.list(out, 0);
    }

    /** Lo escribe con esa sangría. */
    public void list(PrintStream out, int indent) {
        for (int i = 0; i < indent; i++) {
            out.print(" ");
        }
        out.println(this.toString());
    }

    /** Lo escribe en ese escritor. */
    public void list(PrintWriter out) {
        this.list(out, 0);
    }

    /** Lo escribe con esa sangría. */
    public void list(PrintWriter out, int indent) {
        for (int i = 0; i < indent; i++) {
            out.print(" ");
        }
        out.println(this.toString());
    }

    /** La descripción del componente, sin el nombre de la clase. */
    protected String paramString() {
        String s = this.getName() + "," + this.x + "," + this.y + "," + this.width + "x"
                + this.height;
        if (!this.valid) {
            s = s + ",invalid";
        }
        if (!this.visible) {
            s = s + ",hidden";
        }
        if (!this.enabled) {
            s = s + ",disabled";
        }
        return s;
    }

    public String toString() {
        return this.getClass().getName() + "[" + this.paramString() + "]";
    }

    /**
     * La información de accesibilidad de este componente.
     *
     * <p>Devuelve `null` mientras ninguna subclase concreta haya armado la suya, y **no la arma
     * sola**: un componente genérico no sabe qué rol tiene, y armar un contexto que conteste
     * `AWT_COMPONENT` a todo sería peor que no contestar. Las subclases concretas lo redefinen.
     */
    public AccessibleContext getAccessibleContext() {
        return this.accessibleContext;
    }

    /**
     * La accesibilidad de un componente.
     *
     * <p>Informa lo que se puede saber sin pantalla: el nombre, el rol, si está habilitado, si está
     * declarado visible y si puede recibir el foco. Lo que depende de estar en pantalla —{@code
     * SHOWING}, {@code FOCUSED}— no se informa, porque no es cierto.
     */
    protected abstract class AccessibleAWTComponent extends AccessibleContext {

        /** Para las subclases. */
        protected AccessibleAWTComponent() {
        }

        /** Desconocido; las subclases concretas lo afinan. */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.AWT_COMPONENT;
        }

        /** Los estados que se pueden saber sin pantalla. */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet s = new AccessibleStateSet();
            if (Component.this.isEnabled()) {
                s.add(AccessibleState.ENABLED);
            }
            if (Component.this.isFocusable()) {
                s.add(AccessibleState.FOCUSABLE);
            }
            if (Component.this.isVisible()) {
                s.add(AccessibleState.VISIBLE);
            }
            if (Component.this.isShowing()) {
                s.add(AccessibleState.SHOWING);
            }
            return s;
        }

        /** El nombre del componente. */
        public String getAccessibleName() {
            return Component.this.getName();
        }

        /** Cero: un componente sin hijos. */
        public int getAccessibleChildrenCount() {
            return 0;
        }

        /** Siempre `null`. */
        public javax.accessibility.Accessible getAccessibleChild(int i) {
            return null;
        }

        /** Su posición dentro del padre, o -1 si no tiene. */
        public int getAccessibleIndexInParent() {
            Container p = Component.this.getParent();
            if (p == null) {
                return -1;
            }
            Component[] hijos = p.getComponents();
            for (int i = 0; i < hijos.length; i++) {
                if (hijos[i] == Component.this) {
                    return i;
                }
            }
            return -1;
        }

        /** El idioma del componente. */
        public Locale getLocale() {
            return Component.this.getLocale();
        }
    }
}
