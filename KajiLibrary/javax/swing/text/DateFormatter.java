package javax.swing.text;

import java.text.DateFormat;
import java.text.Format;
import java.util.Calendar;
import java.util.Date;

/**
 * Un formateador de fechas.
 *
 * <h2>Casi todo lo hereda</h2>
 *
 * <p>Lo unico que agrega sobre {@link InternationalFormatter} es saber subir y bajar el campo donde
 * esta el cursor: en una fecha, la flecha arriba sobre el mes suma un mes, no un dia. Para eso
 * traduce el campo del formato al campo del {@link Calendar} correspondiente.
 *
 * <p>Sumar un mes no es sumar treinta dias, y por eso la cuenta la hace el {@code Calendar} y no
 * una suma de milisegundos.
 */
public class DateFormatter extends InternationalFormatter {

    /** Un formateador con el formato de fecha corto del idioma del sistema. */
    public DateFormatter() {
        this(DateFormat.getDateInstance());
    }

    /** Un formateador que usa ese formato de fecha. */
    public DateFormatter(DateFormat format) {
        super(format);
        setFormat(format);
    }

    /** El formato de fecha; el valor debe ser un {@link Date}. */
    public void setFormat(DateFormat format) {
        super.setFormat(format);
    }

    boolean getSupportsIncrement() {
        return true;
    }

    /**
     * Suma o resta uno al campo donde esta el cursor.
     *
     * <p>Si el cursor no cae en ningun campo conocido, no hace nada: adivinar cual mover seria
     * peor que no moverse.
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
        int campo = calendarField(getFields(ftf.getCaretPosition()));
        if (campo == -1) {
            return;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime((Date) value);
        cal.add(campo, direction);
        try {
            ftf.setText(valueToString(cal.getTime()));
            ftf.commitEdit();
        } catch (java.text.ParseException pe) {
            // La fecha nueva no se pudo formatear: se deja la anterior.
        }
    }

    /** El campo del calendario que corresponde a esos campos del formato. */
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
