package javax.swing;

import java.applet.Applet;
import java.awt.Component;
import java.awt.Component$BaselineResizeBehavior;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.io.Serializable;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.Locale;

import javax.swing.border.Border;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.ComponentUI;

/**
 * La raiz de todo componente de Swing: lo que un boton, una tabla o un panel tienen en comun.
 *
 * <h2>Lo que esta clase agrega sobre {@link Container}</h2>
 *
 * <p>AWT ya sabe tener hijos, un tamano, una fuente y pintar. Swing agrega tres cosas encima, y las
 * tres estan aca de verdad:
 *
 * <ul>
 * <li><strong>La tuberia de pintado en tres pasos</strong> — {@link #paintComponent},
 *     {@link #paintBorder} y {@link #paintChildren}, en ese orden y cada uno redefinible por
 *     separado. Es lo que permite que una subclase dibuje su contenido sin saber nada de bordes ni
 *     de hijos.</li>
 * <li><strong>El borde como objeto</strong> — {@link #setBorder}. Los {@link #getInsets} salen de
 *     el, asi que un layout respeta el borde sin que nadie se lo cuente.</li>
 * <li><strong>El aspecto separado</strong> — {@link #setUI}: si hay un {@link ComponentUI}, el
 *     dibujado y las medidas se le delegan. Sin el, el componente se las arregla solo, y ese "solo"
 *     es honesto: rellena el fondo si es opaco, y mide con lo que AWT ya sabe.</li>
 * </ul>
 *
 * <h2>Lo que <em>no</em> esta, y por que</h2>
 *
 * <p>Sesenta de los ciento cuarenta y cinco miembros del JDK quedan afuera, y cada grupo por una
 * razon concreta, no por pereza:
 *
 * <ul>
 * <li>Las <strong>acciones por teclado</strong> —{@code registerKeyboardAction}, {@code InputMap},
 *     {@code ActionMap}— necesitan {@code KeyStroke} y {@code Action}, que son un subsistema propio
 *     y no estan.</li>
 * <li>El <strong>foco</strong> mas alla de lo que AWT ya da: {@code InputVerifier},
 *     {@code getNextFocusableComponent}, necesitan un {@code KeyboardFocusManager} que decida.</li>
 * <li><strong>Menus emergentes</strong>, <strong>transferencia</strong>, <strong>tooltips
 *     visibles</strong> y {@code getRootPane} nombran clases que no existen todavia
 *     ({@code JPopupMenu}, {@code TransferHandler}, {@code JToolTip}, {@code JRootPane}).</li>
 * <li>{@code paintImmediately} y el repintado diferido pasan por un {@code RepaintManager} que
 *     dibuja en una <em>pantalla</em>. Esta VM no tiene una: un componente se pinta cuando alguien
 *     le pasa un {@link Graphics}, tipicamente el de una {@code BufferedImage}, y eso es lo que
 *     {@link #paint} hace bien. {@link #repaint} y {@link #revalidate} siguen existiendo con el
 *     significado que pueden tener sin pantalla, y lo dicen.</li>
 * </ul>
 *
 * <p>El criterio es el de siempre: un miembro que falta es un subconjunto legal; uno que fingiera
 * tener un {@code KeyboardFocusManager} detras compila y revienta despues.
 *
 * <h2>Nota sobre el JDK: quien rellena el fondo</h2>
 *
 * <p>En el JDK, {@link #paintComponent} no pinta nada por si mismo: le pide al UI que lo haga, y es
 * el UI basico de cada componente el que rellena el fondo cuando el componente es opaco. Aca, sin
 * un aspecto instalado, esa responsabilidad se quedaria sin dueno y un panel opaco no tendria fondo.
 * Por eso {@code paintComponent} rellena el fondo <strong>cuando no hay UI</strong> y delega cuando
 * lo hay: el comportamiento observable es el mismo, y el reparto de responsabilidades se restituye
 * en cuanto alguien instala un UI.
 */
public abstract class JComponent extends Container implements Serializable {

    private static final long serialVersionUID = -5876370834061273469L;

    /** Condicion de una accion de teclado: solo con el foco. Definida aca por compatibilidad. */
    public static final int WHEN_FOCUSED = 0;

    /** Condicion: cuando el foco esta en un descendiente. */
    public static final int WHEN_ANCESTOR_OF_FOCUSED_COMPONENT = 1;

