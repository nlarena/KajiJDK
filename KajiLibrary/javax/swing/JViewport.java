package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.Serializable;

import javax.accessibility.AccessibleContext;

import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ViewportUI;
import javax.swing.plaf.basic.BasicViewportUI;

/**
 * Una ventana que muestra un pedazo de algo mas grande.
 *
 * <h2>Un agujero y una cosa detras</h2>
 *
 * <p>La ventana tiene un solo hijo, la <em>vista</em>, y la vista suele ser mas grande que ella.
 * Desplazar no mueve la ventana: mueve la vista, en sentido contrario. Por eso
 * {@link #getViewPosition} devuelve la posicion de la vista <em>cambiada de signo</em>: la
 * posicion es "que punto de la vista queda arriba a la izquierda del agujero", y para que ese
 * punto baje, la vista tiene que subir.
 *
 * <p>El recorte no lo hace esta clase: lo hace la cadena de pintado, que le da a cada hijo un
 * contexto grafico recortado a su rectangulo. Una vista en (-15, -20) de 80 por 90 dentro de una
 * ventana de 41 por 31 se dibuja entera y se ve el pedazo que cae adentro.
 *
 * <h2>Sin borde, y sin trucos de pantalla</h2>
 *
 * <p>{@link #setBorder} lanza excepcion: un borde correria el contenido y la cuenta de que se ve
 * dejaria de cerrar. Es de las pocas veces que Swing prefiere prohibir a acomodar.
 *
 * <p>Los tres modos de desplazamiento existen como constantes y como propiedad, pero el unico que
 * se puede cumplir aca es {@link #SIMPLE_SCROLL_MODE}: {@link #BLIT_SCROLL_MODE} copia pixeles ya
 * dibujados <em>en la pantalla</em> y {@link #BACKINGSTORE_SCROLL_MODE} guarda una imagen de
 * respaldo del componente en pantalla. Sin pantalla, {@link #paint} siempre redibuja, que es
 * exactamente lo que el JDK hace cuando el blit no se puede usar. El campo
 * {@link #backingStoreImage} queda siempre en {@code null} por lo mismo.
 */
public class JViewport extends JComponent implements Accessible {

    private static final String uiClassID = "ViewportUI";

    /** Si alguien fijo el tamano de la vista a mano; ver {@link #setViewSize}. */
    protected boolean isViewSizeSet = false;

    /** Donde estaba la vista la ultima vez que se pinto. */
    protected Point lastPaintPosition = null;

    /** @deprecated el modo de respaldo necesita pantalla; ver la nota de la clase. */
    @Deprecated
    protected boolean backingStore = false;

    /** Siempre {@code null}; ver la nota de la clase. */
    protected transient Image backingStoreImage = null;

    /** Si el ultimo cambio de posicion vino de un desplazamiento y todavia no se acomodo. */
    protected boolean scrollUnderway = false;

    /** Copia lo ya dibujado y redibuja solo la franja nueva; necesita pantalla. */
    public static final int BLIT_SCROLL_MODE = 1;

    /** Guarda una imagen de respaldo del componente; necesita pantalla. */
    public static final int BACKINGSTORE_SCROLL_MODE = 2;

    /** Redibuja todo; es lo que hace esta VM siempre. */
    public static final int SIMPLE_SCROLL_MODE = 3;

    private int scrollMode = BLIT_SCROLL_MODE;

    private ComponentListener viewListener = null;
    private transient ChangeEvent changeEvent = null;
    private boolean hasHadValidView = false;

    /** Una ventana vacia, opaca y con la distribucion de {@link ViewportLayout}. */
    public JViewport() {
        super();
        setLayout(createLayoutManager());
        setOpaque(true);
        updateUI();
    }

    public ViewportUI getUI() {
        return (ViewportUI) ui;
    }

    public void setUI(ViewportUI ui) {
        super.setUI(ui);
    }

