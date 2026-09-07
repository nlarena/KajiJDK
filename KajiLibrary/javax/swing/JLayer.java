package javax.swing;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.LayerUI;

/**
 * Envuelve un componente para decorarlo o para espiar sus eventos.
 *
 * <h2>Envolver en lugar de heredar</h2>
 *
 * <p>Todo lo interesante esta en el {@link LayerUI}; ver su nota. Esta clase es el envase: contiene
 * al componente, le da el tamano, y le pasa al aspecto lo que hay que dibujar y los eventos que se
 * hayan pedido.
 *
 * <h2>Es final, y no lo puede evitar</h2>
 *
 * <p>La clase es final a proposito. Heredar de ella seria volver justo a lo que viene a evitar: si
 * se pudiera, la forma natural de usarla seria una subclase por decoracion, y no se podrian apilar
 * dos. Apilar se hace envolviendo dos veces.
 *
 * <h2>El vidrio de arriba</h2>
 *
 * <p>Ademas del componente hay un {@link JPanel} transparente encima. Sirve para atrapar el mouse
 * sin que llegue a lo de abajo -- lo que hace un velo de "cargando" -- y para dibujar sin
 * interferir. Es un panel de verdad, asi que se le pueden poner componentes.
 *
 * @param <V> el tipo del componente que se envuelve.
 */
