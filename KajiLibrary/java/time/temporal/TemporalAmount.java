package java.time.temporal;

// Same-package import works around the finder for cross-package types (finding #4 territory).
import java.util.List;

// KajiLibrary's java.time.temporal.TemporalAmount — a quantity of time, such as "6 hours" or
// "3 years, 2 months". Duration and Period implement it; `temporal.plus(amount)` dispatches to
// `amount.addTo(temporal)`.
public interface TemporalAmount {

    long get(TemporalUnit unit);

    List<TemporalUnit> getUnits();

    Temporal addTo(Temporal temporal);

    Temporal subtractFrom(Temporal temporal);
}
