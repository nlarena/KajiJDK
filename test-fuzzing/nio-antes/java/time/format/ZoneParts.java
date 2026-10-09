package java.time.format;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.Chronology;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalQueries;
import java.util.Iterator;
import java.util.Set;

// The zone offset written as a number: `+01:00`, `-0330`, `Z`.
//
// The pattern it receives --`"+HH:MM:ss"` and the JDK's other twenty-one-- is not kept as a string:
// it is broken into three decisions (how many hour digits, whether the minutes always come out or
// only when they are not zero, likewise the seconds) and it is those three that drive the writing
// and the reading. Keeping it as a string would mean reinterpreting it on every call and, worse,
// keeping two separate interpretations for the two directions.
//
// **The capital letter rules.** `MM` always comes out; `mm` comes out only when there is something
// to say. That is the whole difference between `"+HH:MM"` (which writes `+05:00`) and `"+HH:mm"`
// (which writes `+05`).
final class OffsetPart extends Part {

    static final int ABSENT = 0;
    static final int CONDITIONAL = 1;
    static final int ALWAYS = 2;

    private final int hourWidth;
    private final boolean colons;
    private final int minuteMode;
    private final int secondMode;
    private final String noOffset;

    OffsetPart(String pattern, String noOffset) {
        this.noOffset = noOffset;
        if (pattern == null || pattern.length() < 2 || pattern.charAt(0) != '+') {
            throw new IllegalArgumentException("Invalid zone offset pattern: " + pattern);
        }
        int i = 1;
        int hours = 0;
        while (i < pattern.length() && pattern.charAt(i) == 'H') {
            hours = hours + 1;
            i = i + 1;
        }
        if (hours < 1 || hours > 2) {
            throw new IllegalArgumentException("Invalid zone offset pattern: " + pattern);
        }
        boolean colon = false;
        if (i < pattern.length() && pattern.charAt(i) == ':') {
            colon = true;
            i = i + 1;
        }
        int min = ABSENT;
        if (i < pattern.length() && (pattern.charAt(i) == 'm' || pattern.charAt(i) == 'M')) {
            min = pattern.charAt(i) == 'M' ? ALWAYS : CONDITIONAL;
            char c = pattern.charAt(i);
            int n = 0;
            while (i < pattern.length() && pattern.charAt(i) == c) {
                n = n + 1;
                i = i + 1;
            }
            if (n != 2) {
                throw new IllegalArgumentException("Invalid zone offset pattern: " + pattern);
            }
        }
        if (i < pattern.length() && pattern.charAt(i) == ':') {
            if (!colon) {
                throw new IllegalArgumentException("Invalid zone offset pattern: " + pattern);
            }
            i = i + 1;
        }
        int sec = ABSENT;
        if (i < pattern.length() && (pattern.charAt(i) == 's' || pattern.charAt(i) == 'S')) {
            sec = pattern.charAt(i) == 'S' ? ALWAYS : CONDITIONAL;
            char c = pattern.charAt(i);
            int n = 0;
            while (i < pattern.length() && pattern.charAt(i) == c) {
                n = n + 1;
                i = i + 1;
            }
            if (n != 2) {
                throw new IllegalArgumentException("Invalid zone offset pattern: " + pattern);
            }
        }
        if (i != pattern.length() || (sec != ABSENT && min == ABSENT)) {
            throw new IllegalArgumentException("Invalid zone offset pattern: " + pattern);
        }
        this.hourWidth = hours;
        this.colons = colon;
        this.minuteMode = min;
        this.secondMode = sec;
    }

    boolean print(PrintContext ctx, StringBuilder out) {
        Long v = ctx.value(ChronoField.OFFSET_SECONDS);
        if (v == null) {
            return false;
        }
        long total = v.longValue();
        if (total == 0L && this.noOffset.length() > 0) {
            out.append(this.noOffset);
            return true;
        }
        long abs = total < 0L ? -total : total;
        long h = abs / 3600L;
        long m = abs / 60L % 60L;
        long s = abs % 60L;
        boolean secOut = this.secondMode == ALWAYS || (this.secondMode == CONDITIONAL && s != 0L);
        boolean minOut = this.minuteMode == ALWAYS
                || (this.minuteMode == CONDITIONAL && (m != 0L || secOut));
        out.append(total < 0L ? '-' : '+');
        Part.writeDigits(ctx, out, Long.toString(h), this.hourWidth);
        if (minOut) {
            if (this.colons) {
                out.append(':');
            }
            Part.writeDigits(ctx, out, Long.toString(m), 2);
            if (secOut) {
                if (this.colons) {
                    out.append(':');
                }
                Part.writeDigits(ctx, out, Long.toString(s), 2);
            }
        }
        return true;
    }

