package javax.swing.plaf;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Component$BaselineResizeBehavior;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;

import javax.swing.JComponent;
import javax.swing.JLayer;

/**
 * A {@link JLayer}'s look and feel: where what is added to a component is written.
 *
 * <h2>Decorating without inheriting</h2>
 *
 * <p>To add something to a component -- a veil while it loads, a highlight, a character counter
 * -- the old way is to inherit from it. That forces one subclass per component and cannot be
 * combined: a text field with a veil and a highlight would need both inheritances.
 *
 * <p>Here nothing is inherited: it is wrapped. The {@link JLayer} contains the component and
 * asks this object to draw on top and to watch the events. The same {@code LayerUI} serves for
 * any component, and two are stacked by wrapping twice.
 *
 * <h2>The events have to be asked for</h2>
 *
 * <p>{@link #eventDispatched} receives nothing until somebody calls
 * {@link JLayer#setLayerEventMask}. It is not an oversight: watching every event of every
 * wrapped component would be expensive, and almost no decoration needs them.
 *
 * <p>Besides, the events arrive <em>before</em> the component gets them. That is what allows a
 * layer to eat them -- by consuming them -- so as to disable what it wraps without touching it.
 *
 * @param <V> the type of the component that is wrapped.
 */
public class LayerUI<V extends Component> extends ComponentUI implements Serializable {

    private final PropertyChangeSupport propertyChangeSupport =
            new PropertyChangeSupport(this);

    public LayerUI() {
    }

    /**
     * Draws the layer.
     *
     * <p>By default it draws what it wraps and nothing else. A layer that wants to paint on top
     * calls {@code super.paint(g, c)} first and then draws its own; one that wants to paint
     * underneath does the reverse.
     */
    public void paint(Graphics g, JComponent c) {
        c.paint(g);
    }

    /**
     * An event of the wrapped component or of its children.
     *
     * <p>Nothing arrives until it is asked for with {@link JLayer#setLayerEventMask}; see the class
     * note. By default it hands out by type to the {@code process...} methods below.
     */
    public void eventDispatched(AWTEvent e, JLayer<? extends V> l) {
        if (e instanceof FocusEvent) {
            processFocusEvent((FocusEvent) e, l);
        } else if (e instanceof MouseWheelEvent) {
            processMouseWheelEvent((MouseWheelEvent) e, l);
        } else if (e instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) e;
            if (me.getID() == MouseEvent.MOUSE_MOVED || me.getID() == MouseEvent.MOUSE_DRAGGED) {
                processMouseMotionEvent(me, l);
            } else {
                processMouseEvent(me, l);
            }
        } else if (e instanceof KeyEvent) {
            processKeyEvent((KeyEvent) e, l);
        } else if (e instanceof ComponentEvent) {
            processComponentEvent((ComponentEvent) e, l);
        } else if (e instanceof InputMethodEvent) {
            processInputMethodEvent((InputMethodEvent) e, l);
        } else if (e instanceof HierarchyEvent) {
            HierarchyEvent he = (HierarchyEvent) e;
            if (he.getID() == HierarchyEvent.ANCESTOR_MOVED
                    || he.getID() == HierarchyEvent.ANCESTOR_RESIZED) {
                processHierarchyBoundsEvent(he, l);
            } else {
                processHierarchyEvent(he, l);
            }
        }
    }

    protected void processComponentEvent(ComponentEvent e, JLayer<? extends V> l) {
    }

    protected void processFocusEvent(FocusEvent e, JLayer<? extends V> l) {
    }

    protected void processKeyEvent(KeyEvent e, JLayer<? extends V> l) {
    }

    protected void processMouseEvent(MouseEvent e, JLayer<? extends V> l) {
    }

    protected void processMouseMotionEvent(MouseEvent e, JLayer<? extends V> l) {
    }

    protected void processMouseWheelEvent(MouseWheelEvent e, JLayer<? extends V> l) {
    }

    protected void processInputMethodEvent(InputMethodEvent e, JLayer<? extends V> l) {
    }

    protected void processHierarchyEvent(HierarchyEvent e, JLayer<? extends V> l) {
    }

    protected void processHierarchyBoundsEvent(HierarchyEvent e, JLayer<? extends V> l) {
    }

    /**
     * It is called when the system's look and feel changes; it propagates to the wrapped component.
     */
    public void updateUI(JLayer<? extends V> l) {
    }

    /**
     * Hooks itself to the layer.
     *
     * <p>It listens to its properties so as to be able to react to the view or the event mask
     * being changed.
     */
    public void installUI(JComponent c) {
        addPropertyChangeListener((JLayer<?>) c);
    }

    public void uninstallUI(JComponent c) {
        removePropertyChangeListener((JLayer<?>) c);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        return propertyChangeSupport.getPropertyChangeListeners();
    }

    public void addPropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }

    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return propertyChangeSupport.getPropertyChangeListeners(propertyName);
    }

    /**
     * Reports that a property of the look and feel changed.
     *
     * <p>One same look and feel may be installed on several layers, so the notice does not say in
     * which one it happened: each layer receives it and decides. Hence the reaction goes in
     * {@link #applyPropertyChange}, which does know which one it is about.
     */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * The layer calls it when the look and feel reports a change; see {@link #firePropertyChange}.
     */
    public void applyPropertyChange(PropertyChangeEvent e, JLayer<? extends V> l) {
    }

    public int getBaseline(JComponent c, int width, int height) {
        return -1;
    }

    public Component$BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        return Component$BaselineResizeBehavior.OTHER;
    }

    /** Arranges what is inside the layer. */
    public void doLayout(JLayer<? extends V> l) {
        Component view = l.getView();
        if (view != null) {
            java.awt.Insets insets = l.getInsets();
            view.setBounds(insets.left, insets.top,
                    l.getWidth() - insets.left - insets.right,
                    l.getHeight() - insets.top - insets.bottom);
        }
        java.awt.Component glassPane = l.getGlassPane();
        if (glassPane != null) {
            glassPane.setBounds(0, 0, l.getWidth(), l.getHeight());
        }
    }

    public Dimension getPreferredSize(JComponent c) {
        Component view = ((JLayer<?>) c).getView();
        if (view != null) {
            return view.getPreferredSize();
        }
        return c.getSize();
    }

    public Dimension getMinimumSize(JComponent c) {
        Component view = ((JLayer<?>) c).getView();
        if (view != null) {
            return view.getMinimumSize();
        }
        return c.getSize();
    }

    public Dimension getMaximumSize(JComponent c) {
        Component view = ((JLayer<?>) c).getView();
        if (view != null) {
            return view.getMaximumSize();
        }
        return c.getSize();
    }

    /**
     * Repaints that area of the layer.
     *
     * <p>It is the hook that lets a layer repaint itself whole when a part changes: a decoration
     * that surrounds the component cannot be repainted piecemeal.
     */
    public void paintImmediately(int x, int y, int width, int height, JLayer<? extends V> l) {
        l.paintImmediately(x, y, width, height);
    }

    /** An image that was loading made progress; it returns whether the notice is still needed. */
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h,
            JLayer<? extends V> l) {
        return l.imageUpdate(img, infoflags, x, y, w, h);
    }
}
