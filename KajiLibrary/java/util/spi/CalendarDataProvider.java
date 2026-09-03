package java.util.spi;

import java.util.Locale;

/**
 * KajiLibrary's java.util.spi.CalendarDataProvider -- los dos numeros que definen una semana.
 *
 * <p>Son pocos pero son los que rompen fechas cuando estan mal:
 *
 * <ul>
 *   <li><b>El primer dia de la semana</b>: domingo en Estados Unidos, lunes en casi toda Europa y
 *       Latinoamerica, sabado en varios paises arabes.
 *   <li><b>Los dias minimos de la primera semana</b>: cuantos dias del ano nuevo tiene que tener una
 *       semana para contar como la primera. Con 1 la primera semana puede tener un solo dia; con 4
 *       --la regla ISO-- la del 1 de enero puede pertenecer al año anterior.
 * </ul>
 *
 * <p>El segundo es el que sorprende: es la razon por la que el 1 de enero puede caer en la "semana
 * 52 del año pasado".
 */
public abstract class CalendarDataProvider extends LocaleServiceProvider {

    protected CalendarDataProvider() {
    }

    /**
     * Que dia empieza la semana, con los valores de {@code Calendar}: 1 domingo, 2 lunes, ... 7
     * sabado.
     *
     * @return 0 si este proveedor no tiene el dato para ese local
     */
    public abstract int getFirstDayOfWeek(Locale locale);

    /**
     * Cuantos dias del año nuevo necesita una semana para ser la primera.
     *
     * @return 0 si este proveedor no tiene el dato para ese local
     */
    public abstract int getMinimalDaysInFirstWeek(Locale locale);
}
