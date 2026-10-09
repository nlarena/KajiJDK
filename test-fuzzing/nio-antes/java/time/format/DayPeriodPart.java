package java.time.format;

import java.time.temporal.ChronoField;

// The period of the day written in words: "in the morning", "nachmittags", "madrugada".
//
// ===============================================================================================
// HOW IT DIFFERS FROM AM/PM
// ===============================================================================================
//
// In that it does not split the day in two but into the pieces that language names, which are
// neither the same nor even: German has seven, Spanish five, English six. And some last one minute
// --`noon` is exactly twelve-- while the one next door lasts six hours.
//
// That is the reason it exists: `2 PM` is a literal translation that in half the languages is not
// said, and "a las dos de la tarde" is. The data is in `DayPeriods`, extracted from the JDK.
//
// **While parsing it resolves to the midpoint of the stretch.** "In the morning" does not say what
// time it is, so the JDK takes the middle of its range --minute 360 for a stretch from 1 to 720--
// and that is what ends up set. It is a convention, not a deduction, and it is the JDK's.
final class DayPeriodPart extends Part {

    private final TextStyle style;

    DayPeriodPart(TextStyle style) {
        this.style = style;
    }

    boolean print(PrintContext ctx, StringBuilder out) {
        Long value = ctx.value(ChronoField.MINUTE_OF_DAY);
        if (value == null) {
            return ctx.missingOrThrow("DayPeriod");
        }
        out.append(DayPeriods.name(ctx.locale, this.style, (int) value.longValue()));
        return true;
    }

    int parse(ParseContext ctx, String text, int pos) {
        String[] names = DayPeriods.names(ctx.locale, this.style);
        // From the longest to the shortest: in German `nachmittags` starts with `nachts` badly cut,
        // and settling for the first that fits would give the wrong period.
        int best = -1;
        int bestLength = -1;
        for (int i = 0; i < names.length; i++) {
            String n = names[i];
            if (n.length() > bestLength
                    && text.regionMatches(!ctx.caseSensitive, pos, n, 0, n.length())) {
                best = i;
                bestLength = n.length();
            }
        }
        if (best < 0) {
            return ~pos;
        }
        int middle = DayPeriods.middle(ctx.locale, this.style, best);
        ctx.put(ChronoField.MINUTE_OF_DAY, (long) middle);
        return pos + bestLength;
    }

    @Override
    public String toString() {
        return "DayPeriod(" + this.style + ")";
    }
}
