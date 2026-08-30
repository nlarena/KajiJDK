package java.time;

// KajiLibrary's java.time.ZoneOffset — a fixed offset from UTC/Greenwich, e.g. +05:30. A value
// type. A KajiLibrary subset: it does NOT extend ZoneId (which would pull in ZoneRules and the
// timezone database) and omits the Temporal accessors; it is a Comparable offset with its
// factory methods, getTotalSeconds/getId, and value semantics.
public final class ZoneOffset extends ZoneId
        implements Comparable<ZoneOffset>, java.time.temporal.TemporalAccessor,
        java.time.temporal.TemporalAdjuster {

    public static final ZoneOffset UTC = ZoneOffset.ofTotalSeconds(0);
    public static final ZoneOffset MIN = ZoneOffset.ofTotalSeconds(-18 * 3600);
    public static final ZoneOffset MAX = ZoneOffset.ofTotalSeconds(18 * 3600);

    private final int totalSeconds;
    private final String id;

    private ZoneOffset(int totalSeconds) {
        this.totalSeconds = totalSeconds;
        this.id = buildId(totalSeconds);
    }

    public static ZoneOffset ofTotalSeconds(int totalSeconds) {
        return new ZoneOffset(totalSeconds);
    }

    public static ZoneOffset ofHours(int hours) {
        return ofTotalSeconds(hours * 3600);
    }

    public static ZoneOffset ofHoursMinutes(int hours, int minutes) {
        return ofTotalSeconds(hours * 3600 + minutes * 60);
    }

    public static ZoneOffset ofHoursMinutesSeconds(int hours, int minutes, int seconds) {
        return ofTotalSeconds(hours * 3600 + minutes * 60 + seconds);
    }

    public static ZoneOffset of(String offsetId) {
        return ofTotalSeconds(parseOffset(offsetId));
    }

    /**
     * Las reglas de esta zona: un desplazamiento **fijo**, el suyo.
     *
     * <p>Un `ZoneOffset` es la unica clase de zona que no tiene horario de verano por definicion:
     * es el desplazamiento, no un lugar donde el desplazamiento cambia.
     */
    public java.time.zone.ZoneRules getRules() {
        return java.time.zone.ZoneRules.of(this);
    }

    /** Un desplazamiento ya esta normalizado: es el mismo. */
    public ZoneId normalized() {
        return this;
    }

    /** El desplazamiento que `temporal` tiene. */
    public static ZoneOffset from(java.time.temporal.TemporalAccessor temporal) {
        if (temporal == null) {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof ZoneOffset) {
            return (ZoneOffset) temporal;
        }
        if (!temporal.isSupported(java.time.temporal.ChronoField.OFFSET_SECONDS)) {
            throw new java.time.DateTimeException(
                    "Unable to obtain ZoneOffset from TemporalAccessor: " + temporal);
        }
        return ZoneOffset.ofTotalSeconds(
                (int) temporal.getLong(java.time.temporal.ChronoField.OFFSET_SECONDS));
    }

    // ---- TemporalAccessor -----------------------------------------------------------------------
    //
    // Un desplazamiento sabe **un solo** campo, `OFFSET_SECONDS`. Decir que sabe otros --devolviendo
    // cero-- convertiria un error del que llama en un dato equivocado que sigue viaje.

    public boolean isSupported(java.time.temporal.TemporalField field) {
        return field == java.time.temporal.ChronoField.OFFSET_SECONDS;
    }

    public long getLong(java.time.temporal.TemporalField field) {
        if (field == java.time.temporal.ChronoField.OFFSET_SECONDS) {
            return this.getTotalSeconds();
        }
        if (field instanceof java.time.temporal.ChronoField) {
            throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.getFrom(this);
    }

    public int get(java.time.temporal.TemporalField field) {
        if (field == java.time.temporal.ChronoField.OFFSET_SECONDS) {
            return this.getTotalSeconds();
        }
        return (int) this.getLong(field);
    }

    public java.time.temporal.ValueRange range(java.time.temporal.TemporalField field) {
        if (field == java.time.temporal.ChronoField.OFFSET_SECONDS) {
            return java.time.temporal.ChronoField.OFFSET_SECONDS.range();
        }
        if (field instanceof java.time.temporal.ChronoField) {
            throw new java.time.temporal.UnsupportedTemporalTypeException("Unsupported field: " + field);
        }
        return field.rangeRefinedBy(this);
    }

    public <R> R query(java.time.temporal.TemporalQuery<R> query) {
        if (query == java.time.temporal.TemporalQueries.offset()
                || query == java.time.temporal.TemporalQueries.zone()) {
            return (R) this;
        }
        return query.queryFrom(this);
    }

    /** Devuelve `temporal` con este desplazamiento puesto. */
    public java.time.temporal.Temporal adjustInto(java.time.temporal.Temporal temporal) {
        return temporal.with(java.time.temporal.ChronoField.OFFSET_SECONDS, this.getTotalSeconds());
    }

    public int getTotalSeconds() {
        return this.totalSeconds;
    }

    public String getId() {
        return this.id;
    }

    public String toString() {
        return this.id;
    }

    // Offsets sort ahead of smaller ones: a larger (more east) offset compares as "less".
    public int compareTo(ZoneOffset other) {
        return other.totalSeconds - this.totalSeconds;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ZoneOffset) {
            return this.totalSeconds == ((ZoneOffset) obj).totalSeconds;
        }
        return false;
    }

    public int hashCode() {
        return this.totalSeconds;
    }

    // The ISO id: "Z" for zero, else [+/-]HH:MM[:SS].
    private static String buildId(int totalSeconds) {
        if (totalSeconds == 0) {
            return "Z";
        }
        int abs;
        if (totalSeconds < 0) {
            abs = -totalSeconds;
        } else {
            abs = totalSeconds;
        }
        int hours = abs / 3600;
        int minutes = (abs / 60) % 60;
        int secs = abs % 60;
        StringBuilder buf = new StringBuilder();
        if (totalSeconds < 0) {
            buf.append("-");
        } else {
            buf.append("+");
        }
        if (hours < 10) {
            buf.append("0");
        }
        buf.append(Integer.toString(hours));
        if (minutes < 10) {
            buf.append(":0");
        } else {
            buf.append(":");
        }
        buf.append(Integer.toString(minutes));
        if (secs != 0) {
            if (secs < 10) {
                buf.append(":0");
            } else {
                buf.append(":");
            }
            buf.append(Integer.toString(secs));
        }
        return buf.toString();
    }

    // Parses "Z" or [+/-]HH[[:]MM[[:]SS]] (colons optional) into a total-seconds offset.
    private static int parseOffset(String s) {
        if (s.equals("Z")) {
            return 0;
        }
        int sign;
        if (s.charAt(0) == '-') {
            sign = -1;
        } else {
            sign = 1;
        }
        StringBuilder db = new StringBuilder();
        for (int i = 1; i < s.length(); i = i + 1) {
            char c = s.charAt(i);
            if (c != ':') {
                db.append(c);
            }
        }
        String d = db.toString();
        int hh = parseDigits(d, 0, 2);
        int mm = 0;
        int ss = 0;
        if (d.length() >= 4) {
            mm = parseDigits(d, 2, 4);
        }
        if (d.length() >= 6) {
            ss = parseDigits(d, 4, 6);
        }
        return sign * (hh * 3600 + mm * 60 + ss);
    }

    private static int parseDigits(String s, int from, int to) {
        int v = 0;
        for (int k = from; k < to; k = k + 1) {
            v = v * 10 + (s.charAt(k) - '0');
        }
        return v;
    }
}