    /** Condicion: cuando la ventana tiene el foco. */
    public static final int WHEN_IN_FOCUSED_WINDOW = 2;

    /** Ninguna condicion registrada. */
    public static final int UNDEFINED_CONDITION = -1;

    /** La clave de propiedad de cliente bajo la que vive el texto de ayuda. */
    public static final String TOOL_TIP_TEXT_KEY = "ToolTipText";

    /** El aspecto instalado, o {@code null} si el componente se dibuja y se mide solo. */
    protected transient ComponentUI ui;

    /** Los oyentes propios de Swing; los de AWT viven en {@link Component}. */
    protected EventListenerList listenerList = new EventListenerList();

    private static Locale defaultLocale;

    private Border border;
    private boolean opaque;
    private float alignmentX = -1.0f;
    private float alignmentY = -1.0f;
    private Hashtable<Object, Object> clientProperties;
    private boolean doubleBuffered;
    private boolean autoscrolls;
    private boolean opaquePuesto;
    private boolean autoscrollsPuesto;
    private int debugGraphicsOptions;
    private boolean requestFocusEnabled = true;
    private boolean verifyInputWhenFocusTarget = true;
    private boolean paintingForPrint;
    private VetoableChangeSupport vetoableChangeSupport;

    /** Un componente vacio, no opaco, sin borde ni aspecto. */
    public JComponent() {
        super();
    }

    // -- el aspecto -----------------------------------------------------------------------------

    /**
     * Instala un aspecto, desinstalando el anterior.
     *
     * <p>Protegido porque cada subclase expone el suyo con el tipo preciso —{@code setUI(ButtonUI)}—
     * y este es el mecanismo comun debajo. Termina con {@link #revalidate} y {@link #repaint}: un
     * aspecto nuevo puede medir distinto.
     */
    protected void setUI(ComponentUI newUI) {
        if (this.ui != null) {
            this.ui.uninstallUI(this);
        }
        ComponentUI viejo = this.ui;
        this.ui = newUI;
        if (this.ui != null) {
            this.ui.installUI(this);
        }
        firePropertyChange("UI", viejo, newUI);
        revalidate();
        repaint();
    }

    /** El aspecto instalado, o {@code null}. */
    public ComponentUI getUI() {
        return this.ui;
    }

    /**
     * Vuelve a pedirle el aspecto al {@code UIManager}.
     *
     * <p>No hace nada, y no es un marcador: en el JDK este metodo tambien esta vacio en
     * {@code JComponent}, porque cada subclase sabe que clave pedir. Lo que falta es el
     * {@code UIManager} al que pedirsela, y eso lo dice cada subclase en el suyo.
     */
    public void updateUI() {
    }

    /** La clave con la que el {@code UIManager} buscaria el aspecto de esta clase. */
    public String getUIClassID() {
        return "ComponentUI";
    }

    // -- la tuberia de pintado --------------------------------------------------------------------

    /**
     * Pinta el componente entero: contenido, borde e hijos, en ese orden.
     *
     * <p>El orden es el contrato: el borde va encima del contenido —para que un contenido que se
     * pase de la raya quede debajo del marco— y los hijos van encima de todo. Las subclases no
     * redefinen esto sino {@link #paintComponent}.
     */
    public void paint(Graphics g) {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        Graphics cg = getComponentGraphics(g);
        paintComponent(cg);
        paintBorder(cg);
        paintChildren(cg);
    }

    /**
     * Pinta el contenido propio, sin borde ni hijos.
     *
     * <p>Con aspecto, se lo delega sobre una <em>copia</em> del contexto: el UI puede cambiar color,
     * fuente o recorte y nada de eso tiene que llegar al borde ni a los hijos. Sin aspecto, rellena
     * el fondo si el componente es opaco — ver la nota de la clase sobre por que ese fondo se pinta
     * aca y no en el UI que no hay.
     */
    protected void paintComponent(Graphics g) {
        if (this.ui != null) {
            Graphics copia = g.create();
            try {
                this.ui.update(copia, this);
            } finally {
                copia.dispose();
            }
            return;
        }
        if (isOpaque()) {
            java.awt.Color viejo = g.getColor();
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(viejo);
        }
    }

    /** Pinta el borde, si hay, sobre el contenido. */
    protected void paintBorder(Graphics g) {
        Border b = getBorder();
        if (b != null) {
            b.paintBorder(this, g, 0, 0, getWidth(), getHeight());
        }
    }

