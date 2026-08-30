package java.time;

import java.util.List;
import java.util.ArrayList;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

// KajiLibrary's java.time.Duration — a time-based amount, as seconds + nanoseconds (nanos kept
// normalised to [0, 1e9), seconds may be negative). Immutable value type: every operation returns
// a fresh Duration. Implements TemporalAmount (so `temporal.plus(duration)` works) and Comparable.
// Ya no es un subconjunto: la superficie publica esta completa.
public final class Duration implements TemporalAmount, Comparable<Duration> {

    private static final long NANOS_PER_SECOND = 1000000000L;

    private final long seconds;
    private final int nanos;

    private Duration(long seconds, int nanos) {
        this.seconds = seconds;
        this.nanos = nanos;
    }

    // Build a Duration from seconds + a nano adjustment, carrying nanos into seconds with floor
    // semantics so nanos ends in [0, 1e9) even when the adjustment is negative.
    private static Duration create(long seconds, long nanoAdjustment) {
        long extraSeconds = nanoAdjustment / NANOS_PER_SECOND;
        long nos = nanoAdjustment % NANOS_PER_SECOND;
        if (nos < 0) {
            nos = nos + NANOS_PER_SECOND;
            extraSeconds = extraSeconds - 1;
        }
        return new Duration(seconds + extraSeconds, (int) nos);
    }

    /** La duracion de longitud cero. */
    public static final Duration ZERO = new Duration(0L, 0);

    public static Duration ofSeconds(long seconds) {
        return new Duration(seconds, 0);
    }

    public static Duration ofSeconds(long seconds, long nanoAdjustment) {
        return create(seconds, nanoAdjustment);
    }

    public static Duration ofMillis(long millis) {
        return create(millis / 1000L, (millis % 1000L) * 1000000L);
    }

    public static Duration ofNanos(long nanos) {
        return create(nanos / NANOS_PER_SECOND, nanos % NANOS_PER_SECOND);
    }

    public static Duration ofMinutes(long minutes) {
        return new Duration(minutes * 60L, 0);
    }

    public static Duration ofHours(long hours) {
        return new Duration(hours * 3600L, 0);
    }

    public static Duration ofDays(long days) {
        return new Duration(days * 86400L, 0);
    }

    public long getSeconds() {
        return this.seconds;
    }

    public int getNano() {
        return this.nanos;
    }

    public boolean isZero() {
        return this.seconds == 0 && this.nanos == 0;
    }

    public boolean isNegative() {
        return this.seconds < 0;
    }

    public Duration plusSeconds(long secondsToAdd) {
        return create(this.seconds + secondsToAdd, this.nanos);
    }

    public Duration minusSeconds(long secondsToSubtract) {
        return create(this.seconds - secondsToSubtract, this.nanos);
    }

    public Duration plus(Duration other) {
        return create(this.seconds + other.seconds, (long) this.nanos + other.nanos);
    }

    public Duration minus(Duration other) {
        return create(this.seconds - other.seconds, (long) this.nanos - other.nanos);
    }

    public Duration negated() {
        return create(-this.seconds, -(long) this.nanos);
    }

    public long toMillis() {
        return this.seconds * 1000L + this.nanos / 1000000L;
    }

    public long toNanos() {
        return this.seconds * NANOS_PER_SECOND + this.nanos;
    }

    // Natural order by length. Synthesizes the compareTo(Object) bridge.
    // ---- fabricas -------------------------------------------------------------------------------

    /**
     * `amount` unidades de `unit`.
     *
     * <p>Solo acepta unidades **exactas**: las de tiempo, y `DAYS` --que aca vale 24 horas
     * justas--. `MONTHS` y `YEARS` se rechazan, y no es una limitacion: un mes no dura siempre lo
     * mismo, asi que no hay una cantidad de segundos que le corresponda. Eso es lo que modela
     * `Period`, no `Duration`.
     *
     * @throws java.time.DateTimeException si la unidad no tiene una duracion exacta
     */
    public static Duration of(long amount, TemporalUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        return ZERO.plus(amount, unit);
    }

