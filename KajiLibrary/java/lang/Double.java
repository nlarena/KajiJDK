package java.lang;

// KajiLibrary's java.lang.Double — the boxed-double wrapper (extends Number, implements
// Comparable). All numeric views except doubleValue are narrowing, so they need a cast.
public final class Double extends Number implements Comparable<Double> {

    private final double value;

    public Double(double value) {
        this.value = value;
    }

    public static Double valueOf(double d) {
        return new Double(d);
    }

    public int intValue() {
        return (int) value;
    }

    public long longValue() {
        return (long) value;
    }

    public float floatValue() {
        return (float) value;
    }

    public double doubleValue() {
        return value;
    }

    public int compareTo(Double o) {
        return this.value < o.value ? -1 : (this.value > o.value ? 1 : 0);
    }

    // The IEEE-754 bit pattern of `value` as a long — a pure VM intrinsic (bytecode can't
    // reinterpret a double's bits). Used by the %a hex-float conversion.
    public static native long doubleToLongBits(double value);

    public static String toString(double d) {
        return shortestDecimal(d, false);
    }

    public String toString() {
        return Double.toString(this.value);
    }

    // The shortest decimal string that round-trips to `value` (JDK's Double.toString
    // contract). It searches significant-digit counts 1..17, rounding the mantissa half-even
    // and checking whether the candidate reconstructs to the original value; the first that
    // does is the shortest. `asFloat` round-trips against float precision (for Float.toString).
    // Matches the JDK for ordinary magnitudes; extreme exponents (|10^p| beyond an exact
    // double) or full-17-digit values may differ in the last place. Shared with Float.
    static String shortestDecimal(double value, boolean asFloat) {
        if (value != value) {
            return "NaN";
        }
        if (value - value != 0.0) {
            if (value < 0) {
                return "-Infinity";
            }
            return "Infinity";
        }
        boolean neg = value < 0 || (value == 0 && 1.0 / value < 0);
        double d;
        if (neg) {
            d = -value;
        } else {
            d = value;
        }
        String s;
        if (d == 0) {
            s = "0.0";
        } else {
            int e = 0;
            double m = d;
            while (m >= 10) {
                m = m / 10;
                e = e + 1;
            }
            while (m < 1) {
                m = m * 10;
                e = e - 1;
            }
            long r = 0;
            int fe = e;
            for (int k = 1; k <= 17; k = k + 1) {
                long pw = 1;
                for (int i = 0; i < k - 1; i = i + 1) {
                    pw = pw * 10;
                }
                long cand = roundHalfEven(m * pw);
                int ce = e;
                if (cand >= pw * 10) {
                    cand = pw;
                    ce = e + 1;
                }
                double back = reconstruct(cand, ce - (k - 1));
                boolean ok;
                if (asFloat) {
                    ok = ((float) back == (float) d);
                } else {
                    ok = (back == d);
                }
                if (ok) {
                    r = cand;
                    fe = ce;
                    break;
                }
                if (k == 17) {
                    r = cand;
                    fe = ce;
                }
            }
            s = assembleDecimal(Long.toString(r), fe);
        }
        if (neg) {
            return "-" + s;
        }
        return s;
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

    private static double pow10e(int n) {
        double r = 1.0;
        for (int i = 0; i < n; i = i + 1) {
            r = r * 10.0;
        }
        return r;
    }

    private static double tenPow(int n) {
        double r = 1.0;
        if (n >= 0) {
            for (int i = 0; i < n; i = i + 1) {
                r = r * 10.0;
            }
        } else {
            for (int i = 0; i < -n; i = i + 1) {
                r = r / 10.0;
            }
        }
        return r;
    }

    // r * 10^p, correctly rounded via a single multiply/divide by an exact power of ten when
    // |p| <= 22, else the (double-rounded) fallback.
    private static double reconstruct(long r, int p) {
        if (p >= 0) {
            if (p <= 22) {
                return (double) r * pow10e(p);
            }
            return (double) r * tenPow(p);
        }
        if (-p <= 22) {
            return (double) r / pow10e(-p);
        }
        return (double) r * tenPow(p);
    }

    // Lays out significant `digits` with the first digit at place 10^e in the JDK style:
    // plain decimal for e in [-3, 6], scientific (d.dddEexp) otherwise; always one fractional
    // digit.
    private static String assembleDecimal(String digits, int e) {
        int len = digits.length();
        if (e >= -3 && e < 7) {
            if (e >= 0) {
                if (len <= e + 1) {
                    String ip = digits;
                    for (int i = len; i < e + 1; i = i + 1) {
                        ip = ip + "0";
                    }
                    return ip + ".0";
                }
                return digits.substring(0, e + 1) + "." + digits.substring(e + 1, len);
            }
            String zeros = "";
            for (int i = 0; i < -e - 1; i = i + 1) {
                zeros = zeros + "0";
            }
            return "0." + zeros + digits;
        }
        String mant;
        if (len > 1) {
            mant = digits.charAt(0) + "." + digits.substring(1, len);
        } else {
            mant = digits.charAt(0) + ".0";
        }
        return mant + "E" + Integer.toString(e);
    }
}
