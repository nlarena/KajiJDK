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
 * A knob that is dragged in order to choose a number from a range.
 *
 * <h2>A range with an extent</h2>
 *
 * <p>The model is a {@link BoundedRangeModel}, the same as the scroll bars', and it brings an
 * <em>extent</em> which in a slider is almost always zero. That it is zero is not a detail:
 * with an extent of {@code e} the value cannot go past {@code maximum - e}, so an extent other
 * than zero shifts the cap without anybody having asked for it.
 *
 * <h2>The notices are forwarded, not re-emitted</h2>
 *
 * <p>The control signs itself up as a listener of its own model and turns each notice of the
 * model into a notice of its own, with the control as the source. That is why
 * {@link #createChangeListener} is protected: a subclass may change what is done with the
 * model's notice, not where it comes from.
 *
 * <h2>The labels</h2>
 *
 * <p>{@link #setLabelTable} receives a dictionary from value to component. They have to be
 * drawn separately ({@link #setPaintLabels}), and the order between the two calls does not
 * matter. What does matter is that setting labels switches the automatic spacing off: they are
 * two different ways of deciding where each tick goes.
 */
public class JSlider extends JComponent implements SwingConstants, Accessible {

    private static final String uiClassID = "SliderUI";

    private boolean paintTicks = false;
    private boolean paintTrack = true;
    private boolean paintLabels = false;
    private boolean isInverted = false;

    /** The range; see the class note. */
    protected BoundedRangeModel sliderModel;

    /** Every how much a big tick goes, or zero if there are none. */
    protected int majorTickSpacing;

    /** Every how much a small tick goes, or zero if there are none. */
    protected int minorTickSpacing;

    /** Whether the value jumps to the nearest tick. */
    protected boolean snapToTicks = false;

    boolean snapToValue = true;

    /** Horizontal or vertical. */
    protected int orientation;

    /** The bridge between the model and this control; see the class note. */
    protected ChangeListener changeListener = createChangeListener();

    /** The event, built once and reused. */
    protected transient ChangeEvent changeEvent = null;

    private Dictionary<?, ?> labelTable;

    /** From 0 to 100, starting at 50, horizontal. */
    public JSlider() {
        this(HORIZONTAL, 0, 100, 50);
    }

    /**
     * From 0 to 100, starting at 50, with that orientation.
     *
     * @throws IllegalArgumentException if the orientation is neither horizontal nor vertical.
     */
    public JSlider(int orientation) {
        this(orientation, 0, 100, 50);
    }

    /** Horizontal, in that range, starting in the middle. */
    public JSlider(int min, int max) {
        this(HORIZONTAL, min, max, (min + max) / 2);
    }

    /** Horizontal, in that range, starting at that value. */
    public JSlider(int min, int max, int value) {
        this(HORIZONTAL, min, max, value);
    }

    /**
     * Everything set by hand.
     *
     * @throws IllegalArgumentException if the orientation is neither horizontal nor vertical.
     */
    public JSlider(int orientation, int min, int max, int value) {
        checkOrientation(orientation);
        this.orientation = orientation;
        setModel(new DefaultBoundedRangeModel(value, 0, min, max));
        updateUI();
    }

    /** With that model, horizontal. */
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
     * It asks for the look and feel again.
     *
     * <p>It also tells the labels: they are components of its own that the control does not
     * repaint by itself.
     */
    public void updateUI() {
        updateLabelUIs();
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** The bridge between the model and this control; see the class note. */
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

    /** It hands out a change notice with this control as the source. */
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
     * It changes the range.
     *
     * <p>The listener moves from the old model to the new one: leaving it set would make the
     * control go on reacting to a range it no longer shows.
     *
     * <p><strong>It accepts null</strong>, and this is measured: the JDK does not validate, it
     * keeps the null and gives notice of the change. What happens afterwards is that almost
     * everything else in the control blows up on asking the model for the value. It is copied all
     * the same, because rejecting it here would change which call the error appears in and that is
     * precisely the kind of difference that is paid for dearly.
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

    /** Whether the user is in the middle of a drag. */
    public boolean getValueIsAdjusting() {
        return getModel().getValueIsAdjusting();
    }

    /**
     * It marks that the value is changing.
     *
     * <p>It serves in order not to recompute at every pixel of the drag: whoever listens may wait
     * for it to go back to false.
     */
    public void setValueIsAdjusting(boolean b) {
        BoundedRangeModel m = getModel();
        m.setValueIsAdjusting(b);
        // There is no property notice: the only one that comes out is the model's, forwarded as a
                // change of state. The JDK gives notice through accessibility and nothing else, and
                // it is measured.
    }

    /** The extent; see the class note. */
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
     * Horizontal or vertical.
     *
     * @throws IllegalArgumentException if it is not one of the two.
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

    /** It changes the typeface, and with it the labels'. */
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

    /** The labels, or null if there are none; see the class note. */
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

    /** It asks the look and feel for the labels again. */
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

    /** Labels every so many, starting at the minimum. */
    public Hashtable<Integer, JComponent> createStandardLabels(int increment) {
        return createStandardLabels(increment, getMinimum());
    }

    /**
     * Labels every so many, starting at that value.
     *
     * @throws IllegalArgumentException if the step is not positive or the start falls outside the
     *     range.
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

    /** Whether the minimum goes on the side the maximum normally takes. */
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

    /** Every how much a big tick goes; zero switches them off. */
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

    /** Whether the value jumps to the nearest tick on releasing. */
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

    /** Whether the track is drawn; switching it off leaves only the knob and the ticks. */
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
     * Whether the labels are drawn.
     *
     * <p>Switching it on with no labels set and with big ticks defined builds them by itself,
     * which is what makes the common case a single call.
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

    /** It turns the model's notice into one of the control's; see the class note. */
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