    /**
     * La duracion entre dos puntos, medida en segundos y nanos.
     *
     * <p>Negativa si `end` es anterior a `start`, que es lo que la hace componible: siempre vale
     * `start.plus(between(start, end)).equals(end)`.
     */
    public static Duration between(Temporal startInclusive, Temporal endExclusive) {
        if (startInclusive == null || endExclusive == null) {
            throw new NullPointerException();
        }
        long segs = startInclusive.until(endExclusive, ChronoUnit.SECONDS);
        long nanos = 0L;
        if (startInclusive.isSupported(ChronoField.NANO_OF_SECOND)
                && endExclusive.isSupported(ChronoField.NANO_OF_SECOND)) {
            nanos = endExclusive.getLong(ChronoField.NANO_OF_SECOND)
                    - startInclusive.getLong(ChronoField.NANO_OF_SECOND);
        }
        return create(segs, nanos);
    }

    /** La duracion equivalente a `amount`, que tiene que estar en unidades exactas. */
    public static Duration from(TemporalAmount amount) {
        if (amount == null) {
            throw new NullPointerException("amount");
        }
        Duration d = ZERO;
        List<TemporalUnit> unidades = amount.getUnits();
        int i = 0;
        while (i < unidades.size()) {
            TemporalUnit u = unidades.get(i);
            d = d.plus(amount.get(u), u);
            i = i + 1;
        }
        return d;
    }

    // ---- las partes ------------------------------------------------------------------------------
    //
    // Dos familias que es facil confundir, y la diferencia importa: `toMinutes()` es la duracion
    // **entera** expresada en minutos, y `toMinutesPart()` es el campo de los minutos dentro de la
    // hora --de 0 a 59--. Para 3661 segundos, la primera da 61 y la segunda 1.

    /** El total, en dias de 24 horas, truncado hacia cero. */
    public long toDays() {
        return this.seconds / 86400L;
    }

    /** El total, en horas, truncado hacia cero. */
    public long toHours() {
        return this.seconds / 3600L;
    }

    /** El total, en minutos, truncado hacia cero. */
    public long toMinutes() {
        return this.seconds / 60L;
    }

    /** El total, en segundos, truncado hacia cero. */
    public long toSeconds() {
        return this.seconds;
    }

    /** Los dias, como parte. Igual que `toDays()`: no hay unidad mas grande de la que sea parte. */
    public long toDaysPart() {
        return this.seconds / 86400L;
    }

    /** Las horas dentro del dia, de 0 a 23. */
    public int toHoursPart() {
        return (int) (this.toHours() % 24L);
    }

    /** Los minutos dentro de la hora, de 0 a 59. */
    public int toMinutesPart() {
        return (int) (this.toMinutes() % 60L);
    }

    /** Los segundos dentro del minuto, de 0 a 59. */
    public int toSecondsPart() {
        return (int) (this.seconds % 60L);
    }

    /** Los milisegundos dentro del segundo, de 0 a 999. */
    public int toMillisPart() {
        return this.nanos / 1000000;
    }

    /** Los nanosegundos dentro del segundo, de 0 a 999999999. */
    public int toNanosPart() {
        return this.nanos;
    }

    /** Si es estrictamente mayor que cero. El complemento de `isNegative`, sin el cero. */
    public boolean isPositive() {
        return this.seconds > 0L || (this.seconds == 0L && this.nanos > 0);
    }

    // ---- aritmetica ------------------------------------------------------------------------------