    /** Instala el aspecto basico; ver {@code JButton#updateUI}. */
    public void updateUI() {
        setUI((ViewportUI) BasicViewportUI.createUI(this));
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** Agregar un componente a una ventana es ponerlo de vista; ver {@link #setView}. */
    protected void addImpl(Component child, Object constraints, int index) {
        setView(child);
    }

    /** Quita la vista, y con ella el escucha que avisaba de sus cambios de tamano. */
    public void remove(Component child) {
        child.removeComponentListener(viewListener);
        super.remove(child);
    }

    /**
     * Desplaza lo justo para que ese rectangulo de la vista quede a la vista.
     *
     * <p>"Lo justo" es literal: si ya se ve entero no mueve nada, y si no entra lo alinea por el
     * borde mas cercano en vez de centrarlo. Es lo que evita que ir a un elemento salte de golpe
     * cuando ya estaba casi visible.
     */
    public void scrollRectToVisible(Rectangle contentRect) {
        Component view = getView();
        if (view == null) {
            return;
        }
        if (!view.isValid()) {
            validateView();
        }
        int dx = positionAdjustment(getWidth(), contentRect.width, contentRect.x);
        int dy = positionAdjustment(getHeight(), contentRect.height, contentRect.y);

        if (dx != 0 || dy != 0) {
            Point viewPosition = getViewPosition();
            Dimension viewSize = view.getSize();
            int startX = viewPosition.x;
            int startY = viewPosition.y;
            Dimension extent = getExtentSize();

            viewPosition.x = viewPosition.x - dx;
            viewPosition.y = viewPosition.y - dy;

            // El recorte contra los bordes solo corre si la vista esta maquetada: una vista sin
            // maquetar suele medir cero por cero, y recortar contra ese cero mandaria todo al
            // origen. En esta VM nada llega a estar maquetado —`validate` necesita una ventana—,
            // asi que este recorte no corre nunca y la posicion puede quedar fuera de rango. Es
            // lo mismo que hace el JDK sin ventana.
            if (view.isValid()) {
                if (getParent() == null || getParent().getComponentOrientation().isLeftToRight()) {
                    if (viewPosition.x + extent.width > viewSize.width) {
                        viewPosition.x = Math.max(0, viewSize.width - extent.width);
                    } else if (viewPosition.x < 0) {
                        viewPosition.x = 0;
                    }
                } else {
                    if (extent.width > viewSize.width) {
                        viewPosition.x = viewSize.width - extent.width;
                    } else {
                        viewPosition.x = Math.max(0,
                                Math.min(viewSize.width - extent.width, viewPosition.x));
                    }
                }
                if (viewPosition.y + extent.height > viewSize.height) {
                    viewPosition.y = Math.max(0, viewSize.height - extent.height);
                } else if (viewPosition.y < 0) {
                    viewPosition.y = 0;
                }
            }
            if (viewPosition.x != startX || viewPosition.y != startY) {
                setViewPosition(viewPosition);
                scrollUnderway = false;
            }
        }
    }

    /**
     * Maqueta el subarbol antes de calcular, para no medir sobre un tamano viejo.
     *
     * <p>Sin ventana no maqueta nada —{@code validate} lo dice— y por eso el recorte de
     * {@link #scrollRectToVisible} no llega a correr; ver la nota de ahi.
     */
    private void validateView() {
        Container raiz = getParent();
        while (raiz != null && !raiz.isValidateRoot()) {
            raiz = raiz.getParent();
        }
        if (raiz != null) {
            raiz.validate();
        }
    }

    /**
     * Cuanto hay que correr para que un hijo de ese ancho, en esa posicion, entre en ese padre.
     *
     * <p>Seis casos, y en todos gana el borde mas cercano: si ya entra, cero; si sobresale de un
     * lado, lo justo para pegarlo a ese lado; si es mas grande que el padre, se alinea el borde
     * por el que se lo pidio.
     */
    private int positionAdjustment(int parentWidth, int childWidth, int childAt) {
        if (childAt >= 0 && childWidth + childAt <= parentWidth) {
            return 0;
        }
        if (childAt <= 0 && childWidth + childAt >= parentWidth) {
            return 0;
        }
        if (childAt > 0 && childWidth <= parentWidth) {
            return parentWidth - (childWidth + childAt);
        }
        if (childAt >= 0 && childWidth >= parentWidth) {
            return -childAt;
        }
        if (childAt <= 0 && childWidth <= parentWidth) {
            return -childAt;
        }
        if (childAt <= 0 && childWidth >= parentWidth) {
            return parentWidth - (childWidth + childAt);
        }
        return 0;
    }

    /** Un {@link IllegalArgumentException}: una ventana no lleva borde; ver la nota de la clase. */
    public final void setBorder(Border border) {
        if (border != null) {
            throw new IllegalArgumentException("JViewport.setBorder() not supported");
        }
    }

    /** Cero por los cuatro lados, siempre. */
    public final Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }

    /** Cero por los cuatro lados; escribe en el que le pasan y lo devuelve, sin reservar. */
    public final Insets getInsets(Insets insets) {
        insets.left = 0;
        insets.top = 0;
        insets.right = 0;
        insets.bottom = 0;
        return insets;
    }

    /** No: la vista puede ser transparente y dejar ver el fondo de la ventana. */
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    /** Si, cuando hay un desplazamiento en curso: el repintado tiene que empezar por aca. */
    protected boolean isPaintingOrigin() {
        return scrollMode == BACKINGSTORE_SCROLL_MODE;
    }

    /** Donde esta la vista, en coordenadas de la ventana y sin cambiar de signo. */
    private Point getViewLocation() {
        Component view = getView();
        if (view != null) {
            return view.getLocation();
        }
        return new Point(0, 0);
    }

    /** Redibuja; ver la nota de la clase sobre por que no hay blit ni respaldo. */
    public void paint(Graphics g) {
        int width = getWidth();
        int height = getHeight();
        if ((width <= 0) || (height <= 0)) {
            return;
        }
        super.paint(g);
        lastPaintPosition = getViewLocation();
    }

    /** Cambiar de tamano cambia lo que se ve, y eso es un cambio de estado. */
    public void reshape(int x, int y, int w, int h) {
        boolean sizeChanged = (getWidth() != w) || (getHeight() != h);
        if (sizeChanged) {
            backingStoreImage = null;
        }
        super.reshape(x, y, w, h);
        if (sizeChanged) {
            fireStateChanged();
        }
    }

    /** El modo de desplazamiento; los tres se guardan, uno solo se cumple. */
    public void setScrollMode(int mode) {
        scrollMode = mode;
        backingStore = mode == BACKINGSTORE_SCROLL_MODE;
    }

    public int getScrollMode() {
        return scrollMode;
    }

    /** @deprecated es {@code getScrollMode() == BACKINGSTORE_SCROLL_MODE}. */
    @Deprecated
    public boolean isBackingStoreEnabled() {
        return scrollMode == BACKINGSTORE_SCROLL_MODE;
    }

    /** @deprecated es {@link #setScrollMode}. */
    @Deprecated
    public void setBackingStoreEnabled(boolean enabled) {
        if (enabled) {
            setScrollMode(BACKINGSTORE_SCROLL_MODE);
        } else {
            setScrollMode(BLIT_SCROLL_MODE);
        }
    }

    /** La vista, o {@code null}. */
    public Component getView() {
        return (getComponentCount() > 0) ? getComponent(0) : null;
    }

    /**
     * Pone la vista, sacando la anterior.
     *
     * <p>No usa {@code removeAll}: el JDK tampoco, porque {@code removeAll} no pasa por
     * {@link #remove} y el escucha de la vista vieja quedaria puesto.
     */
    public void setView(Component view) {
        int n = getComponentCount();
        for (int i = n - 1; i >= 0; i--) {
            remove(getComponent(i));
        }
        isViewSizeSet = false;
        if (view != null) {
            super.addImpl(view, null, -1);
            viewListener = createViewListener();
            view.addComponentListener(viewListener);
        }
        if (hasHadValidView) {
            fireStateChanged();
        } else if (view != null) {
            hasHadValidView = true;
        }
        revalidate();
        repaint();
    }

    /**
     * El tamano de la vista: el que le fijaron, o el que ella prefiere.
     *
     * <p>La distincion importa: mientras nadie lo fije, agrandar la ventana puede agrandar la
     * vista; una vez fijado, manda lo fijado.
     */
    public Dimension getViewSize() {
        Component view = getView();
        if (view == null) {
            return new Dimension(0, 0);
        } else if (isViewSizeSet) {
            return view.getSize();
        } else {
            return view.getPreferredSize();
        }
    }

    public void setViewSize(Dimension newSize) {
        Component view = getView();
        if (view != null) {
            Dimension oldSize = view.getSize();
            if (!newSize.equals(oldSize)) {
                scrollUnderway = false;
                view.setSize(newSize);
                isViewSizeSet = true;
                fireStateChanged();
            }
        }
    }

    /** Que punto de la vista queda arriba a la izquierda; ver la nota de la clase. */
    public Point getViewPosition() {
        Component view = getView();
        if (view != null) {
            Point p = view.getLocation();
            p.x = -p.x;
            p.y = -p.y;
            return p;
        }
        return new Point(0, 0);
    }

    /** Lleva ese punto de la vista a la esquina de la ventana, moviendo la vista al reves. */
    public void setViewPosition(Point p) {
        Component view = getView();
        if (view == null) {
            return;
        }
        int oldX;
        int oldY;
        int x = p.x;
        int y = p.y;

        Rectangle r = view.getBounds();
        oldX = r.x;
        oldY = r.y;

        int newX = -x;
        int newY = -y;

        if ((oldX != newX) || (oldY != newY)) {
            scrollUnderway = true;
            view.setLocation(newX, newY);
            fireStateChanged();
        }
    }

    /** Lo que se ve de la vista: la posicion mas el tamano del agujero. */
    public Rectangle getViewRect() {
        return new Rectangle(getViewPosition(), getExtentSize());
    }

    /**
     * Que parte se puede copiar y que parte hay que redibujar al desplazar {@code dx, dy}.
     *
     * <p>Solo sirve cuando el movimiento es en una sola direccion y menor que la ventana: si se
     * salto mas de una pantalla no queda nada aprovechable, y contesta {@code false}. El calculo
     * es aritmetica pura y esta completo aunque el blit que lo usaria necesite pantalla.
     */
    protected boolean computeBlit(int dx, int dy, Point blitFrom, Point blitTo, Dimension blitSize,
            Rectangle blitPaint) {
        int dxAbs = Math.abs(dx);
        int dyAbs = Math.abs(dy);
        Dimension extentSize = getExtentSize();

        if ((dx == 0) && (dy != 0) && (dyAbs < extentSize.height)) {
            if (dy < 0) {
                blitFrom.y = -dy;
                blitTo.y = 0;
                blitPaint.y = extentSize.height + dy;
            } else {
                blitFrom.y = 0;
                blitTo.y = dy;
                blitPaint.y = 0;
            }
            blitPaint.x = 0;
            blitFrom.x = 0;
            blitTo.x = 0;
            blitSize.width = extentSize.width;
            blitSize.height = extentSize.height - dyAbs;
            blitPaint.width = extentSize.width;
            blitPaint.height = dyAbs;
            return true;
        } else if ((dy == 0) && (dx != 0) && (dxAbs < extentSize.width)) {
            if (dx < 0) {
                blitFrom.x = -dx;
                blitTo.x = 0;
                blitPaint.x = extentSize.width + dx;
            } else {
                blitFrom.x = 0;
                blitTo.x = dx;
                blitPaint.x = 0;
            }
            blitPaint.y = 0;
            blitFrom.y = 0;
            blitTo.y = 0;
            blitSize.width = extentSize.width - dxAbs;
            blitSize.height = extentSize.height;
            blitPaint.width = dxAbs;
            blitPaint.height = extentSize.height;
            return true;
        } else {
            blitFrom.x = 0;
            blitFrom.y = 0;
            blitTo.x = 0;
            blitTo.y = 0;
            blitSize.width = 0;
            blitSize.height = 0;
            blitPaint.x = 0;
            blitPaint.y = 0;
            blitPaint.width = 0;
            blitPaint.height = 0;
            return false;
        }
    }

    /** El tamano del agujero: el de la ventana misma. */
    public Dimension getExtentSize() {
        return getSize();
    }

    /**
     * Ese tamano en coordenadas de la vista: el mismo.
     *
     * <p>El JDK lo usa para ventanas con transformacion —una lupa—, que aca no existen. Devolver
     * una copia y no el mismo objeto es parte del contrato: quien lo recibe puede modificarlo.
     */
    public Dimension toViewCoordinates(Dimension size) {
        return new Dimension(size);
    }

    /** Ese punto en coordenadas de la vista: el mismo. */
    public Point toViewCoordinates(Point p) {
        return new Point(p);
    }

    /** Cambia el tamano del agujero; es {@code setSize} mas el aviso. */
    public void setExtentSize(Dimension newExtent) {
        Dimension oldExtent = getExtentSize();
        if (!newExtent.equals(oldExtent)) {
            setSize(newExtent);
            fireStateChanged();
        }
    }

    /** El escucha que avisa cuando la vista cambia de tamano. */
    protected ViewListener createViewListener() {
        return new ViewListener();
    }

    /** La distribucion de una ventana: la compartida de {@link ViewportLayout}. */
    protected LayoutManager createLayoutManager() {
        return ViewportLayout.SHARED_INSTANCE;
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    /**
     * Avisa que cambio lo que se ve.
     *
     * <p>Es la unica senal que un {@link JScrollPane} necesita: de aca salen los numeros con los
     * que sincroniza sus dos barras.
     */
    protected void fireStateChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
    }

    /** Le pasa el pedido al padre, corrido a sus coordenadas. */
    public void repaint(long tm, int x, int y, int w, int h) {
        Component parent = getParent();
        if (parent != null) {
            parent.repaint(tm, x + getX(), y + getY(), w, h);
        } else {
            super.repaint(tm, x, y, w, h);
        }
    }

    protected String paramString() {
        String isViewSizeSetString = (isViewSizeSet ? "true" : "false");
        String lastPaintPositionString = (lastPaintPosition != null
                ? lastPaintPosition.toString() : "");
        String scrollUnderwayString = (scrollUnderway ? "true" : "false");

        return super.paramString() + ",isViewSizeSet=" + isViewSizeSetString
                + ",lastPaintPosition=" + lastPaintPositionString + ",scrollUnderway="
                + scrollUnderwayString;
    }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    /** Sin contexto de accesibilidad: no hay tecnologia asistiva que lo lea en esta VM. */
    public AccessibleContext getAccessibleContext() {
        return null;
    }

    /**
     * Escucha a la vista: si cambia de tamano, cambia lo que se ve.
     *
     * <p>Es una clase y no una expresion suelta porque {@link #createViewListener} la devuelve, y
     * una subclase puede querer la suya.
     */
    protected class ViewListener extends ComponentAdapter implements Serializable {

        public ViewListener() {
        }

        public void componentResized(ComponentEvent e) {
            fireStateChanged();
            revalidate();
        }
    }
}
