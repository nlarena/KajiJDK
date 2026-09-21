package javax.swing.text;

import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;

/**
 * A number formatter.
 *
 * <h2>The problem of the type</h2>
 *
 * <p>A {@link NumberFormat} always returns a {@code Long} or a {@code Double}, never an
 * {@code Integer} or a {@code BigDecimal}. If the field had an {@code Integer} and the user types
 * another number, without correcting the type the value would change class halfway and the
 * program that reads it would blow up with an invalid conversion.
 *
 * <p>That is why {@link #stringToValue} converts the result again to the class
 * {@link #getValueClass} says. That is also what gives sense to
 * {@code new NumberFormatter().setValueClass(Integer.class)}: without it, the type would be
 * chosen by the format and not by whoever uses the field.
 */
public class NumberFormatter extends InternationalFormatter {

    /** A formatter with the number format of the system's language. */
    public NumberFormatter() {
        this(NumberFormat.getNumberInstance());
    }

    /** A formatter that uses that number format. */
    public NumberFormatter(NumberFormat format) {
        super(format);
        setFormat(format);
        setValueClass(null);
    }

    /** The format; a {@link NumberFormat} is expected. */
    public void setFormat(Format format) {
        super.setFormat(format);
    }

    boolean getSupportsIncrement() {
        return true;
    }

    /**
     * The text's value, converted to the requested class.
     *
     * @throws ParseException if the text is not a number or does not fit the requested class.
     */
    public Object stringToValue(String text) throws ParseException {
        // The conversion to the requested type is done by {@link InternationalFormatter}, which
                // needs it before comparing the range. Repeating it here would give two paths that
                // may not agree.
        return super.stringToValue(text);
    }

    /** It turns the number into that class, without silently losing what does not fit. */
    private Object convert(Number n, Class<?> vc) throws ParseException {
        if (vc == Integer.class) {
            return Integer.valueOf(n.intValue());
        }
        if (vc == Long.class) {
            return Long.valueOf(n.longValue());
        }
        if (vc == Short.class) {
            return Short.valueOf(n.shortValue());
        }
        if (vc == Byte.class) {
            return Byte.valueOf(n.byteValue());
        }
        if (vc == Float.class) {
            return Float.valueOf(n.floatValue());
        }
        if (vc == Double.class) {
            return Double.valueOf(n.doubleValue());
        }
        if (vc == java.math.BigInteger.class) {
            return java.math.BigInteger.valueOf(n.longValue());
        }
        if (vc == java.math.BigDecimal.class) {
            return new java.math.BigDecimal(n.toString());
        }
        // A class that is not known: the constructor taking a string is tried.
        return super.stringToValue(n.toString());
    }

    /** It adds or subtracts one from the number. */
    void adjustValue(int direction) {
        javax.swing.JFormattedTextField ftf = getFormattedTextField();
        if (ftf == null) {
            return;
        }
        Object value = ftf.getValue();
        if (!(value instanceof Number)) {
            return;
        }
        Number n = (Number) value;
        Object newValue;
        if (n instanceof Double || n instanceof Float) {
            newValue = Double.valueOf(n.doubleValue() + direction);
        } else {
            newValue = Long.valueOf(n.longValue() + direction);
        }
        try {
            Class<?> vc = getValueClass();
            if (vc != null) {
                newValue = convert((Number) newValue, vc);
            }
            ftf.setText(valueToString(newValue));
            ftf.commitEdit();
        } catch (ParseException pe) {
            // The new number could not be formatted: the previous one is left.
        }
    }
}
