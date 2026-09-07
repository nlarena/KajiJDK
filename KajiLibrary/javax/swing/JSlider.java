package javax.swing;

import java.awt.Font;
import java.awt.Image;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.SliderUI;

/**
 * Una perilla que se arrastra para elegir un numero de un rango.
 *
 * <h2>Un rango con extension</h2>
 *
 * <p>El modelo es un {@link BoundedRangeModel}, el mismo de las barras de desplazamiento, y trae
 * una <em>extension</em> que en un control deslizante casi siempre es cero. Que sea cero no es un
 * detalle: con extension {@code e} el valor no puede pasar de {@code maximo - e}, asi que una
 * extension distinta de cero corre el tope sin que nadie lo haya pedido.
 *
 * <h2>Los avisos se reenvian, no se reemiten</h2>
 *
 * <p>El control se anota como oyente de su propio modelo y convierte cada aviso del modelo en un
 * aviso propio, con el control como origen. Por eso {@link #createChangeListener} es protegido: una
 * subclase puede cambiar que se hace con el aviso del modelo, no de donde viene.
 *
 * <h2>Las etiquetas</h2>
 *
 * <p>{@link #setLabelTable} recibe un diccionario de valor a componente. Hay que dibujarlas aparte
 * ({@link #setPaintLabels}), y el orden entre las dos llamadas no importa. Lo que si importa es que
 * poner etiquetas apaga el espaciado automatico: son dos formas distintas de decidir donde va cada
 * marca.
 */
public class JSlider extends JComponent implements SwingConstants, Accessible {

    private static final String uiClassID = "SliderUI";

    private boolean paintTicks = false;
    private boolean paintTrack = true;
    private boolean paintLabels = false;
    private boolean isInverted = false;

    /** El rango; ver la nota de la clase. */
    protected BoundedRangeModel sliderModel;

    /** Cada cuanto va una marca grande, o cero si no hay. */
    protected int majorTickSpacing;

    /** Cada cuanto va una marca chica, o cero si no hay. */
    protected int minorTickSpacing;

    /** Si el valor salta a la marca mas cercana. */
    protected boolean snapToTicks = false;

    boolean snapToValue = true;

    /** Horizontal o vertical. */
    protected int orientation;

    /** El puente entre el modelo y este control; ver la nota de la clase. */
    protected ChangeListener changeListener = createChangeListener();

    /** El evento, armado una vez y reusado. */
    protected transient ChangeEvent changeEvent = null;

    private Dictionary<?, ?> labelTable;

    /** De 0 a 100, arrancando en 50, horizontal. */
    public JSlider() {
        this(HORIZONTAL, 0, 100, 50);
    }

    /**
     * De 0 a 100, arrancando en 50, con esa orientacion.
     *
     * @throws IllegalArgumentException si la orientacion no es horizontal ni vertical.
     */
    public JSlider(int orientation) {
        this(orientation, 0, 100, 50);
    }

    /** Horizontal, en ese rango, arrancando en el medio. */
    public JSlider(int min, int max) {
        this(HORIZONTAL, min, max, (min + max) / 2);
    }

    /** Horizontal, en ese rango, arrancando en ese valor. */
    public JSlider(int min, int max, int value) {
        this(HORIZONTAL, min, max, value);
    }

    /**
     * Todo puesto a mano.
     *
     * @throws IllegalArgumentException si la orientacion no es horizontal ni vertical.
     */
    public JSlider(int orientation, int min, int max, int value) {
        checkOrientation(orientation);
        this.orientation = orientation;
        setModel(new DefaultBoundedRangeModel(value, 0, min, max));
        updateUI();
    }

    /** Con ese modelo, horizontal. */
    public JSlider(BoundedRangeModel brm) {
        this.orientation = JSlider.HORIZONTAL;
        setModel(brm);
        updateUI();
    }

    public SliderUI getUI() {
        return (SliderUI) ui;
    }

    public void setUI(SliderUI ui) {
        super.setUI(ui);
    }

    /**
     * Vuelve a pedir el aspecto.
     *
     * <p>Tambien les avisa a las etiquetas: son componentes propios que el control no repinta solo.
     */
    public void updateUI() {
        updateLabelUIs();
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** El puente entre el modelo y este control; ver la nota de la clase. */
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

    /** Reparte un aviso de cambio con este control como origen. */
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
        return sliderModel;
    }

