package javax.swing;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

/**
 * A sequence of dates for a {@link JSpinner}.
 *
 * <h2>The field decides how much it advances by</h2>
 *
 * <p>{@link #setCalendarField} chooses what is added: {@link Calendar#DAY_OF_MONTH} advances one
 * day at a time, {@link Calendar#MONTH} one month at a time. It is not the same as adding a
 * fixed number of milliseconds -- a month lasts differently according to which it is, and a day
 * lasts differently when the summer time changes. That is why {@link Calendar} advances, which
 * knows about calendars, and not a subtraction of times.
 *
 * <p>That field is also what the control changes by itself while editing: standing on the month,
 * the arrows move months; standing on the year, years. Hence it is a property and not a
 * parameter.
 *
 * <h2>The bounds and the value</h2>
 *
 * <p>As in {@link SpinnerNumberModel}, the bounds do not clip: they only make the arrow return
 * null on going past.
 */
public class SpinnerDateModel extends AbstractSpinnerModel implements Serializable {

    private Comparable<Date> start;
    private Comparable<Date> end;
    private Calendar value;
    private int calendarField;

    /**
     * The {@link Calendar} fields that can be added.
     *
     * <p>It is a table and not a {@code switch} because they are constants of another class.
     */
    private static final int[] FIELDS = {
        Calendar.ERA,
        Calendar.YEAR,
        Calendar.MONTH,
        Calendar.WEEK_OF_YEAR,
        Calendar.WEEK_OF_MONTH,
        Calendar.DAY_OF_MONTH,
        Calendar.DAY_OF_YEAR,
        Calendar.DAY_OF_WEEK,
        Calendar.DAY_OF_WEEK_IN_MONTH,
        Calendar.AM_PM,
        Calendar.HOUR,
        Calendar.HOUR_OF_DAY,
        Calendar.MINUTE,
        Calendar.SECOND,
        Calendar.MILLISECOND,
    };

    private static boolean validField(int calendarField) {
        for (int i = 0; i < FIELDS.length; i++) {
            if (FIELDS[i] == calendarField) {
                return true;
            }
        }
        return false;
    }

    /**
     * With date, bounds and field.
     *
     * <p>The bounds may be null, which is like saying "no cap".
     *
     * @throws IllegalArgumentException if the date is null, if the field is not one of those that
     *     can be added, or if start &lt;= date &lt;= end does not hold.
     */
    public SpinnerDateModel(Date value, Comparable<Date> start, Comparable<Date> end,
            int calendarField) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (!validField(calendarField)) {
            throw new IllegalArgumentException("invalid calendarField");
        }
        if (!(((start == null) || (start.compareTo(value) <= 0))
                && ((end == null) || (end.compareTo(value) >= 0)))) {
            throw new IllegalArgumentException("(start <= value <= end) is false");
        }
        this.value = Calendar.getInstance();
        this.start = start;
        this.end = end;
        this.calendarField = calendarField;
        this.value.setTime(value);
    }

    /** From now on, one day at a time, with no caps. */
    public SpinnerDateModel() {
        this(new Date(), null, null, Calendar.DAY_OF_MONTH);
    }

    /** The earliest date; null removes the cap. */
    public void setStart(Comparable<Date> start) {
        if ((start == null) ? (this.start != null) : !start.equals(this.start)) {
            this.start = start;
            fireStateChanged();
        }
    }

    public Comparable<Date> getStart() {
        return start;
    }

    /** The latest date; null removes the cap. */
    public void setEnd(Comparable<Date> end) {
        if ((end == null) ? (this.end != null) : !end.equals(this.end)) {
            this.end = end;
            fireStateChanged();
        }
    }

    public Comparable<Date> getEnd() {
        return end;
    }

    /**
     * How much each arrow advances by; see the class note.
     *
     * @throws IllegalArgumentException if it is not a field that can be added.
     */
    public void setCalendarField(int calendarField) {
        if (!validField(calendarField)) {
            throw new IllegalArgumentException("invalid calendarField");
        }
        if (calendarField != this.calendarField) {
            this.calendarField = calendarField;
            fireStateChanged();
        }
    }

    public int getCalendarField() {
        return calendarField;
    }

    /** It adds one of the chosen field, in that direction. */
    private Date run(int dir) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(value.getTime());
        cal.add(calendarField, dir);
        return cal.getTime();
    }

    /** The next one, or null if it goes past the end. */
    public Object getNextValue() {
        Date next = run(1);
        return ((end == null) || (end.compareTo(next) >= 0)) ? next : null;
    }

    /** The previous one, or null if it goes past the start. */
    public Object getPreviousValue() {
        Date prev = run(-1);
        return ((start == null) || (start.compareTo(prev) <= 0)) ? prev : null;
    }

    /** The date, already as a {@link Date}. */
    public Date getDate() {
        return value.getTime();
    }

    public Object getValue() {
        return value.getTime();
    }

    /**
     * It changes the date.
     *
     * @throws IllegalArgumentException if it is not a {@link Date}.
     */
    public void setValue(Object value) {
        if ((value == null) || !(value instanceof Date)) {
            throw new IllegalArgumentException("illegal value");
        }
        if (!value.equals(this.value.getTime())) {
            this.value.setTime((Date) value);
            fireStateChanged();
        }
    }
}
