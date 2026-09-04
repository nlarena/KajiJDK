package java.awt;

import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

/**
 * Una ventana con borde, barra de título y, si se le pone, barra de menús.
 *
 * <p>Es la ventana principal de una aplicación de escritorio. Lo que agrega respecto de
 * {@link Window} es todo lo que dibuja el escritorio alrededor del contenido: el marco, el título,
 * los botones de minimizar y cerrar.
 *
 * <p>El **estado extendido** es una máscara de bits y no un valor, y eso importa: una ventana puede
 * estar maximizada horizontalmente y no verticalmente, o minimizada **y** maximizada a la vez —
 * cuando se restaure vuelve maximizada. Un solo valor no podría expresarlo.
 *
 * <p>Los márgenes de {@link Container#getInsets} son, en un marco, el grosor de esa decoración. Acá
 * son cero: sin escritorio no hay marco que ocupe lugar.
 */
public class Frame extends Window implements MenuContainer {

    private static final long serialVersionUID = 2673458971256075116L;

    /** Ni minimizada ni maximizada. */
    public static final int NORMAL = 0;

    /** Minimizada. */
    public static final int ICONIFIED = 1;

    /** Maximizada a lo ancho. */
    public static final int MAXIMIZED_HORIZ = 2;

    /** Maximizada a lo alto. */
    public static final int MAXIMIZED_VERT = 4;

    /** Maximizada en las dos direcciones. */
    public static final int MAXIMIZED_BOTH = MAXIMIZED_VERT | MAXIMIZED_HORIZ;

    /**
     * El cursor de siempre.
     *
     * @deprecated es del modelo de 1.0. Usar {@link Cursor#DEFAULT_CURSOR}.
     */
    @Deprecated
    public static final int DEFAULT_CURSOR = Cursor.DEFAULT_CURSOR;

    /**
     * La cruz.
     *
     * @deprecated usar {@link Cursor#CROSSHAIR_CURSOR}.
     */
    @Deprecated
    public static final int CROSSHAIR_CURSOR = Cursor.CROSSHAIR_CURSOR;

    /**
     * La barra de texto.
     *
     * @deprecated usar {@link Cursor#TEXT_CURSOR}.
     */
    @Deprecated
    public static final int TEXT_CURSOR = Cursor.TEXT_CURSOR;

    /**
     * El reloj de espera.
     *
     * @deprecated usar {@link Cursor#WAIT_CURSOR}.
     */
    @Deprecated
    public static final int WAIT_CURSOR = Cursor.WAIT_CURSOR;

    /**
     * Redimensionar desde el borde inferior izquierdo.
     *
     * @deprecated usar {@link Cursor#SW_RESIZE_CURSOR}.
     */
    @Deprecated
    public static final int SW_RESIZE_CURSOR = Cursor.SW_RESIZE_CURSOR;

    /**
     * Desde el borde inferior derecho.
     *
     * @deprecated usar {@link Cursor#SE_RESIZE_CURSOR}.
     */
    @Deprecated
    public static final int SE_RESIZE_CURSOR = Cursor.SE_RESIZE_CURSOR;

    /**
     * Desde el borde superior izquierdo.
     *
     * @deprecated usar {@link Cursor#NW_RESIZE_CURSOR}.
     */
    @Deprecated
    public static final int NW_RESIZE_CURSOR = Cursor.NW_RESIZE_CURSOR;

    /**
     * Desde el borde superior derecho.
     *
     * @deprecated usar {@link Cursor#NE_RESIZE_CURSOR}.
     */
    @Deprecated
    public static final int NE_RESIZE_CURSOR = Cursor.NE_RESIZE_CURSOR;

    /**
     * Desde el borde de arriba.
     *
     * @deprecated usar {@link Cursor#N_RESIZE_CURSOR}.
     */
    @Deprecated
    public static final int N_RESIZE_CURSOR = Cursor.N_RESIZE_CURSOR;

    /**
     * Desde el borde de abajo.
     *
     * @deprecated usar {@link Cursor#S_RESIZE_CURSOR}.
     */
    @Deprecated
    public static final int S_RESIZE_CURSOR = Cursor.S_RESIZE_CURSOR;

    /**
     * Desde el borde izquierdo.
     *
     * @deprecated usar {@link Cursor#W_RESIZE_CURSOR}.
     */
    @Deprecated
    public static final int W_RESIZE_CURSOR = Cursor.W_RESIZE_CURSOR;

    /**
     * Desde el borde derecho.
     *
     * @deprecated usar {@link Cursor#E_RESIZE_CURSOR}.
     */
    @Deprecated
    public static final int E_RESIZE_CURSOR = Cursor.E_RESIZE_CURSOR;