    /**
     * Cambia el rango.
     *
     * <p>El oyente se muda del modelo viejo al nuevo: dejarlo puesto haria que el control siguiera
     * reaccionando a un rango que ya no muestra.
     *
     * <p><strong>Acepta nulo</strong>, y esto esta medido: el JDK no valida, guarda el nulo y avisa
     * el cambio. Lo que pasa despues es que casi todo lo demas del control revienta al preguntarle
     * el valor al modelo. Se copia igual, porque rechazarlo aca cambiaria en que llamada aparece el
     * error y esa es justamente la clase de diferencia que se paga cara.
     */
    public void setModel(BoundedRangeModel newModel) {
        BoundedRangeModel oldModel = getModel();
        if (oldModel != null) {
            oldModel.removeChangeListener(changeListener);
        }
        sliderModel = newModel;
        if (newModel != null) {
            newModel.addChangeListener(changeListener);
        }
        firePropertyChange("model", oldModel, sliderModel);
    }

    public int getValue() {
        return getModel().getValue();
    }

    public void setValue(int n) {
        BoundedRangeModel m = getModel();
        int oldValue = m.getValue();
        if (oldValue == n) {
            return;
        }
        m.setValue(n);
    }

    public int getMinimum() {
        return getModel().getMinimum();
    }

    public void setMinimum(int minimum) {
        int oldMin = getModel().getMinimum();
        getModel().setMinimum(minimum);
        firePropertyChange("minimum", Integer.valueOf(oldMin), Integer.valueOf(minimum));
    }

    public int getMaximum() {
        return getModel().getMaximum();
    }

    public void setMaximum(int maximum) {
        int oldMax = getModel().getMaximum();
        getModel().setMaximum(maximum);
        firePropertyChange("maximum", Integer.valueOf(oldMax), Integer.valueOf(maximum));
    }

    /** Si el usuario esta en medio de un arrastre. */
    public boolean getValueIsAdjusting() {
        return getModel().getValueIsAdjusting();
    }

    /**
     * Marca que el valor esta cambiando.
     *
     * <p>Sirve para no recalcular en cada pixel del arrastre: quien escucha puede esperar a que
     * vuelva a falso.
     */
    public void setValueIsAdjusting(boolean b) {
        BoundedRangeModel m = getModel();
        m.setValueIsAdjusting(b);
        // No hay aviso de propiedad: el unico que sale es el del modelo, reenviado como cambio de
        // estado. El JDK avisa por accesibilidad y nada mas, y esta medido.
    }

    /** La extension; ver la nota de la clase. */
    public int getExtent() {
        return getModel().getExtent();
    }

    public void setExtent(int extent) {
        getModel().setExtent(extent);
    }

    public int getOrientation() {
        return orientation;
    }

    /**
     * Horizontal o vertical.
     *
     * @throws IllegalArgumentException si no es una de las dos.
     */
    public void setOrientation(int orientation) {
        checkOrientation(orientation);
        int oldValue = this.orientation;
        this.orientation = orientation;
        firePropertyChange("orientation", oldValue, orientation);
        if (orientation != oldValue) {
            revalidate();
        }
    }

    private void checkOrientation(int orientation) {
        if (orientation != VERTICAL && orientation != HORIZONTAL) {
            throw new IllegalArgumentException("orientation must be one of: VERTICAL, HORIZONTAL");
        }
    }

    /** Cambia la tipografia, y con ella la de las etiquetas. */
    public void setFont(Font font) {
        super.setFont(font);
        updateLabelSizes();
    }

