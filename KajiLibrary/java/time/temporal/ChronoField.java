package java.time.temporal;

// KajiLibrary's java.time.temporal.ChronoField -- los `TemporalField` estandar, los treinta.
//
// Cada uno se describe con **cuatro** cosas, y de ahi sale todo lo demas: que cuenta (la unidad
// base), dentro de que lo cuenta (la unidad de rango), que valores admite, y si es de fecha o de
// hora. `MINUTE_OF_DAY` cuenta minutos dentro de un dia, admite 0..1439, y es de hora.
//
// El orden de declaracion es el del JDK, y no es cosmetico: `values()` y `ordinal()` son
// observables, asi que reordenarlos seria una diferencia de comportamiento.
//
// **Los dos pares que se confunden.** `HOUR_OF_AMPM` va 0..11 y `CLOCK_HOUR_OF_AMPM` va 1..12 --el
// reloj no tiene "las 0", tiene "las 12"--; igual `HOUR_OF_DAY` (0..23) contra `CLOCK_HOUR_OF_DAY`
// (1..24). Elegir el equivocado da un error de una hora dos veces al dia, que es exactamente la
// clase de error que sobrevive a una prueba descuidada.
public enum ChronoField implements TemporalField {

    NANO_OF_SECOND(ChronoUnit.NANOS, ChronoUnit.SECONDS, 0L, 999999999L, false, true),
    NANO_OF_DAY(ChronoUnit.NANOS, ChronoUnit.DAYS, 0L, 86399999999999L, false, true),
    MICRO_OF_SECOND(ChronoUnit.MICROS, ChronoUnit.SECONDS, 0L, 999999L, false, true),
    MICRO_OF_DAY(ChronoUnit.MICROS, ChronoUnit.DAYS, 0L, 86399999999L, false, true),
    MILLI_OF_SECOND(ChronoUnit.MILLIS, ChronoUnit.SECONDS, 0L, 999L, false, true),
    MILLI_OF_DAY(ChronoUnit.MILLIS, ChronoUnit.DAYS, 0L, 86399999L, false, true),
    SECOND_OF_MINUTE(ChronoUnit.SECONDS, ChronoUnit.MINUTES, 0L, 59L, false, true),
    SECOND_OF_DAY(ChronoUnit.SECONDS, ChronoUnit.DAYS, 0L, 86399L, false, true),
    MINUTE_OF_HOUR(ChronoUnit.MINUTES, ChronoUnit.HOURS, 0L, 59L, false, true),
    MINUTE_OF_DAY(ChronoUnit.MINUTES, ChronoUnit.DAYS, 0L, 1439L, false, true),
    HOUR_OF_AMPM(ChronoUnit.HOURS, ChronoUnit.HALF_DAYS, 0L, 11L, false, true),
    CLOCK_HOUR_OF_AMPM(ChronoUnit.HOURS, ChronoUnit.HALF_DAYS, 1L, 12L, false, true),
    HOUR_OF_DAY(ChronoUnit.HOURS, ChronoUnit.DAYS, 0L, 23L, false, true),
    CLOCK_HOUR_OF_DAY(ChronoUnit.HOURS, ChronoUnit.DAYS, 1L, 24L, false, true),
    AMPM_OF_DAY(ChronoUnit.HALF_DAYS, ChronoUnit.DAYS, 0L, 1L, false, true),
    DAY_OF_WEEK(ChronoUnit.DAYS, ChronoUnit.WEEKS, 1L, 7L, true, false),
    ALIGNED_DAY_OF_WEEK_IN_MONTH(ChronoUnit.DAYS, ChronoUnit.WEEKS, 1L, 7L, true, false),
    ALIGNED_DAY_OF_WEEK_IN_YEAR(ChronoUnit.DAYS, ChronoUnit.WEEKS, 1L, 7L, true, false),
    // El maximo chico es 28 --febrero no bisiesto-- y el grande 31. `range()` da el rango general;
    // `rangeRefinedBy` sobre una fecha concreta lo afina.
    DAY_OF_MONTH(ChronoUnit.DAYS, ChronoUnit.MONTHS, 1L, 28L, 31L, true, false),
    DAY_OF_YEAR(ChronoUnit.DAYS, ChronoUnit.YEARS, 1L, 365L, 366L, true, false),
    EPOCH_DAY(ChronoUnit.DAYS, ChronoUnit.FOREVER, -365243219162L, 365241780471L, true, false),
    ALIGNED_WEEK_OF_MONTH(ChronoUnit.WEEKS, ChronoUnit.MONTHS, 1L, 4L, 5L, true, false),
    ALIGNED_WEEK_OF_YEAR(ChronoUnit.WEEKS, ChronoUnit.YEARS, 1L, 53L, true, false),
    MONTH_OF_YEAR(ChronoUnit.MONTHS, ChronoUnit.YEARS, 1L, 12L, true, false),
    PROLEPTIC_MONTH(ChronoUnit.MONTHS, ChronoUnit.FOREVER, -11999999988L, 11999999999L, true, false),
    YEAR_OF_ERA(ChronoUnit.YEARS, ChronoUnit.FOREVER, 1L, 999999999L, 1000000000L, true, false),
    YEAR(ChronoUnit.YEARS, ChronoUnit.FOREVER, -999999999L, 999999999L, true, false),
    ERA(ChronoUnit.ERAS, ChronoUnit.FOREVER, 0L, 1L, true, false),
    // Los dos ultimos son los unicos que **no son ni de fecha ni de hora**, y por eso llevan `false`
    // en las dos banderas. Se miden en segundos, lo cual invita a marcarlos como de hora --y asi
    // estaban--, pero eso es lo que no son: un `LocalTime` no puede contestar ninguno de los dos. Uno
    // necesita fecha, hora y zona a la vez; el otro es el desplazamiento mismo, que no es un instante
    // dentro del dia. Marcarlos de hora hacia que `LocalTime.isSupported(OFFSET_SECONDS)` dijera que
    // si y despues `getLong` tirara, que es exactamente la contradiccion que `isSupported` existe para
    // evitar.
    INSTANT_SECONDS(ChronoUnit.SECONDS, ChronoUnit.FOREVER, Long.MIN_VALUE, Long.MAX_VALUE, false, false),
    // +-18 horas: el maximo que la especificacion admite para un desplazamiento de zona.
    OFFSET_SECONDS(ChronoUnit.SECONDS, ChronoUnit.FOREVER, -64800L, 64800L, false, false);

