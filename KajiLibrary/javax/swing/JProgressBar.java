package javax.swing;

import java.awt.Graphics;
import java.text.Format;
import java.text.NumberFormat;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ProgressBarUI;

/**
 * Una barra que muestra cuanto falta.
 *
 * <h2>Determinada e indeterminada</h2>
 *
 * <p>La barra normal muestra una proporcion: hace falta saber cuanto es el total. Cuando no se
 * sabe -- se esta esperando una respuesta, se esta leyendo algo de largo desconocido --
 * {@link #setIndeterminate} la pasa a un rebote continuo, que dice "sigo trabajando" sin decir
 * cuanto falta. Es una decision honesta: una barra que avanza inventando el total miente.
 *
 * <h2>El texto</h2>
 *
 * <p>{@link #getString} devuelve el porcentaje si nadie puso otra cosa, y lo que se haya puesto si
 * se puso. Que el texto se dibuje o no es aparte ({@link #setStringPainted}), asi que se puede
 * dejar preparado y prenderlo despues.
 *
 * <h2>El modelo es el mismo del deslizante</h2>
 *
 * <p>Un {@link BoundedRangeModel}, con extension cero. {@link #getPercentComplete} es la unica
 * cuenta propia: la posicion dentro del rango, entre cero y uno. Con el rango vacio da
 * {@code NaN}, que es lo que el JDK devuelve y lo que la division dice.
 */
public class JProgressBar extends JComponent implements SwingConstants, Accessible {

    private static final String uiClassID = "ProgressBarUI";

    /** Horizontal o vertical. */
    protected int orientation;

    /** Si se dibuja el borde. */
    protected boolean paintBorder;

    /** El rango. */
    protected BoundedRangeModel model;

    /** El texto puesto a mano, o nulo para el porcentaje. */
    protected String progressString;

    /** Si el texto se dibuja. */
    protected boolean paintString;

    /** El evento, armado una vez y reusado. */
    protected transient ChangeEvent changeEvent = null;

    /** El puente entre el modelo y esta barra. */
    protected ChangeListener changeListener;

    private transient Format format;
    private boolean indeterminate;

    /** De 0 a 100, horizontal. */
    public JProgressBar() {
        this(HORIZONTAL, 0, 100);
    }

    /**
     * De 0 a 100, con esa orientacion.
     *
     * @throws IllegalArgumentException si la orientacion no es horizontal ni vertical.
     */
    public JProgressBar(int orient) {
        this(orient, 0, 100);
    }

    /** Horizontal, en ese rango. */
    public JProgressBar(int min, int max) {
        this(HORIZONTAL, min, max);
    }

    /**
     * Con orientacion y rango.
     *
     * @throws IllegalArgumentException si la orientacion no es horizontal ni vertical.
     */
    public JProgressBar(int orient, int min, int max) {
        model = new DefaultBoundedRangeModel(min, 0, min, max);
        changeListener = createChangeListener();
        model.addChangeListener(changeListener);
        updateUI();
        setOrientation(orient);
        setBorderPainted(true);
        setStringPainted(false);
        setString(null);
        setIndeterminate(false);
    }

    /**
     * Con ese modelo, horizontal.
     *
     * @throws NullPointerException si el modelo es nulo.
     */
    public JProgressBar(BoundedRangeModel newModel) {
        model = newModel;
        changeListener = createChangeListener();
        model.addChangeListener(changeListener);
        updateUI();
        setOrientation(HORIZONTAL);
        setBorderPainted(true);
        setStringPainted(false);
        setString(null);
        setIndeterminate(false);
    }

    public int getOrientation() {
        return orientation;
    }

    /**
     * Horizontal o vertical.
     *
     * @throws IllegalArgumentException si no es una de las dos.
     */
    public void setOrientation(int newOrientation) {
        if (orientation != newOrientation) {
            if (newOrientation != VERTICAL && newOrientation != HORIZONTAL) {
                throw new IllegalArgumentException(newOrientation
                        + " is not a legal orientation");
            }
            int oldOrient = orientation;
            orientation = newOrientation;
            firePropertyChange("orientation", oldOrient, newOrientation);
            revalidate();
        }
    }

    public boolean isStringPainted() {
        return paintString;
    }

    public void setStringPainted(boolean b) {
        boolean oldValue = paintString;
        paintString = b;
        firePropertyChange("stringPainted", oldValue, paintString);
        if (paintString != oldValue) {
            revalidate();
            repaint();
        }
    }