    public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) {
        if (!isShowing()) {
            return false;
        }
        return super.imageUpdate(img, infoflags, x, y, w, h);
    }

    /** Las etiquetas, o nulo si no hay; ver la nota de la clase. */
    public Dictionary<?, ?> getLabelTable() {
        return labelTable;
    }

    public void setLabelTable(Dictionary<?, ?> labels) {
        Dictionary<?, ?> oldTable = labelTable;
        labelTable = labels;
        updateLabelUIs();
        firePropertyChange("labelTable", oldTable, labelTable);
        if (labels != oldTable) {
            revalidate();
            repaint();
        }
    }

    /** Les vuelve a pedir el aspecto a las etiquetas. */
    protected void updateLabelUIs() {
        Dictionary<?, ?> labelTable = getLabelTable();
        if (labelTable == null) {
            return;
        }
        Enumeration<?> labels = labelTable.keys();
        while (labels.hasMoreElements()) {
            JComponent component = (JComponent) labelTable.get(labels.nextElement());
            component.updateUI();
            component.setSize(component.getPreferredSize());
        }
    }

    private void updateLabelSizes() {
        Dictionary<?, ?> labelTable = getLabelTable();
        if (labelTable == null) {
            return;
        }
        Enumeration<?> labels = labelTable.keys();
        while (labels.hasMoreElements()) {
            JComponent component = (JComponent) labelTable.get(labels.nextElement());
            component.setSize(component.getPreferredSize());
        }
    }

    /** Etiquetas cada tantos, arrancando en el minimo. */
    public Hashtable<Integer, JComponent> createStandardLabels(int increment) {
        return createStandardLabels(increment, getMinimum());
    }

    /**
     * Etiquetas cada tantos, arrancando en ese valor.
     *
     * @throws IllegalArgumentException si el paso no es positivo o el arranque cae fuera del
     *     rango.
     */
    public Hashtable<Integer, JComponent> createStandardLabels(int increment, int start) {
        if (start > getMaximum() || start < getMinimum()) {
            throw new IllegalArgumentException("Slider label start point out of range.");
        }
        if (increment <= 0) {
            throw new IllegalArgumentException("Label increment must be > 0");
        }
        Hashtable<Integer, JComponent> table = new Hashtable<Integer, JComponent>();
        for (int labelIndex = start; labelIndex <= getMaximum(); labelIndex += increment) {
            JLabel label = new JLabel(String.valueOf(labelIndex));
            label.setSize(label.getPreferredSize());
            table.put(Integer.valueOf(labelIndex), label);
        }
        return table;
    }

    /** Si el minimo va del lado que normalmente ocupa el maximo. */
    public boolean getInverted() {
        return isInverted;
    }

    public void setInverted(boolean b) {
        boolean oldValue = isInverted;
        isInverted = b;
        firePropertyChange("inverted", oldValue, isInverted);
        if (b != oldValue) {
            repaint();
        }
    }

    /** Cada cuanto va una marca grande; cero apaga. */
    public int getMajorTickSpacing() {
        return majorTickSpacing;
    }

    public void setMajorTickSpacing(int n) {
        int oldValue = majorTickSpacing;
        majorTickSpacing = n;
        if (labelTable == null && getMajorTickSpacing() > 0 && getPaintLabels()) {
            setLabelTable(createStandardLabels(getMajorTickSpacing()));
        }
        firePropertyChange("majorTickSpacing", oldValue, majorTickSpacing);
        if (majorTickSpacing != oldValue && getPaintTicks()) {
            repaint();
        }
    }

    public int getMinorTickSpacing() {
        return minorTickSpacing;
    }

    public void setMinorTickSpacing(int n) {
        int oldValue = minorTickSpacing;
        minorTickSpacing = n;
        firePropertyChange("minorTickSpacing", oldValue, minorTickSpacing);
        if (minorTickSpacing != oldValue && getPaintTicks()) {
            repaint();
        }
    }

    /** Si el valor salta a la marca mas cercana al soltar. */
    public boolean getSnapToTicks() {
        return snapToTicks;
    }

    boolean getSnapToValue() {
        return snapToValue;
    }

    public void setSnapToTicks(boolean b) {
        boolean oldValue = snapToTicks;
        snapToTicks = b;
        firePropertyChange("snapToTicks", oldValue, snapToTicks);
    }

    void setSnapToValue(boolean b) {
        boolean oldValue = snapToValue;
        snapToValue = b;
        firePropertyChange("snapToValue", oldValue, snapToValue);
    }

    public boolean getPaintTicks() {
        return paintTicks;
    }

    public void setPaintTicks(boolean b) {
        boolean oldValue = paintTicks;
        paintTicks = b;
        firePropertyChange("paintTicks", oldValue, paintTicks);
        if (paintTicks != oldValue) {
            revalidate();
            repaint();
        }
    }

    /** Si se dibuja el riel; apagarlo deja solo la perilla y las marcas. */
    public boolean getPaintTrack() {
        return paintTrack;
    }

    public void setPaintTrack(boolean b) {
        boolean oldValue = paintTrack;
        paintTrack = b;
        firePropertyChange("paintTrack", oldValue, paintTrack);
        if (paintTrack != oldValue) {
            repaint();
        }
    }

    /**
     * Si se dibujan las etiquetas.
     *
     * <p>Prenderlo sin etiquetas puestas y con marcas grandes definidas las arma solo, que es lo
     * que hace que el caso comun sea una sola llamada.
     */
    public boolean getPaintLabels() {
        return paintLabels;
    }

    public void setPaintLabels(boolean b) {
        boolean oldValue = paintLabels;
        paintLabels = b;
        if (labelTable == null && getMajorTickSpacing() > 0) {
            setLabelTable(createStandardLabels(getMajorTickSpacing()));
        }
        firePropertyChange("paintLabels", oldValue, paintLabels);
        if (paintLabels != oldValue) {
            revalidate();
            repaint();
        }
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    /** Convierte el aviso del modelo en uno del control; ver la nota de la clase. */
    private static class ModelListener implements ChangeListener, java.io.Serializable {

        private final JSlider control;

        ModelListener(JSlider control) {
            this.control = control;
        }

        public void stateChanged(ChangeEvent e) {
            control.fireStateChanged();
        }
    }
}
