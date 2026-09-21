package javax.swing;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.SpinnerUI;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

/**
 * A field with two arrows for walking through a sequence.
 *
 * <h2>Three pieces, not one</h2>
 *
 * <p>The {@link SpinnerModel} knows which the value is and which comes afterwards. The
 * <em>editor</em> shows it and allows it to be typed. The look and feel draws the arrows.
 * Changing the model changes the sequence <em>and</em> the editor, unless one was set by hand:
 * see {@link #setEditor}.
 *
 * <h2>Why the editor is a pane and not a field</h2>
 *
 * <p>{@link DefaultEditor} is a {@link JPanel} with a {@link JFormattedTextField} inside, and
 * it is its own layout. It could be the field directly; it is not because that way an editor of
 * one's own may have several pieces -- three fields for a date, for instance -- without
 * changing anything above.
 *
 * <h2>The value travels in both directions</h2>
 *
 * <p>When the model changes, the editor listens and updates the field. When the user types and
 * confirms, the field gives notice and the editor passes it on to the model. If the model
 * rejects it, the editor gives the field back its previous value: it is what keeps a value the
 * model never accepted from staying on the screen.
 */
public class JSpinner extends JComponent implements Accessible {

    private static final String uiClassID = "SpinnerUI";

    /**
     * A switched-off action, for covering the text field's.
     *
     * <p>A binding from keys to a switched-off action counts as though it did not exist, so
     * putting this one into the field's map lets the control's arrows win over the field's.
     */
    private static final Action DISABLED_ACTION = new DisabledAction();

    private SpinnerModel model;
    private JComponent editor;
    private ChangeListener modelListener;
    private transient ChangeEvent changeEvent;
    private boolean editorExplicitlySet = false;

    /**
     * With that model.
     *
     * @throws NullPointerException if the model is null.
     */
    public JSpinner(SpinnerModel model) {
        if (model == null) {
            throw new NullPointerException("model cannot be null");
        }
        this.model = model;
        this.editor = createEditor(model);
        setOpaque(true);
        updateUI();
    }

    /** With a freshly made {@link SpinnerNumberModel}: integers from zero, one at a time. */
    public JSpinner() {
        this(new SpinnerNumberModel());
    }

    public SpinnerUI getUI() {
        return (SpinnerUI) ui;
    }

    public void setUI(SpinnerUI ui) {
        super.setUI(ui);
    }

    public String getUIClassID() {
        return uiClassID;
    }

    public void updateUI() {
    }

    /**
     * It chooses the editor that suits that model.
     *
     * <p>The order matters: date and list are asked about before number, because they are the
     * cases with an editor of their own. A model that is none of the three is left with the base
     * editor, which shows the value but does not allow it to be typed -- there would be no way of
     * interpreting what was typed.
     */
    protected JComponent createEditor(SpinnerModel model) {
        if (model instanceof SpinnerDateModel) {
            return new DateEditor(this);
        } else if (model instanceof SpinnerListModel) {
            return new ListEditor(this);
        } else if (model instanceof SpinnerNumberModel) {
            return new NumberEditor(this);
        } else {
            return new DefaultEditor(this);
        }
    }

    /**
     * It changes the model, and with it the editor.
     *
     * <p>Unless the editor was set by hand: in that case it is respected, because changing it
     * would erase a decision of the program's.
     *
     * @throws IllegalArgumentException if the model is null.
     */
    public void setModel(SpinnerModel model) {
        if (model == null) {
            throw new IllegalArgumentException("null model");
        }
        if (!model.equals(this.model)) {
            SpinnerModel oldModel = this.model;
            this.model = model;
            if (modelListener != null) {
                oldModel.removeChangeListener(modelListener);
                this.model.addChangeListener(modelListener);
            }
            firePropertyChange("model", oldModel, model);
            if (!editorExplicitlySet) {
                setEditor(createEditor(model));
                editorExplicitlySet = false;
            }
            repaint();
            revalidate();
        }
    }

