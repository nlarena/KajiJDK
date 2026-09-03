package java.util.spi;

import java.util.Locale;
import java.util.Map;

/**
 * KajiLibrary's java.util.spi.CalendarNameProvider -- los nombres de los campos de un calendario.
 *
 * <p>Meses, dias de la semana, AM/PM, eras. Lo que devuelve depende de tres cosas a la vez: el campo,
 * el estilo --largo, corto, narrow-- y el local.
 *
 * <h2>El estilo standalone, que es lo que no es obvio</h2>
 *
 * <p>Varios idiomas escriben el nombre de un mes distinto segun aparezca <b>solo</b> --en el
 * encabezado de un calendario-- o <b>dentro de una fecha</b>. En ruso el genitivo; en checo y en
 * finlandes lo mismo. Por eso los estilos vienen de a pares, con y sin
 * {@code Calendar.STANDALONE_MASK}, y un proveedor que devuelva lo mismo para los dos esta bien en
 * español y mal en ruso.
 *
 * <h2>getDisplayNames es la direccion inversa</h2>
 *
 * <p>{@link #getDisplayName} traduce un valor a un nombre; {@link #getDisplayNames} devuelve el mapa
 * <b>nombre a valor</b>, y sirve para <b>parsear</b>. Por eso puede tener mas entradas que valores:
 * varias formas del mismo mes apuntan al mismo numero.
 */
public abstract class CalendarNameProvider extends LocaleServiceProvider {

    protected CalendarNameProvider() {
    }

    /**
     * El nombre de ese valor de ese campo.
     *
     * @param calendarType el tipo de calendario: {@code "gregory"}, {@code "buddhist"}, ...
     * @param field        el campo de {@code Calendar}
     * @param value        el valor del campo
     * @param style        el estilo, con o sin {@code STANDALONE_MASK}; ver la nota de la clase
     * @return null si este proveedor no tiene ese nombre
     */
    public abstract String getDisplayName(String calendarType, int field, int value, int style,
        Locale locale);

    /**
     * El mapa nombre a valor, para parsear. Ver la nota de la clase.
     *
     * @return null si este proveedor no tiene esos nombres
     */
    public abstract Map<String, Integer> getDisplayNames(String calendarType, int field, int style,
        Locale locale);
}