public final class JLayer<V extends Component> extends JComponent implements Scrollable,
        PropertyChangeListener, Accessible {

    private V view;
    private JPanel glassPane;
    private long eventMask;
    private transient boolean isPainting;
    private AccessibleContext accessibleContext;

    /** Una capa vacia. */
    public JLayer() {
        this(null);
    }

    /** Una capa alrededor de ese componente. */
    public JLayer(V view) {
        this(view, new LayerUI<V>());
    }

    /** Una capa alrededor de ese componente, con ese aspecto. */
    public JLayer(V view, LayerUI<V> ui) {
        setGlassPane(createGlassPane());
        setView(view);
        setUI(ui);
    }

    /** El componente envuelto. */
    public V getView() {
        return view;
    }

    /**
     * Cambia el componente envuelto.
     *
     * <p>Se saca el anterior y se agrega el nuevo en el mismo lugar. El vidrio se queda: pertenece
     * a la capa, no al componente.
     */
    public void setView(V view) {
        Component oldView = getView();
        if (oldView != null) {
            super.remove(oldView);
        }
        if (view != null) {
            super.addImpl(view, null, getComponentCount());
        }
        this.view = view;
        firePropertyChange("view", oldView, view);
        revalidate();
        repaint();
    }

    /**
     * Cambia el aspecto.
     *
     * <p>Es donde vive la decoracion, asi que cambiarlo cambia lo que la capa hace.
     */
    public void setUI(LayerUI<? super V> ui) {
        super.setUI(ui);
    }

    public LayerUI<? super V> getUI() {
        return (LayerUI<? super V>) ui;
    }

    /** El panel transparente que va encima; ver la nota de la clase. */
    public JPanel getGlassPane() {
        return glassPane;
    }

    public void setGlassPane(JPanel glassPane) {
        Component oldGlassPane = getGlassPane();
        boolean visible = false;
        if (oldGlassPane != null) {
            visible = oldGlassPane.isVisible();
            super.remove(oldGlassPane);
        }
        if (glassPane != null) {
            glassPane.setVisible(visible);
            super.addImpl(glassPane, null, 0);
        }
        this.glassPane = glassPane;
        firePropertyChange("glassPane", oldGlassPane, glassPane);
        revalidate();
        repaint();
    }

    /** El vidrio de siempre: transparente y visible. */
    public JPanel createGlassPane() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setVisible(true);
        p.setLayout(null);
        return p;
    }

    /**
     * No se puede poner acomodador.
     *
     * <p>La capa acomoda sus dos hijos ella misma: el componente ocupa todo menos los margenes y el
     * vidrio ocupa todo. Un acomodador cualquiera romperia esa relacion, que es de lo que depende
     * que el vidrio quede justo encima.
     *
     * @throws IllegalArgumentException si el acomodador no es nulo.
     */
    public void setLayout(LayoutManager mgr) {
        if (mgr != null) {
            throw new IllegalArgumentException("JLayer.setLayout() not supported");
        }
    }

    /**
     * No se puede poner borde.
     *
     * <p>El borde va en el componente envuelto, que es a quien pertenece.
     *
     * @throws IllegalArgumentException si el borde no es nulo.
     */
    public void setBorder(Border border) {
        if (border != null) {
            throw new IllegalArgumentException("JLayer.setBorder() not supported");
        }
    }

    public Border getBorder() {
        return null;
    }

    /**
     * No se pueden agregar hijos.
     *
     * <p>Los dos hijos que tiene los pone ella. Agregar un tercero dejaria un componente sin lugar
     * asignado, porque la capa no acomoda por lista.
     *
     * @throws UnsupportedOperationException siempre.
     */
    protected void addImpl(Component comp, Object constraints, int index) {
        throw new UnsupportedOperationException(
                "Adding components to JLayer is not supported, use setView() or setGlassPane()");
    }

    /**
     * No se pueden sacar hijos.
     *
     * @throws IllegalArgumentException si no es el componente ni el vidrio.
     */
    public void remove(Component comp) {
        if (comp == null) {
            super.remove(comp);
        } else if (comp == getView()) {
            setView(null);
        } else if (comp == getGlassPane()) {
            setGlassPane(null);
        } else {
            super.remove(comp);
        }
    }

    public void removeAll() {
        if (view != null) {
            setView(null);
        }
        if (glassPane != null) {
            setGlassPane(null);
        }
    }

    /**
     * Si la capa dibuja fuera de sus limites.
     *
     * <p>Siempre cierto. Una decoracion puede pintar donde quiera -- un resaltado alrededor, una
     * sombra --, asi que el sistema de repintado tiene que preguntarle a la capa y no adivinar.
     */
    protected boolean isPaintingOrigin() {
        return true;
    }

    public void paintImmediately(int x, int y, int w, int h) {
        if (!isPainting) {
            LayerUI<? super V> ui = getUI();
            if (ui != null) {
                ui.paintImmediately(x, y, w, h, this);
                return;
            }
        }
        super.paintImmediately(x, y, w, h);
    }

    public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) {
        if (!isPainting) {
            LayerUI<? super V> ui = getUI();
            if (ui != null) {
                return ui.imageUpdate(img, infoflags, x, y, w, h, this);
            }
        }
        return super.imageUpdate(img, infoflags, x, y, w, h);
    }

    /**
     * Dibuja la capa.
     *
     * <p>Todo el dibujo pasa por el aspecto. La bandera evita volver a entrar: el aspecto llama a
     * {@code c.paint(g)} para dibujar lo de abajo, y sin ella eso volveria aca.
     */
    public void paint(Graphics g) {
        if (!isPainting) {
            isPainting = true;
            try {
                super.paintComponent(g);
            } finally {
                isPainting = false;
            }
        } else {
            super.paint(g);
        }
    }

    protected void paintComponent(Graphics g) {
    }

    /**
     * Si los hijos no se pisan.
     *
     * <p>Siempre falso: el vidrio esta justo encima del componente, por definicion se pisan.
     * Contestar que si haria que el sistema dibujara solo uno de los dos.
     */
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    /** Le pasa al aspecto los cambios de propiedad de la capa y de el mismo. */
    public void propertyChange(PropertyChangeEvent evt) {
        LayerUI<? super V> ui = getUI();
        if (ui != null) {
            ui.applyPropertyChange(evt, this);
        }
    }

    /**
     * Que eventos quiere ver el aspecto.
     *
     * <p>Cero, que es lo de fabrica, significa ninguno. Ver la nota de {@link LayerUI}.
     */
    public void setLayerEventMask(long layerEventMask) {
        long oldEventMask = getLayerEventMask();
        this.eventMask = layerEventMask;
        firePropertyChange("layerEventMask", oldEventMask, layerEventMask);
    }

    public long getLayerEventMask() {
        return eventMask;
    }

    public void updateUI() {
        LayerUI<? super V> ui = getUI();
        if (ui != null) {
            ui.updateUI(this);
        }
    }

    /** Lo que pida el componente envuelto, si es desplazable. */
    public Dimension getPreferredScrollableViewportSize() {
        if (getView() instanceof Scrollable) {
            return ((Scrollable) getView()).getPreferredScrollableViewportSize();
        }
        return getPreferredSize();
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation,
            int direction) {
        if (getView() instanceof Scrollable) {
            return ((Scrollable) getView()).getScrollableBlockIncrement(visibleRect,
                    orientation, direction);
        }
        return (orientation == SwingConstants.VERTICAL) ? visibleRect.height
                : visibleRect.width;
    }

    public boolean getScrollableTracksViewportHeight() {
        if (getView() instanceof Scrollable) {
            return ((Scrollable) getView()).getScrollableTracksViewportHeight();
        }
        return false;
    }

    public boolean getScrollableTracksViewportWidth() {
        if (getView() instanceof Scrollable) {
            return ((Scrollable) getView()).getScrollableTracksViewportWidth();
        }
        return false;
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation,
            int direction) {
        if (getView() instanceof Scrollable) {
            return ((Scrollable) getView()).getScrollableUnitIncrement(visibleRect,
                    orientation, direction);
        }
        return 1;
    }

    public void addNotify() {
        super.addNotify();
    }

    public void removeNotify() {
        super.removeNotify();
    }

    /** Acomoda el componente y el vidrio; lo hace el aspecto. */
    public void doLayout() {
        LayerUI<? super V> ui = getUI();
        if (ui != null) {
            ui.doLayout(this);
        }
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
