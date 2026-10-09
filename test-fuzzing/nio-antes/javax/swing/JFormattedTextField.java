package javax.swing;

import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.InputMethodEvent;
import java.text.Format;
import java.text.ParseException;

import javax.swing.text.AttributeSet;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.NavigationFilter;
import javax.swing.text.NumberFormatter;
import javax.swing.text.TextAction;

/**
 * A text field that keeps a value, not a string.
 *
 * <h2>The value and the text are two things</h2>
 *
 * <p>An ordinary text field has text. This one has a <em>value</em> (a date, a number,
 * whatever) and a formatter that converts it into text in order to show it and back into a
 * value when the user finishes editing. The text that is seen is a representation; the value is
 * what matters.
 *
 * <p>That they are two things is what allows the field to show <code>1.234,50</code> and the
 * program to read a {@code double}. And also what forces one to decide what happens when the
 * text cannot be converted: that is {@link #setFocusLostBehavior}.
 *
 * <h2>Why there is a factory and not a formatter</h2>
 *
 * <p>Many times the text that is shown and the one that is edited are not the same: an amount
 * is shown with a currency sign and edited without it. The factory returns a different
 * formatter according to whether the field has the focus or not. With a single formatter one of
 * the two would have to be chosen.
 */
public class JFormattedTextField extends JTextField {

    private static final String uiClassID = "FormattedTextFieldUI";
    private static final Action[] defaultActions = {new CommitAction(), new CancelAction()};

    /**
     * On losing the focus, pass the text to the value; if that cannot be done, leave it invalid.
     */
    public static final int COMMIT = 0;

    /** The same, but if that cannot be done go back to the last good value. */
    public static final int COMMIT_OR_REVERT = 1;

    /** On losing the focus, always go back to the value. */
    public static final int REVERT = 2;

    /** On losing the focus, do nothing. */
    public static final int PERSIST = 3;

    private AbstractFormatterFactory factory;
    private AbstractFormatter format;
    private Object value;
    private boolean editValid;
    private int focusLostBehavior;
    private boolean edited;
    private boolean composedTextExists = false;

    /** An empty field, with no formatter yet. */
    public JFormattedTextField() {
        super();
        enableEvents(java.awt.AWTEvent.FOCUS_EVENT_MASK);
        setFocusLostBehavior(COMMIT_OR_REVERT);
    }

    /** A field with that value; the formatter comes from the value's type. */
    public JFormattedTextField(Object value) {
        this();
        setValue(value);
    }

    /** A field that uses that {@code java.text} format. */
    public JFormattedTextField(Format format) {
        this();
        setFormatterFactory(getDefaultFormatterFactory(format));
    }

    /** A field with that formatter, for editing and for showing. */
    public JFormattedTextField(AbstractFormatter formatter) {
        this(new DefaultFormatterFactory(formatter));
    }

    /** A field with that formatter factory. */
    public JFormattedTextField(AbstractFormatterFactory factory) {
        this();
        setFormatterFactory(factory);
    }

    /** A field with that factory and that initial value. */
    public JFormattedTextField(AbstractFormatterFactory factory, Object currentValue) {
        this(currentValue);
        setFormatterFactory(factory);
    }

    /** What to do when the field loses the focus; see the class note. */
    public void setFocusLostBehavior(int behavior) {
        if (behavior != COMMIT && behavior != COMMIT_OR_REVERT
                && behavior != PERSIST && behavior != REVERT) {
            throw new IllegalArgumentException("setFocusLostBehavior must be one of: "
                    + "JFormattedTextField.COMMIT, JFormattedTextField.COMMIT_OR_REVERT, "
                    + "JFormattedTextField.PERSIST or JFormattedTextField.REVERT");
        }
        focusLostBehavior = behavior;
    }

    public int getFocusLostBehavior() {
        return focusLostBehavior;
    }

    /** The formatter factory; changing it formats the value again. */
    public void setFormatterFactory(AbstractFormatterFactory tf) {
        AbstractFormatterFactory oldFactory = factory;
        factory = tf;
        firePropertyChange("formatterFactory", oldFactory, tf);
        setValue(getValue(), true, false);
    }

    public AbstractFormatterFactory getFormatterFactory() {
        return factory;
    }

    /**
     * The formatter that is set now.
     *
     * <p>It is not set by whoever uses the field but by the field itself, asking the factory for
     * it. It is protected precisely for that.
     */
    protected void setFormatter(AbstractFormatter format) {
        AbstractFormatter oldFormat = this.format;
        if (oldFormat != null) {
            oldFormat.uninstall();
        }
        setEditValid(true);
        this.format = format;
        if (format != null) {
            format.install(this);
        }
        setEdited(false);
        firePropertyChange("textFormatter", oldFormat, format);
    }