    int parse(ParseContext ctx, String text, int pos) {
        int noOffsetLength = this.noOffset.length();
        if (noOffsetLength == 0) {
            // `appendOffset(pattern, "")` has no text for zero: zero is written with numbers. Only
            // the end of the text is taken as "there was no offset".
            if (pos == text.length()) {
                ctx.offset = ZoneOffset.UTC;
                ctx.put(ChronoField.OFFSET_SECONDS, 0L);
                return pos;
            }
        } else if (text.regionMatches(!ctx.caseSensitive, pos, this.noOffset, 0, noOffsetLength)) {
            ctx.offset = ZoneOffset.UTC;
            ctx.put(ChronoField.OFFSET_SECONDS, 0L);
            return pos + noOffsetLength;
        }
        if (pos >= text.length()) {
            return ~pos;
        }
        char sign = text.charAt(pos);
        if (sign != '+' && sign != '-') {
            return ~pos;
        }
        int negative = sign == '-' ? -1 : 1;
        int[] parts = {0, 0, 0};
        int p = pos + 1;
        int read = this.readPair(text, p, this.hourWidth == 1, parts, 0);
        if (read < 0) {
            return ~pos;
        }
        p = read;
        if (this.minuteMode != ABSENT) {
            int q = p;
            if (this.colons) {
                if (q < text.length() && text.charAt(q) == ':') {
                    q = q + 1;
                } else {
                    q = -1;
                }
            }
            if (q >= 0) {
                int r = this.readPair(text, q, false, parts, 1);
                if (r >= 0) {
                    p = r;
                    if (this.secondMode != ABSENT) {
                        int q2 = p;
                        if (this.colons) {
                            if (q2 < text.length() && text.charAt(q2) == ':') {
                                q2 = q2 + 1;
                            } else {
                                q2 = -1;
                            }
                        }
                        if (q2 >= 0) {
                            int r2 = this.readPair(text, q2, false, parts, 2);
                            if (r2 >= 0) {
                                p = r2;
                            }
                        }
                    }
                } else if (ctx.strict && this.minuteMode == ALWAYS) {
                    return ~pos;
                }
            } else if (ctx.strict && this.minuteMode == ALWAYS) {
                return ~pos;
            }
        }
        if (parts[1] > 59 || parts[2] > 59) {
            return ~pos;
        }
        int total = negative * (parts[0] * 3600 + parts[1] * 60 + parts[2]);
        ZoneOffset off;
        try {
            off = ZoneOffset.ofTotalSeconds(total);
        } catch (DateTimeException e) {
            return ~pos;
        }
        ctx.offset = off;
        ctx.put(ChronoField.OFFSET_SECONDS, (long) total);
        return p;
    }

    // Two digits --or one, when the pattern is `+H` and the second is not there. Returns the next
    // index, or -1.
    private int readPair(String text, int pos, boolean oneWillDo, int[] parts, int which) {
        if (pos >= text.length()) {
            return -1;
        }
        int d1 = text.charAt(pos) - '0';
        if (d1 < 0 || d1 > 9) {
            return -1;
        }
        if (pos + 1 < text.length()) {
            int d2 = text.charAt(pos + 1) - '0';
            if (d2 >= 0 && d2 <= 9) {
                parts[which] = d1 * 10 + d2;
                return pos + 2;
            }
        }
        if (oneWillDo || which == 0) {
            parts[which] = d1;
            return pos + 1;
        }
        return -1;
    }
}

// The zone's identifier: `Europe/Paris`, `Z`, `+05:00`.
//
// Three modes, which are the JDK's three `append*` and are told apart **by what they ask for**, not
// by what they write:
//   - `ZONE` (`appendZoneId`) asks for the declared zone. An `OffsetDateTime` has none, so it does
//     not print.
//   - `ZONE_OR_OFFSET` (`appendZoneOrOffsetId`) accepts the offset standing in for the zone, which
//     is the only thing known about the place when there is no region.
//   - `REGION` (`appendZoneRegionId`) writes only when it really is a region: it is the part behind
//     the `[Europe/Paris]` of `ISO_ZONED_DATE_TIME`, where a `[+02:00]` would be repeated
//     information.
final class ZoneIdPart extends Part {

