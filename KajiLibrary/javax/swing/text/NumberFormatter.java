package javax.swing.text;

import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;

/**
 * Un formateador de numeros.
 *
 * <h2>El problema del tipo</h2>
 *
 * <p>Un {@link NumberFormat} devuelve siempre un {@code Long} o un {@code Double}, nunca un
 * {@code Integer} ni un {@code BigDecimal}. Si el campo tenia un {@code Integer} y el usuario
 * escribe otro numero, sin corregir el tipo el valor cambiaria de clase a mitad de camino y el
 * programa que lo lee reventaria con una conversion invalida.
 *
 * <p>Por eso {@link #stringToValue} vuelve a convertir el resultado a la clase que dice
 * {@link #getValueClass}. Eso ademas es lo que le da sentido a
 * {@code new NumberFormatter().setValueClass(Integer.class)}: sin eso, el tipo lo elegiria el
 * formato y no quien usa el campo.
 */
public class NumberFormatter extends InternationalFormatter {

    /** Un formateador con el formato de numeros del idioma del sistema. */
    public NumberFormatter() {
        this(NumberFormat.getNumberInstance());
    }

    /** Un formateador que usa ese formato de numeros. */
    public NumberFormatter(NumberFormat format) {
        super(format);
        setFormat(format);
        setValueClass(null);
    }

    /** El formato; se espera un {@link NumberFormat}. */
    public void setFormat(Format format) {
        super.setFormat(format);
    }

    boolean getSupportsIncrement() {
        return true;
    }

    /**
     * El valor del texto, convertido a la clase pedida.
     *
     * @throws ParseException si el texto no es un numero o no entra en la clase pedida.
     */
    public Object stringToValue(String text) throws ParseException {
        // La conversion al tipo pedido la hace {@link InternationalFormatter}, que la necesita
        // antes de comparar el rango. Repetirla aca daria dos caminos que pueden no coincidir.
        return super.stringToValue(text);
    }

    /** Pasa el numero a esa clase, sin perder lo que no cabe en silencio. */
    private Object convertir(Number n, Class<?> vc) throws ParseException {
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
        // Una clase que no se conoce: se prueba el constructor de una cadena.
        return super.stringToValue(n.toString());
    }

    /** Suma o resta uno al numero. */
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
        Object nuevo;
        if (n instanceof Double || n instanceof Float) {
            nuevo = Double.valueOf(n.doubleValue() + direction);
        } else {
            nuevo = Long.valueOf(n.longValue() + direction);
        }
        try {
            Class<?> vc = getValueClass();
            if (vc != null) {
                nuevo = convertir((Number) nuevo, vc);
            }
            ftf.setText(valueToString(nuevo));
            ftf.commitEdit();
        } catch (ParseException pe) {
            // El numero nuevo no se pudo formatear: se deja el anterior.
        }
    }
}