    /**
     * Pinta los hijos, del ultimo agregado al primero.
     *
     * <p>Al reves del orden de agregado, porque el primero que se agrega es el que queda arriba: es
     * la regla del z-order de AWT. Cada hijo recibe un contexto trasladado a su esquina y recortado
     * a su tamano, asi que no puede dibujar afuera de si mismo.
     */
    protected void paintChildren(Graphics g) {
        synchronized (getTreeLock()) {
            for (int i = getComponentCount() - 1; i >= 0; i--) {
                Component hijo = getComponent(i);
                if (!hijo.isVisible()) {
                    continue;
                }
                Graphics cg = g.create(hijo.getX(), hijo.getY(), hijo.getWidth(),
                        hijo.getHeight());
                if (cg == null) {
                    continue;
                }
                try {
                    hijo.paint(cg);
                } finally {
                    cg.dispose();
                }
            }
        }
    }

    /**
     * Pinta sin borrar el fondo primero.
     *
     * <p>AWT borra en {@code update} y despues pinta; Swing no borra nunca aca, porque el fondo lo
     * decide {@link #paintComponent} segun {@link #isOpaque}. Es lo que evita el parpadeo de AWT.
     */
    public void update(Graphics g) {
        paint(g);
    }

    /**
     * El contexto con el que se pinta este componente: el dado, con su color y su fuente puestos.
     *
     * <p>Sin esto, un componente pintaria con el color y la fuente que le dejo el anterior.
     */
    protected Graphics getComponentGraphics(Graphics g) {
        g.setColor(getForeground());
        g.setFont(getFont());
        return g;
    }

    /** Imprime: la misma tuberia que {@link #paint}, con {@link #isPaintingForPrint} en alto. */
    public void print(Graphics g) {
        this.paintingForPrint = true;
        try {
            Graphics cg = getComponentGraphics(g);
            printComponent(cg);
            printBorder(cg);
            printChildren(cg);
        } finally {
            this.paintingForPrint = false;
        }
    }

    /** Imprime el componente y sus hijos. */
    public void printAll(Graphics g) {
        print(g);
    }

    /** Imprime el contenido; por omision, lo pinta. */
    protected void printComponent(Graphics g) {
        paintComponent(g);
    }

    /** Imprime el borde; por omision, lo pinta. */
    protected void printBorder(Graphics g) {
        paintBorder(g);
    }

    /** Imprime los hijos; por omision, los pinta. */
    protected void printChildren(Graphics g) {
        paintChildren(g);
    }

    /** Si el pintado en curso es una impresion; ver {@link #print}. */
    public boolean isPaintingForPrint() {
        return this.paintingForPrint;
    }

    /**
     * Si el pintado en curso es un mosaico de un pintado mayor.
     *
     * <p>Siempre {@code false}: el mosaico lo arma el {@code RepaintManager} cuando dibuja a una
     * pantalla por partes, y aca el pintado es de una sola vez sobre la imagen que le den.
     */
    public boolean isPaintingTile() {
        return false;
    }

    /** Si este componente es un origen de pintado propio. {@code false}, salvo que una subclase diga. */
    protected boolean isPaintingOrigin() {
        return false;
    }

    /**
     * Si los hijos no se solapan, y por lo tanto se pueden pintar sin recortarse entre si.
     *
     * <p>{@code true} por omision, como en el JDK: un contenedor con hijos que se pisan lo redefine.
     */
    public boolean isOptimizedDrawingEnabled() {
        return true;
    }

    /** Si una validacion puede detenerse aca. {@code false}: solo las raices lo dicen. */
    public boolean isValidateRoot() {
        return false;
    }

    /**
     * Pide un relayout.
     *
     * <p>En el JDK lo encola el {@code RepaintManager} y se hace despues, junto. Sin cola de eventos
     * que lo despache, aca es sincronico: invalida y valida al padre, que es el que sabe disponer.
     */
    public void revalidate() {
        invalidate();
        Container p = getParent();
        if (p != null) {
            p.validate();
        } else {
            validate();
        }
    }

    /** Repinta una region dada como rectangulo. */
    public void repaint(Rectangle r) {
        repaint(0, r.x, r.y, r.width, r.height);
    }

