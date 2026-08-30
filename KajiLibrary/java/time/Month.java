package java.time;

// KajiLibrary's java.time.Month — the twelve months, JANUARY (1) … DECEMBER (12). A KajiLibrary
// subset (the JDK's Month also implements TemporalAccessor/TemporalAdjuster and has firstDayOfYear
// etc.); here it's a plain enum with the value, length, and rotation helpers LocalDate needs.
public enum Month implements java.time.temporal.TemporalAccessor,
        java.time.temporal.TemporalAdjuster {

    JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE,
    JULY, AUGUST, SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER;

    // Explicit (empty) constructor: without one, the frozen javac synthesises a degenerate
    // public ()V constructor for this enum instead of the private (String,int) one (finding #18).
    Month() {}

    // 1..12 (unlike ordinal()'s 0..11).
    public int getValue() {
        return this.ordinal() + 1;
    }

    public static Month of(int month) {
        return Month.values()[month - 1];
    }

    // Days in this month; February depends on whether it's a leap year.
    public int length(boolean leapYear) {
        int o = this.ordinal();
        if (o == 1) {
            return leapYear ? 29 : 28;
        }
        if (o == 3 || o == 5 || o == 8 || o == 10) {
            return 30;
        }
        return 31;
    }

    /**
     * El mes que `temporal` tiene.
     *
     * @throws java.time.DateTimeException si no lo tiene
     */
    public static Month from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof Month) {
            return (Month) temporal;
        }
        return Month.of(temporal.get(java.time.temporal.ChronoField.MONTH_OF_YEAR));
    }

    /** El minimo de dias que puede tener: 28 para febrero, su longitud fija para el resto. */
    public int minLength() {
        return this.getValue() == 2 ? 28 : this.length(false);
    }

    /** El maximo: 29 para febrero. */
    public int maxLength() {
        return this.getValue() == 2 ? 29 : this.length(false);
    }

    /**
     * El dia del año en que empieza este mes, contando desde 1.
     *
     * <p>Enero da 1, febrero 32, y de marzo en adelante depende del bisiesto.
     */
    public int firstDayOfYear(boolean leapYear) {
        int extra = leapYear ? 1 : 0;
        int v = this.getValue();
        if (v == 1) {
            return 1;
        }
        if (v == 2) {
            return 32;
        }
        if (v == 3) {
            return 60 + extra;
        }
        if (v == 4) {
            return 91 + extra;
        }
        if (v == 5) {
            return 121 + extra;
        }
        if (v == 6) {
            return 152 + extra;
        }
        if (v == 7) {
            return 182 + extra;
        }
        if (v == 8) {
            return 213 + extra;
        }
        if (v == 9) {
            return 244 + extra;
        }
        if (v == 10) {
            return 274 + extra;
        }
        if (v == 11) {
            return 305 + extra;
        }
        return 335 + extra;
    }

    /** El primer mes del trimestre al que este pertenece: enero, abril, julio u octubre. */
    public Month firstMonthOfQuarter() {
        return Month.of((this.ordinal() / 3) * 3 + 1);
    }

    // ---- TemporalAccessor -----------------------------------------------------------------------

    public boolean isSupported(java.time.temporal.TemporalField field) {
        return field == java.time.temporal.ChronoField.MONTH_OF_YEAR;
    }

    public long getLong(java.time.temporal.TemporalField field) {
        if (field == java.time.temporal.ChronoField.MONTH_OF_YEAR) {
            return this.getValue();
        }
        if (field instanceof java.time.temporal.ChronoField) {
            throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.getFrom(this);
    }

    public int get(java.time.temporal.TemporalField field) {
        if (field == java.time.temporal.ChronoField.MONTH_OF_YEAR) {
            return this.getValue();
        }
        return (int) this.getLong(field);
    }

    public java.time.temporal.ValueRange range(java.time.temporal.TemporalField field) {
        if (field == java.time.temporal.ChronoField.MONTH_OF_YEAR) {
            return java.time.temporal.ValueRange.of(1L, 12L);
        }
        if (field instanceof java.time.temporal.ChronoField) {
            throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.rangeRefinedBy(this);
    }

    public <R> R query(java.time.temporal.TemporalQuery<R> query) {
        if (query == java.time.temporal.TemporalQueries.precision()) {
            return (R) java.time.temporal.ChronoUnit.MONTHS;
        }
        return query.queryFrom(this);
    }

    public java.time.temporal.Temporal adjustInto(java.time.temporal.Temporal temporal) {
        return temporal.with(java.time.temporal.ChronoField.MONTH_OF_YEAR, this.getValue());
    }

    /** El nombre del mes. Ver la nota de `DayOfWeek.getDisplayName`: no depende de la region. */
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

    public Month plus(long months) {
        int amount = (int) (months % 12);
        return Month.values()[(this.ordinal() + (amount + 12)) % 12];
    }

    public Month minus(long months) {
        return this.plus(-(months % 12));
    }
}