    /**
     * Esta duracion mas `amount` unidades de `unit`.
     *
     * @throws java.time.DateTimeException si la unidad no tiene una duracion exacta
     */
    public Duration plus(long amountToAdd, TemporalUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (unit == ChronoUnit.DAYS) {
            return this.plusSeconds(amountToAdd * 86400L);
        }
        if (unit == ChronoUnit.HALF_DAYS) {
            return this.plusSeconds(amountToAdd * 43200L);
        }
        if (unit == ChronoUnit.HOURS) {
            return this.plusHours(amountToAdd);
        }
        if (unit == ChronoUnit.MINUTES) {
            return this.plusMinutes(amountToAdd);
        }
        if (unit == ChronoUnit.SECONDS) {
            return this.plusSeconds(amountToAdd);
        }
        if (unit == ChronoUnit.MILLIS) {
            return this.plusMillis(amountToAdd);
        }
        if (unit == ChronoUnit.MICROS) {
            return this.plusNanos(amountToAdd * 1000L);
        }
        if (unit == ChronoUnit.NANOS) {
            return this.plusNanos(amountToAdd);
        }
        throw new java.time.DateTimeException("Unit must not have an estimated duration: " + unit);
    }

    public Duration minus(long amountToSubtract, TemporalUnit unit) {
        return this.plus(-amountToSubtract, unit);
    }

    public Duration plusDays(long daysToAdd) {
        return this.plusSeconds(daysToAdd * 86400L);
    }

    public Duration plusHours(long hoursToAdd) {
        return this.plusSeconds(hoursToAdd * 3600L);
    }

    public Duration plusMinutes(long minutesToAdd) {
        return this.plusSeconds(minutesToAdd * 60L);
    }

    public Duration plusMillis(long millisToAdd) {
        return create(this.seconds + millisToAdd / 1000L,
                (long) this.nanos + (millisToAdd % 1000L) * 1000000L);
    }

    public Duration plusNanos(long nanosToAdd) {
        return create(this.seconds, (long) this.nanos + nanosToAdd);
    }

    public Duration minusDays(long daysToSubtract) {
        return this.plusDays(-daysToSubtract);
    }

    public Duration minusHours(long hoursToSubtract) {
        return this.plusHours(-hoursToSubtract);
    }

    public Duration minusMinutes(long minutesToSubtract) {
        return this.plusMinutes(-minutesToSubtract);
    }

    public Duration minusMillis(long millisToSubtract) {
        return this.plusMillis(-millisToSubtract);
    }

    public Duration minusNanos(long nanosToSubtract) {
        return this.plusNanos(-nanosToSubtract);
    }

    /** Esta duracion multiplicada por `multiplicand`. */
    public Duration multipliedBy(long multiplicand) {
        if (multiplicand == 0L) {
            return ZERO;
        }
        if (multiplicand == 1L) {
            return this;
        }
        // Se opera en nanos totales, que es donde la multiplicacion es una sola cuenta. El rango
        // util queda acotado por el `long`, igual que en el JDK.
        return Duration.ofNanos(this.toNanos() * multiplicand);
    }

    /**
     * Esta duracion dividida por `divisor`, truncando hacia cero.
     *
     * @throws ArithmeticException si `divisor` es cero
     */
    public Duration dividedBy(long divisor) {
        if (divisor == 0L) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        if (divisor == 1L) {
            return this;
        }
        return Duration.ofNanos(this.toNanos() / divisor);
    }

    /**
     * Cuantas veces entra `divisor` en esta duracion, truncando hacia cero.
     *
     * @throws ArithmeticException si `divisor` es cero
     */
    public long dividedBy(Duration divisor) {
        if (divisor == null) {
            throw new NullPointerException("divisor");
        }
        long d = divisor.toNanos();
        if (d == 0L) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return this.toNanos() / d;
    }

    /** El valor absoluto: esta misma si no es negativa, la negada si lo es. */
    public Duration abs() {
        return this.isNegative() ? this.negated() : this;
    }

    /** Esta duracion con otros segundos, conservando los nanos. */
    public Duration withSeconds(long seconds) {
        return create(seconds, (long) this.nanos);
    }