    /**
     * El texto: el que se haya puesto, y si no el porcentaje.
     *
     * <p>El porcentaje sale del formato de porcentajes del idioma, asi que un rango vacio -- cuya
     * proporcion es {@code NaN} -- se escribe como lo escriba ese formato, no como un cero.
     */
    public String getString() {
        if (progressString != null) {
            return progressString;
        }
        if (format == null) {
            format = NumberFormat.getPercentInstance();
        }
        return format.format(Double.valueOf(getPercentComplete()));
    }

    public void setString(String s) {
        String oldValue = progressString;
        progressString = s;
        firePropertyChange("string", oldValue, progressString);
        if (progressString == null || oldValue == null || !progressString.equals(oldValue)) {
            repaint();
        }
    }

    /**
     * Que parte del rango se lleva el valor, de cero a uno.
     *
     * <p><strong>Un rango vacio da {@code NaN}</strong>, no cero: la cuenta es una division y el JDK
     * no la protege. Esta medido. Devolver cero seria mas comodo y diria que la barra esta al
     * principio, que no es lo mismo que decir que la pregunta no tiene respuesta.
     */
    public double getPercentComplete() {
        long span = model.getMaximum() - model.getMinimum();
        double currentValue = model.getValue();
        return (currentValue - model.getMinimum()) / span;
    }

    public boolean isBorderPainted() {
        return paintBorder;
    }

    public void setBorderPainted(boolean b) {
        boolean oldValue = paintBorder;
        paintBorder = b;
        firePropertyChange("borderPainted", oldValue, paintBorder);
        if (paintBorder != oldValue) {
            repaint();
        }
    }

    /** Dibuja el borde solo si esta prendido. */
    protected void paintBorder(Graphics g) {
        if (isBorderPainted()) {
            super.paintBorder(g);
        }
    }

    public ProgressBarUI getUI() {
        return (ProgressBarUI) ui;
    }

    public void setUI(ProgressBarUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** El puente entre el modelo y esta barra. */
    protected ChangeListener createChangeListener() {
        return new ModelListener(this);
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

    /** Reparte un aviso de cambio con esta barra como origen. */
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

    public BoundedRangeModel getModel() {
        return model;
    }

    /** Cambia el rango; el oyente se muda del modelo viejo al nuevo. */
    public void setModel(BoundedRangeModel newModel) {
        BoundedRangeModel oldModel = getModel();
        if (newModel != oldModel) {
            if (oldModel != null) {
                oldModel.removeChangeListener(changeListener);
                changeListener = null;
            }
            model = newModel;
            if (newModel != null) {
                changeListener = createChangeListener();
                newModel.addChangeListener(changeListener);
            }
            if (accessibleContext != null) {
                accessibleContext.firePropertyChange("AccessibleValue", oldModel, newModel);
            }
            if (model != null) {
                model.setExtent(0);
            }
            repaint();
        }
    }

    public int getValue() {
        return getModel().getValue();
    }

    public int getMinimum() {
        return getModel().getMinimum();
    }

    public int getMaximum() {
        return getModel().getMaximum();
    }

    public void setValue(int n) {
        BoundedRangeModel brm = getModel();
        int oldValue = brm.getValue();
        brm.setValue(n);
        if (accessibleContext != null) {
            accessibleContext.firePropertyChange("AccessibleValue", Integer.valueOf(oldValue),
                    Integer.valueOf(brm.getValue()));
        }
    }

    public void setMinimum(int n) {
        getModel().setMinimum(n);
    }

    public void setMaximum(int n) {
        getModel().setMaximum(n);
    }

    /** Pasa a la barra que rebota; ver la nota de la clase. */
    public void setIndeterminate(boolean newValue) {
        boolean oldValue = indeterminate;
        indeterminate = newValue;
        firePropertyChange("indeterminate", oldValue, indeterminate);
    }

    public boolean isIndeterminate() {
        return indeterminate;
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    /** Convierte el aviso del modelo en uno de la barra. */
    private static class ModelListener implements ChangeListener, java.io.Serializable {

        private final JProgressBar barra;

        ModelListener(JProgressBar barra) {
            this.barra = barra;
        }

        public void stateChanged(ChangeEvent e) {
            barra.fireStateChanged();
        }
    }
}
