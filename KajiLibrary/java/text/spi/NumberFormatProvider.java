package java.text.spi;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.spi.LocaleServiceProvider;

/**
 * KajiLibrary's java.text.spi.NumberFormatProvider -- como se escribe un numero.
 *
 * <p>Cuatro formas obligatorias y una con default, y las cuatro son distintas de verdad y no solo de
 * decoracion: la de moneda pone el simbolo donde esa cultura lo pone --antes en ingles, despues en
 * frances-- y con los decimales que la moneda usa; la entera redondea en vez de truncar; la de
 * porcentaje multiplica por cien.
 *
 * <p>{@link #getCompactNumberInstance} tiene default y lanza: es la que escribe {@code "1,2 M"} en
 * vez de {@code "1200000"}, llego mucho despues que el resto, y un proveedor viejo que no la conozca
 * tiene que seguir compilando. Lanzar --en vez de devolver el formato normal-- es lo correcto:
 * devolver {@code "1200000"} donde se pidio la forma compacta rompe la maqueta de quien la pidio, y
 * en silencio.
 */
public abstract class NumberFormatProvider extends LocaleServiceProvider {

    protected NumberFormatProvider() {
    }

    /** Con simbolo de moneda, donde esa cultura lo pone. */
    public abstract NumberFormat getCurrencyInstance(Locale locale);

    /** Entero. Redondea, no trunca. */
    public abstract NumberFormat getIntegerInstance(Locale locale);

    /** El de proposito general. */
    public abstract NumberFormat getNumberInstance(Locale locale);

    /** Porcentaje: multiplica por cien y agrega el signo. */
    public abstract NumberFormat getPercentInstance(Locale locale);

    /**
     * La forma compacta.
     *
     * @throws UnsupportedOperationException por omision; ver la nota de la clase para por que lanza
     *     en vez de caer al formato normal
     */
    public NumberFormat getCompactNumberInstance(Locale locale, NumberFormat.Style formatStyle) {
        throw new UnsupportedOperationException(
            "The " + getClass().getName() + " should override this method");
    }
}