    /**
     * Pinta ahora una region, sin esperar a la cola.
     *
     * <p>No hace nada, y lo dice: pintar "ahora" es pintar a la pantalla, y esta VM no tiene una.
     * Un componente se pinta cuando alguien le pasa un {@link Graphics} a {@link #paint}. No se
     * inventa un {@link Graphics} de la nada porque {@link #getGraphics} devuelve {@code null}, que
     * es la respuesta correcta sin superficie.
     */
    public void paintImmediately(int x, int y, int w, int h) {
    }

    /** Ver {@link #paintImmediately(int, int, int, int)}. */
    public void paintImmediately(Rectangle r) {
        paintImmediately(r.x, r.y, r.width, r.height);
    }

    // -- borde, insets, opacidad -----------------------------------------------------------------

    /**
     * Pone un borde; {@code null} lo saca.
     *
     * <p>Cambiar el borde cambia los insets, y con ellos el layout: de ahi el {@link #revalidate}.
     */
    public void setBorder(Border border) {
        Border viejo = this.border;
        this.border = border;
        firePropertyChange("border", viejo, border);
        if (border != viejo) {
            if (border == null || viejo == null
                    || !border.getBorderInsets(this).equals(viejo.getBorderInsets(this))) {
                revalidate();
            }
            repaint();
        }
    }

    /** El borde, o {@code null}. */
    public Border getBorder() {
        return this.border;
    }

    /**
     * Cuanto espacio reserva el borde, o los insets de AWT si no hay borde.
     *
     * <p>Es lo que hace que un layout respete el borde sin saber que existe: pregunta los insets, y
     * los insets ya lo tienen adentro.
     */
    public Insets getInsets() {
        if (this.border != null) {
            return this.border.getBorderInsets(this);
        }
        return super.getInsets();
    }

    /** Lo mismo, llenando el objeto dado para no alocar. */
    public Insets getInsets(Insets insets) {
        if (insets == null) {
            insets = new Insets(0, 0, 0, 0);
        }
        Insets i = getInsets();
        insets.top = i.top;
        insets.left = i.left;
        insets.bottom = i.bottom;
        insets.right = i.right;
        return insets;
    }

    /**
     * Declara si este componente cubre todos sus pixeles.
     *
     * <p>Es una promesa, no una medida: quien la hace se compromete a que {@link #paintComponent}
     * pinte el area entera. Swing la usa para no pintar lo que hay debajo. Mentir aqui no falla:
     * deja basura en pantalla.
     */
    public void setOpaque(boolean isOpaque) {
        boolean viejo = this.opaque;
        this.opaque = isOpaque;
        this.opaquePuesto = true;
        firePropertyChange("opaque", viejo, isOpaque);
    }

    /** Si cubre todos sus pixeles. {@code false} por omision, que es lo seguro. */
    public boolean isOpaque() {
        return this.opaque;
    }

    // -- tamanos y alineacion ---------------------------------------------------------------------