    static final int ZONE = 0;
    static final int ZONE_OR_OFFSET = 1;
    static final int REGION = 2;

    private final int mode;

    ZoneIdPart(int mode) {
        this.mode = mode;
    }

    boolean print(PrintContext ctx, StringBuilder out) {
        ZoneId z;
        if (this.mode == ZONE_OR_OFFSET) {
            z = ctx.query(TemporalQueries.zone());
        } else {
            z = ctx.query(TemporalQueries.zoneId());
        }
        if (z == null) {
            return ctx.missingOrThrow("ZoneId");
        }
        if (this.mode == REGION && z instanceof ZoneOffset) {
            // A region that is not one. `ISO_ZONED_DATE_TIME` has it inside an optional section
            // precisely for this: `2024-01-01T00:00+02:00` comes out with no `[...]`, and not with
            // `[+02:00]`.
            return ctx.missingOrThrow("ZoneRegionId");
        }
        out.append(z.getId());
        return true;
    }

    // The characters a zone identifier allows. The `+`/`-` are there because `+05:00` is also a
    // legal `ZoneId` identifier.
    private static boolean isIdChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || c == '/' || c == '_' || c == '.' || c == '-' || c == '+' || c == ':' || c == '~';
    }

    int parse(ParseContext ctx, String text, int pos) {
        if (pos >= text.length()) {
            return ~pos;
        }
        char c = text.charAt(pos);
        if (this.mode != REGION && (c == '+' || c == '-')) {
            OffsetPart off = new OffsetPart("+HH:MM:ss", "");
            int r = off.parse(ctx, text, pos);
            if (r < 0) {
                return r;
            }
            ctx.zone = ctx.offset;
            return r;
        }
        if (this.mode != REGION && (c == 'Z' || c == 'z')) {
            // Only when `Z` stands alone: `Zulu` is a named zone and not the zero offset.
            if (pos + 1 >= text.length() || !isIdChar(text.charAt(pos + 1))) {
                ctx.zone = ZoneOffset.UTC;
                ctx.offset = ZoneOffset.UTC;
                ctx.put(ChronoField.OFFSET_SECONDS, 0L);
                return pos + 1;
            }
        }
        int end = pos;
        while (end < text.length() && isIdChar(text.charAt(end))) {
            end = end + 1;
        }
        // From the longest candidate backwards: `Europe/Paris]` has to give `Europe/Paris`, and
        // `America/New_York` cannot stop at `America`.
        Set<String> known = ZoneId.getAvailableZoneIds();
        int length = end;
        while (length > pos) {
            String id = text.substring(pos, length);
            if (known.contains(id)) {
                ctx.zone = ZoneId.of(id);
                return length;
            }
            length = length - 1;
        }
        // `UTC`, `GMT` and `UT` are not in the region list but are legal prefixes, with or without
        // an offset glued to them.
        String[] prefixes = {"UTC", "GMT", "UT"};
        int i = 0;
        while (i < prefixes.length) {
            String pre = prefixes[i];
            if (text.regionMatches(!ctx.caseSensitive, pos, pre, 0, pre.length())) {
                int p = pos + pre.length();
                if (p < text.length() && (text.charAt(p) == '+' || text.charAt(p) == '-')) {
                    OffsetPart off = new OffsetPart("+HH:MM:ss", "");
                    int r = off.parse(ctx, text, p);
                    if (r >= 0) {
                        ctx.zone = ZoneId.of(text.substring(pos, r));
                        return r;
                    }
                }
                ctx.zone = ZoneId.of(pre);
                return p;
            }
            i = i + 1;
        }
        return ~pos;
    }
}

// The calendar's identifier: `ISO`, `ThaiBuddhist`.
//
// It is the **id**, not the name: `appendChronologyText` --which does need CLDR-- is elsewhere.
// This one can be here because the id is not translatable text, it is the key.
final class ChronoIdPart extends Part {