    /**
     * La mano.
     *
     * @deprecated usar {@link Cursor#HAND_CURSOR}.
     */
    @Deprecated
    public static final int HAND_CURSOR = Cursor.HAND_CURSOR;

    /**
     * La cruz de mover.
     *
     * @deprecated usar {@link Cursor#MOVE_CURSOR}.
     */
    @Deprecated
    public static final int MOVE_CURSOR = Cursor.MOVE_CURSOR;

    private static final List<Frame> todos = new ArrayList<Frame>();

    private String title = "Untitled";
    private MenuBar menuBar;
    private boolean resizable = true;
    private boolean undecorated;
    private int state = NORMAL;
    private Rectangle maximizedBounds;

    /**
     * Un marco sin título.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public Frame() throws HeadlessException {
        this("");
    }

    /**
     * Con esa configuración gráfica.
     *
     * @throws IllegalArgumentException si la configuración no es de una pantalla
     */
    public Frame(GraphicsConfiguration gc) {
        this("", gc);
    }

    /**
     * Con ese título.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public Frame(String title) throws HeadlessException {
        this(title, null);
    }

    /**
     * Con título y configuración gráfica.
     *
     * @throws IllegalArgumentException si la configuración no es de una pantalla
     */
    public Frame(String title, GraphicsConfiguration gc) {
        super(null, gc);
        this.title = title;
        synchronized (todos) {
            todos.add(this);
        }
    }

    /** Avisa que puede mostrarse, y se lo avisa a la barra de menús. */
    public void addNotify() {
        synchronized (this.getTreeLock()) {
            if (this.menuBar != null) {
                this.menuBar.addNotify();
            }
            super.addNotify();
        }
    }

    /** Avisa que dejó de poder mostrarse. */
    public void removeNotify() {
        synchronized (this.getTreeLock()) {
            if (this.menuBar != null) {
                this.menuBar.removeNotify();
            }
            super.removeNotify();
        }
    }

    /** El texto de la barra de título. */
    public String getTitle() {
        return this.title;
    }

    /** Le cambia el título; un `null` se toma como vacío. */
    public void setTitle(String title) {
        String viejo = this.title;
        synchronized (this) {
            this.title = title == null ? "" : title;
        }
        this.firePropertyChange("title", viejo, this.title);
    }

    /**
     * El ícono de la ventana.
     *
     * @return el primero de {@link Window#getIconImages}, o `null` si no hay
     */
    public Image getIconImage() {
        java.util.List<Image> l = this.getIconImages();
        if (l.isEmpty()) {
            return null;
        }
        return l.get(0);
    }

    /** Le pone ícono. */
    public void setIconImage(Image image) {
        super.setIconImage(image);
    }

    /**
     * La barra de menús.
     *
     * @return la barra, o `null` si no tiene
     */
    public MenuBar getMenuBar() {
        return this.menuBar;
    }

    /**
     * Le pone barra de menús.
     *
     * <p>Cambia el espacio disponible para el contenido, así que invalida el marco.
     */
    public void setMenuBar(MenuBar mb) {
        synchronized (this.getTreeLock()) {
            if (this.menuBar == mb) {
                return;
            }
            if (this.menuBar != null) {
                this.menuBar.setParent(null);
            }
            this.menuBar = mb;
            if (mb != null) {
                if (mb.getParent() != null) {
                    ((MenuContainer) mb.getParent()).remove(mb);
                }
                mb.setParent(this);
            }
            this.invalidate();
        }
    }

    /** Si el usuario puede cambiarle el tamaño. */
    public boolean isResizable() {
        return this.resizable;
    }

    /** Declara si el usuario puede cambiarle el tamaño. */
    public void setResizable(boolean resizable) {
        boolean viejo;
        synchronized (this) {
            viejo = this.resizable;
            this.resizable = resizable;
        }
        this.firePropertyChange("resizable", viejo, resizable);
    }

    /**
     * Minimiza o restaura.
     *
     * @deprecated sólo puede expresar minimizado y normal. Usar {@link #setExtendedState}.
     */
    @Deprecated
    public synchronized void setState(int state) {
        int nuevo = this.state;
        if (state == ICONIFIED) {
            nuevo = nuevo | ICONIFIED;
        } else {
            nuevo = nuevo & ~ICONIFIED;
        }
        this.setExtendedState(nuevo);
    }

    /**
     * Si está minimizada.
     *
     * @deprecated no ve los estados de maximización. Usar {@link #getExtendedState}.
     */
    @Deprecated
    public synchronized int getState() {
        return (this.state & ICONIFIED) != 0 ? ICONIFIED : NORMAL;
    }

