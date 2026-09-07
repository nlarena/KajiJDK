package java.time.format;

import java.time.DateTimeException;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.temporal.TemporalField;
import java.time.temporal.ValueRange;

// A field written with digits: the `2024` of a year, the `02` of a month.
//
// `min` is how many digits are written at least --the zero padding-- and `max` how many are allowed
// at most. While reading, `max` is what stops it: `appendValue(YEAR, 4, 10, EXCEEDS_PAD)` followed
// by `'-'` reads `2024` and stops at the dash because the dash is not a digit, but
// `appendValue(YEAR, 4)` glued to `appendValue(MONTH_OF_YEAR, 2)` --the `BASIC_ISO_DATE`-- only
// works because the first one plants itself at four.
//
// **What is missing: the JDK's "adjacent value parsing".** When two *variable* width fields are
// glued together with no separator, the JDK enters a special mode in which the first yields digits
// to the second. Here reading is greedy and stops at `max`, so two glued variable fields read
// wrongly. It is not faked: `DateTimeFormatterBuilder` has no way of expressing that case without a
// separator, and all the predefined formatters use fixed widths or separators. It is written down
// as a divergence.
class NumberPart extends Part {

    final TemporalField field;
    final int min;
    final int max;
    final SignStyle sign;

    NumberPart(TemporalField field, int min, int max, SignStyle sign) {
        this.field = field;
        this.min = min;
        this.max = max;
        this.sign = sign;
    }

    // The value that actually gets written. `ReducedNumberPart` overrides it to cut the century.
    long valueToWrite(PrintContext ctx, long value) {
        return value;
    }

    boolean print(PrintContext ctx, StringBuilder out) {
        Long v = ctx.value(this.field);
        if (v == null) {
            return false;
        }
        long value = this.valueToWrite(ctx, v.longValue());
        String digits = Long.toString(value);
        boolean negative = digits.startsWith("-");
        if (negative) {
            digits = digits.substring(1);
        }
        if (digits.length() > this.max) {
            throw new DateTimeException("Field " + this.field + " cannot be printed as the value "
                    + value + " exceeds the maximum print width of " + this.max);
        }
        DecimalStyle s = ctx.symbols;
        if (negative) {
            if (this.sign == SignStyle.NOT_NEGATIVE) {
                throw new DateTimeException("Field " + this.field
                        + " cannot be printed as the value " + value
                        + " cannot be negative according to the SignStyle");
            }
            if (this.sign != SignStyle.NEVER) {
                out.append(s.getNegativeSign());
            }
        } else {
            if (this.sign == SignStyle.ALWAYS
                    || (this.sign == SignStyle.EXCEEDS_PAD && digits.length() > this.min)) {
                out.append(s.getPositiveSign());
            }
        }
        Part.writeDigits(ctx, out, digits, this.min);
        return true;
    }

    int parse(ParseContext ctx, String text, int pos) {
        int p = pos;
        boolean negative = false;
        boolean signPresent = false;
        if (p < text.length()) {
            char c = text.charAt(p);
            if (c == ctx.symbols.getPositiveSign()) {
                if (this.sign == SignStyle.NEVER || this.sign == SignStyle.NOT_NEGATIVE) {
                    return ~pos;
                }
                signPresent = true;
                p = p + 1;
            } else if (c == ctx.symbols.getNegativeSign()) {
                if (this.sign == SignStyle.NEVER) {
                    return ~pos;
                }
                negative = true;
                signPresent = true;
                p = p + 1;
            }
        }
        if (ctx.strict && this.sign == SignStyle.ALWAYS && !signPresent) {
            return ~pos;
        }
        // With a sign written, the minimum width no longer counts the padding: `+2024` brings four
        // digits all the same, but a `-1` with `min` 4 would be a text no `print` generates, and in
        // lenient mode it is accepted anyway. The maximum is always honoured: it is the only thing
        // that stops the greedy read.
        int from = p;
        long value = 0L;
        while (p < text.length() && p - from < this.max) {
            int d = Part.digit(ctx, text.charAt(p));
            if (d < 0) {
                break;
            }
            value = value * 10L + (long) d;
            p = p + 1;
        }
        int read = p - from;
        if (read == 0) {
            return ~pos;
        }
        if (ctx.strict && read < this.min) {
            return ~pos;
        }
        this.deposit(ctx, negative ? -value : value, read);
        return p;
    }

    void deposit(ParseContext ctx, long value, int read) {
        ctx.put(this.field, value);
    }
}

// `appendValueReduced`: the two-digit year, and its generalization.
//
// It writes **only the last `min` digits** while the value falls in the window starting at `base`
// and lasting `10^min`; outside the window it writes the whole number. While reading, a text of
// exactly `min` digits is completed towards the window, and a longer one is taken as it stands.
//
// The asymmetry is on purpose and is the JDK's: `withYear(2024)` with base 2000 writes `24`, but
// `withYear(1875)` writes `1875` instead of `75`, because `75` would be read back as 2075. A
// formatter that writes something it itself reads back wrongly is worse than one that writes too
// much.
final class ReducedNumberPart extends NumberPart {

    private final int base;
    private final ChronoLocalDate baseDate;

