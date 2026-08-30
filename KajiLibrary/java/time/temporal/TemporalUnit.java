package java.time.temporal;

// KajiLibrary's java.time.temporal.TemporalUnit — a unit of date/time, such as days or hours.
// `ChronoUnit` is the standard enum of these.
public interface TemporalUnit {

    /**
     * Devuelve `temporal` mas `amount` de esta unidad.
     *
     * <p>El tipo de retorno repite el del parametro para que el resultado conserve el tipo concreto:
     * sumarle dias a un `LocalDate` devuelve un `LocalDate`.
     */
    <R extends Temporal> R addTo(R temporal, long amount);

    /**
     * Cuanto dura esta unidad.
     *
     * <p>Para las unidades **estimadas** --meses, años-- es un promedio, y por eso `isDurationEstimated`
     * existe: usar este valor para aritmetica exacta sobre esas unidades da un resultado equivocado.
     */
    java.time.Duration getDuration();

    // How many of this unit lie between two temporals (exclusive of the end).
    long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive);

    boolean isSupportedBy(Temporal temporal);

    boolean isDateBased();

    boolean isTimeBased();

    boolean isDurationEstimated();
}