    /**
     * Cambia el estado de la ventana.
     *
     * <p>Es una máscara: se pueden combinar {@link #ICONIFIED} con los de maximización, y significa
     * que al restaurarla va a volver maximizada.
     */
    public void setExtendedState(int state) {
        synchronized (this) {
            this.state = state;
        }
    }

    /** El estado, como máscara de bits. */
    public int getExtendedState() {
        return this.state;
    }

    /**
     * Hasta dónde se maximiza.
     *
     * @param bounds el rectángulo, o `null` para que sea toda la pantalla
     */
    public synchronized void setMaximizedBounds(Rectangle bounds) {
        this.maximizedBounds = bounds;
    }

    /**
     * Hasta dónde se maximiza.
     *
     * @return el rectángulo, o `null` si es toda la pantalla
     */
    public Rectangle getMaximizedBounds() {
        return this.maximizedBounds;
    }

    /**
     * Le saca la decoración.
     *
     * @throws IllegalComponentStateException si la ventana ya puede mostrarse: la decoración la pone
     *     el escritorio al crearla, y después ya es tarde
     */
    public void setUndecorated(boolean undecorated) {
        synchronized (this.getTreeLock()) {
            if (this.isDisplayable()) {
                throw new IllegalComponentStateException(
                        "The frame is displayable.");
            }
            this.undecorated = undecorated;
        }
    }

    /** Si no tiene decoración. */
    public boolean isUndecorated() {
        return this.undecorated;
    }

    /**
     * Le cambia la opacidad.
     *
     * @throws IllegalComponentStateException si la ventana está decorada: el escritorio no puede
     *     hacer translúcido un marco que dibuja él
     */
    public void setOpacity(float opacity) {
        synchronized (this.getTreeLock()) {
            if (opacity < 1.0f && !this.isUndecorated()) {
                throw new IllegalComponentStateException("The frame is decorated");
            }
            super.setOpacity(opacity);
        }
    }

    /**
     * Le recorta la forma.
     *
     * @throws IllegalComponentStateException si la ventana está decorada, por el mismo motivo
     */
    public void setShape(Shape shape) {
        synchronized (this.getTreeLock()) {
            if (shape != null && !this.isUndecorated()) {
                throw new IllegalComponentStateException("The frame is decorated");
            }
            super.setShape(shape);
        }
    }

    /**
     * Le cambia el fondo.
     *
     * @throws IllegalComponentStateException si se pide transparencia sobre una ventana decorada
     */
    public void setBackground(Color bgColor) {
        synchronized (this.getTreeLock()) {
            if (bgColor != null && bgColor.getAlpha() < 255 && !this.isUndecorated()) {
                throw new IllegalComponentStateException("The frame is decorated");
            }
            super.setBackground(bgColor);
        }
    }

    /** Le saca la barra de menús si es eso lo que se pasa. */
    public void remove(MenuComponent m) {
        if (m == this.menuBar) {
            this.setMenuBar(null);
        } else {
            super.remove(m);
        }
    }

    /**
     * Le pone cursor por número.
     *
     * @deprecated es del modelo de 1.0. Usar {@link Component#setCursor(Cursor)}.
     */
    @Deprecated
    public void setCursor(int cursorType) {
        if (cursorType < DEFAULT_CURSOR || cursorType > MOVE_CURSOR) {
            throw new IllegalArgumentException("illegal cursor type");
        }
        this.setCursor(Cursor.getPredefinedCursor(cursorType));
    }

    /**
     * Qué cursor tiene, por número.
     *
     * @deprecated es del modelo de 1.0. Usar {@link Component#getCursor}.
     */
    @Deprecated
    public int getCursorType() {
        return this.getCursor().getType();
    }

    /** Todos los marcos de esta aplicación. */
    public static Frame[] getFrames() {
        synchronized (todos) {
            return todos.toArray(new Frame[todos.size()]);
        }
    }

    protected String paramString() {
        String s = super.paramString();
        if (this.title != null) {
            s = s + ",title=" + this.title;
        }
        if (this.resizable) {
            s = s + ",resizable";
        }
        return s;
    }

    /** La información de accesibilidad de este marco. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTFrame();
        }
        return this.accessibleContext;
    }

    /** La accesibilidad de un marco. */
    protected class AccessibleAWTFrame extends AccessibleAWTWindow {

        /** Para las subclases. */
        protected AccessibleAWTFrame() {
        }

        /** Es un marco. */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.FRAME;
        }

        /** Los de una ventana, más si se puede redimensionar y si está minimizado. */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet s = super.getAccessibleStateSet();
            if (Frame.this.isResizable()) {
                s.add(AccessibleState.RESIZABLE);
            }
            if ((Frame.this.getExtendedState() & ICONIFIED) != 0) {
                s.add(AccessibleState.ICONIFIED);
            }
            return s;
        }
    }
}
