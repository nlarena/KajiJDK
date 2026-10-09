package javax.swing;

import java.awt.Adjustable;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.Serializable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ScrollBarUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * A scroll bar: a {@link BoundedRangeModel} with two buttons and a thumb.
 *
 * <h2>The model is the bar</h2>
 *
 * <p>Value, extent, minimum and maximum are in the model, and the bar only re-exposes them with
 * {@link Adjustable}'s names -- {@code visibleAmount} is the extent --. The thumb is not an
 * object: it is the drawing of the range {@code [value, value + extent]} over the range
 * {@code [minimum, maximum]}, and that is why it grows when more content is seen.
 *
 * <h2>Two steps</h2>
 *
 * <p>The <em>unit</em> one is what an arrow advances; the <em>block</em> one, what a click on
 * the track advances, which by default is a whole screenful. Both are asked for with a
 * direction, because a content of uneven rows advances differently upwards and downwards; this
 * class always returns the same number, and it is {@code JScrollPane} that puts in a bar that
 * asks the content.
 *
 * <p>The model's changes come out as an {@link AdjustmentEvent} of type {@code TRACK}: the JDK
 * does not tell where the change came from once it has reached the model.
 */
public class JScrollBar extends JComponent implements Adjustable, Accessible {

    private static final String uiClassID = "ScrollBarUI";

    /** The model; see the class note. */
    protected BoundedRangeModel model;

    protected int orientation;

    protected int unitIncrement;

    protected int blockIncrement;

    private ChangeListener fwdAdjustmentEvents = new ModelListener();

    /** A {@code switch} here would be two `case`s with {@code Adjustable}'s constants (#503). */
    private void checkOrientation(int orientation) {
        if (orientation != VERTICAL && orientation != HORIZONTAL) {
            throw new IllegalArgumentException(
                    "orientation must be one of: VERTICAL, HORIZONTAL");
        }
    }

    /** A bar with that orientation and those four numbers. */
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

    /** A bar from 0 to 100, at zero and with an extent of 10. */
    public JScrollBar(int orientation) {
        this(orientation, 0, 10, 0, 100);
    }

    /** A vertical bar. */
    public JScrollBar() {
        this(VERTICAL);
    }

    public void setUI(ScrollBarUI ui) {
        super.setUI(ui);
    }

    public ScrollBarUI getUI() {
        return (ScrollBarUI) ui;
    }

    /** It installs the basic look and feel; see {@code JButton#updateUI}. */
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

    /** It changes the model, taking along the listener that turns its changes into events. */
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
     * How much a small step advances in that direction.
     *
     * <p>Always the same; see the class note about who does look at the direction.
     */
    public int getUnitIncrement(int direction) {
        return unitIncrement;
    }

    public void setUnitIncrement(int unitIncrement) {
        int oldValue = this.unitIncrement;
        this.unitIncrement = unitIncrement;
        firePropertyChange("unitIncrement", oldValue, unitIncrement);
    }

    /** How much a big step advances in that direction. */
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

    /** The model's extent, with the name {@link Adjustable} gives it. */
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

    /** It changes the four numbers at once, giving notice only once. */
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

    /** It turns the model's changes into adjustment events; named and not anonymous (#499). */
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

    /**
     * Rigid in its width and flexible in its length: that is the whole criterion of the three
     * measurements.
     */
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

    /** It enables the bar and its two buttons: a live arrow on a dead bar would make no sense. */
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

    /** With no accessibility context: there is no assistive technology that reads it on this VM. */
    public AccessibleContext getAccessibleContext() {
        return null;
    }
}
