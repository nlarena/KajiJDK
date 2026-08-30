package java.util;

import java.io.Serializable;
import java.time.ZoneId;
import java.util.stream.Stream;

// The legacy time-zone type: an offset from UTC, plus whatever daylight-saving rules apply, plus
// an id. Abstract because the rules are the part it cannot know — a subclass supplies them.
//
// A KajiLibrary subset, and deliberately the *same* subset java.time already committed to (see
// ZoneId): only fixed-offset zones exist here. Region ids like "Europe/Paris" need the IANA tzdb
// transition tables, which is a data wall, not a coding one. Two consequences worth stating
// plainly rather than discovering later:
//
//   - getTimeZone("Europe/Paris") returns GMT. That is not an invention: the JDK also returns GMT
//     for an id it does not recognise. What differs is how much it recognises.
//   - getAvailableIDs() lists what this actually supports, not the ~600 region ids.
//
// getDefault() is GMT for the same reason ZoneId.systemDefault() is UTC: there is no OS time-zone
// lookup here. setDefault() works, so a program that needs a specific zone can install one.
public abstract class TimeZone implements Serializable, Cloneable {

    // Style for getDisplayName: the abbreviation, e.g. "PST".
    public static final int SHORT = 0;

    // Style for getDisplayName: the full name, e.g. "Pacific Standard Time".
    public static final int LONG = 1;

    // One hour in milliseconds: the daylight saving amount a zone here would use.
    private static final int ONE_HOUR = 3600000;

    // The id of this zone, e.g. "GMT" or "GMT+05:30".
    private String ID;

    // The process-wide default, installed by setDefault. Null until first read, so getDefault
    // does not force a zone to exist before anyone asks for one.
    private static TimeZone defaultZone;

    // A zone with no id and no rules yet; a subclass supplies both.
    public TimeZone() {
    }

    // The offset from UTC in milliseconds at the instant described by these calendar fields.
    // The one question a subclass must answer.
    public abstract int getOffset(int era, int year, int month, int day, int dayOfWeek,
                                  int milliseconds);

    // The offset from UTC in milliseconds at the given instant, daylight saving included.
    public int getOffset(long date) {
        if (this.inDaylightTime(new Date(date))) {
            return this.getRawOffset() + this.getDSTSavings();
        }
        return this.getRawOffset();
    }

    // Sets the base offset from UTC, before any daylight saving.
    public abstract void setRawOffset(int offsetMillis);

    // The base offset from UTC in milliseconds, before any daylight saving.
    public abstract int getRawOffset();

    // This zone's id.
    public String getID() {
        return this.ID;
    }

    // Sets this zone's id. Does not change the rules — only the label.
    public void setID(String ID) {
        if (ID == null) {
            throw new NullPointerException();
        }
        this.ID = ID;
    }

    // The long, standard-time name in the default locale.
    public final String getDisplayName() {
        return this.getDisplayName(false, LONG, Locale.getDefault());
    }

    // The long, standard-time name in the given locale.
    public final String getDisplayName(Locale locale) {
        return this.getDisplayName(false, LONG, locale);
    }

    // The name in the default locale, for the given daylight flag and style.
    public final String getDisplayName(boolean daylight, int style) {
        return this.getDisplayName(daylight, style, Locale.getDefault());
    }

    // The name of this zone.
    //
    // A KajiLibrary subset: the JDK reads localised zone names out of the locale data bundles.
    // With no such bundles here, every style and locale yields the id — which is never wrong
    // about which zone it names, only less friendly. The style is still validated, because
    // passing a bad style is a caller bug either way.
    public String getDisplayName(boolean daylight, int style, Locale locale) {
        if (style != SHORT && style != LONG) {
            throw new IllegalArgumentException("Illegal style: " + style);
        }
        if (locale == null) {
            throw new NullPointerException();
        }
        return this.getID();
    }

    // How much daylight saving this zone adds when it is in effect: one hour for a zone that
    // observes it, zero otherwise.
    public int getDSTSavings() {
        if (this.useDaylightTime()) {
            return ONE_HOUR;
        }
        return 0;
    }

    // Whether this zone observes daylight saving at all.
    public abstract boolean useDaylightTime();

    // Whether this zone observes daylight saving now, or is in it now. Distinct from
    // useDaylightTime for a zone that has stopped observing DST but is asked about a past date.
    public boolean observesDaylightTime() {
        return this.useDaylightTime() || this.inDaylightTime(new Date());
    }

