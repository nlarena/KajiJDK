package java.time.temporal;

// KajiLibrary's java.time.temporal.TemporalAccessor -- read-only access to a date or a time, field
// by field. It is the base of every value type (LocalDate, Instant, ...).
public interface TemporalAccessor {

    boolean isSupported(TemporalField field);

    long getLong(TemporalField field);

    // The field's value as an int (throws if it overflows, in the JDK; we just narrow).
    default int get(TemporalField field) {
        return (int) this.getLong(field);
    }

    /**
     * `field`'s range of valid values **in this** temporal.
     *
     * <p>Refined by the object itself: `DAY_OF_MONTH` over a leap February gives 1..29. The default
     * delegates to the field, which is what knows; a type that can refine further overrides it.
     */
    default ValueRange range(TemporalField field) {
        if (field instanceof ChronoField) {
            if (this.isSupported(field)) {
                return field.range();
            }
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.rangeRefinedBy(this);
    }

    /**
     * It queries this temporal with a strategy.
     *
     * <p>It is the package's extension point: instead of a method per thing one might want to know,
     * the question is passed in. `TemporalQueries` carries the standard ones.
     */
    default <R> R query(TemporalQuery<R> query) {
        return query.queryFrom(this);
    }
}
