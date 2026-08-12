package java.time.chrono;

// KajiLibrary's java.time.chrono.Chronology — a calendar system (the ISO-8601 calendar, plus in the
// full JDK the Japanese/Hijrah/Minguo/ThaiBuddhist systems). A KajiLibrary subset: identification,
// leap-year test, era lookup and date construction (from year/month/day or an epoch-day). Ordered by
// id, so it extends Comparable<Chronology>.
public interface Chronology extends Comparable<Chronology> {

    String getId();

    String getCalendarType();

    boolean isLeapYear(long prolepticYear);

    ChronoLocalDate date(int prolepticYear, int month, int dayOfMonth);

    ChronoLocalDate dateEpochDay(long epochDay);

    Era eraOf(int eraValue);

    int compareTo(Chronology other);
}
