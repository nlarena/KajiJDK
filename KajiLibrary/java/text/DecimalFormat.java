package java.text;

import java.math.BigDecimal;
import java.math.RoundingMode;

// KajiLibrary's java.text.DecimalFormat — pattern-based number formatting. A pattern like
// `#,##0.00` is parsed into a prefix/suffix, a minimum integer-digit count, grouping, and a
// min/max fraction-digit count; format(double) then renders a number to match.
//
// The PATTERN and the SYMBOLS are separate concerns, and keeping them apart is the design:
// the pattern says the structure and is always written in the standard alphabet ('#', '0',
// ',', '.'), while DecimalFormatSymbols says which characters draw it. So the same
// `#,##0.00` renders 1,234.50 with US symbols and 1.234,50 with German ones, and applyPattern
// never has to know which locale it is in. (The JDK's applyLocalizedPattern, which reads the
// pattern itself in localized characters, is not implemented here.)
//
// Subset: `%` suffix (x100) and an optional negative subpattern after ';'. Parsing (parse()),
// currency (¤) and scientific notation are future work.
//
// Rounding goes through java.math.BigDecimal, not through double arithmetic. That matters twice:
//   - a value on an exact rounding boundary now agrees with the JDK. format(0.005) with "0.00"
//     gives "0.01", because BigDecimal.valueOf(double) goes through the double's SHORTEST decimal
//     representation ("0.005"), which is the same thing the JDK's formatter rounds;
//   - magnitudes past ~9.2e18 work. The previous version computed the integer part with
//     `(long) magnitude`, which SATURATES at Long.MAX_VALUE and then produced structurally corrupt
//     output — "9,223,372,036,854,775,807.-9223372036854775808%" for a large long under a percent
//     pattern. Digits now come from BigDecimal.toPlainString(), which has no such ceiling.
public class DecimalFormat extends NumberFormat {

    private String pattern;
    private String posPrefix;
    private String posSuffix;
    private String negPrefix;
    private String negSuffix;
    private int minInt;
    private int minFrac;
    private int maxFrac;
    private boolean grouping;
    private int multiplier;
    private DecimalFormatSymbols symbols;

    public DecimalFormat() {
        this.symbols = new DecimalFormatSymbols();
        this.applyPattern("#,##0.###");
    }

    public DecimalFormat(String pattern) {
        this.symbols = new DecimalFormatSymbols();
        this.applyPattern(pattern);
    }

    // The symbols are assigned first: applyPattern derives the default negative prefix from the
    // minus sign, so a pattern applied before them would bake in the wrong one.
    public DecimalFormat(String pattern, DecimalFormatSymbols symbols) {
        this.symbols = (DecimalFormatSymbols) symbols.clone();
        this.applyPattern(pattern);
    }

    public DecimalFormatSymbols getDecimalFormatSymbols() {
        return (DecimalFormatSymbols) this.symbols.clone();
    }

    // Copied on the way in and out, so a caller mutating its own instance cannot reach inside a
    // live formatter. Re-applies the pattern because the default negative prefix depends on it.
    public void setDecimalFormatSymbols(DecimalFormatSymbols newSymbols) {
        this.symbols = (DecimalFormatSymbols) newSymbols.clone();
        this.applyPattern(this.pattern);
    }

    public void applyPattern(String pattern) {
        this.pattern = pattern;
        this.posPrefix = "";
        this.posSuffix = "";
        this.negPrefix = String.valueOf(this.symbols.getMinusSign());
        this.negSuffix = "";
        this.minInt = 1;
        this.minFrac = 0;
        this.maxFrac = 0;
        this.grouping = false;
        this.multiplier = 1;

        int semi = idx(pattern, ';');
        String pos;
        if (semi < 0) {
            pos = pattern;
        } else {
            pos = pattern.substring(0, semi);
        }
        this.parseSub(pos);
        if (semi >= 0) {
            String neg = pattern.substring(semi + 1, pattern.length());
            int ns = firstDigitPat(neg);
            int ne = numEnd(neg, ns);
            this.negPrefix = neg.substring(0, ns);
            this.negSuffix = neg.substring(ne, neg.length());
        } else {
            this.negPrefix = this.symbols.getMinusSign() + this.posPrefix;
            this.negSuffix = this.posSuffix;
        }
    }

    public String toPattern() {
        return this.pattern;
    }