    public AbstractFormatter getFormatter() {
        return format;
    }

    /** It changes the value and updates the text. */
    public void setValue(Object value) {
        if (getFormatterFactory() == null) {
            setFormatterFactory(getDefaultFormatterFactory(value));
        }
        setValue(value, true, true);
    }

    public Object getValue() {
        return value;
    }

    /**
     * It passes the text that is seen to the value.
     *
     * @throws ParseException if the text cannot be converted.
     */
    public void commitEdit() throws ParseException {
        AbstractFormatter format = getFormatter();
        if (format != null) {
            setValue(format.stringToValue(getText()), false, true);
        }
    }

    /** Whether what has been typed so far can be converted into a value. */
    public boolean isEditValid() {
        return editValid;
    }

    /** It gives notice that what was typed does not serve; the look and feel usually beeps. */
    protected void invalidEdit() {
        UIManagerBeep();
    }

    private void UIManagerBeep() {
        java.awt.Toolkit.getDefaultToolkit().beep();
    }

    protected void processInputMethodEvent(InputMethodEvent e) {
        java.text.AttributedCharacterIterator text = e.getText();
        int commitCount = e.getCommittedCharacterCount();
        // A text that is being composed (in Japanese, for instance) is not validated until it
                // finishes: validating it half done would mark as invalid something that is not
                // anything yet.
        composedTextExists = ((text != null)
                && (text.getEndIndex() - (text.getBeginIndex() + commitCount)) > 0);
        super.processInputMethodEvent(e);
    }

    /** On gaining or losing the focus the formatter changes, and sometimes the value. */
    protected void processFocusEvent(FocusEvent e) {
        super.processFocusEvent(e);
        if (composedTextExists) {
            return;
        }
        if (e.isTemporary()) {
            return;
        }
        if (isEdited() && e.getID() == FocusEvent.FOCUS_LOST) {
            int fb = getFocusLostBehavior();
            if (fb == JFormattedTextField.COMMIT
                    || fb == JFormattedTextField.COMMIT_OR_REVERT) {
                try {
                    commitEdit();
                    // Format it again: the value may look different once kept.
                    setValue(getValue(), true, true);
                } catch (ParseException pe) {
                    if (fb == JFormattedTextField.COMMIT_OR_REVERT) {
                        setValue(getValue(), true, true);
                    }
                }
            } else if (fb == JFormattedTextField.REVERT) {
                setValue(getValue(), true, true);
            }
        }
        setFormatter(getFormatterFactory() == null ? null
                : getFormatterFactory().getFormatter(this));
    }

