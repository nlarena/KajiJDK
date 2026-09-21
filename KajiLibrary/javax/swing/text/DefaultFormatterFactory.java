package javax.swing.text;

import java.io.Serializable;

import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField$AbstractFormatter;
import javax.swing.JFormattedTextField$AbstractFormatterFactory;

/**
 * A factory that chooses between four formatters according to the field's state.
 *
 * <h2>Why four</h2>
 *
 * <p>The text that is shown and the one that is edited do not have to be the same. An amount is
 * shown as <code>$ 1,234.50</code> and edited as <code>1234.5</code>: with the currency sign in
 * front, the user would have to skip over it with the arrows every time.
 *
 * <p>Hence the four: one for when the field is blank, one for when it is being edited, one for
 * when it is only being looked at, and a fallback for when one of the others is not set. Most
 * uses set only the fallback.
 */
public class DefaultFormatterFactory extends JFormattedTextField$AbstractFormatterFactory
        implements Serializable {

    private JFormattedTextField$AbstractFormatter defaultFormat;
    private JFormattedTextField$AbstractFormatter displayFormat;
    private JFormattedTextField$AbstractFormatter editFormat;
    private JFormattedTextField$AbstractFormatter nullFormat;

    /** A factory with no formatter; it returns null until one is set on it. */
    public DefaultFormatterFactory() {
    }

    /** A factory with that fallback formatter. */
    public DefaultFormatterFactory(JFormattedTextField$AbstractFormatter defaultFormat) {
        this(defaultFormat, null);
    }

    /** A factory with the fallback and the look-only one. */
    public DefaultFormatterFactory(JFormattedTextField$AbstractFormatter defaultFormat,
            JFormattedTextField$AbstractFormatter displayFormat) {
        this(defaultFormat, displayFormat, null);
    }

    /** A factory with the fallback, the look-only one and the editing one. */
    public DefaultFormatterFactory(JFormattedTextField$AbstractFormatter defaultFormat,
            JFormattedTextField$AbstractFormatter displayFormat,
            JFormattedTextField$AbstractFormatter editFormat) {
        this(defaultFormat, displayFormat, editFormat, null);
    }

    /** A factory with all four. */
    public DefaultFormatterFactory(JFormattedTextField$AbstractFormatter defaultFormat,
            JFormattedTextField$AbstractFormatter displayFormat,
            JFormattedTextField$AbstractFormatter editFormat,
            JFormattedTextField$AbstractFormatter nullFormat) {
        this.defaultFormat = defaultFormat;
        this.displayFormat = displayFormat;
        this.editFormat = editFormat;
        this.nullFormat = nullFormat;
    }

    /** The one used when none of the others applies. */
    public void setDefaultFormatter(JFormattedTextField$AbstractFormatter atf) {
        defaultFormat = atf;
    }

    public JFormattedTextField$AbstractFormatter getDefaultFormatter() {
        return defaultFormat;
    }

    /** The one used when the field does not have the focus. */
    public void setDisplayFormatter(JFormattedTextField$AbstractFormatter atf) {
        displayFormat = atf;
    }

    public JFormattedTextField$AbstractFormatter getDisplayFormatter() {
        return displayFormat;
    }

    /** The one used when the field has the focus. */
    public void setEditFormatter(JFormattedTextField$AbstractFormatter atf) {
        editFormat = atf;
    }

    public JFormattedTextField$AbstractFormatter getEditFormatter() {
        return editFormat;
    }

    /** The one used when the value is null. */
    public void setNullFormatter(JFormattedTextField$AbstractFormatter atf) {
        nullFormat = atf;
    }

    public JFormattedTextField$AbstractFormatter getNullFormatter() {
        return nullFormat;
    }

    /** It chooses the formatter that corresponds to the field's state. */
    public JFormattedTextField$AbstractFormatter getFormatter(JFormattedTextField source) {
        JFormattedTextField$AbstractFormatter format = null;

        if (source == null) {
            return null;
        }
        Object value = source.getValue();

        if (value == null) {
            format = getNullFormatter();
        }
        if (format == null) {
            if (source.hasFocus()) {
                format = getEditFormatter();
            } else {
                format = getDisplayFormatter();
            }
            if (format == null) {
                format = getDefaultFormatter();
            }
        }
        return format;
    }
}
