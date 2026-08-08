package java.time.temporal;

// KajiLibrary's java.time.temporal.TemporalAccessor — read-only access to a date/time by field.
// The base of all the value types (LocalDate, Instant, …). A KajiLibrary subset: `query` and
// `range` (generic/ValueRange defaults) are omitted — a generic method inherited from a super-
// interface isn't resolvable on a value type yet (finding #15 family).
public interface TemporalAccessor {

    boolean isSupported(TemporalField field);

    long getLong(TemporalField field);

    // The field's value as an int (throws if it overflows, in the JDK; we just narrow).
    default int get(TemporalField field) {
        return (int) this.getLong(field);
    }
}
