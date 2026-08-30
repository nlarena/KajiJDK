package java.util;

// The concrete TimeZone behind every zone this library hands out: a constant offset from UTC and
// no daylight saving. Package-private on purpose — it is an internal, and the contract only ever
// promises a TimeZone back.
//
// This is where the fixed-offset policy of TimeZone actually lives: without the IANA tzdb there
// are no transitions to model, so getOffset is the raw offset at every instant, useDaylightTime
// is false, and inDaylightTime is false. A zone that lies about none of this is more useful than
// one that guesses.
final class FixedTimeZone extends TimeZone {

    // What parseCustom returns for an id it cannot read. Integer.MIN_VALUE rather than -1,
    // because -1 is a legal (if odd) millisecond offset and 0 is GMT itself.
    static final int BAD_OFFSET = -2147483648;

    private static final int ONE_MINUTE = 60000;
    private static final int ONE_HOUR = 3600000;

    // The offset from UTC in milliseconds. Not final: TimeZone.setRawOffset is public API.
    private int rawOffset;

    FixedTimeZone(String id, int rawOffset) {
        this.setID(id);
        this.rawOffset = rawOffset;
    }

    // Constant offset, so the calendar fields do not matter.
    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
        return this.rawOffset;
    }

    public void setRawOffset(int offsetMillis) {
        this.rawOffset = offsetMillis;
    }

    public int getRawOffset() {
        return this.rawOffset;
    }

    // No transitions, so no daylight saving, ever.
    public boolean useDaylightTime() {
        return false;
    }

    public boolean inDaylightTime(Date date) {
        return false;
    }

    // Reads a custom id of the form "GMT+HH:MM" and returns the offset in milliseconds, or
    // BAD_OFFSET if it does not parse.
    //
    // The accepted forms are the JDK's: after "GMT" and a sign, either "H", "HH", "H:MM",
    // "HH:MM", "HMM" or "HHMM". Hours are bounded at 23 and minutes at 59 — the same bound the
    // JDK applies, which is why "GMT+24:00" is not a zone but "GMT+23:59" is.
    static int parseCustom(String id) {
        if (id.length() < 5) {
            return BAD_OFFSET;
        }
        char sign = id.charAt(3);
        if (sign != '+' && sign != '-') {
            return BAD_OFFSET;
        }
        String body = id.substring(4);
        int hours;
        int minutes;
        int colon = body.indexOf(':');
        if (colon >= 0) {
            hours = digits(body.substring(0, colon));
            minutes = digits(body.substring(colon + 1));
            if (body.length() - colon - 1 != 2) {
                return BAD_OFFSET;
            }
        } else if (body.length() <= 2) {
            hours = digits(body);
            minutes = 0;
        } else if (body.length() == 3 || body.length() == 4) {
            int split = body.length() - 2;
            hours = digits(body.substring(0, split));
            minutes = digits(body.substring(split));
        } else {
            return BAD_OFFSET;
        }
        if (hours < 0 || minutes < 0 || hours > 23 || minutes > 59) {
            return BAD_OFFSET;
        }
        int magnitude = hours * ONE_HOUR + minutes * ONE_MINUTE;
        if (sign == '-') {
            return -magnitude;
        }
        return magnitude;
    }

    // The non-negative value of a run of ASCII digits, or -1 if it is empty or holds anything
    // else. Deliberately not Integer.parseInt: a bad id here is a normal outcome to be reported,
    // not an exception to be thrown and caught one frame up.
    private static int digits(String s) {
        if (s.length() == 0) {
            return -1;
        }
        int value = 0;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            value = value * 10 + (c - '0');
            i = i + 1;
        }
        return value;
    }

    // The canonical id for an offset: "GMT+HH:MM", the form the JDK normalises custom ids to.
    static String normalize(int offsetMillis) {
        return "GMT" + offsetSuffix(offsetMillis);
    }

    // The "+HH:MM" part of a canonical id, which is also exactly what ZoneId.of accepts.
    static String offsetSuffix(int offsetMillis) {
        int magnitude = offsetMillis;
        String sign = "+";
        if (magnitude < 0) {
            magnitude = -magnitude;
            sign = "-";
        }
        int hours = magnitude / ONE_HOUR;
        int minutes = (magnitude % ONE_HOUR) / ONE_MINUTE;
        return sign + two(hours) + ":" + two(minutes);
    }

    // A number as two digits, zero-padded.
    private static String two(int n) {
        if (n < 10) {
            return "0" + n;
        }
        return "" + n;
    }
}
