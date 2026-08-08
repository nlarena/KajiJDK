package java.text;

// KajiLibrary's java.text.DecimalFormat — pattern-based number formatting. A pattern like
// `#,##0.00` is parsed into a prefix/suffix, a minimum integer-digit count, grouping, and a
// min/max fraction-digit count; format(double) then renders a number to match. Subset:
// US/ROOT symbols (',' grouping, '.' decimal), `%` suffix (x100), and an optional negative
// subpattern after ';'. Parsing (parse()), currency (¤), scientific notation and non-US
// symbols are future work.
//
// Rounding is HALF_EVEN on the nearest double (the JDK's default), which can differ from the
// JDK by one in the last place for a value that sits exactly on a rounding boundary in binary
// (e.g. format(0.005) with "0.00" gives "0.00" here vs "0.01" in the JDK, since the exact
// binary value of 0.005 is a hair above the half). Exact-decimal rounding needs bignum.
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

    public DecimalFormat() {
        this.applyPattern("#,##0.###");
    }

    public DecimalFormat(String pattern) {
        this.applyPattern(pattern);
    }

    public void applyPattern(String pattern) {
        this.pattern = pattern;
        this.posPrefix = "";
        this.posSuffix = "";
        this.negPrefix = "-";
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
            this.negPrefix = "-" + this.posPrefix;
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

    String formatImpl(double number) {
        double v = number * this.multiplier;
        boolean neg = v < 0;
        double mag;
        if (neg) {
            mag = -v;
        } else {
            mag = v;
        }
        long ipart = (long) mag;
        double f = mag - (double) ipart;
        long fpow = 1;
        for (int i = 0; i < this.maxFrac; i = i + 1) {
            fpow = fpow * 10;
        }
        long fr = roundHalfEven(f * fpow);
        if (fr >= fpow) {
            fr = fr - fpow;
            ipart = ipart + 1;
        }
        String is = Long.toString(ipart);
        while (is.length() < this.minInt) {
            is = "0" + is;
        }
        if (this.grouping) {
            is = group(is);
        }
        String fs;
        if (this.maxFrac == 0) {
            fs = "";
        } else {
            fs = Long.toString(fr);
            while (fs.length() < this.maxFrac) {
                fs = "0" + fs;
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
            body = is + "." + fs;
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

    private static String group(String d) {
        StringBuilder sb = new StringBuilder();
        int n = d.length();
        for (int i = 0; i < n; i = i + 1) {
            if (i > 0 && (n - i) % 3 == 0) {
                sb.append(',');
            }
            sb.append(d.charAt(i));
        }
        return sb.toString();
    }

    private static long roundHalfEven(double x) {
        long fl = (long) x;
        double frac = x - (double) fl;
        if (frac > 0.5) {
            return fl + 1;
        }
        if (frac < 0.5) {
            return fl;
        }
        if ((fl & 1L) == 0) {
            return fl;
        }
        return fl + 1;
    }
}
