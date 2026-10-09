package java.time.temporal;

// KajiLibrary's java.time.temporal.TemporalQuery<R> — a function that extracts information of
// type R from a TemporalAccessor (e.g. "which precision?", "which zone?"). The functional
// interface `temporal.query(q)` dispatches to.
public interface TemporalQuery<R> {

    R queryFrom(TemporalAccessor temporal);
}
