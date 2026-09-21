package java.text;

import java.math.BigDecimal;

/**
 * It evaluates the CLDR's plural rules, the ones that decide whether a number takes "1 file" or
 * "2 files".
 *
 * <p>{@link CompactNumberFormat} uses it when a compact pattern brings variants by category. It is
 * not public: in the JDK this logic is internal too, and exposing it would mean deciding on an API
 * the standard does not define.
 *
 * <p><b>Why an evaluator is needed and not a table.</b> The categories are not deducible from the
 * number: Russian puts "few" from 2 to 4 except from 12 to 14, Polish distinguishes by the remainder
 * of the ten AND of the hundred, and French counts 0 as singular. The rule is an expression, and the
 * CLDR publishes it as text -- so the honest thing is to evaluate it, not to guess three cases.
 *
 * <p>The syntax supported, which is the standard's:
 * {@code category ':' condition} separated by {@code ';'}, with
 * {@code condition = and ('or' and)*}, {@code and = relation ('and' relation)*},
 * {@code relation = operand ['%' n] ('='|'!=') range (',' range)*} and
 * {@code range = n | n '..' m}. The operands are the CLDR's six: {@code n} (absolute value),
 * {@code i} (integer part), {@code v} and {@code w} (how many decimals, with and without trailing
 * zeros), {@code f} and {@code t} (the decimals as an integer, with and without trailing zeros). The
 * samples ({@code @integer}, {@code @decimal}) are ignored, which is what they deserve: they are
 * documentation.
 */
final class PluralRules {

    private PluralRules() {
    }

    /**
     * The category the value falls into, or {@code "other"} if no rule matches.
     *
     * <p>{@code "other"} is not a filler value: the CLDR guarantees every locale has it and that it
     * is the one that applies when there is no more specific rule.
     */
    static String category(String rules, BigDecimal value) {
        if (rules == null || rules.length() == 0) {
            return "other";
        }
        Operands op = new Operands(value);
        int i = 0;
        while (i < rules.length()) {
            int end = rules.indexOf(';', i);
            if (end < 0) {
                end = rules.length();
            }
            String rule = rules.substring(i, end);
            int colon = rule.indexOf(':');
            if (colon > 0) {
                String cat = rule.substring(0, colon).trim();
                String cond = rule.substring(colon + 1, rule.length());
                int sample = cond.indexOf('@');
                if (sample >= 0) {
                    cond = cond.substring(0, sample);
                }
                if (PluralRules.condition(cond.trim(), op)) {
                    return cat;
                }
            }
            i = end + 1;
        }
        return "other";
    }

    private static boolean condition(String s, Operands op) {
        if (s.length() == 0) {
            return true;
        }
        String[] parts = PluralRules.split(s, " or ");
        for (int i = 0; i < parts.length; i = i + 1) {
            if (PluralRules.conjunction(parts[i], op)) {
                return true;
            }
        }
        return false;
    }

    private static boolean conjunction(String s, Operands op) {
        String[] parts = PluralRules.split(s, " and ");
        for (int i = 0; i < parts.length; i = i + 1) {
            if (!PluralRules.relationOf(parts[i].trim(), op)) {
                return false;
            }
        }
        return true;
    }

    private static boolean relationOf(String s, Operands op) {
        boolean negated = false;
        int cut = s.indexOf("!=");
        int opLength = 2;
        if (cut >= 0) {
            negated = true;
        } else {
            cut = s.indexOf('=');
            opLength = 1;
            if (cut < 0) {
                return false;
            }
        }
        String toTheLeft = s.substring(0, cut).trim();
        String toTheRight = s.substring(cut + opLength, s.length()).trim();

        long value;
        int mod = toTheLeft.indexOf('%');
        if (mod >= 0) {
            long base = op.value(toTheLeft.substring(0, mod).trim());
            long m = PluralRules.integerPart(toTheLeft.substring(mod + 1, toTheLeft.length()).trim());
            if (m == 0) {
                return false;
            }
            value = base % m;
        } else {
            value = op.value(toTheLeft);
        }

        boolean inside = false;
        String[] ranges = PluralRules.split(toTheRight, ",");
        for (int i = 0; i < ranges.length; i = i + 1) {
            String r = ranges[i].trim();
            int dots = r.indexOf("..");
            if (dots >= 0) {
                long a = PluralRules.integerPart(r.substring(0, dots).trim());
                long b = PluralRules.integerPart(r.substring(dots + 2, r.length()).trim());
                if (value >= a && value <= b) {
                    inside = true;
                }
            } else if (value == PluralRules.integerPart(r)) {
                inside = true;
            }
        }
        if (negated) {
            return !inside;
        }
        return inside;
    }

    private static long integerPart(String s) {
        if (s.length() == 0) {
            return Long.MIN_VALUE;
        }
        long v = 0;
        for (int i = 0; i < s.length(); i = i + 1) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return Long.MIN_VALUE;
            }
            v = v * 10 + (c - '0');
        }
        return v;
    }

    // A literal split on a delimiter; there is no nesting in this grammar, so no tokeniser is
    // needed.
    private static String[] split(String s, String delim) {
        int n = 1;
        int i = s.indexOf(delim);
        while (i >= 0) {
            n = n + 1;
            i = s.indexOf(delim, i + delim.length());
        }
        String[] out = new String[n];
        int k = 0;
        int from = 0;
        i = s.indexOf(delim);
        while (i >= 0) {
            out[k] = s.substring(from, i);
            k = k + 1;
            from = i + delim.length();
            i = s.indexOf(delim, from);
        }
        out[k] = s.substring(from, s.length());
        return out;
    }

    /** The six operands the CLDR defines over a number, computed once. */
    private static final class Operands {

        private final long n;
        private final long i;
        private final long v;
        private final long w;
        private final long f;
        private final long t;

        Operands(BigDecimal value) {
            BigDecimal abs = value.abs();
            this.i = abs.setScale(0, java.math.RoundingMode.DOWN).longValue();
            this.n = this.i;
            int scale = abs.scale();
            if (scale < 0) {
                scale = 0;
            }
            this.v = scale;
            BigDecimal fraction = abs.subtract(new BigDecimal(this.i));
            this.f = fraction.movePointRight(scale).setScale(0, java.math.RoundingMode.DOWN)
                    .longValue();
            BigDecimal withoutZeros = fraction.stripTrailingZeros();
            int scale2 = withoutZeros.scale();
            if (scale2 < 0) {
                scale2 = 0;
            }
            this.w = scale2;
            this.t = withoutZeros.movePointRight(scale2).setScale(0, java.math.RoundingMode.DOWN)
                    .longValue();
        }

        long value(String operand) {
            if (operand.equals("n")) {
                return this.n;
            }
            if (operand.equals("i")) {
                return this.i;
            }
            if (operand.equals("v")) {
                return this.v;
            }
            if (operand.equals("w")) {
                return this.w;
            }
            if (operand.equals("f")) {
                return this.f;
            }
            if (operand.equals("t")) {
                return this.t;
            }
            return Long.MIN_VALUE;
        }
    }
}