    ReducedNumberPart(TemporalField field, int min, int max, int base,
            ChronoLocalDate baseDate) {
        super(field, min, max, SignStyle.NOT_NEGATIVE);
        this.base = base;
        this.baseDate = baseDate;
    }

    private static long powerOf10(int n) {
        long r = 1L;
        int i = 0;
        while (i < n) {
            r = r * 10L;
            i = i + 1;
        }
        return r;
    }

    // The effective base. With a base date, the value is read back **in whichever chronology is in
    // play**: the "year 2000" of an ISO date is not the same number in the Japanese calendar, and
    // taking the ISO number would give a window a couple of millennia off.
    private int base(Chronology chronology) {
        if (this.baseDate == null) {
            return this.base;
        }
        if (chronology == null) {
            return (int) this.baseDate.getLong(this.field);
        }
        return (int) chronology.date(this.baseDate).getLong(this.field);
    }

    long valueToWrite(PrintContext ctx, long value) {
        int b = this.base(ctx.query(java.time.temporal.TemporalQueries.chronology()));
        long window = powerOf10(this.min);
        long abs = value < 0L ? -value : value;
        if (value >= (long) b && value < (long) b + window) {
            return abs % window;
        }
        // Outside the window what gets written is **whatever fits in `max`**, not the whole number:
        // with `max == min` the year is cut all the same (1875 comes out `75`), and with a larger
        // `max` it comes out complete (`1875`). The cut is not a silent loss: `max` is precisely
        // the promise of how many digits there will be at most.
        return abs % powerOf10(this.max);
    }

    void deposit(ParseContext ctx, long value, int read) {
        long v = value;
        if (read == this.min && v >= 0L) {
            int b = this.base(ctx.chronology);
            long window = powerOf10(this.min);
            long remainder = (long) b % window;
            long whole = (long) b - remainder;
            if (b > 0) {
                v = whole + v;
            } else {
                v = whole - v;
            }
            if (v < (long) b) {
                v = v + window;
            }
        }
        ctx.put(this.field, v);
    }
}

// `appendFraction`: a field's fractional part, typically the nanos.
//
// **It does not write the number, it writes the fraction.** `NANO_OF_SECOND` is worth 400000000 and
// the text is `.4`, not `.400000000`: the value is divided by the size of its range and emitted in
// base ten, trimming the zeros on the right down to `min` digits. It is the only way for `.5` and
// `.500` to mean the same thing, which is what ISO-8601 asks for.
final class FractionPart extends Part {

    private final TemporalField field;
    private final int min;
    private final int max;
    private final boolean point;

    FractionPart(TemporalField field, int min, int max, boolean point) {
        this.field = field;
        this.min = min;
        this.max = max;
        this.point = point;
    }

    private static long powerOf10(int n) {
        long r = 1L;
        int i = 0;
        while (i < n) {
            r = r * 10L;
            i = i + 1;
        }
        return r;
    }

    boolean print(PrintContext ctx, StringBuilder out) {
        Long v = ctx.value(this.field);
        if (v == null) {
            return false;
        }
        ValueRange range = this.field.range();
        long minimum = range.getMinimum();
        long size = range.getMaximum() - minimum + 1L;
        long remainder = v.longValue() - minimum;
        StringBuilder digits = new StringBuilder();
        int i = 0;
        while (i < this.max) {
            remainder = remainder * 10L;
            digits.append((char) ('0' + (int) (remainder / size)));
            remainder = remainder % size;
            i = i + 1;
        }
        String d = digits.toString();
        int end = d.length();
        while (end > this.min && end > 0 && d.charAt(end - 1) == '0') {
            end = end - 1;
        }
        d = d.substring(0, end);
        if (d.length() == 0) {
            // With `min` zero and a value of zero there is no fraction to write, and **no point
            // either**: `10:15:30` and not `10:15:30.`.
            return true;
        }
        if (this.point) {
            out.append(ctx.symbols.getDecimalSeparator());
        }
        Part.writeDigits(ctx, out, d, 0);
        return true;
    }

    int parse(ParseContext ctx, String text, int pos) {
        int p = pos;
        boolean pointPresent = false;
        if (this.point) {
            if (p < text.length() && text.charAt(p) == ctx.symbols.getDecimalSeparator()) {
                pointPresent = true;
                p = p + 1;
            } else if (this.min > 0) {
                return ~pos;
            } else {
                return pos;
            }
        }
        int from = p;
        long accumulated = 0L;
        while (p < text.length() && p - from < this.max) {
            int d = Part.digit(ctx, text.charAt(p));
            if (d < 0) {
                break;
            }
            accumulated = accumulated * 10L + (long) d;
            p = p + 1;
        }
        int read = p - from;
        if (read < this.min || (pointPresent && read == 0)) {
            return ~pos;
        }
        if (read == 0) {
            return pos;
        }
        ValueRange range = this.field.range();
        long minimum = range.getMinimum();
        long size = range.getMaximum() - minimum + 1L;
        // `accumulated / 10^read` is the fraction; multiplying it by the range's size brings it
        // back to the field's scale. The multiplication comes **before** the division so as not to
        // lose the low digits: 4/10 * 1000000000 in integers gives 0 if divided first.
        long value = minimum + accumulated * size / powerOf10(read);
        ctx.put(this.field, value);
        return p;
    }
}
