package java.util.spi;

import java.util.Locale;

/**
 * KajiLibrary's java.util.spi.TimeZoneNameProvider -- what a time zone is called.
 *
 * <h2>Specific against generic, which are different things</h2>
 *
 * <ul>
 *   <li>{@link #getDisplayName} gives the name <b>of one of the year's two halves</b>: "Eastern
 *       Standard Time" or "Eastern Daylight Time". The boolean chooses which.
 *   <li>{@link #getGenericDisplayName} gives the one that serves both: "Eastern Time". It is the one
 *       to show when there is no concrete date -- in a zone picker, say, where saying "Daylight
 *       Time" would be false half the year.
 * </ul>
 *
 * <p>The generic one has a default and returns null because many zones do not have one: a zone with
 * no daylight saving needs no distinction, and others simply have no agreed name.
 */
public abstract class TimeZoneNameProvider extends LocaleServiceProvider {

    protected TimeZoneNameProvider() {
    }

    /**
     * The name of one of the year's two halves.
     *
     * @param ID       the zone's identifier ({@code "America/Argentina/Buenos_Aires"})
     * @param daylight whether the daylight-saving name is being asked for
     * @param style    {@code TimeZone.LONG} or {@code TimeZone.SHORT}
     * @return null if this provider does not have it
     */
    public abstract String getDisplayName(String ID, boolean daylight, int style, Locale locale);

    /** The name that serves both halves. See the class's note. */
    public String getGenericDisplayName(String ID, int style, Locale locale) {
        return null;
    }
}
