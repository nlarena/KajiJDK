package javax.swing;

import java.awt.Adjustable;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.Serializable;

import javax.accessibility.AccessibleContext;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ScrollBarUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * Una barra de desplazamiento: un {@link BoundedRangeModel} con dos botones y un pulgar.
 *
 * <h2>El modelo es la barra</h2>
 *
 * <p>Valor, extension, minimo y maximo estan en el modelo, y la barra solo los reexpone con los
 * nombres de {@link Adjustable} —{@code visibleAmount} es la extension—. El pulgar no es un objeto:
 * es el dibujo del rango {@code [valor, valor + extension]} sobre el rango
 * {@code [minimo, maximo]}, y por eso se agranda cuando se ve mas contenido.
 *
 * <h2>Dos escalones</h2>
 *
 * <p>El <em>unitario</em> es lo que avanza una flecha; el <em>de bloque</em>, lo que avanza un clic
 * en la pista, que por omision es una pantalla entera. Los dos se preguntan con una direccion,
 * porque un contenido de filas desparejas avanza distinto para arriba que para abajo; esta clase
 * devuelve siempre el mismo numero, y es {@code JScrollPane} el que pone una barra que le pregunta
 * al contenido.
 *
 * <p>Los cambios del modelo salen como {@link AdjustmentEvent} de tipo {@code TRACK}: el JDK no
 * distingue de donde vino el cambio una vez que llego al modelo.
 */
public class JScrollBar extends JComponent implements Adjustable, Accessible {

    private static final String uiClassID = "ScrollBarUI";

    /** El modelo; ver la nota de la clase. */
    protected BoundedRangeModel model;

    protected int orientation;

    protected int unitIncrement;

    protected int blockIncrement;

    private ChangeListener fwdAdjustmentEvents = new ModelListener();

    /** Un {@code switch} aca serian dos `case` con constantes de {@code Adjustable} (#503). */
    private void checkOrientation(int orientation) {
        if (orientation != VERTICAL && orientation != HORIZONTAL) {
            throw new IllegalArgumentException(
                    "orientation must be one of: VERTICAL, HORIZONTAL");
        }
    }

    /** Una barra con esa orientacion y esos cuatro numeros. */
    public JScrollBar(int orientation, int value, int extent, int min, int max) {
        checkOrientation(orientation);
        this.unitIncrement = 1;
        this.blockIncrement = (extent == 0) ? 1 : extent;
        this.orientation = orientation;
        this.model = new DefaultBoundedRangeModel(value, extent, min, max);
        this.model.addChangeListener(fwdAdjustmentEvents);
        setRequestFocusEnabled(false);
        updateUI();
    }

    /** Una barra de 0 a 100, en cero y con una extension de 10. */
    public JScrollBar(int orientation) {
        this(orientation, 0, 10, 0, 100);
    }

    /** Una barra vertical. */
    public JScrollBar() {
        this(VERTICAL);
    }

    public void setUI(ScrollBarUI ui) {
        super.setUI(ui);
    }

    public ScrollBarUI getUI() {
        return (ScrollBarUI) ui;
    }

    /** Instala el aspecto basico; ver {@code JButton#updateUI}. */
    public void updateUI() {
        setUI((ScrollBarUI) BasicScrollBarUI.createUI(this));
    }

    public String getUIClassID() {
        return uiClassID;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        checkOrientation(orientation);
        int oldValue = this.orientation;
        this.orientation = orientation;
        firePropertyChange("orientation", oldValue, orientation);

        if (orientation != oldValue) {
            revalidate();
        }
    }

    public BoundedRangeModel getModel() {
        return model;
    }

    /** Cambia el modelo, llevandose el escucha que convierte sus cambios en eventos. */
    public void setModel(BoundedRangeModel newModel) {
        BoundedRangeModel oldModel = model;
        if (model != null) {
            model.removeChangeListener(fwdAdjustmentEvents);
        }
        model = newModel;
        if (model != null) {
            model.addChangeListener(fwdAdjustmentEvents);
        }
        firePropertyChange("model", oldModel, model);
    }

    /**
     * Cuanto avanza un paso chico en esa direccion.
     *
     * <p>Siempre lo mismo; ver la nota de la clase sobre quien si mira la direccion.
     */
    public int getUnitIncrement(int direction) {
        return unitIncrement;
    }

    public void setUnitIncrement(int unitIncrement) {
        int oldValue = this.unitIncrement;
        this.unitIncrement = unitIncrement;
        firePropertyChange("unitIncrement", oldValue, unitIncrement);
    }

    /** Cuanto avanza un paso grande en esa direccion. */
    public int getBlockIncrement(int direction) {
        return blockIncrement;
    }

