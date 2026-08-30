package java.time;

// KajiLibrary's java.time.DayOfWeek — the seven days, MONDAY (1) … SUNDAY (7) (ISO order). A
// Implementa TemporalAccessor y TemporalAdjuster, como el del JDK; un enum simple
// with the value and rotation helpers.
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
     * El dia de la semana que `temporal` tiene.
     *
     * @throws java.time.DateTimeException si no lo tiene
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
    // Un `DayOfWeek` sabe **un solo** campo, el suyo. Los demas no los tiene, y decir que si los
    // tiene --devolviendo cero, por ejemplo-- convertiria un error del que llama en un dato
    // equivocado que sigue viaje.

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
     * Devuelve `temporal` movido a este dia de la semana.
     *
     * <p>Es la mitad `TemporalAdjuster` del enum, la que hace andar
     * `fecha.with(DayOfWeek.MONDAY)`.
     */
    public java.time.temporal.Temporal adjustInto(java.time.temporal.Temporal temporal) {
        return temporal.with(java.time.temporal.ChronoField.DAY_OF_WEEK, this.getValue());
    }

    /**
     * El nombre del dia en esa region.
     *
     * <p>Devuelve el nombre en ingles, que es el del `Locale.ROOT`, para cualquier region: esta
     * biblioteca no trae los datos de localizacion de fechas. Se documenta en vez de fingir --
     * devolver el nombre equivocado en silencio seria peor que decir cual se devuelve--, y el
     * `TextStyle` si se respeta, porque eso no depende de la region.
     */
    public String getDisplayName(java.time.format.TextStyle style, java.util.Locale locale) {
        if (style == null || locale == null) {
            throw new NullPointerException();
        }
        String largo = this.name().charAt(0) + this.name().substring(1).toLowerCase();
        if (style == java.time.format.TextStyle.NARROW
                || style == java.time.format.TextStyle.NARROW_STANDALONE) {
            return largo.substring(0, 1);
        }
        if (style == java.time.format.TextStyle.SHORT
                || style == java.time.format.TextStyle.SHORT_STANDALONE) {
            return largo.substring(0, 3);
        }
        return largo;
    }

    public DayOfWeek plus(long days) {
        int amount = (int) (days % 7);
        return DayOfWeek.values()[(this.ordinal() + (amount + 7)) % 7];
    }

    public DayOfWeek minus(long days) {
        return this.plus(-(days % 7));
    }
}