    boolean print(PrintContext ctx, StringBuilder out) {
        Chronology c = ctx.query(TemporalQueries.chronology());
        if (c == null) {
            return false;
        }
        out.append(c.getId());
        return true;
    }

    int parse(ParseContext ctx, String text, int pos) {
        Chronology best = null;
        int bestLength = 0;
        Iterator<Chronology> it = Chronology.getAvailableChronologies().iterator();
        while (it.hasNext()) {
            Chronology c = it.next();
            String id = c.getId();
            if (id.length() > bestLength
                    && text.regionMatches(!ctx.caseSensitive, pos, id, 0, id.length())) {
                best = c;
                bestLength = id.length();
            }
        }
        if (best == null) {
            return ~pos;
        }
        ctx.chronology = best;
        return pos + bestLength;
    }
}

// `appendInstant`: the instant in UTC, always, with `Z` at the end.
//
// **Why it is one part and not a composition of the others.** An instant has no local date or time
// until a zone is put on it; what it has is `INSTANT_SECONDS`. Writing it requires converting to
// UTC first, and --above all-- reading it requires accepting two things no `LocalTime` admits:
// `24:00`, which is the next day's midnight, and `:60`, the leap second. ISO-8601 writes them and
// the JDK reads them; a composition of ordinary parts would reject them while resolving.
//
// `digits`: -2 writes 0, 3, 6 or 9 --whatever is needed, in groups of three, which is what ISO asks
// for; -1 the minimum; 0..9 exactly that many.
final class InstantPart extends Part {

    private static final long SECONDS_PER_DAY = 86400L;

    private final int digits;
    private final Part[] reader;

    InstantPart(int digits) {
        this.digits = digits;
        // The reader is built once. The hours and the seconds go with `max` 2 but **without** a
        // range cap: the range is checked afterwards, once it is known whether `24` and `60` are
        // the special cases or an error.
        Part[] p = new Part[11];
        p[0] = new NumberPart(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD);
        p[1] = new LiteralPart("-");
        p[2] = new NumberPart(ChronoField.MONTH_OF_YEAR, 2, 2, SignStyle.NOT_NEGATIVE);
        p[3] = new LiteralPart("-");
        p[4] = new NumberPart(ChronoField.DAY_OF_MONTH, 2, 2, SignStyle.NOT_NEGATIVE);
        p[5] = new LiteralPart("T");
        p[6] = new NumberPart(ChronoField.HOUR_OF_DAY, 2, 2, SignStyle.NOT_NEGATIVE);
        p[7] = new LiteralPart(":");
        p[8] = new NumberPart(ChronoField.MINUTE_OF_HOUR, 2, 2, SignStyle.NOT_NEGATIVE);
        p[9] = new CompositePart(new Part[] {
            new LiteralPart(":"),
            new NumberPart(ChronoField.SECOND_OF_MINUTE, 2, 2, SignStyle.NOT_NEGATIVE),
            new CompositePart(new Part[] {
                new FractionPart(ChronoField.NANO_OF_SECOND, 0, 9, true),
            }, true),
        }, true);
        p[10] = new OffsetPart("+HH:MM:ss", "Z");
        this.reader = p;
    }

    boolean print(PrintContext ctx, StringBuilder out) {
        Long seconds = ctx.value(ChronoField.INSTANT_SECONDS);
        if (seconds == null) {
            return false;
        }
        long nanos = 0L;
        if (ctx.temporal().isSupported(ChronoField.NANO_OF_SECOND)) {
            nanos = ctx.temporal().getLong(ChronoField.NANO_OF_SECOND);
        }
        LocalDateTime ldt = LocalDateTime.ofEpochSecond(seconds.longValue(), 0, ZoneOffset.UTC);
        StringBuilder tmp = new StringBuilder();
        long year = (long) ldt.getYear();
        String d = Long.toString(year < 0L ? -year : year);
        if (year > 9999L) {
            tmp.append('+');
        } else if (year < 0L) {
            tmp.append('-');
        }
        Part.writeDigits(ctx, tmp, d, 4);
        tmp.append('-');
        Part.writeDigits(ctx, tmp, Long.toString((long) ldt.getMonthValue()), 2);
        tmp.append('-');
        Part.writeDigits(ctx, tmp, Long.toString((long) ldt.getDayOfMonth()), 2);
        tmp.append('T');
        Part.writeDigits(ctx, tmp, Long.toString((long) ldt.getHour()), 2);
        tmp.append(':');
        Part.writeDigits(ctx, tmp, Long.toString((long) ldt.getMinute()), 2);
        tmp.append(':');
        Part.writeDigits(ctx, tmp, Long.toString((long) ldt.getSecond()), 2);
        this.fraction(ctx, tmp, nanos);
        tmp.append('Z');
        out.append(tmp.toString());
        return true;
    }