    public void setBlockIncrement(int blockIncrement) {
        int oldValue = this.blockIncrement;
        this.blockIncrement = blockIncrement;
        firePropertyChange("blockIncrement", oldValue, blockIncrement);
    }

    public int getUnitIncrement() {
        return unitIncrement;
    }

    public int getBlockIncrement() {
        return blockIncrement;
    }

    public int getValue() {
        return getModel().getValue();
    }

    public void setValue(int value) {
        BoundedRangeModel m = getModel();
        m.setValue(value);
    }

    /** La extension del modelo, con el nombre que le da {@link Adjustable}. */
    public int getVisibleAmount() {
        return getModel().getExtent();
    }

    public void setVisibleAmount(int extent) {
        getModel().setExtent(extent);
    }

    public int getMinimum() {
        return getModel().getMinimum();
    }

    public void setMinimum(int minimum) {
        getModel().setMinimum(minimum);
    }

    public int getMaximum() {
        return getModel().getMaximum();
    }

    public void setMaximum(int maximum) {
        getModel().setMaximum(maximum);
    }

    public boolean getValueIsAdjusting() {
        return getModel().getValueIsAdjusting();
    }

    public void setValueIsAdjusting(boolean b) {
        BoundedRangeModel m = getModel();
        m.setValueIsAdjusting(b);
    }

    /** Cambia los cuatro numeros de una vez, avisando una sola vez. */
    public void setValues(int newValue, int newExtent, int newMin, int newMax) {
        BoundedRangeModel m = getModel();
        m.setRangeProperties(newValue, newExtent, newMin, newMax, m.getValueIsAdjusting());
    }

    public void addAdjustmentListener(AdjustmentListener l) {
        listenerList.add(AdjustmentListener.class, l);
    }

    public void removeAdjustmentListener(AdjustmentListener l) {
        listenerList.remove(AdjustmentListener.class, l);
    }

    public AdjustmentListener[] getAdjustmentListeners() {
        return listenerList.getListeners(AdjustmentListener.class);
    }

    protected void fireAdjustmentValueChanged(int id, int type, int value) {
        fireAdjustmentValueChanged(id, type, value, getValueIsAdjusting());
    }

    private void fireAdjustmentValueChanged(int id, int type, int value, boolean isAdjusting) {
        Object[] listeners = listenerList.getListenerList();
        AdjustmentEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == AdjustmentListener.class) {
                if (e == null) {
                    e = new AdjustmentEvent(this, id, type, value, isAdjusting);
                }
                ((AdjustmentListener) listeners[i + 1]).adjustmentValueChanged(e);
            }
        }
    }

    /** Convierte los cambios del modelo en eventos de ajuste; nombrada y no anonima (#499). */
    private class ModelListener implements ChangeListener, Serializable {
        public void stateChanged(ChangeEvent e) {
            Object obj = e.getSource();
            if (obj instanceof BoundedRangeModel) {
                BoundedRangeModel m = (BoundedRangeModel) obj;
                fireAdjustmentValueChanged(AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
                        AdjustmentEvent.TRACK, m.getValue(), m.getValueIsAdjusting());
            }
        }
    }

    /** Rigida en su ancho y flexible en su largo: ese es todo el criterio de las tres medidas. */
    public Dimension getMinimumSize() {
        Dimension pref = getPreferredSize();
        if (orientation == VERTICAL) {
            return new Dimension(pref.width, 5);
        }
        return new Dimension(5, pref.height);
    }

    public Dimension getMaximumSize() {
        Dimension pref = getPreferredSize();
        if (getOrientation() == VERTICAL) {
            return new Dimension(pref.width, Short.MAX_VALUE);
        }
        return new Dimension(Short.MAX_VALUE, pref.height);
    }

    public void setMinimumSize(Dimension minimumSize) {
        super.setMinimumSize(minimumSize);
    }

    public void setMaximumSize(Dimension maximumSize) {
        super.setMaximumSize(maximumSize);
    }

    /** Habilita la barra y sus dos botones: una flecha viva en una barra muerta no tendria sentido. */
    public void setEnabled(boolean x) {
        super.setEnabled(x);
        Component[] children = getComponents();
        for (int i = 0; i < children.length; i++) {
            children[i].setEnabled(x);
        }
    }

    protected String paramString() {
        return super.paramString() + ",blockIncrement=" + blockIncrement + ",orientation="
                + (orientation == VERTICAL ? "VERTICAL" : "HORIZONTAL") + ",unitIncrement="
                + unitIncrement;
    }

    /** Sin contexto de accesibilidad: no hay tecnologia asistiva que lo lea en esta VM. */
    public AccessibleContext getAccessibleContext() {
        return null;
    }
}