    /**
     * El tamano preferido: el fijado, si alguien lo fijo; si no, el del aspecto; si no, el del
     * layout de AWT.
     *
     * <p>El orden es el del JDK y es lo que permite que {@code setPreferredSize} le gane al aspecto
     * sin que el aspecto se entere.
     */
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        if (this.ui != null) {
            Dimension d = this.ui.getPreferredSize(this);
            if (d != null) {
                return d;
            }
        }
        return super.getPreferredSize();
    }

    /** Como {@link #getPreferredSize}, para el minimo. */
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        if (this.ui != null) {
            Dimension d = this.ui.getMinimumSize(this);
            if (d != null) {
                return d;
            }
        }
        return super.getMinimumSize();
    }

    /** Como {@link #getPreferredSize}, para el maximo. */
    public Dimension getMaximumSize() {
        if (isMaximumSizeSet()) {
            return super.getMaximumSize();
        }
        if (this.ui != null) {
            Dimension d = this.ui.getMaximumSize(this);
            if (d != null) {
                return d;
            }
        }
        return super.getMaximumSize();
    }

    /** Fija la alineacion horizontal, recortada a {@code [0, 1]}. */
    public void setAlignmentX(float alignmentX) {
        this.alignmentX = Math.max(0.0f, Math.min(1.0f, alignmentX));
    }

    /** La alineacion horizontal; la de AWT si no se fijo ninguna. */
    public float getAlignmentX() {
        if (this.alignmentX < 0.0f) {
            return super.getAlignmentX();
        }
        return this.alignmentX;
    }

    /** Fija la alineacion vertical, recortada a {@code [0, 1]}. */
    public void setAlignmentY(float alignmentY) {
        this.alignmentY = Math.max(0.0f, Math.min(1.0f, alignmentY));
    }

    /** La alineacion vertical; la de AWT si no se fijo ninguna. */
    public float getAlignmentY() {
        if (this.alignmentY < 0.0f) {
            return super.getAlignmentY();
        }
        return this.alignmentY;
    }

    /** La linea de base: la del aspecto si hay, si no la de AWT. */
    public int getBaseline(int width, int height) {
        if (this.ui != null) {
            return this.ui.getBaseline(this, width, height);
        }
        return super.getBaseline(width, height);
    }

    /**
     * Como se mueve la linea de base; la del aspecto si hay.
     *
     * <p>Con el nombre binario {@code Component$BaselineResizeBehavior}: un tipo anidado de otro
     * archivo no resuelve por su nombre Java en nuestro compilador (#101).
     */
    public Component$BaselineResizeBehavior getBaselineResizeBehavior() {
        if (this.ui != null) {
            return this.ui.getBaselineResizeBehavior(this);
        }
        return super.getBaselineResizeBehavior();
    }

    /** Si el punto cae adentro; el aspecto puede darle una forma que no sea el rectangulo. */
    public boolean contains(int x, int y) {
        if (this.ui != null) {
            return this.ui.contains(this, x, y);
        }
        return super.contains(x, y);
    }

    // -- region visible ---------------------------------------------------------------------------

    /**
     * Calcula que parte de este componente se ve, intersecando con todos los ancestros.
     *
     * <p>Un componente adentro de un area de desplazamiento tiene casi todo tapado: lo que se ve es
     * la interseccion de su rectangulo con el de cada contenedor hasta arriba, cada uno en las
     * coordenadas de este.
     */
    public void computeVisibleRect(Rectangle visibleRect) {
        visibleRect.x = 0;
        visibleRect.y = 0;
        visibleRect.width = getWidth();
        visibleRect.height = getHeight();
        int dx = 0;
        int dy = 0;
        Container p = getParent();
        Component actual = this;
        while (p != null) {
            dx = dx + actual.getX();
            dy = dy + actual.getY();
            // El rectangulo del padre, traido a las coordenadas de este componente.
            int px = -dx;
            int py = -dy;
            int x1 = Math.max(visibleRect.x, px);
            int y1 = Math.max(visibleRect.y, py);
            int x2 = Math.min(visibleRect.x + visibleRect.width, px + p.getWidth());
            int y2 = Math.min(visibleRect.y + visibleRect.height, py + p.getHeight());
            visibleRect.x = x1;
            visibleRect.y = y1;
            visibleRect.width = Math.max(0, x2 - x1);
            visibleRect.height = Math.max(0, y2 - y1);
            if (p instanceof Window || p instanceof Applet) {
                break;
            }
            actual = p;
            p = p.getParent();
        }
    }

    /** La parte visible, en un rectangulo nuevo. */
    public Rectangle getVisibleRect() {
        Rectangle r = new Rectangle();
        computeVisibleRect(r);
        return r;
    }

    /**
     * Pide que un rectangulo de este componente quede a la vista.
     *
     * <p>El pedido sube: se traduce a las coordenadas del padre y se le reenvia. Quien lo atiende de
     * verdad es un area de desplazamiento mas arriba, que redefine este metodo. Sin ninguna, el
     * pedido llega a la raiz y no pasa nada — que es lo correcto, porque no hay nada que desplazar.
     */
    public void scrollRectToVisible(Rectangle aRect) {
        Container p = getParent();
        if (p instanceof JComponent) {
            aRect.x = aRect.x + getX();
            aRect.y = aRect.y + getY();
            ((JComponent) p).scrollRectToVisible(aRect);
            aRect.x = aRect.x - getX();
            aRect.y = aRect.y - getY();
        }
    }

    /** La ventana o applet que contiene a este componente, o {@code null} si no esta en ninguna. */
    public Container getTopLevelAncestor() {
        Container p = getParent();
        while (p != null) {
            if (p instanceof Window || p instanceof Applet) {
                return p;
            }
            p = p.getParent();
        }
        return null;
    }

    // -- propiedades de cliente y ayuda -----------------------------------------------------------

    /**
     * Guarda un valor bajo una clave, sin que la clase lo declare.
     *
     * <p>Es el cajon donde un aspecto o una herramienta deja datos propios sobre un componente
     * ajeno. {@code null} borra. Avisa como cambio de propiedad con la clave como nombre, asi que se
     * puede escuchar.
     */
    public final void putClientProperty(Object key, Object value) {
        if (key == null) {
            throw new NullPointerException("La clave no puede ser null");
        }
        Object viejo;
        synchronized (this) {
            if (this.clientProperties == null) {
                if (value == null) {
                    return;
                }
                this.clientProperties = new Hashtable<Object, Object>();
            }
            viejo = this.clientProperties.get(key);
            if (value != null) {
                this.clientProperties.put(key, value);
            } else {
                this.clientProperties.remove(key);
            }
        }
        firePropertyChange(key.toString(), viejo, value);
    }

    /** El valor bajo esa clave, o {@code null}. */
    public final Object getClientProperty(Object key) {
        if (key == null) {
            return null;
        }
        synchronized (this) {
            if (this.clientProperties == null) {
                return null;
            }
            return this.clientProperties.get(key);
        }
    }

    /**
     * Fija el texto de ayuda.
     *
     * <p>Se guarda como propiedad de cliente, como en el JDK. Mostrarlo en una burbuja pide un
     * {@code ToolTipManager} y un {@code JToolTip} que no estan; el texto queda disponible para
     * quien lo pregunte.
     */
    public void setToolTipText(String text) {
        putClientProperty(TOOL_TIP_TEXT_KEY, text);
    }

    /** El texto de ayuda, o {@code null}. */
    public String getToolTipText() {
        return (String) getClientProperty(TOOL_TIP_TEXT_KEY);
    }

    /** El texto de ayuda para ese punto; por omision, el mismo para todo el componente. */
    public String getToolTipText(MouseEvent event) {
        return getToolTipText();
    }

    /** Donde mostrar la ayuda; {@code null} deja elegir a quien la muestre. */
    public java.awt.Point getToolTipLocation(MouseEvent event) {
        return null;
    }

    // -- banderas varias --------------------------------------------------------------------------

    /**
     * Si se pinta primero fuera de pantalla y despues se copia entero.
     *
     * <p>Se guarda y se reporta. Sin pantalla no hay parpadeo que evitar, asi que el pintado va
     * directo a la imagen que le den, que es en si misma un buffer.
     */
    public void setDoubleBuffered(boolean aFlag) {
        this.doubleBuffered = aFlag;
    }

    public boolean isDoubleBuffered() {
        return this.doubleBuffered;
    }

    /** Si arrastrar el mouse fuera del componente lo desplaza. Se guarda; lo usa un area de desplazamiento. */
    public void setAutoscrolls(boolean autoscrolls) {
        this.autoscrolls = autoscrolls;
        this.autoscrollsPuesto = true;
    }

    public boolean getAutoscrolls() {
        return this.autoscrolls;
    }

    /** Las banderas de depuracion del pintado. Se guardan; no hay {@code DebugGraphics}. */
    public void setDebugGraphicsOptions(int debugOptions) {
        this.debugGraphicsOptions = debugOptions;
    }

    public int getDebugGraphicsOptions() {
        return this.debugGraphicsOptions;
    }

    /** Si el componente acepta que le pidan el foco por programa. */
    public void setRequestFocusEnabled(boolean requestFocusEnabled) {
        this.requestFocusEnabled = requestFocusEnabled;
    }

    public boolean isRequestFocusEnabled() {
        return this.requestFocusEnabled;
    }

    /** Si el verificador de entrada del componente que pierde el foco debe correr antes. */
    public void setVerifyInputWhenFocusTarget(boolean verifyInputWhenFocusTarget) {
        boolean viejo = this.verifyInputWhenFocusTarget;
        this.verifyInputWhenFocusTarget = verifyInputWhenFocusTarget;
        firePropertyChange("verifyInputWhenFocusTarget", viejo, verifyInputWhenFocusTarget);
    }

    public boolean getVerifyInputWhenFocusTarget() {
        return this.verifyInputWhenFocusTarget;
    }

    /** Pide el foco, sin importar {@link #isRequestFocusEnabled}. */
    public void grabFocus() {
        requestFocus();
    }

    /**
     * Le da el foco al primer descendiente que pueda tenerlo.
     *
     * @deprecated como en el JDK; la politica de recorrido de foco la decide el
     *     {@code KeyboardFocusManager}, que no esta
     */
    @Deprecated
    public boolean requestDefaultFocus() {
        return false;
    }

    /** La localidad por omision de los componentes nuevos. */
    public static Locale getDefaultLocale() {
        if (defaultLocale == null) {
            defaultLocale = Locale.getDefault();
        }
        return defaultLocale;
    }

    /** Cambia la localidad por omision. */
    public static void setDefaultLocale(Locale l) {
        defaultLocale = l;
    }

    // -- oyentes de ancestro y vetables ----------------------------------------------------------

    /** Agrega un oyente de cambios en los ancestros. */
    public void addAncestorListener(AncestorListener listener) {
        this.listenerList.add(AncestorListener.class, listener);
    }

    /** Saca un oyente de ancestros. */
    public void removeAncestorListener(AncestorListener listener) {
        this.listenerList.remove(AncestorListener.class, listener);
    }

    /** Los oyentes de ancestros. */
    public AncestorListener[] getAncestorListeners() {
        return this.listenerList.getListeners(AncestorListener.class);
    }

    /**
     * Agrega un oyente que puede <em>vetar</em> un cambio de propiedad.
     *
     * <p>Distinto de un oyente comun: este se consulta antes, y si tira
     * {@link PropertyVetoException} el cambio no ocurre. Lo usa {@code JInternalFrame} para que
     * alguien pueda impedir que una ventana se cierre.
     */
    public synchronized void addVetoableChangeListener(VetoableChangeListener listener) {
        if (this.vetoableChangeSupport == null) {
            this.vetoableChangeSupport = new VetoableChangeSupport(this);
        }
        this.vetoableChangeSupport.addVetoableChangeListener(listener);
    }

    /** Saca un oyente vetable. */
    public synchronized void removeVetoableChangeListener(VetoableChangeListener listener) {
        if (this.vetoableChangeSupport != null) {
            this.vetoableChangeSupport.removeVetoableChangeListener(listener);
        }
    }

    /** Los oyentes vetables. */
    public synchronized VetoableChangeListener[] getVetoableChangeListeners() {
        if (this.vetoableChangeSupport == null) {
            return new VetoableChangeListener[0];
        }
        return this.vetoableChangeSupport.getVetoableChangeListeners();
    }

    /**
     * Consulta a los oyentes vetables antes de un cambio.
     *
     * @throws PropertyVetoException si alguno se opone; el cambio no debe hacerse
     */
    protected void fireVetoableChange(String propertyName, Object oldValue, Object newValue)
            throws PropertyVetoException {
        if (this.vetoableChangeSupport != null) {
            this.vetoableChangeSupport.fireVetoableChange(propertyName, oldValue, newValue);
        }
    }

    /** Publico aca, protegido en AWT: Swing deja que cualquiera avise por un componente. */
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    /** Publico aca, protegido en AWT. */
    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Los oyentes de un tipo, incluidos los propios de Swing.
     *
     * <p>AWT solo conoce los suyos; los de ancestro y los vetables viven aca, asi que hay que
     * atenderlos antes de delegar.
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (listenerType == AncestorListener.class) {
            return this.listenerList.getListeners(listenerType);
        }
        if (listenerType == VetoableChangeListener.class) {
            @SuppressWarnings("unchecked")
            T[] r = (T[]) getVetoableChangeListeners();
            return r;
        }
        return super.getListeners(listenerType);
    }

    // -- ciclo de vida en la jerarquia -----------------------------------------------------------

    /**
     * Entra en una jerarquia con ventana.
     *
     * <p>Aca es donde se avisa a los oyentes de ancestro que hay ancestros. El JDK ademas registra
     * oyentes en cada ancestro para seguir los movimientos; sin pantalla no hay movimiento que
     * seguir, asi que solo se avisan la entrada y la salida.
     */
    public void addNotify() {
        super.addNotify();
        Container p = getParent();
        avisarAncestros(AncestorEvent.ANCESTOR_ADDED, p, p == null ? null : p.getParent());
    }

    /** Sale de la jerarquia. */
    public void removeNotify() {
        Container p = getParent();
        avisarAncestros(AncestorEvent.ANCESTOR_REMOVED, p, p == null ? null : p.getParent());
        super.removeNotify();
    }

    private void avisarAncestros(int id, Container ancestro, Container padreDelAncestro) {
        AncestorListener[] oyentes = getAncestorListeners();
        if (oyentes.length == 0) {
            return;
        }
        AncestorEvent e = new AncestorEvent(this, id, ancestro, padreDelAncestro);
        for (int i = 0; i < oyentes.length; i++) {
            if (id == AncestorEvent.ANCESTOR_ADDED) {
                oyentes[i].ancestorAdded(e);
            } else {
                oyentes[i].ancestorRemoved(e);
            }
        }
    }

    // -- estado que se propaga -------------------------------------------------------------------

    /** Habilita o deshabilita, avisando y repintando: un componente deshabilitado se ve distinto. */
    public void setEnabled(boolean enabled) {
        boolean viejo = isEnabled();
        super.setEnabled(enabled);
        firePropertyChange("enabled", viejo, enabled);
        if (enabled != viejo) {
            repaint();
        }
    }

    /** Muestra u oculta; un cambio pide relayout al padre, porque ocupa o libera lugar. */
    public void setVisible(boolean aFlag) {
        if (aFlag != isVisible()) {
            super.setVisible(aFlag);
            Container p = getParent();
            if (p != null) {
                p.invalidate();
                p.validate();
                p.repaint();
            }
        }
    }

    public void setForeground(java.awt.Color fg) {
        super.setForeground(fg);
        repaint();
    }

    public void setBackground(java.awt.Color bg) {
        super.setBackground(bg);
        repaint();
    }

    public void setFont(java.awt.Font font) {
        super.setFont(font);
        revalidate();
        repaint();
    }

    /**
     * @deprecated como en el JDK; queda por compatibilidad y delega en AWT
     */
    @Deprecated
    public void reshape(int x, int y, int w, int h) {
        super.reshape(x, y, w, h);
    }

    // -- teclado ----------------------------------------------------------------------------------

    /**
     * Procesa una tecla: primero los oyentes de AWT, despues {@link #processComponentKeyEvent}.
     *
     * <p>El JDK ademas consulta las acciones registradas por teclado, que no estan; ver la nota de
     * la clase.
     */
    protected void processKeyEvent(KeyEvent e) {
        super.processKeyEvent(e);
        if (!e.isConsumed()) {
            processComponentKeyEvent(e);
        }
    }

    /** El gancho para que una subclase atienda teclas sin registrar oyentes. Vacio por omision. */
    protected void processComponentKeyEvent(KeyEvent e) {
    }

    // -- depuracion -------------------------------------------------------------------------------

    protected String paramString() {
        String bordeStr = this.border == null ? "" : this.border.toString();
        return super.paramString()
                + ",alignmentX=" + String.valueOf(this.alignmentX)
                + ",alignmentY=" + String.valueOf(this.alignmentY)
                + ",border=" + bordeStr
                + ",flags=" + (this.opaque ? "opaque" : "")
                + ",maximumSize=" + (isMaximumSizeSet() ? getMaximumSize().toString() : "")
                + ",minimumSize=" + (isMinimumSizeSet() ? getMinimumSize().toString() : "")
                + ",preferredSize=" + (isPreferredSizeSet() ? getPreferredSize().toString() : "");
    }

    /**
     * Pone una propiedad en nombre de un aspecto: solo si el usuario no la puso antes.
     *
     * <p>Es lo que hay detras de {@code LookAndFeel.installProperty}. Las propiedades que un
     * aspecto puede proponer son pocas y estan enumeradas: "opaque" y "autoscrolls" aca, y las
     * subclases agregan las suyas. El JDK acepta tambien las teclas de recorrido del foco, que en
     * esta VM no hay como instalar. Cada una recuerda si la puso el usuario ({@code *Puesto});
     * esta llamada respeta eso y, al terminar, deja el recuerdo como estaba.
     */
    void setUIProperty(String propertyName, Object value) {
        if ("opaque".equals(propertyName)) {
            if (!opaquePuesto) {
                setOpaque(((Boolean) value).booleanValue());
                opaquePuesto = false;
            }
        } else if ("autoscrolls".equals(propertyName)) {
            if (!autoscrollsPuesto) {
                setAutoscrolls(((Boolean) value).booleanValue());
                autoscrollsPuesto = false;
            }
        } else {
            throw new IllegalArgumentException("property \"" + propertyName
                    + "\" cannot be set using this method");
        }
    }
}
