package java.time;

// KajiLibrary's java.time.DayOfWeek — the seven days, MONDAY (1) … SUNDAY (7) (ISO order). It
// implements TemporalAccessor and TemporalAdjuster, as the JDK's does; a plain enum with the value
// and rotation helpers.
public enum DayOfWeek implements java.time.temporal.TemporalAccessor,
        java.time.temporal.TemporalAdjuster {

    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;

    // Explicit (empty) constructor — see Month (finding #18): without it the enum's synthesised
    // constructor comes out as a degenerate public ()V.
    DayOfWeek() {}

    public int getValue() {
        return this.ordinal() + 1;
    }

    public static DayOfWeek of(int dayOfWeek) {
        return DayOfWeek.values()[dayOfWeek - 1];
    }

    /**
     * The weekday `temporal` holds.
     *
     * @throws java.time.DateTimeException if it holds none
     */
    public static DayOfWeek from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof DayOfWeek) {
            return (DayOfWeek) temporal;
        }
        return DayOfWeek.of(temporal.get(java.time.temporal.ChronoField.DAY_OF_WEEK));
    }

    // ---- TemporalAccessor -----------------------------------------------------------------------
    //
    // A `DayOfWeek` knows **one single** field, its own. It does not have the others, and saying it
    // does --by returning zero, say-- would turn a caller's mistake into a wrong datum that travels
    // on.

    public boolean isSupported(java.time.temporal.TemporalField field) {
        return field == java.time.temporal.ChronoField.DAY_OF_WEEK;
    }

    public long getLong(java.time.temporal.TemporalField field) {
        if (field == java.time.temporal.ChronoField.DAY_OF_WEEK) {
            return this.getValue();
        }
        if (field instanceof java.time.temporal.ChronoField) {
            throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.getFrom(this);
    }

    public int get(java.time.temporal.TemporalField field) {
        if (field == java.time.temporal.ChronoField.DAY_OF_WEEK) {
            return this.getValue();
        }
        return (int) this.getLong(field);
    }

    public java.time.temporal.ValueRange range(java.time.temporal.TemporalField field) {
        if (field == java.time.temporal.ChronoField.DAY_OF_WEEK) {
            return java.time.temporal.ValueRange.of(1L, 7L);
        }
        if (field instanceof java.time.temporal.ChronoField) {
            throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.rangeRefinedBy(this);
    }

    public <R> R query(java.time.temporal.TemporalQuery<R> query) {
        if (query == java.time.temporal.TemporalQueries.precision()) {
            return (R) java.time.temporal.ChronoUnit.DAYS;
        }
        return query.queryFrom(this);
    }

    /**
     * It returns `temporal` moved to this weekday.
     *
     * <p>It is the enum's `TemporalAdjuster` half, the one that makes
     * `date.with(DayOfWeek.MONDAY)` work.
     */
    public java.time.temporal.Temporal adjustInto(java.time.temporal.Temporal temporal) {
        return temporal.with(java.time.temporal.ChronoField.DAY_OF_WEEK, this.getValue());
    }

    /**
     * The day's name in that region.
     *
     * <p>It returns the English name, which is `Locale.ROOT`'s, for any region: this library does not
     * carry the date localisation data. It is documented instead of faked -- returning the wrong name
     * in silence would be worse than saying which one is returned -- and the `TextStyle` **is**
     * honoured, because that does not depend on the region.
     */
    public String getDisplayName(java.time.format.TextStyle style, java.util.Locale locale) {
        if (style == null || locale == null) {
            throw new NullPointerException();
        }
        String length = this.name().charAt(0) + this.name().substring(1).toLowerCase();
        if (style == java.time.format.TextStyle.NARROW
                || style == java.time.format.TextStyle.NARROW_STANDALONE) {
            return length.substring(0, 1);
        }
        if (style == java.time.format.TextStyle.SHORT
                || style == java.time.format.TextStyle.SHORT_STANDALONE) {
            return length.substring(0, 3);
        }
        return length;
    }

    public DayOfWeek plus(long days) {
        int amount = (int) (days % 7);
        return DayOfWeek.values()[(this.ordinal() + (amount + 7)) % 7];
    }

    public DayOfWeek minus(long days) {
        return this.plus(-(days % 7));
    }
}
