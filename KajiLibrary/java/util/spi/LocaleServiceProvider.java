package java.util.spi;

import java.util.Locale;

/**
 * KajiLibrary's java.util.spi.LocaleServiceProvider -- la raiz de los proveedores de datos locales.
 *
 * <p>Es lo que permite que una aplicacion agregue soporte para un idioma que el runtime no trae, o
 * que corrija el que trae. Todo lo dependiente de idioma que hay en {@code java.util} y
 * {@code java.text} --nombres de meses, simbolos de moneda, reglas de ordenamiento-- se puede
 * reemplazar por esta via, y ninguna de las clases que los usan tiene que enterarse.
 *
 * <h2>Por que existe isSupportedLocale ademas de getAvailableLocales</h2>
 *
 * <p>Parece redundante y no lo es. {@code getAvailableLocales()} devuelve una lista <b>finita</b>, y
 * hay locales que no se pueden enumerar: los que llevan extensiones Unicode
 * ({@code es-AR-u-ca-buddhist}) forman un conjunto infinito. El default de
 * {@link #isSupportedLocale} compara contra la lista despues de sacar las extensiones, que es lo
 * correcto para casi todos; un proveedor que sepa contestar por extension lo sobrescribe.
 *
 * <p><b>Esta biblioteca no registra ningun proveedor.</b> Las clases estan para que uno que se
 * escriba encaje.
 */
public abstract class LocaleServiceProvider {

    protected LocaleServiceProvider() {
    }

    /**
     * Los locales para los que este proveedor tiene datos.
     *
     * <p>Tiene que incluir {@code Locale.ROOT} o no; lo que no puede es devolver null.
     */
    public abstract Locale[] getAvailableLocales();

    /**
     * Si este proveedor sirve para ese local.
     *
     * <p>El default busca en {@link #getAvailableLocales()} el local <b>sin sus extensiones</b>. Ver
     * la nota de la clase para por que no alcanza con la lista sola.
     */
    public boolean isSupportedLocale(Locale locale) {
        // Sin extensiones: `es-AR-u-ca-buddhist` lo atiende el mismo proveedor que `es-AR`.
        locale = locale.stripExtensions();
        Locale[] available = getAvailableLocales();
        int i = 0;
        while (i < available.length) {
            if (locale.equals(available[i].stripExtensions())) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }
}
