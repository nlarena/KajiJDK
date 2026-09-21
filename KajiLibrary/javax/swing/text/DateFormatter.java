package javax.swing.text;

import java.text.DateFormat;
import java.text.Format;
import java.util.Calendar;
import java.util.Date;

/**
 * A date formatter.
 *
 * <h2>It inherits almost everything</h2>
 *
 * <p>The only thing it adds over {@link InternationalFormatter} is knowing how to increase and
 * decrease the field the cursor is in: in a date, the up arrow over the month adds a month, not a
 * day. For that it translates the format's field into the corresponding {@link Calendar} field.
 *
 * <p>Adding a month is not adding thirty days, and that is why the {@code Calendar} does the sum
 * and not an addition of milliseconds.
 */
public class DateFormatter extends InternationalFormatter {

    /** A formatter with the short date format of the system's language. */
    public DateFormatter() {
        this(DateFormat.getDateInstance());
    }

    /** A formatter that uses that date format. */
    public DateFormatter(DateFormat format) {
        super(format);
        setFormat(format);
    }

    /** The date format; the value must be a {@link Date}. */
    public void setFormat(DateFormat format) {
        super.setFormat(format);
    }

    boolean getSupportsIncrement() {
        return true;
    }

    /**
     * It adds or subtracts one from the field the cursor is in.
     *
     * <p>If the cursor falls in no known field, it does nothing: guessing which one to move would
     * be worse than not moving.
     */
    void adjustValue(int direction) {
        javax.swing.JFormattedTextField ftf = getFormattedTextField();
        if (ftf == null) {
            return;
        }
        Object value = ftf.getValue();
        if (!(value instanceof Date)) {
            return;
        }
        int field = calendarField(getFields(ftf.getCaretPosition()));
        if (field == -1) {
            return;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime((Date) value);
        cal.add(field, direction);
        try {
            ftf.setText(valueToString(cal.getTime()));
            ftf.commitEdit();
        } catch (java.text.ParseException pe) {
            // The new date could not be formatted: the previous one is left.
        }
    }

    /** The calendar field that corresponds to those format fields. */
    private int calendarField(Format.Field[] fields) {
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] instanceof DateFormat.Field) {
                int cf = ((DateFormat.Field) fields[i]).getCalendarField();
                if (cf != -1) {
                    return cf;
                }
            }
        }
        return -1;
    }
}
