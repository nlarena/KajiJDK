package java.time.temporal;

// KajiLibrary's java.time.temporal.JulianFields — the three day-numbering schemes that count days
// from a fixed origin instead of from a year/month/day. All three are the SAME quantity as
// ChronoField.EPOCH_DAY, just measured from a different day zero, so each is a plain offset:
//
//   Julian Day           day 0 = -4713-11-24 (proleptic ISO)   epochDay + 2440588
//   Modified Julian Day  day 0 = 1858-11-17                    epochDay + 40587
//   Rata Die             day 1 = 0001-01-01                    epochDay + 719163
//
// The JDK models each as a constant of a private inner enum; ours are package-private top-level
// classes in this file (nested types don't resolve — finding #101).
public final class JulianFields {

    // The offsets live in the instances, not in `static final long` constants: a static-final
    // primitive is a compile-time constant, and our compiler leaves its value only in the
    // ConstantValue attribute, where it reads back as 0 at runtime (finding #112).
    public static final TemporalField JULIAN_DAY = new JulianField("JulianDay", 2440588L);

    public static final TemporalField MODIFIED_JULIAN_DAY = new JulianField("ModifiedJulianDay", 40587L);

    public static final TemporalField RATA_DIE = new JulianField("RataDie", 719163L);

    private JulianFields() {
    }
}

// One day-numbering scheme: EPOCH_DAY shifted by a fixed origin.
final class JulianField implements TemporalField {

    private final String name;
    private final long offset;

    JulianField(String name, long offset) {
        this.name = name;
        this.offset = offset;
    }

    public long getFrom(TemporalAccessor temporal) {
        return temporal.getLong(ChronoField.EPOCH_DAY) + this.offset;
    }

    public boolean isSupportedBy(TemporalAccessor temporal) {
        return temporal.isSupported(ChronoField.EPOCH_DAY);
    }

    // Los tres campos julianos son el dia epoch corrido por una constante, asi que las cuatro
    // descripciones salen de `EPOCH_DAY`: cuentan dias, dentro de "para siempre", con el mismo rango
    // desplazado, y se ajustan poniendo el dia epoch que corresponde.

    public TemporalUnit getBaseUnit() {
        return ChronoUnit.DAYS;
    }

    public TemporalUnit getRangeUnit() {
        return ChronoUnit.FOREVER;
    }

    public ValueRange range() {
        ValueRange dias = ChronoField.EPOCH_DAY.range();
        return ValueRange.of(dias.getMinimum() + this.offset, dias.getMaximum() + this.offset);
    }

    public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
        if (!this.isSupportedBy(temporal)) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + this.name);
        }
        return this.range();
    }

    public <R extends Temporal> R adjustInto(R temporal, long newValue) {
        if (!this.range().isValidValue(newValue)) {
            throw new java.time.DateTimeException("Invalid value for " + this.name + ": " + newValue);
        }
        return (R) temporal.with(ChronoField.EPOCH_DAY, newValue - this.offset);
    }

    public boolean isDateBased() {
        return true;
    }

    public boolean isTimeBased() {
        return false;
    }

    public String toString() {
        return this.name;
    }
}
