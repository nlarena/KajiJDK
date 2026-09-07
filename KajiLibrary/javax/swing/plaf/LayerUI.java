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
 * El aspecto de un {@link JLayer}: donde se escribe lo que se le agrega a un componente.
 *
 * <h2>Decorar sin heredar</h2>
 *
 * <p>Para agregarle algo a un componente -- un velo mientras carga, un resaltado, un contador de
 * caracteres -- la forma vieja es heredar de el. Eso obliga a una subclase por componente y no se
 * puede combinar: un campo de texto con velo y con resaltado necesitaria las dos herencias.
 *
 * <p>Aca no se hereda: se envuelve. El {@link JLayer} contiene al componente y le pide a este
 * objeto que dibuje encima y que mire los eventos. El mismo {@code LayerUI} sirve para cualquier
 * componente, y dos se apilan envolviendo dos veces.
 *
 * <h2>Los eventos hay que pedirlos</h2>
 *
 * <p>{@link #eventDispatched} no recibe nada hasta que alguien llame a
 * {@link JLayer#setLayerEventMask}. No es un olvido: mirar todos los eventos de todos los
 * componentes envueltos costaria caro, y casi ninguna decoracion los necesita.
 *
 * <p>Ademas los eventos llegan <em>antes</em> que al componente. Es lo que permite que una capa se
 * los coma -- consumiendolos -- para deshabilitar lo que envuelve sin tocarlo.
 *
 * @param <V> el tipo del componente que se envuelve.
 */
public class LayerUI<V extends Component> extends ComponentUI implements Serializable {

    private final PropertyChangeSupport propertyChangeSupport =
            new PropertyChangeSupport(this);

    public LayerUI() {
    }

    /**
     * Dibuja la capa.
     *
     * <p>Por omision dibuja lo que envuelve y nada mas. Una capa que quiera pintar encima llama
     * primero a {@code super.paint(g, c)} y despues dibuja lo suyo; una que quiera pintar debajo
     * hace al reves.
     */
    public void paint(Graphics g, JComponent c) {
        c.paint(g);
    }

    /**
     * Un evento del componente envuelto o de sus hijos.
     *
     * <p>No llega nada hasta que se pida con {@link JLayer#setLayerEventMask}; ver la nota de la
     * clase. Por omision reparte segun el tipo a los {@code process...} de abajo.
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

    /** Se llama cuando cambia el aspecto del sistema; propaga al componente envuelto. */
    public void updateUI(JLayer<? extends V> l) {
    }

    /**
     * Se engancha a la capa.
     *
     * <p>Escucha sus propiedades para poder reaccionar a que cambien la vista o la mascara de
     * eventos.
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
     * Avisa que una propiedad del aspecto cambio.
     *
     * <p>Un mismo aspecto puede estar puesto en varias capas, asi que el aviso no dice en cual
     * paso: cada capa lo recibe y decide. De ahi que la reaccion vaya en
     * {@link #applyPropertyChange}, que si sabe de cual se trata.
     */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    /** Lo llama la capa cuando el aspecto avisa un cambio; ver {@link #firePropertyChange}. */
    public void applyPropertyChange(PropertyChangeEvent e, JLayer<? extends V> l) {
    }

    public int getBaseline(JComponent c, int width, int height) {
        return -1;
    }

    public Component$BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        return Component$BaselineResizeBehavior.OTHER;
    }

    /** Acomoda lo que hay adentro de la capa. */
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
     * Repinta esa zona de la capa.
     *
     * <p>Es el gancho que permite a una capa repintarse entera cuando cambia una parte: una
     * decoracion que rodea al componente no se puede repintar de a pedazos.
     */
    public void paintImmediately(int x, int y, int width, int height, JLayer<? extends V> l) {
        l.paintImmediately(x, y, width, height);
    }

    /** Una imagen que se estaba cargando avanzo; devuelve si sigue haciendo falta el aviso. */
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h,
            JLayer<? extends V> l) {
        return l.imageUpdate(img, infoflags, x, y, w, h);
    }
}
