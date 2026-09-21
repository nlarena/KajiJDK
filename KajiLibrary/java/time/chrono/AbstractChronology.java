package java.time.chrono;

import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.Map;

// KajiLibrary's java.time.chrono.AbstractChronology — the base class for Chronology implementations,
// supplying the identity/order plumbing (compare and equals by id, string form = id) so concrete
// chronologies only implement their calendar rules. A KajiLibrary subset of the JDK class.
public abstract class AbstractChronology implements Chronology {

    protected AbstractChronology() {
    }

    public int compareTo(Chronology other) {
        return this.getId().compareTo(other.getId());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AbstractChronology) {
            return this.compareTo((AbstractChronology) obj) == 0;
        }
        return false;
    }

    public int hashCode() {
        return this.getClass().hashCode() ^ this.getId().hashCode();
    }

    public String toString() {
        return this.getId();
    }

    /**
     * It rebuilds a date out of the loose fields a parse left behind.
     *
     * <p>The map is **consumed**: the fields used are removed, and what is left are the ones that
     * were not understood. It is the JDK's convention and not a detail: the caller then checks that
     * the map came back empty, and if it did not it knows exactly what was left over.
     *
     * <p>The order in which the combinations are tried is not arbitrary, it goes from the most
     * specific to the most general: `EPOCH_DAY` alone already says everything; `ERA` + `YEAR_OF_ERA`
     * are turned into `YEAR` before the month is looked at, because the year of the era is no use on
     * its own; and `DAY_OF_YEAR` is tried after month+day because if both are there, month and day
     * are what the user wrote.
     *
     * <p>**A subset of the JDK's resolveWith**, and it is worth saying which: resolved here are
     * `EPOCH_DAY`, `PROLEPTIC_MONTH`, `ERA`/`YEAR_OF_ERA` and the two date forms --year+month+day and
     * year+day-of-year--. The week combinations --`ALIGNED_WEEK_OF_MONTH` with `DAY_OF_WEEK`, and
     * `WeekFields`'-- are not; with those fields the map comes back unresolved instead of resolving
     * wrongly.
     */
    public ChronoLocalDate resolveDate(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
        if (fieldValues == null) {
            throw new NullPointerException("fieldValues");
        }
        if (resolverStyle == null) {
            throw new NullPointerException("resolverStyle");
        }
        boolean lenient = resolverStyle == ResolverStyle.LENIENT;

        // The epoch day names the date all by itself: there is nothing to combine and nothing to validate.
        Long epochDay = fieldValues.remove(ChronoField.EPOCH_DAY);
        if (epochDay != null) {
            return this.dateEpochDay(epochDay.longValue());
        }

        // The proleptic month is year and month together in one number: it is split before going on.
        Long prolepticMonth = fieldValues.remove(ChronoField.PROLEPTIC_MONTH);
        if (prolepticMonth != null) {
            long pm = prolepticMonth.longValue();
            if (!lenient) {
                ChronoField.PROLEPTIC_MONTH.checkValidValue(pm);
            }
            long yearNum = Math.floorDiv(pm, 12L);
            long monthNum = Math.floorMod(pm, 12L) + 1L;
            fieldValues.put(ChronoField.YEAR, Long.valueOf(yearNum));
            fieldValues.put(ChronoField.MONTH_OF_YEAR, Long.valueOf(monthNum));
        }

        // The era plus the year of the era give the proleptic year, the only one that is counted with.
        Long eraValue = fieldValues.remove(ChronoField.ERA);
        Long yearOfEra = fieldValues.remove(ChronoField.YEAR_OF_ERA);
        if (yearOfEra != null && !fieldValues.containsKey(ChronoField.YEAR)) {
            int yoe = (int) yearOfEra.longValue();
            Era era;
            if (eraValue != null) {
                era = this.eraOf((int) eraValue.longValue());
            } else {
                // With no era written, the last on the list: it is the current one, which is what
                // somebody writing a year with no era means.
                List2 list = new List2(this.eras());
                era = list.last();
            }
            fieldValues.put(ChronoField.YEAR,
                    Long.valueOf((long) this.prolepticYear(era, yoe)));
        }

        Long year = fieldValues.remove(ChronoField.YEAR);
        if (year == null) {
            // With no year there is no date. The fields taken out are put back: the caller has to be
            // able to see what was there, not a half-emptied map.
            if (eraValue != null) {
                fieldValues.put(ChronoField.ERA, eraValue);
            }
            if (yearOfEra != null) {
                fieldValues.put(ChronoField.YEAR_OF_ERA, yearOfEra);
            }
            return null;
        }
        int yearNum = (int) year.longValue();

        Long month = fieldValues.remove(ChronoField.MONTH_OF_YEAR);
        Long dayOfMonth = fieldValues.remove(ChronoField.DAY_OF_MONTH);
        if (month != null && dayOfMonth != null) {
            long m = month.longValue();
            long d = dayOfMonth.longValue();
            if (lenient) {
                // Lenient: the overflows carry. `2011-02-31` is the 3rd of March, and a month 14 is
                // the following year's February. The first day is built and then added to.
                ChronoLocalDate base = this.date(yearNum, 1, 1);
                ChronoLocalDate withMonth = base.plus(m - 1L, java.time.temporal.ChronoUnit.MONTHS);
                return withMonth.plus(d - 1L, java.time.temporal.ChronoUnit.DAYS);
            }
            if (resolverStyle == ResolverStyle.SMART) {
                // Smart: the month has to exist, but a day that goes past is clipped to the month's
                // last. It is what makes `31 February` be the 28th and not an error.
                ChronoField.DAY_OF_MONTH.checkValidValue(d);
                ChronoLocalDate first = this.date(yearNum, (int) m, 1);
                int length = first.lengthOfMonth();
                long day = d > (long) length ? (long) length : d;
                return this.date(yearNum, (int) m, (int) day);
            }
            return this.date(yearNum, (int) m, (int) d);
        }

        Long dayOfYear = fieldValues.remove(ChronoField.DAY_OF_YEAR);
        if (dayOfYear != null) {
            if (lenient) {
                ChronoLocalDate base = this.dateYearDay(yearNum, 1);
                return base.plus(dayOfYear.longValue() - 1L, java.time.temporal.ChronoUnit.DAYS);
            }
            return this.dateYearDay(yearNum, (int) dayOfYear.longValue());
        }

        // There was a year but it was not enough for a date: what was taken out is put back, for the
        // same reason as above.
        if (month != null) {
            fieldValues.put(ChronoField.MONTH_OF_YEAR, month);
        }
        if (dayOfMonth != null) {
            fieldValues.put(ChronoField.DAY_OF_MONTH, dayOfMonth);
        }
        fieldValues.put(ChronoField.YEAR, year);
        return null;
    }
}

// A three-line wrapper for taking the last element of the era list without chaining through an
// interface-typed intermediate, which gets lost silently (#108).
final class List2 {

    private final java.util.List<Era> list;

    List2(java.util.List<Era> list) {
        this.list = list;
    }

    Era last() {
        int n = this.list.size();
        return this.list.get(n - 1);
    }
}
