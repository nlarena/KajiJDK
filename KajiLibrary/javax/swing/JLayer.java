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
 * It wraps a component in order to decorate it or to spy on its events.
 *
 * <h2>Wrapping instead of inheriting</h2>
 *
 * <p>Everything interesting is in the {@link LayerUI}; see its note. This class is the
 * container: it holds the component, gives it its size, and passes the look and feel what has
 * to be drawn and the events that were asked for.
 *
 * <h2>It is final, and it cannot avoid it</h2>
 *
 * <p>The class is final on purpose. Inheriting from it would be going back to just what it
 * comes to avoid: if it were possible, the natural way of using it would be a subclass per
 * decoration, and two could not be stacked. Stacking is done by wrapping twice.
 *
 * <h2>The glass on top</h2>
 *
 * <p>Besides the component there is a transparent {@link JPanel} on top. It serves to catch the
 * mouse without its reaching what is below -- which is what a "loading" veil does -- and to
 * draw without interfering. It is a real panel, so components can be put into it.
 *
 * @param <V> the type of the component that is wrapped.
 */
public final class JLayer<V extends Component> extends JComponent implements Scrollable,
        PropertyChangeListener, Accessible {

    private V view;
    private JPanel glassPane;
    private long eventMask;
    private transient boolean isPainting;
    private AccessibleContext accessibleContext;

    /** An empty layer. */
    public JLayer() {
        this(null);
    }

    /** A layer around that component. */
    public JLayer(V view) {
        this(view, new LayerUI<V>());
    }

    /** A layer around that component, with that look and feel. */
    public JLayer(V view, LayerUI<V> ui) {
        setGlassPane(createGlassPane());
        setView(view);
        setUI(ui);
    }

    /** The wrapped component. */
    public V getView() {
        return view;
    }

    /**
     * It changes the wrapped component.
     *
     * <p>The previous one is taken out and the new one added in the same place. The glass stays: it
     * belongs to the layer, not to the component.
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
     * It changes the look and feel.
     *
     * <p>It is where the decoration lives, so changing it changes what the layer does.
     */
    public void setUI(LayerUI<? super V> ui) {
        super.setUI(ui);
    }

    public LayerUI<? super V> getUI() {
        return (LayerUI<? super V>) ui;
    }

    /** The transparent panel that goes on top; see the class note. */
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

    /** The usual glass: transparent and visible. */
    public JPanel createGlassPane() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setVisible(true);
        p.setLayout(null);
        return p;
    }

    /**
     * No layout can be set.
     *
     * <p>The layer lays its two children out itself: the component takes up everything but the
     * margins and the glass takes up everything. Any layout would break that relation, which is
     * what the glass being right on top depends on.
     *
     * @throws IllegalArgumentException if the layout is not null.
     */
    public void setLayout(LayoutManager mgr) {
        if (mgr != null) {
            throw new IllegalArgumentException("JLayer.setLayout() not supported");
        }
    }

    /**
     * No border can be set.
     *
     * <p>The border goes on the wrapped component, which is who it belongs to.
     *
     * @throws IllegalArgumentException if the border is not null.
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
     * No children can be added.
     *
     * <p>The two children it has are set by it. Adding a third would leave a component with no
     * place assigned, because the layer does not lay out by list.
     *
     * @throws UnsupportedOperationException always.
     */
    protected void addImpl(Component comp, Object constraints, int index) {
        throw new UnsupportedOperationException(
                "Adding components to JLayer is not supported, use setView() or setGlassPane()");
    }

    /**
     * No children can be removed.
     *
     * @throws IllegalArgumentException if it is neither the component nor the glass.
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
     * Whether the layer draws outside its bounds.
     *
     * <p>Always true. A decoration may paint wherever it likes -- a highlight around, a shadow --,
     * so the repainting system has to ask the layer and not guess.
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
     * It draws the layer.
     *
     * <p>All the drawing goes through the look and feel. The flag keeps it from coming back in:
     * the look and feel calls {@code c.paint(g)} in order to draw what is below, and without it
     * that would come back here.
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
     * Whether the children do not overlap.
     *
     * <p>Always false: the glass is right on top of the component, by definition they overlap.
     * Answering yes would make the system draw only one of the two.
     */
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    /** It passes the layer's and its own property changes on to the look and feel. */
    public void propertyChange(PropertyChangeEvent evt) {
        LayerUI<? super V> ui = getUI();
        if (ui != null) {
            ui.applyPropertyChange(evt, this);
        }
    }

    /**
     * Which events the look and feel wants to see.
     *
     * <p>Zero, which is the factory setting, means none. See {@link LayerUI}'s note.
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

    /** Whatever the wrapped component asks for, if it is scrollable. */
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

    /** It lays the component and the glass out; the look and feel does it. */
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