    private void fraction(PrintContext ctx, StringBuilder out, long nanos) {
        if (this.digits == 0) {
            return;
        }
        String nine = Long.toString(nanos);
        while (nine.length() < 9) {
            nine = "0" + nine;
        }
        int howMany;
        if (this.digits > 0) {
            howMany = this.digits;
        } else if (this.digits == -1) {
            howMany = 9;
            while (howMany > 0 && nine.charAt(howMany - 1) == '0') {
                howMany = howMany - 1;
            }
        } else {
            // -2: in groups of three, which is how ISO-8601 writes millis, micros and nanos.
            if (nanos == 0L) {
                howMany = 0;
            } else if (nanos % 1000000L == 0L) {
                howMany = 3;
            } else if (nanos % 1000L == 0L) {
                howMany = 6;
            } else {
                howMany = 9;
            }
        }
        if (howMany == 0) {
            return;
        }
        out.append(ctx.symbols.getDecimalSeparator());
        Part.writeDigits(ctx, out, nine.substring(0, howMany), 0);
    }

    int parse(ParseContext ctx, String text, int pos) {
        ParseContext inner = new ParseContext(ctx.locale, ctx.symbols, false, null, null);
        inner.caseSensitive = ctx.caseSensitive;
        int p = pos;
        int i = 0;
        while (i < this.reader.length) {
            p = this.reader[i].parse(inner, text, p);
            if (p < 0) {
                return ~pos;
            }
            i = i + 1;
        }
        Long year = inner.fields.get(ChronoField.YEAR);
        Long month = inner.fields.get(ChronoField.MONTH_OF_YEAR);
        Long day = inner.fields.get(ChronoField.DAY_OF_MONTH);
        Long hour = inner.fields.get(ChronoField.HOUR_OF_DAY);
        Long minute = inner.fields.get(ChronoField.MINUTE_OF_HOUR);
        Long second = inner.fields.get(ChronoField.SECOND_OF_MINUTE);
        Long nano = inner.fields.get(ChronoField.NANO_OF_SECOND);
        Long off = inner.fields.get(ChronoField.OFFSET_SECONDS);
        if (year == null || month == null || day == null || hour == null || minute == null
                || off == null) {
            return ~pos;
        }
        long h = hour.longValue();
        long s = second == null ? 0L : second.longValue();
        // The two no local time admits. They are normalized **here** --24:00 becomes 00:00 of the
        // next day, :60 becomes :59-- and what was done is written down, because the resulting
        // instant is the right one and whoever calls `parsedLeapSecond` deserves to know the text
        // said 60.
        int extraDay = 0;
        boolean leap = false;
        if (h == 24L && minute.longValue() == 0L && s == 0L && (nano == null || nano.longValue() == 0L)) {
            h = 0L;
            extraDay = 1;
        } else if (h > 23L) {
            return ~pos;
        }
        if (s == 60L) {
            s = 59L;
            leap = true;
        } else if (s > 59L) {
            return ~pos;
        }
        LocalDate date;
        try {
            date = LocalDate.of((int) year.longValue(), (int) month.longValue(),
                    (int) day.longValue());
        } catch (java.time.DateTimeException e) {
            return ~pos;
        }
        long epochDay = date.toEpochDay() + (long) extraDay;
        long secondsOfDay = h * 3600L + minute.longValue() * 60L + s;
        long instant = epochDay * SECONDS_PER_DAY + secondsOfDay - off.longValue();
        ctx.put(ChronoField.INSTANT_SECONDS, instant);
        ctx.put(ChronoField.NANO_OF_SECOND, nano == null ? 0L : nano.longValue());
        ctx.put(ChronoField.OFFSET_SECONDS, off.longValue());
        ctx.offset = ZoneOffset.ofTotalSeconds((int) off.longValue());
        if (leap) {
            ctx.leapSecond = true;
        }
        return p;
    }
}