    /**
     * Esta duracion con otros nanos, conservando los segundos.
     *
     * @throws java.time.DateTimeException si `nanoOfSecond` cae fuera de [0, 999999999]
     */
    public Duration withNanos(int nanoOfSecond) {
        if (nanoOfSecond < 0 || nanoOfSecond > 999999999) {
            throw new java.time.DateTimeException(
                    "Invalid value for NanoOfSecond (valid values 0 - 999999999): " + nanoOfSecond);
        }
        return new Duration(this.seconds, nanoOfSecond);
    }

    /**
     * Esta duracion truncada a un multiplo de `unit`.
     *
     * <p>Trunca **hacia abajo**, hacia el cero, y por eso no sirve cualquier unidad: tiene que
     * dividir exactamente un dia. `HOURS` si, `DAYS` si, pero no una unidad estimada.
     *
     * @throws java.time.DateTimeException si la unidad no divide un dia
     */
    public Duration truncatedTo(TemporalUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (unit == ChronoUnit.SECONDS && this.seconds >= 0 && this.nanos == 0) {
            return this;
        }
        long unidadEnNanos = 0L;
        if (unit == ChronoUnit.NANOS) {
            unidadEnNanos = 1L;
        } else if (unit == ChronoUnit.MICROS) {
            unidadEnNanos = 1000L;
        } else if (unit == ChronoUnit.MILLIS) {
            unidadEnNanos = 1000000L;
        } else if (unit == ChronoUnit.SECONDS) {
            unidadEnNanos = NANOS_PER_SECOND;
        } else if (unit == ChronoUnit.MINUTES) {
            unidadEnNanos = 60L * NANOS_PER_SECOND;
        } else if (unit == ChronoUnit.HOURS) {
            unidadEnNanos = 3600L * NANOS_PER_SECOND;
        } else if (unit == ChronoUnit.HALF_DAYS) {
            unidadEnNanos = 43200L * NANOS_PER_SECOND;
        } else if (unit == ChronoUnit.DAYS) {
            unidadEnNanos = 86400L * NANOS_PER_SECOND;
        } else {
            throw new java.time.DateTimeException("Unit is too large to be used for truncation");
        }
        // Trunca **hacia cero**, no hacia abajo: -1.5s a segundos es -1s, y -90s a minutos es -60s.
        //
        // Lo verifique contra `java` real porque lo tenia al reves. "Truncar" sugiere ir hacia abajo
        // --que es lo que hace `Math.floorDiv`-- y aca es hacia cero, que para los negativos es la
        // direccion opuesta. La division entera de Java ya trunca hacia cero, asi que la cuenta sale
        // sola; lo que hacia falta era **no** corregir el resto negativo.
        return Duration.ofNanos(this.toNanos() / unidadEnNanos * unidadEnNanos);
    }

    public int compareTo(Duration other) {
        if (this.seconds < other.seconds) {
            return -1;
        }
        if (this.seconds > other.seconds) {
            return 1;
        }
        return this.nanos - other.nanos;
    }

    // --- TemporalAmount ---

    public long get(TemporalUnit unit) {
        if (unit == ChronoUnit.SECONDS) {
            return this.seconds;
        }
        if (unit == ChronoUnit.NANOS) {
            return this.nanos;
        }
        throw new IllegalArgumentException();
    }

    public List<TemporalUnit> getUnits() {
        List<TemporalUnit> units = new ArrayList<TemporalUnit>();
        units.add(ChronoUnit.SECONDS);
        units.add(ChronoUnit.NANOS);
        return units;
    }

    public Temporal addTo(Temporal temporal) {
        Temporal result = temporal;
        if (this.seconds != 0) {
            result = result.plus(this.seconds, ChronoUnit.SECONDS);
        }
        if (this.nanos != 0) {
            result = result.plus(this.nanos, ChronoUnit.NANOS);
        }
        return result;
    }

