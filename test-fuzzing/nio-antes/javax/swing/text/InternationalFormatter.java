package javax.swing.text;

import java.text.AttributedCharacterIterator;
import java.text.Format;
import java.text.Format$Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JFormattedTextField;

/**
 * A formatter that delegates to a {@code java.text} {@link Format}.
 *
 * <h2>What it adds over {@link DefaultFormatter}</h2>
 *
 * <p>Three things. First, the conversion is done by the {@code Format}, which knows about
 * language: thousands separators, month names, currency signs. Second, a range:
 * {@link #setMinimum} and {@link #setMaximum} reject values outside it. Third, and this is the
 * interesting one, it knows <em>which part</em> of the text is which: {@link #getFields} says
 * whether position 3 falls in the month or in the year.
 *
 * <h2>What knowing the fields is for</h2>
 *
 * <p>For moving between fields with the arrows and for increasing and decreasing the value of a
 * single one. Without that, a date field would be just any string and the arrows could do
 * nothing better than move the cursor one letter.
 */
public class InternationalFormatter extends DefaultFormatter {

    private static final Format$Field[] EMPTY_FIELD_ARRAY = new Format$Field[0];

    private Format format;
    private Comparable<?> min;
    private Comparable<?> max;
    private transient Map<Format$Field, Object> literalMask;
    private transient AttributedCharacterIterator iterator;
    private transient boolean validMask;
    private transient String string;

    /** A formatter with no format; it behaves like the usual one until one is set on it. */
    public InternationalFormatter() {
        setOverwriteMode(false);
    }

    /** A formatter that uses that format. */
    public InternationalFormatter(Format format) {
        this();
        setFormat(format);
    }

    /** The format that converts between value and text. */
    public void setFormat(Format format) {
        this.format = format;
    }

    public Format getFormat() {
        return format;
    }

    /**
     * The smallest acceptable value.
     *
     * <p>If the current value is smaller, it is raised to the minimum: leaving it out of range
     * would make the field show something it itself declares invalid.
     */
    public void setMinimum(Comparable<?> minimum) {
        if (getValueClass() == null && minimum != null) {
            setValueClass(minimum.getClass());
        }
        min = minimum;
    }

    public Comparable<?> getMinimum() {
        return min;
    }

    /** The largest acceptable value. */
    public void setMaximum(Comparable<?> max) {
        if (getValueClass() == null && max != null) {
            setValueClass(max.getClass());
        }
        this.max = max;
    }

    public Comparable<?> getMaximum() {
        return max;
    }

    public void install(JFormattedTextField ftf) {
        super.install(ftf);
        updateMaskIfNecessary();
    }

    /** The value's text, according to the format. */
    public String valueToString(Object value) throws ParseException {
        if (value == null) {
            return "";
        }
        Format f = getFormat();
        if (f == null) {
            return value.toString();
        }
        return f.format(value);
    }

    /**
     * That text's value, within the range.
     *
     * @throws ParseException if the text cannot be converted or falls outside the range.
     */
    public Object stringToValue(String text) throws ParseException {
        Object value = stringToValue(text, getFormat());

        // First the type, then the range. A Format returns whatever type it likes (a NumberFormat
                // always gives Long or Double), so comparing before converting would be comparing
                // an Integer with a Long and ending in an invalid conversion, not in a bad range.
        Class<?> vc = getValueClass();
        if (value != null && vc != null && !vc.isInstance(value)) {
            value = super.stringToValue(value.toString());
        }
        try {
            if (!isValidValue(value, true)) {
                throw new ParseException("Value not within min/max range", 0);
            }
        } catch (ClassCastException cce) {
            throw new ParseException("Class cast exception comparing values: " + cce, 0);
        }
        return value;
    }

    private Object stringToValue(String text, Format f) throws ParseException {
        if (f == null) {
            return super.stringToValue(text);
        }
        return f.parseObject(text);
    }

    /** Whether the value falls within the range. */
    private boolean isValidValue(Object value, boolean wantsCCE) {
        Comparable<Object> min = (Comparable<Object>) getMinimum();
        try {
            if (min != null && min.compareTo(value) > 0) {
                return false;
            }
        } catch (ClassCastException cce) {
            if (wantsCCE) {
                throw cce;
            }
            return false;
        }
        Comparable<Object> max = (Comparable<Object>) getMaximum();
        try {
            if (max != null && max.compareTo(value) < 0) {
                return false;
            }
        } catch (ClassCastException cce) {
            if (wantsCCE) {
                throw cce;
            }
            return false;
        }
        return true;
    }

