package java.util.spi;

import java.util.Locale;

/**
 * KajiLibrary's java.util.spi.CalendarDataProvider -- the two numbers that define a week.
 *
 * <p>They are few but they are the ones that break dates when they are wrong:
 *
 * <ul>
 *   <li><b>The first day of the week</b>: Sunday in the United States, Monday in almost all of
 *       Europe and Latin America, Saturday in several Arab countries.
 *   <li><b>The minimal days in the first week</b>: how many days of the new year a week has to have
 *       to count as the first. With 1 the first week can have a single day; with 4 --the ISO rule--
 *       the one containing the 1st of January can belong to the previous year.
 * </ul>
 *
 * <p>The second is the one that surprises: it is the reason the 1st of January can fall in "week 52
 * of last year".
 */
public abstract class CalendarDataProvider extends LocaleServiceProvider {

    protected CalendarDataProvider() {
    }

    /**
     * Which day the week starts on, with {@code Calendar}'s values: 1 Sunday, 2 Monday, ... 7
     * Saturday.
     *
     * @return 0 if this provider has no datum for that locale
     */
    public abstract int getFirstDayOfWeek(Locale locale);

    /**
     * How many days of the new year a week needs in order to be the first.
     *
     * @return 0 if this provider has no datum for that locale
     */
    public abstract int getMinimalDaysInFirstWeek(Locale locale);
}
