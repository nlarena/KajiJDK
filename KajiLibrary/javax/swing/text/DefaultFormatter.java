package javax.swing.text;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.text.ParseException;

import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField$AbstractFormatter;

/**
 * The usual formatter: it converts with {@code toString} and with a constructor taking a
 * string.
 *
 * <h2>How it converts back</h2>
 *
 * <p>To go from value to text {@code toString} is enough. To come back, it looks in the value's
 * class for a constructor that takes a {@code String} and calls it. That is why it works with no
 * configuration at all with {@code Integer}, {@code Double} or any class that has that
 * constructor, and does not work with those that do not have it.
 *
 * <h2>Valid editing at every keystroke</h2>
 *
 * <p>With {@link #setAllowsInvalid} at false, every key is tried before being accepted: what the
 * text would look like is built, the conversion is attempted, and if it cannot be done the key
 * is rejected. That is what allows a field where something invalid literally cannot be typed.
 *
 * <p>The price is that there are valid intermediate states that become unreachable. Typing
 * <code>-5</code> requires going through <code>-</code>, which is not a number. That is why the
 * default is to let anything be typed and validate afterwards.
 */
public class DefaultFormatter extends JFormattedTextField$AbstractFormatter
        implements Cloneable, Serializable {

    private boolean commitOnEdit;
    private boolean overwriteMode;
    private boolean allowsInvalid;
    private Class<?> valueClass;

    private transient DocumentFilter documentFilter;
    private transient NavigationFilter navigationFilter;

    /** A formatter that lets anything be typed and overwrites while typing. */
    public DefaultFormatter() {
        overwriteMode = true;
        allowsInvalid = true;
    }

    /** It hooks itself to the field and leaves the cursor at the beginning. */
    public void install(JFormattedTextField ftf) {
        super.install(ftf);
        positionCursorAtInitialLocation();
    }

    /** Whether every valid edit passes straight to the field's value. */
    public void setCommitsOnValidEdit(boolean commit) {
        commitOnEdit = commit;
    }

    public boolean getCommitsOnValidEdit() {
        return commitOnEdit;
    }

    /** Whether typing replaces instead of inserting. */
    public void setOverwriteMode(boolean overwriteMode) {
        this.overwriteMode = overwriteMode;
    }

    public boolean getOverwriteMode() {
        return overwriteMode;
    }

    /** Whether the field may be left in a state that does not convert; see the class note. */
    public void setAllowsInvalid(boolean allowsInvalid) {
        this.allowsInvalid = allowsInvalid;
    }

    public boolean getAllowsInvalid() {
        return allowsInvalid;
    }

    /** The class the text is converted to. */
    public void setValueClass(Class<?> valueClass) {
        this.valueClass = valueClass;
    }

    public Class<?> getValueClass() {
        return valueClass;
    }

    /**
     * It converts the text to the value with the constructor taking a string.
     *
     * @throws ParseException if the class does not have that constructor or if it rejects it.
     */
    public Object stringToValue(String string) throws ParseException {
        Class<?> vc = getValueClass();
        JFormattedTextField ftf = getFormattedTextField();

        if (vc == null && ftf != null) {
            Object value = ftf.getValue();
            if (value != null) {
                vc = value.getClass();
            }
        }
        if (vc != null) {
            Constructor<?> cons;
            try {
                cons = vc.getConstructor(new Class<?>[] {String.class});
            } catch (NoSuchMethodException nsme) {
                cons = null;
            }
            if (cons != null) {
                try {
                    return cons.newInstance(new Object[] {string});
                } catch (Throwable ex) {
                    throw new ParseException("Error creating instance", 0);
                }
            } else {
                throw new ParseException("Unable to create instance for " + vc, 0);
            }
        }
        return string;
    }

    /** The value's text: its {@code toString}, or empty if it is null. */
    public String valueToString(Object value) throws ParseException {
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    /** The filter that validates every key; see the class note. */
    protected DocumentFilter getDocumentFilter() {
        if (documentFilter == null) {
            documentFilter = new DefaultDocumentFilter(this);
        }
        return documentFilter;
    }

    /** The filter that decides where the cursor may stop. */
    protected NavigationFilter getNavigationFilter() {
        if (navigationFilter == null) {
            navigationFilter = new DefaultNavigationFilter(this);
        }
        return navigationFilter;
    }

    public Object clone() throws CloneNotSupportedException {
        DefaultFormatter formatter = (DefaultFormatter) super.clone();
        formatter.documentFilter = null;
        formatter.navigationFilter = null;
        return formatter;
    }

    /** Where the cursor starts on installing. */
    void positionCursorAtInitialLocation() {
        JFormattedTextField ftf = getFormattedTextField();
        if (ftf != null) {
            ftf.setCaretPosition(getInitialVisualPosition());
        }
    }

    int getInitialVisualPosition() {
        return 0;
    }

    /**
     * It forwards to {@link #invalidEdit()} from the nested classes.
     *
     * <p>The filter is a nested class and our compiler does not yet let it touch a
     * {@code protected} member inherited from another package (finding #512 in
     * <code>COMPILER_FINDINGS.md</code>).
     */
    void reportInvalid() {
        invalidEdit();
    }

    /** Whether the text that would be left is acceptable. */
    boolean isValidEdit(String text) {
        if (!getAllowsInvalid()) {
            try {
                stringToValue(text);
            } catch (ParseException pe) {
                return false;
            }
        }
        return true;
    }

    /** It passes the text to the value, if that was asked for. */
    void commitEdit() {
        JFormattedTextField ftf = getFormattedTextField();
        if (ftf != null) {
            try {
                ftf.commitEdit();
                setEditValid(true);
            } catch (ParseException pe) {
                setEditValid(false);
            }
        }
    }

    /** It marks whether what has been typed so far converts. */
    void updateValue(String text) {
        try {
            stringToValue(text);
            setEditValid(true);
            if (getCommitsOnValidEdit()) {
                commitEdit();
            }
        } catch (ParseException pe) {
            setEditValid(false);
        }
    }

    /**
     * It filters the typing: it validates before letting through and overwrites if it applies.
     *
     * <p>In the JDK it is an inner class; here it is static with the formatter as its first
     * parameter. It is not public, so the signature is not seen from outside.
     */
    static class DefaultDocumentFilter extends DocumentFilter implements Serializable {

        private final DefaultFormatter fmt;

        DefaultDocumentFilter(DefaultFormatter fmt) {
            this.fmt = fmt;
        }

        public void remove(DocumentFilter.FilterBypass fb, int offset, int length)
                throws BadLocationException {
            String current = fb.getDocument().getText(0, fb.getDocument().getLength());
            String left = current.substring(0, offset) + current.substring(offset + length);
            if (fmt.isValidEdit(left)) {
                fb.remove(offset, length);
                fmt.updateValue(left);
            } else {
                fmt.reportInvalid();
            }
        }

        public void insertString(DocumentFilter.FilterBypass fb, int offset, String string,
                AttributeSet attr) throws BadLocationException {
            replace(fb, offset, 0, string, attr);
        }

        public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text,
                AttributeSet attr) throws BadLocationException {
            if (text == null) {
                text = "";
            }
            Document doc = fb.getDocument();
            String current = doc.getText(0, doc.getLength());
            int end = offset + length;
            if (fmt.getOverwriteMode() && length == 0) {
                // Overwriting: what is typed eats what is ahead.
                end = Math.min(offset + text.length(), current.length());
            }
            String left = current.substring(0, offset) + text + current.substring(end);
            if (fmt.isValidEdit(left)) {
                fb.replace(offset, end - offset, text, attr);
                fmt.updateValue(left);
            } else {
                fmt.reportInvalid();
            }
        }
    }

    /** It filters the cursor's movement; the usual one does not limit it. */
    static class DefaultNavigationFilter extends NavigationFilter implements Serializable {

        private final DefaultFormatter fmt;

        DefaultNavigationFilter(DefaultFormatter fmt) {
            this.fmt = fmt;
        }

        public void setDot(NavigationFilter.FilterBypass fb, int dot, Position.Bias bias) {
            fb.setDot(dot, bias);
        }

        public void moveDot(NavigationFilter.FilterBypass fb, int dot, Position.Bias bias) {
            fb.moveDot(dot, bias);
        }
    }
}