    private final TemporalUnit baseUnit;
    private final TemporalUnit rangeUnit;
    private final ValueRange range;
    private final boolean dateBased;
    private final boolean timeBased;

    ChronoField(TemporalUnit baseUnit, TemporalUnit rangeUnit, long min, long max,
            boolean dateBased, boolean timeBased) {
        this.baseUnit = baseUnit;
        this.rangeUnit = rangeUnit;
        this.range = ValueRange.of(min, max);
        this.dateBased = dateBased;
        this.timeBased = timeBased;
    }

    ChronoField(TemporalUnit baseUnit, TemporalUnit rangeUnit, long min, long maxSmallest,
            long maxLargest, boolean dateBased, boolean timeBased) {
        this.baseUnit = baseUnit;
        this.rangeUnit = rangeUnit;
        this.range = ValueRange.of(min, maxSmallest, maxLargest);
        this.dateBased = dateBased;
        this.timeBased = timeBased;
    }

    public TemporalUnit getBaseUnit() {
        return this.baseUnit;
    }

    public TemporalUnit getRangeUnit() {
        return this.rangeUnit;
    }

    public ValueRange range() {
        return this.range;
    }

    /**
     * El rango de este campo **para ese** temporal.
     *
     * <p>Le pregunta al temporal, que es el que puede afinar: `DAY_OF_MONTH` sobre un febrero de año
     * bisiesto da 1..29, no el 1..28/31 general.
     */
    public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        return temporal.range(this);
    }

    public long getFrom(TemporalAccessor temporal) {
        return temporal.getLong(this);
    }

    public boolean isSupportedBy(TemporalAccessor temporal) {
        return temporal.isSupported(this);
    }

    public <R extends Temporal> R adjustInto(R temporal, long newValue) {
        return (R) temporal.with(this, newValue);
    }

    public boolean isDateBased() {
        return this.dateBased;
    }

    public boolean isTimeBased() {
        return this.timeBased;
    }

    /**
     * Comprueba que `value` este en el rango general del campo, y lo devuelve.
     *
     * <p>Devuelve el valor en vez de un booleano a proposito: asi se encadena en la expresion que lo
     * usa (`campo.checkValidValue(v)`) y no hay forma de olvidarse de mirar el resultado.
     *
     * @throws java.time.DateTimeException si esta fuera de rango
     */
    public long checkValidValue(long value) {
        return this.range.checkValidValue(value, this);
    }

    /**
     * Idem, y ademas que entre en un `int`.
     *
     * @throws java.time.DateTimeException si esta fuera de rango o no entra en un `int`
     */
    public int checkValidIntValue(long value) {
        return this.range.checkValidIntValue(value, this);
    }

    /** El nombre del campo. Ver la nota de `TemporalField.getDisplayName`: no depende de la region. */
    public String getDisplayName(java.util.Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale");
        }
        return this.toString();
    }
}
