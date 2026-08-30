package java.time.temporal;

// KajiLibrary's java.time.temporal.TemporalField -- un campo de una fecha u hora, como el año o la
// hora del dia. `ChronoField` es el enum estandar de estos.
//
// La interfaz describe el campo desde **dos lados**, y la distincion es la que organiza todo el
// paquete:
//
//   - la **unidad base** es lo que el campo cuenta (el minuto del dia cuenta minutos);
//   - la **unidad de rango** es dentro de que lo cuenta (dentro de un dia).
//
// De ese par sale casi todo lo demas: el rango de valores validos, si el campo es de fecha o de
// hora, y como se lo ajusta sobre un `Temporal`.
public interface TemporalField {

    /** Lo que este campo cuenta. El minuto del dia cuenta `MINUTES`. */
    TemporalUnit getBaseUnit();

    /** Dentro de que lo cuenta. El minuto del dia se cuenta dentro de `DAYS`. */
    TemporalUnit getRangeUnit();

    /**
     * El rango de valores que el campo admite **en general**.
     *
     * <p>Es el rango sin mirar ninguna fecha concreta, y por eso `DAY_OF_MONTH` da 1..28/31: el
     * maximo depende del mes, y aca todavia no hay mes. Para el rango de una fecha dada esta
     * {@link #rangeRefinedBy(TemporalAccessor)}.
     */
    ValueRange range();

    /**
     * El rango de valores para **ese** temporal.
     *
     * <p>Es la version afinada de {@link #range()}: sobre un febrero de año bisiesto,
     * `DAY_OF_MONTH` devuelve 1..29 y no 1..31.
     */
    ValueRange rangeRefinedBy(TemporalAccessor temporal);

    // The value of this field read from `temporal` (delegates to temporal.getLong(this)).
    long getFrom(TemporalAccessor temporal);

    boolean isSupportedBy(TemporalAccessor temporal);

    /**
     * Devuelve `temporal` con este campo puesto en `newValue`.
     *
     * <p>El tipo de retorno repite el del parametro para que el resultado conserve el tipo concreto:
     * ajustar un `LocalDate` devuelve un `LocalDate`, no un `Temporal` que haya que castear.
     */
    <R extends Temporal> R adjustInto(R temporal, long newValue);

    boolean isDateBased();

    boolean isTimeBased();

    /**
     * El nombre del campo en esa region.
     *
     * <p>Devuelve `toString()` para cualquier region: esta biblioteca no trae los datos de
     * localizacion de nombres de campo. Se documenta en vez de fingir.
     */
    default String getDisplayName(java.util.Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale");
        }
        return this.toString();
    }

    /**
     * Resuelve este campo durante el parseo, a partir de los campos ya leidos.
     *
     * <p>`null` --el default-- significa "no se de una forma especial de resolverme": el parser usa
     * el camino generico. Solo lo sobreescribe un campo que sepa derivarse de otros.
     */
    default TemporalAccessor resolve(java.util.Map<TemporalField, Long> fieldValues,
            TemporalAccessor partialTemporal, java.time.format.ResolverStyle resolverStyle) {
        return null;
    }
}
