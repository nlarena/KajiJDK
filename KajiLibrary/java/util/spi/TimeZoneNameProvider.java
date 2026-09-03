package java.util.spi;

import java.util.Locale;

/**
 * KajiLibrary's java.util.spi.TimeZoneNameProvider -- como se llama una zona horaria.
 *
 * <h2>Especifico contra generico, que son cosas distintas</h2>
 *
 * <ul>
 *   <li>{@link #getDisplayName} da el nombre <b>de una de las dos mitades del año</b>: "hora
 *       estandar del este" o "hora de verano del este". El booleano elige cual.
 *   <li>{@link #getGenericDisplayName} da el que sirve para las dos: "hora del este". Es el que hay
 *       que mostrar cuando no hay una fecha concreta -- en un selector de zona, por ejemplo, donde
 *       decir "hora de verano" seria falso la mitad del año.
 * </ul>
 *
 * <p>El generico tiene default y devuelve null porque muchas zonas no tienen uno: una zona sin
 * horario de verano no necesita distinguir, y otras simplemente no tienen nombre acordado.
 */
public abstract class TimeZoneNameProvider extends LocaleServiceProvider {

    protected TimeZoneNameProvider() {
    }

    /**
     * El nombre de una de las dos mitades del año.
     *
     * @param ID       el identificador de la zona ({@code "America/Argentina/Buenos_Aires"})
     * @param daylight si se pide el nombre del horario de verano
     * @param style    {@code TimeZone.LONG} o {@code TimeZone.SHORT}
     * @return null si este proveedor no lo tiene
     */
    public abstract String getDisplayName(String ID, boolean daylight, int style, Locale locale);

    /** El nombre que sirve para las dos mitades. Ver la nota de la clase. */
    public String getGenericDisplayName(String ID, int style, Locale locale) {
        return null;
    }
}
