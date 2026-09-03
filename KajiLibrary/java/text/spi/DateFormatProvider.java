package java.text.spi;

import java.text.DateFormat;
import java.util.Locale;
import java.util.spi.LocaleServiceProvider;

/**
 * KajiLibrary's java.text.spi.DateFormatProvider -- como se escribe una fecha o una hora.
 *
 * <p>Los estilos --{@code FULL}, {@code LONG}, {@code MEDIUM}, {@code SHORT}-- no son "mas o menos
 * largo": son <b>cuatro formatos distintos</b> que cada cultura define por su cuenta. El corto de
 * Estados Unidos es mes/dia/año y el de casi todo el resto es dia/mes/año, asi que la misma cadena
 * {@code "03/04/2026"} son dos fechas distintas segun quien la lea. No hay forma de acertar sin
 * saber el local, y ese es todo el punto de esta clase.
 */
public abstract class DateFormatProvider extends LocaleServiceProvider {

    protected DateFormatProvider() {
    }

    /**
     * Solo la hora.
     *
     * @param style uno de los cuatro de {@code DateFormat}
     */
    public abstract DateFormat getTimeInstance(int style, Locale locale);

    /** Solo la fecha. */
    public abstract DateFormat getDateInstance(int style, Locale locale);

    /** Las dos, cada una con su estilo. */
    public abstract DateFormat getDateTimeInstance(int dateStyle, int timeStyle, Locale locale);
}