    public SpinnerModel getModel() {
        return model;
    }

    /** The value, asking the model for it. */
    public Object getValue() {
        return getModel().getValue();
    }

    /**
     * It changes the value.
     *
     * @throws IllegalArgumentException if the model does not accept it.
     */
    public void setValue(Object value) {
        getModel().setValue(value);
    }

    /** The next one in the sequence, or null if there is none. */
    public Object getNextValue() {
        return getModel().getNextValue();
    }

    /** The previous one, or null if there is none. */
    public Object getPreviousValue() {
        return getModel().getPreviousValue();
    }

    /**
     * It listens to the changes of value.
     *
     * <p>The control signs itself up on the model only when somebody signs up on the control, and
     * not before: a control with no listeners has no reason to listen to its model.
     */
    public void addChangeListener(ChangeListener listener) {
        if (modelListener == null) {
            modelListener = new ModelListener(this);
            getModel().addChangeListener(modelListener);
        }
        listenerList.add(ChangeListener.class, listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        listenerList.remove(ChangeListener.class, listener);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    /** It hands out a change notice; the event is built once and reused. */
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

    /**
     * It sets an editor of one's own.
     *
     * <p>The editor that leaves is told with {@code dismiss} so that it signs off from the model;
     * otherwise, it would go on reacting to the changes of a control it no longer shows.
     *
     * @throws IllegalArgumentException if it is null.
     */
    public void setEditor(JComponent editor) {
        if (editor == null) {
            throw new IllegalArgumentException("null editor");
        }
        if (!editor.equals(this.editor)) {
            JComponent oldEditor = this.editor;
            this.editor = editor;
            if (oldEditor instanceof DefaultEditor) {
                ((DefaultEditor) oldEditor).dismiss(this);
            }
            editorExplicitlySet = true;
            firePropertyChange("editor", oldEditor, editor);
            revalidate();
            repaint();
        }
    }

    public JComponent getEditor() {
        return editor;
    }

    /**
     * It asks the editor to confirm what was typed.
     *
     * @throws ParseException if what was typed cannot be interpreted.
     */
    public void commitEdit() throws ParseException {
        JComponent editor = getEditor();
        if (editor instanceof DefaultEditor) {
            ((DefaultEditor) editor).commitEdit();
        }
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    /** It passes the model's notice on to the control's listeners. */
    private static class ModelListener implements ChangeListener, java.io.Serializable {

        private final JSpinner control;

        ModelListener(JSpinner control) {
            this.control = control;
        }

        public void stateChanged(ChangeEvent e) {
            control.fireStateChanged();
        }
    }

    /** Ver {@link JSpinner#DISABLED_ACTION}. */
    private static class DisabledAction implements Action {

        public Object getValue(String key) {
            return null;
        }

        public void putValue(String key, Object value) {
        }

        public void setEnabled(boolean b) {
        }

        public boolean isEnabled() {
            return false;
        }

        public void addPropertyChangeListener(PropertyChangeListener l) {
        }

        public void removePropertyChangeListener(PropertyChangeListener l) {
        }

        public void actionPerformed(java.awt.event.ActionEvent ae) {
        }
    }

    /**
     * The base editor: a text field that shows the value.
     *
     * <p>It is also its own layout -- it implements {@link LayoutManager} -- because the only
     * thing it has to do is give the field all the space but the margins. A real layout would be
     * more code than it saves.
     *
     * <p>By default the field is not editable: the generic editor does not know how to interpret
     * whatever is typed. The three editors that follow switch it on, each with its formatter.
     */
    public static class DefaultEditor extends JPanel
            implements ChangeListener, PropertyChangeListener, LayoutManager {

        /** With the field already connected to that control. */
        public DefaultEditor(JSpinner spinner) {
            super(null);
            JFormattedTextField ftf = new JFormattedTextField();
            ftf.setName("Spinner.formattedTextField");
            ftf.setValue(spinner.getValue());
            ftf.addPropertyChangeListener(this);
            ftf.setEditable(false);
            ftf.setInheritsPopupMenu(true);
            String toolTipText = spinner.getToolTipText();
            if (toolTipText != null) {
                ftf.setToolTipText(toolTipText);
            }
            add(ftf);
            setLayout(this);
            spinner.addChangeListener(this);
            // See DISABLED_ACTION: the control's arrows have to beat the field's.
            ActionMap ftfMap = ftf.getActionMap();
            if (ftfMap != null) {
                ftfMap.put("increment", DISABLED_ACTION);
                ftfMap.put("decrement", DISABLED_ACTION);
            }
        }

        /** It disconnects it from the control; see {@link JSpinner#setEditor}. */
        public void dismiss(JSpinner spinner) {
            spinner.removeChangeListener(this);
        }

        /** The control that contains it, looking upwards, or null if it is not set. */
        public JSpinner getSpinner() {
            for (Component c = this; c != null; c = c.getParent()) {
                if (c instanceof JSpinner) {
                    return (JSpinner) c;
                }
            }
            return null;
        }

        public JFormattedTextField getTextField() {
            return (JFormattedTextField) getComponent(0);
        }

        /** The model changed: it is passed on to the field. */
        public void stateChanged(ChangeEvent e) {
            JSpinner spinner = (JSpinner) (e.getSource());
            getTextField().setValue(spinner.getValue());
        }

        /**
         * The field changed: it is passed on to the model.
         *
         * <p>If the model rejects it, the field goes back to the previous value. See
         * {@link JSpinner}'s note.
         */
        public void propertyChange(PropertyChangeEvent e) {
            JSpinner spinner = getSpinner();
            if (spinner == null) {
                // It is not set in any control: there is nobody to tell.
                return;
            }
            Object source = e.getSource();
            String name = e.getPropertyName();
            if ((source instanceof JFormattedTextField) && "value".equals(name)) {
                Object lastValue = spinner.getValue();
                try {
                    spinner.setValue(getTextField().getValue());
                } catch (IllegalArgumentException iae) {
                    try {
                        ((JFormattedTextField) source).setValue(lastValue);
                    } catch (IllegalArgumentException iae2) {
                        // Not even the old value serves: there is nothing left to do and the two
                        // are
                                                // left uncoupled, which is what the JDK does.
                    }
                }
            }
        }

        public void addLayoutComponent(String name, Component child) {
        }

        public void removeLayoutComponent(Component child) {
        }

        /** What the margins take up. */
        private Dimension insetSize(Container parent) {
            Insets insets = parent.getInsets();
            int w = insets.left + insets.right;
            int h = insets.top + insets.bottom;
            return new Dimension(w, h);
        }

        public Dimension preferredLayoutSize(Container parent) {
            Dimension preferredSize = insetSize(parent);
            if (parent.getComponentCount() > 0) {
                Dimension childSize = getComponent(0).getPreferredSize();
                preferredSize.width += childSize.width;
                preferredSize.height += childSize.height;
            }
            return preferredSize;
        }

        public Dimension minimumLayoutSize(Container parent) {
            Dimension minimumSize = insetSize(parent);
            if (parent.getComponentCount() > 0) {
                Dimension childSize = getComponent(0).getMinimumSize();
                minimumSize.width += childSize.width;
                minimumSize.height += childSize.height;
            }
            return minimumSize;
        }

        /** It gives the field everything but the margins. */
        public void layoutContainer(Container parent) {
            if (parent.getComponentCount() > 0) {
                Insets insets = parent.getInsets();
                int w = parent.getWidth() - (insets.left + insets.right);
                int h = parent.getHeight() - (insets.top + insets.bottom);
                getComponent(0).setBounds(insets.left, insets.top, w, h);
            }
        }

        /**
         * It confirms what was typed.
         *
         * @throws ParseException if it cannot be interpreted.
         */
        public void commitEdit() throws ParseException {
            getTextField().commitEdit();
        }

        /** The baseline is the field's, shifted by the top margin. */
        public int getBaseline(int width, int height) {
            super.getBaseline(width, height);
            Insets insets = getInsets();
            width = width - insets.left - insets.right;
            height = height - insets.top - insets.bottom;
            int baseline = getComponent(0).getBaseline(width, height);
            if (baseline >= 0) {
                return baseline + insets.top;
            }
            return -1;
        }

        public Component.BaselineResizeBehavior getBaselineResizeBehavior() {
            return getComponent(0).getBaselineResizeBehavior();
        }
    }

    /**
     * The editor for a {@link SpinnerNumberModel}.
     *
     * <p>The formatter knows the model's bounds, so typing something out of range is marked as
     * invalid before reaching the model.
     */
    public static class NumberEditor extends DefaultEditor {

        /** With the number format of the control's language. */
        public NumberEditor(JSpinner spinner) {
            this(spinner, defaultPattern(spinner.getLocale()));
        }

        /**
         * With that {@link DecimalFormat} pattern.
         *
         * @throws IllegalArgumentException if the model is not a {@link SpinnerNumberModel}.
         */
        public NumberEditor(JSpinner spinner, String decimalFormatPattern) {
            super(spinner);
            if (!(spinner.getModel() instanceof SpinnerNumberModel)) {
                throw new IllegalArgumentException("model not a SpinnerNumberModel");
            }
            DecimalFormat format = new DecimalFormat(decimalFormatPattern);
            SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
            NumberFormatter formatter = new NumberEditorFormatter(model, format);
            DefaultFormatterFactory factory = new DefaultFormatterFactory(formatter);
            JFormattedTextField ftf = getTextField();
            ftf.setEditable(true);
            ftf.setFormatterFactory(factory);
            ftf.setHorizontalAlignment(JTextField.RIGHT);
            // The width comes from the longest bound: it is the only thing that is known in advance
                        // about how much room the number is going to take.
            try {
                String maxString = formatter.valueToString(model.getMinimum());
                String minString = formatter.valueToString(model.getMaximum());
                ftf.setColumns(Math.max(maxString.length(), minString.length()));
            } catch (ParseException e) {
                // With no bounds there is nothing to get the width from; the one the field brings
                // is left.
            }
        }

        /** That language's number pattern. */
        private static String defaultPattern(Locale locale) {
            NumberFormat nf = (locale == null) ? NumberFormat.getInstance()
                    : NumberFormat.getInstance(locale);
            if (nf instanceof DecimalFormat) {
                return ((DecimalFormat) nf).toPattern();
            }
            return "#,##0.###";
        }

        public DecimalFormat getFormat() {
            NumberFormatter f = (NumberFormatter) getTextField().getFormatter();
            return (DecimalFormat) f.getFormat();
        }

        public SpinnerNumberModel getModel() {
            return (SpinnerNumberModel) (getSpinner().getModel());
        }

        public void setComponentOrientation(ComponentOrientation o) {
            super.setComponentOrientation(o);
        }
    }

    /** It ties the formatter to the model's bounds; see {@link NumberEditor}. */
    private static class NumberEditorFormatter extends NumberFormatter {

        private final SpinnerNumberModel model;

        NumberEditorFormatter(SpinnerNumberModel model, Format format) {
            super((NumberFormat) format);
            this.model = model;
            setValueClass(model.getValue().getClass());
        }

        public void setMinimum(Comparable<?> min) {
            model.setMinimum(min);
        }

        public Comparable<?> getMinimum() {
            return model.getMinimum();
        }

        public void setMaximum(Comparable<?> max) {
            model.setMaximum(max);
        }

        public Comparable<?> getMaximum() {
            return model.getMaximum();
        }
    }

    /**
     * The editor for a {@link SpinnerListModel}.
     *
     * <p>There is nothing to format: the elements come and go through their {@code toString}.
     */
    public static class ListEditor extends DefaultEditor {

        /**
         * @throws IllegalArgumentException if the model is not a {@link SpinnerListModel}.
         */
        public ListEditor(JSpinner spinner) {
            super(spinner);
            if (!(spinner.getModel() instanceof SpinnerListModel)) {
                throw new IllegalArgumentException("model not a SpinnerListModel");
            }
            getTextField().setEditable(true);
            getTextField().setFormatterFactory(
                    new DefaultFormatterFactory(new ListEditorFormatter()));
        }

        public SpinnerListModel getModel() {
            return (SpinnerListModel) (getSpinner().getModel());
        }
    }

    /** The text is the value and the value is the text; see {@link ListEditor}. */
    private static class ListEditorFormatter extends JFormattedTextField.AbstractFormatter {

        public String valueToString(Object value) throws ParseException {
            return (value == null) ? "" : value.toString();
        }

        public Object stringToValue(String string) throws ParseException {
            return string;
        }
    }

    /**
     * The editor for a {@link SpinnerDateModel}.
     *
     * <p>As in {@link NumberEditor}, the formatter knows the model's bounds.
     */
    public static class DateEditor extends DefaultEditor {

        /** With the date and time format of the control's language. */
        public DateEditor(JSpinner spinner) {
            this(spinner, defaultPattern(spinner.getLocale()));
        }

        /**
         * With that {@link SimpleDateFormat} pattern.
         *
         * @throws IllegalArgumentException if the model is not a {@link SpinnerDateModel}.
         */
        public DateEditor(JSpinner spinner, String dateFormatPattern) {
            super(spinner);
            if (!(spinner.getModel() instanceof SpinnerDateModel)) {
                throw new IllegalArgumentException("model not a SpinnerDateModel");
            }
            Locale loc = spinner.getLocale();
            SimpleDateFormat format = (loc == null) ? new SimpleDateFormat(dateFormatPattern)
                    : new SimpleDateFormat(dateFormatPattern, loc);
            SpinnerDateModel model = (SpinnerDateModel) spinner.getModel();
            DateFormatter formatter = new DateEditorFormatter(model, format);
            DefaultFormatterFactory factory = new DefaultFormatterFactory(formatter);
            JFormattedTextField ftf = getTextField();
            ftf.setEditable(true);
            ftf.setFormatterFactory(factory);
        }

        /** That language's date and time pattern. */
        private static String defaultPattern(Locale locale) {
            DateFormat df = DateFormat.getDateTimeInstance();
            if (df instanceof SimpleDateFormat) {
                return ((SimpleDateFormat) df).toPattern();
            }
            return "d/MM/yy H:mm:ss";
        }

        public SimpleDateFormat getFormat() {
            DateFormatter f = (DateFormatter) getTextField().getFormatter();
            return (SimpleDateFormat) f.getFormat();
        }

        public SpinnerDateModel getModel() {
            return (SpinnerDateModel) (getSpinner().getModel());
        }
    }

    /** It ties the formatter to the model's bounds; see {@link DateEditor}. */
    private static class DateEditorFormatter extends DateFormatter {

        private final SpinnerDateModel model;

        DateEditorFormatter(SpinnerDateModel model, DateFormat format) {
            super(format);
            this.model = model;
        }

        public void setMinimum(Comparable<?> min) {
            model.setStart(asDate(min));
        }

        public Comparable<?> getMinimum() {
            return model.getStart();
        }

        public void setMaximum(Comparable<?> max) {
            model.setEnd(asDate(max));
        }

        public Comparable<?> getMaximum() {
            return model.getEnd();
        }

        /** The generic discard, in a single place. */
        @SuppressWarnings("unchecked")
        private static Comparable<java.util.Date> asDate(Comparable<?> c) {
            return (Comparable<java.util.Date>) c;
        }
    }
}
