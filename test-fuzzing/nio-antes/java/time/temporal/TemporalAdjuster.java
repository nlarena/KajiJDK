package java.time.temporal;

// KajiLibrary's java.time.temporal.TemporalAdjuster — a strategy that produces an adjusted copy
// of a Temporal (e.g. "next Monday", "last day of month"). `temporal.with(adjuster)` dispatches
// to `adjuster.adjustInto(temporal)`.
public interface TemporalAdjuster {

    Temporal adjustInto(Temporal temporal);
}