    // Whether the given instant falls inside this zone's daylight saving period.
    public abstract boolean inDaylightTime(Date date);

    // The zone for the given id.
    //
    // Recognised: "GMT", "UTC", "UT", and the custom forms "GMT+H", "GMT+HH", "GMT+HH:MM" and
    // "GMT+HHMM" with either sign. Anything else yields GMT, which is what the JDK does for an
    // id it does not recognise.
    public static TimeZone getTimeZone(String ID) {
        if (ID == null) {
            throw new NullPointerException();
        }
        if (ID.equals("GMT") || ID.equals("UTC") || ID.equals("UT")) {
            return new FixedTimeZone(ID, 0);
        }
        if (ID.startsWith("GMT+") || ID.startsWith("GMT-")) {
            int offset = FixedTimeZone.parseCustom(ID);
            if (offset != FixedTimeZone.BAD_OFFSET) {
                return new FixedTimeZone(FixedTimeZone.normalize(offset), offset);
            }
        }
        return new FixedTimeZone("GMT", 0);
    }

    // The zone equivalent to the given ZoneId. Only fixed-offset ZoneIds exist here, so this
    // round-trips with toZoneId.
    public static TimeZone getTimeZone(ZoneId zoneId) {
        if (zoneId == null) {
            throw new NullPointerException();
        }
        String id = zoneId.getId();
        if (id.equals("Z")) {
            return new FixedTimeZone("GMT", 0);
        }
        char c = id.charAt(0);
        if (c == '+' || c == '-') {
            return getTimeZone("GMT" + id);
        }
        return new FixedTimeZone("GMT", 0);
    }

    // This zone as a ZoneId.
    public ZoneId toZoneId() {
        int offset = this.getRawOffset();
        if (offset == 0) {
            return ZoneId.of("Z");
        }
        return ZoneId.of(FixedTimeZone.offsetSuffix(offset));
    }

    // The supported ids whose base offset equals the given one.
    public static String[] getAvailableIDs(int rawOffset) {
        if (rawOffset == 0) {
            String[] zero = new String[3];
            zero[0] = "GMT";
            zero[1] = "UTC";
            zero[2] = "UT";
            return zero;
        }
        String[] one = new String[1];
        one[0] = FixedTimeZone.normalize(rawOffset);
        return one;
    }

    // Every id this supports. A KajiLibrary subset: the three zero-offset ids. Custom GMT offset
    // ids are accepted by getTimeZone but not enumerated here, since there are as many of them as
    // there are minutes in a day either side of GMT.
    public static String[] getAvailableIDs() {
        return getAvailableIDs(0);
    }

    // getAvailableIDs(int) as a stream.
    public static Stream<String> availableIDs(int rawOffset) {
        return (Stream<String>) Stream.of(getAvailableIDs(rawOffset));
    }

    // getAvailableIDs() as a stream.
    public static Stream<String> availableIDs() {
        return (Stream<String>) Stream.of(getAvailableIDs());
    }

    // The process-wide default zone: GMT, unless setDefault installed another.
    //
    // A copy is handed out, not the stored zone. TimeZone is mutable through setRawOffset and
    // setID, so returning the shared instance would let any caller silently re-point every other
    // caller's idea of "default".
    public static TimeZone getDefault() {
        synchronized (TimeZone.class) {
            if (defaultZone == null) {
                defaultZone = new FixedTimeZone("GMT", 0);
            }
            return (TimeZone) defaultZone.clone();
        }
    }

    // Installs the process-wide default zone. Passing null restores GMT.
    public static void setDefault(TimeZone zone) {
        synchronized (TimeZone.class) {
            if (zone == null) {
                defaultZone = null;
                return;
            }
            defaultZone = (TimeZone) zone.clone();
        }
    }

    // Whether the other zone has the same rules as this one, ignoring the id. Two zones with
    // different ids and identical rules answer true — that is the point of the method.
    public boolean hasSameRules(TimeZone other) {
        return other != null
            && this.getRawOffset() == other.getRawOffset()
            && this.useDaylightTime() == other.useDaylightTime();
    }

    // A copy of this zone, id included.
    public Object clone() {
        try {
            TimeZone other = (TimeZone) super.clone();
            other.ID = this.ID;
            return other;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
    }
}