    private void parseSub(String s) {
        int ns = firstDigitPat(s);
        int ne = numEnd(s, ns);
        this.posPrefix = s.substring(0, ns);
        this.posSuffix = s.substring(ne, s.length());
        if (idx(this.posSuffix, '%') >= 0) {
            this.multiplier = 100;
        }
        String num = s.substring(ns, ne);
        int dot = idx(num, '.');
        String ip;
        String fp;
        if (dot < 0) {
            ip = num;
            fp = "";
        } else {
            ip = num.substring(0, dot);
            fp = num.substring(dot + 1, num.length());
        }
        this.grouping = idx(ip, ',') >= 0;
        this.minInt = 0;
        for (int i = 0; i < ip.length(); i = i + 1) {
            if (ip.charAt(i) == '0') {
                this.minInt = this.minInt + 1;
            }
        }
        this.minFrac = 0;
        this.maxFrac = 0;
        for (int i = 0; i < fp.length(); i = i + 1) {
            char c = fp.charAt(i);
            if (c == '0') {
                this.minFrac = this.minFrac + 1;
                this.maxFrac = this.maxFrac + 1;
            } else if (c == '#') {
                this.maxFrac = this.maxFrac + 1;
            }
        }
    }

    // ---- the two seams NumberFormat declares ----

    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
        // new BigDecimal(double), NOT valueOf: rounding has to see the double's EXACT binary
        // value. 2.675 is really 2.674999999999999822..., so rounding it to two places gives 2.67 —
        // and going through the shortest decimal form ("2.675") would give 2.68 instead. The JDK
        // rounds on the exact value, and this is what makes us agree with it.
        toAppendTo.append(this.formatDecimal(new BigDecimal(number)));
        return toAppendTo;
    }

    // A long is exact all the way through — no double ever appears, so values past 2^53 keep
    // every digit. That is the whole reason NumberFormat declares a separate long seam.
    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
        toAppendTo.append(this.formatDecimal(BigDecimal.valueOf(number)));
        return toAppendTo;
    }

    private String formatDecimal(BigDecimal value) {
        BigDecimal scaled = value;
        if (this.multiplier != 1) {
            scaled = scaled.multiply(BigDecimal.valueOf((long) this.multiplier));
        }
        boolean neg = scaled.signum() < 0;
        BigDecimal rounded = scaled.abs().setScale(this.maxFrac, RoundingMode.valueOf("HALF_EVEN"));
        // At scale maxFrac, toPlainString() is exactly "<integer digits>.<maxFrac digits>" — or
        // just the integer digits when maxFrac is 0. So splitting on the point IS the digit split.
        String plain = rounded.toPlainString();
        int dot = idx(plain, '.');
        String intDigits;
        String fracDigits;
        if (dot < 0) {
            intDigits = plain;
            fracDigits = "";
        } else {
            intDigits = plain.substring(0, dot);
            fracDigits = plain.substring(dot + 1, plain.length());
        }
        return this.render(neg, intDigits, fracDigits);
    }

    // Digits -> text: pad the integer part to minInt, group it, then trim the fraction back from
    // maxFrac to minFrac, and wrap in the sign's prefix/suffix. Both seams share it, so a long and
    // a double with the same digits render identically.
    private String render(boolean neg, String intDigits, String fracDigits) {
        String is = intDigits;
        while (is.length() < this.minInt) {
            is = "0" + is;
        }
        if (this.grouping) {
            is = this.group(is);
        }
        String fs;
        if (this.maxFrac == 0) {
            fs = "";
        } else {
            fs = fracDigits;
            while (fs.length() < this.maxFrac) {
                fs = fs + "0";
            }
            int end = fs.length();
            while (end > this.minFrac && fs.charAt(end - 1) == '0') {
                end = end - 1;
            }
            fs = fs.substring(0, end);
        }
        String body;
        if (fs.length() == 0) {
            body = is;
        } else {
            body = is + this.symbols.getDecimalSeparator() + fs;
        }
        if (neg) {
            return this.negPrefix + body + this.negSuffix;
        }
        return this.posPrefix + body + this.posSuffix;
    }

    private static int idx(String s, char c) {
        for (int i = 0; i < s.length(); i = i + 1) {
            if (s.charAt(i) == c) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isNumChar(char c) {
        return c == '#' || c == '0' || c == ',' || c == '.';
    }

    private static int firstDigitPat(String s) {
        for (int i = 0; i < s.length(); i = i + 1) {
            char c = s.charAt(i);
            if (c == '#' || c == '0') {
                return i;
            }
        }
        return 0;
    }

    private static int numEnd(String s, int start) {
        int i = start;
        while (i < s.length() && isNumChar(s.charAt(i))) {
            i = i + 1;
        }
        return i;
    }

    private String group(String d) {
        StringBuilder sb = new StringBuilder();
        int n = d.length();
        for (int i = 0; i < n; i = i + 1) {
            if (i > 0 && (n - i) % 3 == 0) {
                sb.append(this.symbols.getGroupingSeparator());
            }
            sb.append(d.charAt(i));
        }
        return sb.toString();
    }

}
