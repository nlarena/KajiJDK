package java.time.chrono;

import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQuery;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;

// KajiLibrary's java.time.chrono.Era -- a calendar's era (ISO's BCE/CE, the Japanese calendar's five
// imperial ones, and the two of each of the others).
//
// That an era should be a `TemporalAccessor` sounds odd until one looks at which field it has:
// exactly one, `ERA`, and none besides. It is not "a date with very little data" but **a value of a
// single field**, and that is just what the interface asks for. Being a `TemporalAdjuster` follows
// from the same: adjusting a date with an era is setting that field.
//
// On `getDisplayName(TextStyle, Locale)`: it is here, and it returns the **numeric value**. See its
// javadoc, which explains why that is not a lie but the branch the contract defines for when there
// is no text data.
public interface Era extends TemporalAccessor, TemporalAdjuster {

    int getValue();

    /**
     * This era's name to show to somebody.
     *
     * <p>It returns **the numeric value**, which is what the contract demands when there is no name
     * for the style and locale asked for: <i>"If no textual mapping is found then the numeric value
     * is returned"</i>.
     *
     * <p>**This library does not carry the CLDR's text data**, so that branch is taken **always**,
     * for any locale. The difference from the JDK is concrete:
     * `IsoEra.CE.getDisplayName(FULL, ENGLISH)` gives `"1"` here and `"AD"` in the JDK.
     *
     * <p>That this can be written at all --and that it had been left out before-- comes down to a
     * detail worth noting: the contract **defines** what to do when there is no name. What would be
     * lying is inventing one. And the number **announces itself**: nobody mistakes `"1"` for a
     * translated name, whereas a `"CE"` returned for a French locale would pass for good.
     *
     * @throws NullPointerException if `style` or `locale` are `null`
     */
    default String getDisplayName(java.time.format.TextStyle style, java.util.Locale locale) {
        if (style == null) {
            throw new NullPointerException("style");
        }
        if (locale == null) {
            throw new NullPointerException("locale");
        }
        return Integer.toString(this.getValue());
    }

    /** An era knows **only** about `ERA`. */
    default boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return field == ChronoField.ERA;
        }
        return field != null && field.isSupportedBy(this);
    }

    default ValueRange range(TemporalField field) {
        if (field == ChronoField.ERA) {
            // The real range depends on the calendar --the Japanese one has five eras and starts at
            // -1-- but a loose `Era` does not know which it belongs to. `ChronoField`'s is the generic
            // range, which is what the JDK returns here; the refined one comes from
            // `Chronology.range(ERA)`.
            return field.range();
        }
        if (field instanceof ChronoField) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.rangeRefinedBy(this);
    }

    default int get(TemporalField field) {
        if (field == ChronoField.ERA) {
            return this.getValue();
        }
        if (field instanceof ChronoField) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        // Bound to a local: chaining through an interface-typed intermediate gets lost (#108).
        ValueRange range = field.rangeRefinedBy(this);
        long value = field.getFrom(this);
        return (int) range.checkValidIntValue(value, field);
    }

    default long getLong(TemporalField field) {
        if (field == ChronoField.ERA) {
            return (long) this.getValue();
        }
        if (field instanceof ChronoField) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.getFrom(this);
    }

    default <R> R query(TemporalQuery<R> query) {
        if (query == java.time.temporal.TemporalQueries.precision()) {
            return (R) java.time.temporal.ChronoUnit.ERAS;
        }
        return query.queryFrom(this);
    }

    /**
     * It sets this era on `temporal`, leaving the year of the era as it was.
     *
     * <p>Mind what that means: `date.with(IsoEra.BCE)` over the year 2024 gives the year -2023, not
     * -2024, because what is kept is the **year of the era** and not the proleptic one. It is what
     * the JDK does and the only coherent thing: the era and the year of the era are a pair.
     */
    default Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.ERA, (long) this.getValue());
    }
}
