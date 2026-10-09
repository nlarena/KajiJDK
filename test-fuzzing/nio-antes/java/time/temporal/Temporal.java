package java.time.temporal;

// KajiLibrary's java.time.temporal.Temporal — a date/time that supports field access (via
// TemporalAccessor) and returns adjusted copies of itself (immutable). The value types implement
// it. The `with(TemporalAdjuster)`/`plus(TemporalAmount)`/`minus(TemporalAmount)` defaults
// delegate to the adjuster/amount, so a value type only implements the field/unit primitives.
public interface Temporal extends TemporalAccessor {

    boolean isSupported(TemporalUnit unit);

    Temporal with(TemporalField field, long newValue);

    Temporal plus(long amountToAdd, TemporalUnit unit);

    Temporal minus(long amountToSubtract, TemporalUnit unit);

    long until(Temporal endExclusive, TemporalUnit unit);

    default Temporal with(TemporalAdjuster adjuster) {
        return adjuster.adjustInto(this);
    }

    default Temporal plus(TemporalAmount amount) {
        return amount.addTo(this);
    }

    default Temporal minus(TemporalAmount amount) {
        return amount.subtractFrom(this);
    }
}
