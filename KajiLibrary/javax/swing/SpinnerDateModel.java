package javax.swing;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

/**
 * Una secuencia de fechas para un {@link JSpinner}.
 *
 * <h2>El campo decide de cuanto avanza</h2>
 *
 * <p>{@link #setCalendarField} elige que se suma: {@link Calendar#DAY_OF_MONTH} avanza de a un dia,
 * {@link Calendar#MONTH} de a un mes. No es lo mismo que sumar una cantidad fija de milisegundos --
 * un mes dura distinto segun cual sea, y un dia dura distinto cuando cambia el horario de verano.
 * Por eso avanza {@link Calendar}, que sabe de calendarios, y no una resta de tiempos.
 *
 * <p>Ese campo es tambien lo que el control cambia solo mientras se edita: parado sobre el mes, las
 * flechas mueven meses; parado sobre el año, años. De ahi que sea una propiedad y no un parametro.
 *
 * <h2>Los limites y el valor</h2>
 *
 * <p>Como en {@link SpinnerNumberModel}, los limites no recortan: solo hacen que la flecha devuelva
 * nulo al pasarse.
 */
public class SpinnerDateModel extends AbstractSpinnerModel implements Serializable {

    private Comparable<Date> start;
    private Comparable<Date> end;
    private Calendar value;
    private int calendarField;

    /**
     * Los campos de {@link Calendar} que se pueden sumar.
     *
     * <p>Es una tabla y no un {@code switch} porque son constantes de otra clase.
     */
    private static final int[] CAMPOS = {
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

    private static boolean campoValido(int calendarField) {
        for (int i = 0; i < CAMPOS.length; i++) {
            if (CAMPOS[i] == calendarField) {
                return true;
            }
        }
        return false;
    }

    /**
     * Con fecha, limites y campo.
     *
     * <p>Los limites pueden ser nulos, que es como decir "sin tope".
     *
     * @throws IllegalArgumentException si la fecha es nula, si el campo no es uno de los que se
     *     pueden sumar, o si no se cumple inicio &lt;= fecha &lt;= fin.
     */
    public SpinnerDateModel(Date value, Comparable<Date> start, Comparable<Date> end,
            int calendarField) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (!campoValido(calendarField)) {
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

    /** Desde ahora, de a un dia, sin topes. */
    public SpinnerDateModel() {
        this(new Date(), null, null, Calendar.DAY_OF_MONTH);
    }

    /** La fecha mas temprana; nulo quita el tope. */
    public void setStart(Comparable<Date> start) {
        if ((start == null) ? (this.start != null) : !start.equals(this.start)) {
            this.start = start;
            fireStateChanged();
        }
    }

    public Comparable<Date> getStart() {
        return start;
    }

    /** La fecha mas tardia; nulo quita el tope. */
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
     * De cuanto avanza cada flecha; ver la nota de la clase.
     *
     * @throws IllegalArgumentException si no es un campo que se pueda sumar.
     */
    public void setCalendarField(int calendarField) {
        if (!campoValido(calendarField)) {
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

    /** Suma uno del campo elegido, en esa direccion. */
    private Date correr(int dir) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(value.getTime());
        cal.add(calendarField, dir);
        return cal.getTime();
    }

    /** La siguiente, o nulo si pasa el fin. */
    public Object getNextValue() {
        Date next = correr(1);
        return ((end == null) || (end.compareTo(next) >= 0)) ? next : null;
    }

    /** La anterior, o nulo si pasa el inicio. */
    public Object getPreviousValue() {
        Date prev = correr(-1);
        return ((start == null) || (start.compareTo(prev) <= 0)) ? prev : null;
    }

    /** La fecha, ya como {@link Date}. */
    public Date getDate() {
        return value.getTime();
    }

    public Object getValue() {
        return value.getTime();
    }

    /**
     * Cambia la fecha.
     *
     * @throws IllegalArgumentException si no es una {@link Date}.
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