    /**
     * The format fields that cover that position of the text.
     *
     * <p>There may be more than one: nested formats make a position belong to a field and to the
     * one that contains it.
     */
    public Format$Field[] getFields(int offset) {
        if (getAllowsInvalid()) {
            // The text may not correspond to the format: the walk is rebuilt.
            updateMask();
        }
        AttributedCharacterIterator iterator = getIterator();
        if (iterator != null && offset >= 0 && offset <= iterator.getEndIndex()) {
            iterator.setIndex(offset);
            Map<AttributedCharacterIterator.Attribute, Object> attrs = iterator.getAttributes();
            if (attrs != null && attrs.size() > 0) {
                List<Format$Field> al = new ArrayList<Format$Field>();
                for (AttributedCharacterIterator.Attribute key : attrs.keySet()) {
                    if (key instanceof Format$Field) {
                        al.add((Format$Field) key);
                    }
                }
                if (al.size() > 0) {
                    return al.toArray(EMPTY_FIELD_ARRAY);
                }
            }
        }
        return EMPTY_FIELD_ARRAY;
    }

    public Object clone() throws CloneNotSupportedException {
        InternationalFormatter formatter = (InternationalFormatter) super.clone();
        formatter.literalMask = null;
        formatter.iterator = null;
        formatter.validMask = false;
        formatter.string = null;
        return formatter;
    }

    /** The actions that increase and decrease the field the cursor is in. */
    protected Action[] getActions() {
        if (getSupportsIncrement()) {
            return new Action[] {
                new IncrementAction(this, "increment", 1),
                new IncrementAction(this, "decrement", -1)
            };
        }
        return null;
    }

    /** Whether increasing and decreasing the value with the arrows makes sense. */
    boolean getSupportsIncrement() {
        return false;
    }

    /**
     * It forwards to {@link #getFormattedTextField()} from the nested classes.
     *
     * <p>Same reason as in {@link DefaultFormatter}: finding #512.
     */
    JFormattedTextField field() {
        return getFormattedTextField();
    }

    AttributedCharacterIterator getIterator() {
        return iterator;
    }

    void updateMaskIfNecessary() {
        Format f = getFormat();
        if (f != null) {
            if (!validMask) {
                updateMask();
            } else {
                String text = getFormattedTextField() == null ? null
                        : getFormattedTextField().getText();
                if (text == null || !text.equals(string)) {
                    updateMask();
                }
            }
        }
    }

    /** It rebuilds the walk of fields from the current text. */
    void updateMask() {
        Format f = getFormat();
        validMask = false;
        iterator = null;
        string = null;
        if (f == null) {
            return;
        }
        JFormattedTextField ftf = getFormattedTextField();
        if (ftf == null) {
            return;
        }
        String text = ftf.getText();
        if (text == null) {
            return;
        }
        try {
            Object value = stringToValue(text, f);
            iterator = f.formatToCharacterIterator(value);
            string = text;
            validMask = true;
        } catch (Exception e) {
            // The text does not correspond to the format: there are no fields to report.
            iterator = null;
        }
    }

    /**
     * It increases or decreases the value.
     *
     * <p>The action of the up and down arrows. Who knows how to increment is the subclass: in a
     * number it is adding one, in a date it depends on the field the cursor is in.
     */
    static class IncrementAction extends javax.swing.AbstractAction {

        private final InternationalFormatter fmt;
        private int direction;

        IncrementAction(InternationalFormatter fmt, String name, int direction) {
            super(name);
            this.fmt = fmt;
            this.direction = direction;
        }

        public void actionPerformed(java.awt.event.ActionEvent ae) {
            JFormattedTextField ftf = fmt.field();
            if (ftf != null && ftf.isEditable()) {
                fmt.adjustValue(direction);
            }
        }
    }

    /** It adds that amount to the value; the subclass that knows how overrides it. */
    void adjustValue(int direction) {
    }
}
