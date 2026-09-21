package java.util.spi;

import java.util.Locale;
import java.util.Map;

/**
 * KajiLibrary's java.util.spi.CalendarNameProvider -- the names of a calendar's fields.
 *
 * <p>Months, weekdays, AM/PM, eras. What it returns depends on three things at once: the field, the
 * style --long, short, narrow-- and the locale.
 *
 * <h2>The standalone style, which is the part that is not obvious</h2>
 *
 * <p>Several languages write a month's name differently depending on whether it appears <b>alone</b>
 * --in a calendar's heading-- or <b>inside a date</b>. In Russian the genitive; in Czech and Finnish
 * the same. That is why the styles come in pairs, with and without
 * {@code Calendar.STANDALONE_MASK}, and a provider that returns the same for both is right in
 * Spanish and wrong in Russian.
 *
 * <h2>getDisplayNames is the reverse direction</h2>
 *
 * <p>{@link #getDisplayName} translates a value into a name; {@link #getDisplayNames} returns the
 * <b>name to value</b> map, and it serves for <b>parsing</b>. That is why it can have more entries
 * than values: several forms of the same month point at the same number.
 */
public abstract class CalendarNameProvider extends LocaleServiceProvider {

    protected CalendarNameProvider() {
    }

    /**
     * The name of that value of that field.
     *
     * @param calendarType the calendar's type: {@code "gregory"}, {@code "buddhist"}, ...
     * @param field        the {@code Calendar} field
     * @param value        the field's value
     * @param style        the style, with or without {@code STANDALONE_MASK}; see the class's note
     * @return null if this provider does not have that name
     */
    public abstract String getDisplayName(String calendarType, int field, int value, int style,
        Locale locale);

    /**
     * The name-to-value map, for parsing. See the class's note.
     *
     * @return null if this provider does not have those names
     */
    public abstract Map<String, Integer> getDisplayNames(String calendarType, int field, int style,
        Locale locale);
}