    public Temporal subtractFrom(Temporal temporal) {
        Temporal result = temporal;
        if (this.seconds != 0) {
            result = result.minus(this.seconds, ChronoUnit.SECONDS);
        }
        if (this.nanos != 0) {
            result = result.minus(this.nanos, ChronoUnit.NANOS);
        }
        return result;
    }

    // ISO-8601: PTnHnMnS (PT0S for zero), with a fractional seconds part when nanos are set.
    public String toString() {
        if (this.seconds == 0 && this.nanos == 0) {
            return "PT0S";
        }
        long hours = this.seconds / 3600;
        long minutes = (this.seconds % 3600) / 60;
        long secs = this.seconds % 60;
        StringBuilder buf = new StringBuilder("PT");
        if (hours != 0) {
            buf.append(Long.toString(hours));
            buf.append("H");
        }
        if (minutes != 0) {
            buf.append(Long.toString(minutes));
            buf.append("M");
        }
        if (secs == 0 && this.nanos == 0 && buf.length() > 2) {
            return buf.toString();
        }
        if (secs < 0 && this.nanos > 0) {
            if (secs == -1) {
                buf.append("-0");
            } else {
                buf.append(Long.toString(secs + 1));
            }
        } else {
            buf.append(Long.toString(secs));
        }
        if (this.nanos > 0) {
            long v;
            if (secs < 0) {
                v = 2000000000L - this.nanos;
            } else {
                v = this.nanos + 1000000000L;
            }
            String s = Long.toString(v);
            String frac = s.substring(1, s.length());
            int end = frac.length();
            while (end > 1 && frac.charAt(end - 1) == '0') {
                end = end - 1;
            }
            buf.append(".");
            buf.append(frac.substring(0, end));
        }
        buf.append("S");
        return buf.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Duration) {
            Duration o = (Duration) obj;
            return this.seconds == o.seconds && this.nanos == o.nanos;
        }
        return false;
    }

    public int hashCode() {
        return (int) (this.seconds ^ (this.seconds >>> 32)) + (51 * this.nanos);
    }

    // Parses an ISO duration PT[nH][nM][n[.n]S] (time components only, each optionally signed).
    public static Duration parse(CharSequence text) {
        String s = text.toString();
        int i = 2;
        long totalSeconds = 0;
        int nanos = 0;
        while (i < s.length()) {
            int sign = 1;
            if (s.charAt(i) == '-') {
                sign = -1;
                i = i + 1;
            } else if (s.charAt(i) == '+') {
                i = i + 1;
            }
            long whole = 0;
            while (i < s.length() && isDigit(s.charAt(i))) {
                whole = whole * 10 + (s.charAt(i) - '0');
                i = i + 1;
            }
            int frac = 0;
            if (i < s.length() && s.charAt(i) == '.') {
                i = i + 1;
                int fStart = i;
                while (i < s.length() && isDigit(s.charAt(i))) {
                    i = i + 1;
                }
                String f = s.substring(fStart, i);
                while (f.length() < 9) {
                    f = f + "0";
                }
                for (int k = 0; k < 9; k = k + 1) {
                    frac = frac * 10 + (f.charAt(k) - '0');
                }
            }
            char u = s.charAt(i);
            i = i + 1;
            if (u == 'H') {
                totalSeconds = totalSeconds + sign * whole * 3600;
            } else if (u == 'M') {
                totalSeconds = totalSeconds + sign * whole * 60;
            } else if (u == 'S') {
                totalSeconds = totalSeconds + sign * whole;
                nanos = nanos + sign * frac;
            }
        }
        while (nanos < 0) {
            nanos = nanos + 1000000000;
            totalSeconds = totalSeconds - 1;
        }
        while (nanos >= 1000000000) {
            nanos = nanos - 1000000000;
            totalSeconds = totalSeconds + 1;
        }
        return Duration.ofSeconds(totalSeconds, nanos);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
