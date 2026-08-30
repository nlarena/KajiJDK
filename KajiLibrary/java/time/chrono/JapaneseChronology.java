package java.time.chrono;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.ValueRange;
import java.util.Arrays;
import java.util.List;

// KajiLibrary's java.time.chrono.JapaneseChronology — the Japanese imperial calendar system. A
// singleton reachable through INSTANCE.
//
// It is ISO-based: months, days and leap years are exactly ISO's, and the proleptic year is the ISO
// year. Only the era layer differs — which is why isLeapYear delegates straight to IsoChronology
// while eraOf/prolepticYear carry the era boundary data (see JapaneseDate.EraTable).
//
// A KajiLibrary subset of the JDK class, mirroring MinguoChronology/ThaiBuddhistChronology.
public final class JapaneseChronology extends AbstractChronology {

    public static final JapaneseChronology INSTANCE = new JapaneseChronology();

    private JapaneseChronology() {
    }

    public String getId() {
        return "Japanese";
    }

    public String getCalendarType() {
        return "japanese";
    }

    public JapaneseDate date(int prolepticYear, int month, int dayOfMonth) {
        return JapaneseDate.of(prolepticYear, month, dayOfMonth);
    }

    public JapaneseDate dateEpochDay(long epochDay) {
        LocalDate iso = LocalDate.ofEpochDay(epochDay);
        return JapaneseDate.of(iso.getYear(), iso.getMonthValue(), iso.getDayOfMonth());
    }

    public boolean isLeapYear(long prolepticYear) {
        return IsoChronology.INSTANCE.isLeapYear(prolepticYear);
    }

    public JapaneseEra eraOf(int eraValue) {
        return JapaneseEra.of(eraValue);
    }

    public int prolepticYear(Era era, int yearOfEra) {
        JapaneseEra japaneseEra = (JapaneseEra) era;
        return EraTable.prolepticYear(japaneseEra, yearOfEra);
    }

    // ---- lo que el calendario tiene que saber contestar ------------------------------------------

    public JapaneseDate date(TemporalAccessor temporal) {
        if (temporal instanceof JapaneseDate) {
            return (JapaneseDate) temporal;
        }
        return this.dateEpochDay(temporal.getLong(ChronoField.EPOCH_DAY));
    }

    public JapaneseDate dateYearDay(int prolepticYear, int dayOfYear) {
        LocalDate iso = LocalDate.ofYearDay(prolepticYear, dayOfYear);
        return JapaneseDate.of(prolepticYear, iso.getMonthValue(), iso.getDayOfMonth());
    }

    public JapaneseDate dateNow() {
        return this.dateNow(Clock.systemDefaultZone());
    }

    public JapaneseDate dateNow(ZoneId zone) {
        return this.dateNow(Clock.system(zone));
    }

    public JapaneseDate dateNow(Clock clock) {
        if (clock == null) {
            throw new NullPointerException("clock");
        }
        LocalDate hoy = LocalDate.now(clock);
        return this.dateEpochDay(hoy.toEpochDay());
    }

    public JapaneseDate date(Era era, int yearOfEra, int month, int dayOfMonth) {
        return this.date(this.prolepticYear(era, yearOfEra), month, dayOfMonth);
    }

    public JapaneseDate dateYearDay(Era era, int yearOfEra, int dayOfYear) {
        return this.dateYearDay(this.prolepticYear(era, yearOfEra), dayOfYear);
    }

    /** Si: los meses, los dias y los anios bisiestos son exactamente los del ISO. */
    public boolean isIsoBased() {
        return true;
    }

    public ValueRange range(ChronoField field) {
        // El anio proleptico japones **es** el anio ISO: los meses, los dias y los anios bisiestos
        // son los mismos. Lo unico propio es la capa de eras, y por eso son los dos unicos campos
        // con un rango distinto.
        if (field == ChronoField.ERA) {
            return EraTable.rangoDeEras();
        }
        if (field == ChronoField.YEAR_OF_ERA) {
            return EraTable.rangoDeAnioDeEra();
        }
        return field.range();
    }

    public List<Era> eras() {
        return EraTable.todas();
    }

    public JapaneseDate resolveDate(java.util.Map<java.time.temporal.TemporalField, Long> fieldValues,
            java.time.format.ResolverStyle resolverStyle) {
        // Ligado a una local: encadenar por un intermedio de tipo interfaz se pierde (#108).
        ChronoLocalDate resuelta = super.resolveDate(fieldValues, resolverStyle);
        return (JapaneseDate) resuelta;
    }
}