    public Action[] getActions() {
        return TextAction.augmentList(super.getActions(), defaultActions);
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** Changing the document sets the value's text again. */
    public void setDocument(Document doc) {
        super.setDocument(doc);
    }

    void setEditValid(boolean isValid) {
        if (isValid != editValid) {
            editValid = isValid;
            firePropertyChange("editValid", Boolean.valueOf(!isValid), Boolean.valueOf(isValid));
        }
    }

    void setEdited(boolean edited) {
        this.edited = edited;
    }

    boolean isEdited() {
        return edited;
    }

    private void setValue(Object value, boolean createFormat, boolean firePC) {
        Object oldValue = this.value;
        this.value = value;

        if (createFormat) {
            AbstractFormatterFactory factory = getFormatterFactory();
            AbstractFormatter atf = (factory != null) ? factory.getFormatter(this) : null;
            setFormatter(atf);
        } else {
            setEdited(false);
        }
        if (firePC) {
            firePropertyChange("value", oldValue, value);
        }
    }

    /**
     * It chooses a factory by looking at the value's type.
     *
     * <p>It is what makes a field built with a date already know how to format dates without
     * anybody saying anything.
     */
    private AbstractFormatterFactory getDefaultFormatterFactory(Object type) {
        if (type instanceof java.text.DateFormat) {
            return new DefaultFormatterFactory(
                    new DateFormatter((java.text.DateFormat) type));
        }
        if (type instanceof java.text.NumberFormat) {
            return new DefaultFormatterFactory(
                    new NumberFormatter((java.text.NumberFormat) type));
        }
        if (type instanceof Format) {
            return new DefaultFormatterFactory(
                    new javax.swing.text.InternationalFormatter((Format) type));
        }
        if (type instanceof java.util.Date) {
            return new DefaultFormatterFactory(new DateFormatter());
        }
        if (type instanceof Number) {
            AbstractFormatter displayFormatter = new NumberFormatter();
            ((javax.swing.text.NumberFormatter) displayFormatter).setValueClass(type.getClass());
            AbstractFormatter editFormatter = new NumberFormatter(
                    new java.text.DecimalFormat("#.#"));
            ((javax.swing.text.NumberFormatter) editFormatter).setValueClass(type.getClass());
            return new DefaultFormatterFactory(displayFormatter, displayFormatter,
                    editFormatter);
        }
        return new DefaultFormatterFactory(new javax.swing.text.DefaultFormatter());
    }

    /**
     * It converts between the value and the text that is seen.
     *
     * <p>Besides converting it may control the editing: {@link #getDocumentFilter} allows what is
     * typed to be filtered and {@link #getNavigationFilter} where the caret may stop. With that a
     * mask is built where certain positions cannot be touched.
     */
    public abstract static class AbstractFormatter implements java.io.Serializable {

        private JFormattedTextField ftf;

        protected AbstractFormatter() {
        }

        /**
         * It hooks itself to the field: it sets the value's text and the filters.
         *
         * <p>If the value cannot be formatted, the field is left empty and invalid; leaving the
         * previous text would show something that no longer corresponds to the value.
         */
        public void install(JFormattedTextField ftf) {
            if (this.ftf != null) {
                uninstall();
            }
            this.ftf = ftf;
            if (ftf != null) {
                try {
                    ftf.setText(valueToString(ftf.getValue()));
                } catch (ParseException pe) {
                    ftf.setText("");
                    setEditValid(false);
                }
                installDocumentFilter(getDocumentFilter());
                ftf.setNavigationFilter(getNavigationFilter());
                ftf.setFormatterActions(getActions());
            }
        }

        public void uninstall() {
            if (this.ftf != null) {
                installDocumentFilter(null);
                this.ftf.setNavigationFilter(null);
                this.ftf.setFormatterActions(null);
            }
            this.ftf = null;
        }

        /** The value that string represents. */
        public abstract Object stringToValue(String text) throws ParseException;

        /** The text that represents that value. */
        public abstract String valueToString(Object value) throws ParseException;

        protected JFormattedTextField getFormattedTextField() {
            return ftf;
        }

        protected void invalidEdit() {
            JFormattedTextField ftf = getFormattedTextField();
            if (ftf != null) {
                ftf.invalidEdit();
            }
        }

        protected void setEditValid(boolean valid) {
            JFormattedTextField ftf = getFormattedTextField();
            if (ftf != null) {
                ftf.setEditValid(valid);
            }
        }

        /** The formatter's own actions; none, unless the subclass adds some. */
        protected Action[] getActions() {
            return null;
        }

        /** The filter that decides what may be typed. */
        protected DocumentFilter getDocumentFilter() {
            return null;
        }

        /** The filter that decides where the caret may stop. */
        protected NavigationFilter getNavigationFilter() {
            return null;
        }

        protected Object clone() throws CloneNotSupportedException {
            AbstractFormatter formatter = (AbstractFormatter) super.clone();
            formatter.ftf = null;
            return formatter;
        }

        private void installDocumentFilter(DocumentFilter filter) {
            JFormattedTextField ftf = getFormattedTextField();
            if (ftf != null) {
                Document doc = ftf.getDocument();
                if (doc instanceof javax.swing.text.AbstractDocument) {
                    ((javax.swing.text.AbstractDocument) doc).setDocumentFilter(filter);
                }
                doc.putProperty(DocumentFilter.class, null);
            }
        }
    }

    /** It returns the formatter that corresponds to the field's state. */
    public abstract static class AbstractFormatterFactory {

        protected AbstractFormatterFactory() {
        }

        /** The formatter for that field, now. */
        public abstract AbstractFormatter getFormatter(JFormattedTextField tf);
    }

    private Action[] formatterActions;

    void setFormatterActions(Action[] actions) {
        formatterActions = actions;
    }

    /** It passes the text to the value; Enter's action. */
    static class CommitAction extends TextAction {

        CommitAction() {
            super("notify-field-accept");
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getFocusedComponent();
            if (target instanceof JFormattedTextField) {
                JFormattedTextField ftf = (JFormattedTextField) target;
                try {
                    ftf.commitEdit();
                    ftf.setValue(ftf.getValue(), true, true);
                } catch (ParseException pe) {
                    ftf.invalidEdit();
                    return;
                }
            }
            if (target instanceof JTextField) {
                ((JTextField) target).postActionEvent();
            }
        }

        public boolean isEnabled() {
            return true;
        }
    }

    /** It goes back to the last good value; Escape's action. */
    static class CancelAction extends TextAction {

        CancelAction() {
            super("reset-field-edit");
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getFocusedComponent();
            if (target instanceof JFormattedTextField) {
                JFormattedTextField ftf = (JFormattedTextField) target;
                ftf.setValue(ftf.getValue(), true, true);
            }
        }

        public boolean isEnabled() {
            return true;
        }
    }
}
