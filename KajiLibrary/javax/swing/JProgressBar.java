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
 * A bar that shows how much is left.
 *
 * <h2>Determinate and indeterminate</h2>
 *
 * <p>The ordinary bar shows a proportion: the total has to be known. When it is not -- an answer
 * is being waited for, something of unknown length is being read --
 * {@link #setIndeterminate} turns it into a continuous bounce, which says "I am still working"
 * without saying how much is left. It is an honest decision: a bar that advances by inventing
 * the total lies.
 *
 * <h2>The text</h2>
 *
 * <p>{@link #getString} returns the percentage if nobody set anything else, and whatever was
 * set if it was set. Whether the text is drawn or not is separate
 * ({@link #setStringPainted}), so it can be left ready and switched on afterwards.
 *
 * <h2>The model is the slider's</h2>
 *
 * <p>A {@link BoundedRangeModel}, with an extent of zero. {@link #getPercentComplete} is the
 * only piece of arithmetic of its own: the position within the range, between zero and one.
 * With the range empty it gives {@code NaN}, which is what the JDK returns and what the
 * division says.
 */
public class JProgressBar extends JComponent implements SwingConstants, Accessible {

    private static final String uiClassID = "ProgressBarUI";

    /** Horizontal or vertical. */
    protected int orientation;

    /** Whether the border is drawn. */
    protected boolean paintBorder;

    /** The range. */
    protected BoundedRangeModel model;

    /** The text set by hand, or null for the percentage. */
    protected String progressString;

    /** Whether the text is drawn. */
    protected boolean paintString;

    /** The event, built once and reused. */
    protected transient ChangeEvent changeEvent = null;

    /** The bridge between the model and this bar. */
    protected ChangeListener changeListener;

    private transient Format format;
    private boolean indeterminate;

    /** From 0 to 100, horizontal. */
    public JProgressBar() {
        this(HORIZONTAL, 0, 100);
    }

    /**
     * From 0 to 100, with that orientation.
     *
     * @throws IllegalArgumentException if the orientation is neither horizontal nor vertical.
     */
    public JProgressBar(int orient) {
        this(orient, 0, 100);
    }

    /** Horizontal, in that range. */
    public JProgressBar(int min, int max) {
        this(HORIZONTAL, min, max);
    }

    /**
     * With orientation and range.
     *
     * @throws IllegalArgumentException if the orientation is neither horizontal nor vertical.
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
     * With that model, horizontal.
     *
     * @throws NullPointerException if the model is null.
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
     * Horizontal or vertical.
     *
     * @throws IllegalArgumentException if it is not one of the two.
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
     * The text: the one that was set, and otherwise the percentage.
     *
     * <p>The percentage comes from the language's percentage format, so an empty range -- whose
     * proportion is {@code NaN} -- is written as that format writes it, not as a zero.
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
     * What part of the range the value takes, from zero to one.
     *
     * <p><strong>An empty range gives {@code NaN}</strong>, not zero: the arithmetic is a division
     * and the JDK does not guard it. It is measured. Returning zero would be more comfortable and
     * would say that the bar is at the beginning, which is not the same as saying that the
     * question has no answer.
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

    /** It draws the border only if it is switched on. */
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

    /** The bridge between the model and this bar. */
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

    /** It hands out a change notice with this bar as the source. */
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

    /** It changes the range; the listener moves from the old model to the new one. */
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

    /** It turns into the bar that bounces; see the class note. */
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

    /** It turns the model's notice into one of the bar's. */
    private static class ModelListener implements ChangeListener, java.io.Serializable {

        private final JProgressBar bar;

        ModelListener(JProgressBar bar) {
            this.bar = bar;
        }

        public void stateChanged(ChangeEvent e) {
            bar.fireStateChanged();
        }
    }
}
