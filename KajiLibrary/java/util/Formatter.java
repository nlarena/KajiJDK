package java.util;

import java.io.Closeable;
import java.nio.charset.Charset;
import java.io.Flushable;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.ChronoField;

// KajiLibrary's java.util.Formatter — printf-style formatting. A Formatter parses a format
// string (literal text and `%`-specifiers) and appends the formatted output to an Appendable
// (a fresh StringBuilder by default, reachable via out() / toString()). Each specifier is
// `%[argument_index$][flags][width][.precision]conversion`.
//
// H6 subset: T1 string/char/boolean (`s S b B c C`, `%%`, `%n`), T2 integer (`d x X o`), and
// T3 floating point (`f e E g G`), with the flags (`- 0 + space , ( #`), width, precision and
// left-justify. Grouping uses ',' (US/ROOT — Locale is H6-T5). Floating point uses a
// double-arithmetic approximation with half-up rounding: faithful to java.util.Formatter for
// ordinary magnitudes, but `%f` of a magnitude >= ~9.2e18 needs bignum and is out of subset.
// Double.toString/Float.toString (shortest round-tripping decimal), Locale, `%t` and
// Formattable are H6-T5.
public final class Formatter implements Closeable, Flushable {

    private static final int F_LEFT = 1;
    private static final int F_ZERO = 2;
    private static final int F_PLUS = 4;
    private static final int F_SPACE = 8;
    private static final int F_COMMA = 16;
    private static final int F_PAREN = 32;
    private static final int F_ALT = 64;

    private Appendable out;
    private Locale locale;
    // La ultima IOException que tiro el destino, si el destino es un OutputStream. `ioException()`
    // la devuelve; con un `Appendable` comun queda siempre null, porque el `append` de esta
    // biblioteca no declara IOException y entonces no hay ninguna que reportar.
    private java.io.IOException ultimaIo;

    public Formatter() {
        this.out = new StringBuilder();
        this.locale = Locale.getDefault();
    }

    public Formatter(Appendable a) {
        this.out = a;
        this.locale = Locale.getDefault();
    }

    public Formatter(Locale l) {
        this.out = new StringBuilder();
        this.locale = l;
    }

    public Formatter(Appendable a, Locale l) {
        this.out = a;
        this.locale = l;
    }

    /**
     * Un `Formatter` que escribe a un stream de bytes, codificando con `charset`.
     *
     * <p>Esta forma **si** se puede implementar de verdad, a diferencia de las de `File` y de nombre
     * de archivo: el stream lo aporta quien llama, ya abierto, asi que no hace falta que la
     * biblioteca sepa tocar el sistema de archivos.
     */
    public Formatter(java.io.OutputStream os, Charset charset, Locale l) {
        if (os == null || charset == null) {
            throw new NullPointerException();
        }
        this.out = new SalidaCodificada(this, os, charset);
        this.locale = l;
    }

    public Formatter(java.io.OutputStream os) {
        this(os, Charset.defaultCharset(), Locale.getDefault());
    }

    /**
     * Un `Formatter` que escribe a un `PrintStream`.
     *
     * <p>Existe aparte de la forma con `OutputStream` --de la que un `PrintStream` **es** un caso--
     * porque el JDK la declara, y porque el charset que se usa es el del stream y no el por defecto:
     * un `PrintStream` ya eligio el suyo al construirse, y pisarlo aca daria bytes distintos de los
     * que ese mismo stream produce con `print`.
     */
    public Formatter(java.io.PrintStream ps) {
        if (ps == null) {
            throw new NullPointerException();
        }
        this.out = ps;
        this.locale = Locale.getDefault();
    }

    /**
     * Ídem, nombrando el charset.
     *
     * <p>Declara `UnsupportedEncodingException` --chequeada-- y no la `UnsupportedCharsetException`
     * que tira `Charset.forName`, porque es lo que declara el JDK y **el `throws` es parte del
     * contrato**: el que llama esta obligado a atajarla, y cambiarla por una no chequeada le sacaria
     * esa obligacion sin avisar.
     *
     * <p>Vale anotarlo: `apidiff` **no** habria visto esta diferencia --normaliza sacando la
     * clausula `throws`--. La encontro compilar la prueba con el `javac` real, que se nego.
     */
    public Formatter(java.io.OutputStream os, String charsetName)
            throws java.io.UnsupportedEncodingException {
        this(os, cargarCharset(charsetName), Locale.getDefault());
    }

    public Formatter(java.io.OutputStream os, String charsetName, Locale l)
            throws java.io.UnsupportedEncodingException {
        this(os, cargarCharset(charsetName), l);
    }

    // `Charset.forName` tira `UnsupportedCharsetException`, que no es la que el contrato pide.
    private static Charset cargarCharset(String nombre) throws java.io.UnsupportedEncodingException {
        if (nombre == null) {
            throw new NullPointerException();
        }
        try {
            return Charset.forName(nombre);
        } catch (RuntimeException e) {
            throw new java.io.UnsupportedEncodingException(nombre);
        }
    }

    /**
     * La ultima `IOException` que tiro el destino, o `null` si no hubo.
     *
     * <p>Es la razon por la que `Formatter` **no propaga** los errores de escritura: sus metodos
     * `format` devuelven `this` para poder encadenarse, y una excepcion chequeada lo rompe. Se
     * guarda y se pregunta despues.
     */
    public java.io.IOException ioException() {
        return this.ultimaIo;
    }

    // Lo llama `SalidaCodificada` cuando el stream falla. Package-private: no es API.
    void registrarIo(java.io.IOException e) {
        this.ultimaIo = e;
    }

    public Appendable out() {
        return this.out;
    }

    public Locale locale() {
        return this.locale;
    }

    public String toString() {
        return this.out.toString();
    }

    public void flush() {
        if (this.out instanceof SalidaCodificada) {
            ((SalidaCodificada) this.out).vaciar();
        }
    }

    public void close() {
        this.flush();
        if (this.out instanceof SalidaCodificada) {
            ((SalidaCodificada) this.out).cerrar();
        }
    }

    // Formats using `l` for this call only (JDK's format(Locale, ...) overload).
    public Formatter format(Locale l, String format, Object... args) {
        Locale saved = this.locale;
        this.locale = l;
        this.format(format, args);
        this.locale = saved;
        return this;
    }

    public Formatter format(String format, Object... args) {
        int argIndex = 0;
        int lastArgIndex = -1;
        int i = 0;
        int len = format.length();
        char grp = groupSep(this.locale);
        char dec = decimalSep(this.locale);
        while (i < len) {
            char c = format.charAt(i);
            if (c != '%') {
                this.out.append(c);
                i = i + 1;
                continue;
            }
            i = i + 1;

            // [argument_index$] or '<' (relative: reuse the previous specifier's argument)
            boolean relative = false;
            int explicitIndex = -1;
            if (i < len && format.charAt(i) == '<') {
                relative = true;
                i = i + 1;
            } else {
                int j = i;
                while (j < len && isDigit(format.charAt(j))) {
                    j = j + 1;
                }
                if (j < len && j > i && format.charAt(j) == '$') {
                    explicitIndex = parseInt(format, i, j);
                    i = j + 1;
                }
            }
            int flags = 0;
            while (i < len && isFlag(format.charAt(i))) {
                char fc = format.charAt(i);
                if (fc == '-') {
                    flags = flags | F_LEFT;
                } else if (fc == '0') {
                    flags = flags | F_ZERO;
                } else if (fc == '+') {
                    flags = flags | F_PLUS;
                } else if (fc == ' ') {
                    flags = flags | F_SPACE;
                } else if (fc == ',') {
                    flags = flags | F_COMMA;
                } else if (fc == '(') {
                    flags = flags | F_PAREN;
                } else if (fc == '#') {
                    flags = flags | F_ALT;
                }
                i = i + 1;
            }
            int width = -1;
            int k = i;
            while (k < len && isDigit(format.charAt(k))) {
                k = k + 1;
            }
            if (k > i) {
                width = parseInt(format, i, k);
                i = k;
            }
            int precision = -1;
            if (i < len && format.charAt(i) == '.') {
                i = i + 1;
                int p = i;
                while (p < len && isDigit(format.charAt(p))) {
                    p = p + 1;
                }
                precision = parseInt(format, i, p);
                i = p;
            }
            char conv = format.charAt(i);
            i = i + 1;

            if (conv == '%') {
                this.out.append('%');
                continue;
            }
            if (conv == 'n') {
                this.out.append('\n');
                continue;
            }

            Object arg;
            if (relative) {
                arg = args[lastArgIndex];
            } else if (explicitIndex >= 0) {
                arg = args[explicitIndex - 1];
                lastArgIndex = explicitIndex - 1;
            } else {
                arg = args[argIndex];
                lastArgIndex = argIndex;
                argIndex = argIndex + 1;
            }

            if (conv == 't' || conv == 'T') {
                char sub = format.charAt(i);
                i = i + 1;
                String dt = formatDateTime(sub, (TemporalAccessor) arg, conv == 'T');
                this.out.append(pad(dt, width, (flags & F_LEFT) != 0));
            } else if (isIntConv(conv)) {
                this.out.append(formatInt(conv, arg, flags, width, grp, dec));
            } else if (isFloatConv(conv)) {
                this.out.append(formatFloat(conv, arg, flags, width, precision, grp, dec));
            } else if ((conv == 's' || conv == 'S') && arg instanceof Formattable) {
                ((Formattable) arg).formatTo(this, flags, width, precision);
            } else {
                this.out.append(pad(convert(conv, arg, precision), width, (flags & F_LEFT) != 0));
            }
        }
        return this;
    }

    private static boolean isIntConv(char c) {
        return c == 'd' || c == 'x' || c == 'X' || c == 'o';
    }

    private static boolean isFloatConv(char c) {
        return c == 'f' || c == 'e' || c == 'E' || c == 'g' || c == 'G' || c == 'a' || c == 'A';
    }

    // Assembles a number from its parts, applying width, zero-pad (between sign/prefix and
    // digits) and left-justify.
    private static String finishNumber(String head, String tail, String prefix, String digits,
                                       boolean left, boolean zero, int width) {
        String mid = prefix + digits;
        int pad = width - (head.length() + mid.length() + tail.length());
        if (pad <= 0) {
            return head + mid + tail;
        }
        if (left) {
            return head + mid + tail + repeat(' ', pad);
        }
        if (zero) {
            return head + prefix + repeat('0', pad) + digits + tail;
        }
        return repeat(' ', pad) + head + mid + tail;
    }

    private static String formatInt(char conv, Object arg, int flags, int width, char grp, char dec) {
        long value;
        int bits;
        if (arg instanceof Long) {
            value = ((Long) arg).longValue();
            bits = 64;
        } else if (arg instanceof Integer) {
            value = ((Integer) arg).intValue();
            bits = 32;
        } else if (arg instanceof Short) {
            value = ((Short) arg).shortValue();
            bits = 32;
        } else if (arg instanceof Byte) {
            value = ((Byte) arg).byteValue();
            bits = 32;
        } else {
            throw new RuntimeException("not an integer argument");
        }

        boolean left = (flags & F_LEFT) != 0;
        boolean zero = (flags & F_ZERO) != 0;
        boolean plus = (flags & F_PLUS) != 0;
        boolean space = (flags & F_SPACE) != 0;
        boolean comma = (flags & F_COMMA) != 0;
        boolean paren = (flags & F_PAREN) != 0;
        boolean alt = (flags & F_ALT) != 0;

        boolean neg = false;
        String digits;
        String prefix = "";
        if (conv == 'd') {
            neg = value < 0;
            digits = decimalMag(value);
            if (comma) {
                digits = group(digits);
            }
        } else {
            long uv;
            if (bits == 32) {
                uv = value & 0xFFFFFFFFL;
            } else {
                uv = value;
            }
            if (conv == 'o') {
                digits = radixUnsigned(uv, 3);
                if (alt) {
                    prefix = "0";
                }
            } else {
                digits = radixUnsigned(uv, 4);
                if (conv == 'X') {
                    digits = upper(digits);
                }
                if (alt) {
                    if (conv == 'X') {
                        prefix = "0X";
                    } else {
                        prefix = "0x";
                    }
                }
            }
        }

        String head;
        String tail;
        if (conv == 'd' && neg && paren) {
            head = "(";
            tail = ")";
        } else {
            tail = "";
            if (conv == 'd' && neg) {
                head = "-";
            } else if (conv == 'd' && plus) {
                head = "+";
            } else if (conv == 'd' && space) {
                head = " ";
            } else {
                head = "";
            }
        }
        return translate(finishNumber(head, tail, prefix, digits, left, zero, width), grp, dec);
    }

    private static String formatFloat(char conv, Object arg, int flags, int width, int precision, char grp, char dec) {
        double dv;
        if (arg instanceof Double) {
            dv = ((Double) arg).doubleValue();
        } else if (arg instanceof Float) {
            dv = (double) ((Float) arg).floatValue();
        } else {
            throw new RuntimeException("not a floating-point argument");
        }
        if (precision < 0 && conv != 'a' && conv != 'A') {
            precision = 6;
        }
        boolean left = (flags & F_LEFT) != 0;
        boolean zero = (flags & F_ZERO) != 0;
        boolean plus = (flags & F_PLUS) != 0;
        boolean space = (flags & F_SPACE) != 0;
        boolean comma = (flags & F_COMMA) != 0;
        boolean paren = (flags & F_PAREN) != 0;
        boolean alt = (flags & F_ALT) != 0;

        boolean nan = (dv != dv);
        boolean inf = !nan && (dv - dv != 0.0);
        boolean neg = dv < 0;
        boolean special = nan || inf;

        String body;
        if (nan) {
            body = "NaN";
            neg = false;
        } else if (inf) {
            body = "Infinity";
        } else {
            double d;
            if (neg) {
                d = -dv;
            } else {
                d = dv;
            }
            if (conv == 'f') {
                body = fixed(d, precision, alt);
            } else if (conv == 'e' || conv == 'E') {
                body = sci(d, precision, conv == 'E', alt);
            } else if (conv == 'a' || conv == 'A') {
                body = hexFloat(d, precision, conv == 'A');
            } else {
                body = general(d, precision, conv == 'G', alt);
            }
        }
        if (comma && !special && conv == 'f') {
            body = groupIntPart(body);
        }
        if ((conv == 'E' || conv == 'G') && special) {
            body = upper(body);
        }
        // Localize the separators ('.' decimal, ',' grouping) — not for %a (hex float always
        // uses '.') nor for NaN/Infinity.
        if (!special && conv != 'a' && conv != 'A') {
            body = translate(body, grp, dec);
        }

        String head;
        String tail;
        if (neg && paren && !nan) {
            head = "(";
            tail = ")";
        } else {
            tail = "";
            if (neg) {
                head = "-";
            } else if (plus) {
                head = "+";
            } else if (space) {
                head = " ";
            } else {
                head = "";
            }
        }
        boolean zeroEff = zero && !special;
        return finishNumber(head, tail, "", body, left, zeroEff, width);
    }

    // Fixed-point: `precision` fractional digits, half-up. Integer and fractional parts are
    // formatted separately so a large magnitude doesn't overflow `value * 10^precision`.
    private static String fixed(double d, int precision, boolean alt) {
        long ip = (long) d;
        double frac = d - (double) ip;
        long fpow = 1;
        for (int i = 0; i < precision; i = i + 1) {
            fpow = fpow * 10;
        }
        long fr = (long) (frac * fpow + 0.5);
        if (fr >= fpow) {
            fr = fr - fpow;
            ip = ip + 1;
        }
        String is = ustr(ip);
        if (precision == 0) {
            if (alt) {
                return is + ".";
            }
            return is;
        }
        String fs = ustr(fr);
        while (fs.length() < precision) {
            fs = "0" + fs;
        }
        return is + "." + fs;
    }

    // Scientific: one lead digit, '.', `precision` fractional digits, and a signed exponent.
    private static String sci(double d, int precision, boolean upper, boolean alt) {
        int exp = 0;
        double m = d;
        if (m != 0) {
            while (m >= 10) {
                m = m / 10;
                exp = exp + 1;
            }
            while (m < 1) {
                m = m * 10;
                exp = exp - 1;
            }
        }
        long pow = 1;
        for (int i = 0; i < precision; i = i + 1) {
            pow = pow * 10;
        }
        long r = (long) (m * pow + 0.5);
        if (r >= pow * 10) {
            r = pow;
            exp = exp + 1;
        }
        String digits = ustr(r);
        while (digits.length() < precision + 1) {
            digits = "0" + digits;
        }
        char lead = digits.charAt(0);
        String mant;
        if (precision > 0) {
            mant = lead + "." + digits.substring(1, digits.length());
        } else if (alt) {
            mant = lead + ".";
        } else {
            mant = "" + lead;
        }
        String e;
        if (upper) {
            e = "E";
        } else {
            e = "e";
        }
        return mant + e + expStr(exp);
    }

    private static String expStr(int exp) {
        String sign;
        int a;
        if (exp < 0) {
            sign = "-";
            a = -exp;
        } else {
            sign = "+";
            a = exp;
        }
        String ds = ustr(a);
        if (ds.length() < 2) {
            ds = "0" + ds;
        }
        return sign + ds;
    }

    // General (%g): `precision` significant digits; fixed notation if the decimal exponent is
    // in [-4, precision), otherwise scientific.
    private static String general(double d, int precision, boolean upper, boolean alt) {
        if (precision == 0) {
            precision = 1;
        }
        int exp = 0;
        if (d != 0) {
            double m = d;
            while (m >= 10) {
                m = m / 10;
                exp = exp + 1;
            }
            while (m < 1) {
                m = m * 10;
                exp = exp - 1;
            }
        }
        if (exp >= -4 && exp < precision) {
            int fp = precision - 1 - exp;
            if (fp < 0) {
                fp = 0;
            }
            return fixed(d, fp, alt);
        }
        return sci(d, precision - 1, upper, alt);
    }

    // Hexadecimal floating point (%a/%A): 0x1.<hex-significand>p<exponent>, read from the
    // double's IEEE-754 bits. Without an explicit precision, trailing zero hex digits are
    // trimmed (the shortest exact form).
    private static String hexFloat(double d, int precision, boolean upper) {
        long bits = Double.doubleToLongBits(d);
        int be = (int) ((bits >> 52) & 0x7FF);
        long mant = bits & 0xFFFFFFFFFFFFFL;
        String lead;
        int pexp;
        if (be == 0) {
            lead = "0";
            if (mant == 0) {
                pexp = 0;
            } else {
                pexp = -1022;
            }
        } else {
            lead = "1";
            pexp = be - 1023;
        }
        StringBuilder hd = new StringBuilder();
        for (int i = 0; i < 13; i = i + 1) {
            int nib = (int) ((mant >> (48 - 4 * i)) & 0xF);
            if (nib < 10) {
                hd.append((char) ('0' + nib));
            } else {
                hd.append((char) ('a' + nib - 10));
            }
        }
        String hex = hd.toString();
        if (precision >= 0) {
            if (precision < 13) {
                hex = hex.substring(0, precision);
            }
            while (hex.length() < precision) {
                hex = hex + "0";
            }
        } else {
            int end = hex.length();
            while (end > 1 && hex.charAt(end - 1) == '0') {
                end = end - 1;
            }
            hex = hex.substring(0, end);
        }
        String body = "0x" + lead + "." + hex + "p" + Integer.toString(pexp);
        if (upper) {
            body = upper(body);
        }
        return body;
    }

    // English (US/ROOT) month and weekday names for the %t name conversions. Locale-specific
    // names are future work (need Locale data).
    private static final String[] MONTHS = {"January", "February", "March", "April", "May",
        "June", "July", "August", "September", "October", "November", "December"};
    private static final String[] MON3 = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul",
        "Aug", "Sep", "Oct", "Nov", "Dec"};
    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
        "Saturday", "Sunday"};
    private static final String[] DAY3 = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    private static String pad2(long v) {
        String s = Long.toString(v);
        if (s.length() < 2) {
            s = "0" + s;
        }
        return s;
    }

    private static String padN(long v, int n) {
        String s = Long.toString(v);
        while (s.length() < n) {
            s = "0" + s;
        }
        return s;
    }

    private static long hour12(long h) {
        long x = h % 12;
        if (x == 0) {
            return 12;
        }
        return x;
    }

    // The %t/%T date-time conversions: reads the requested field from the TemporalAccessor via
    // getLong(ChronoField) and renders it. Subset: Y y C m d e j H I k l M S L N p B b h A a and
    // the composites R T D F r. %T upper-cases the result. Epoch/zone conversions (s Q z Z c)
    // need an Instant/zone and are out of subset.
    private static String formatDateTime(char sub, TemporalAccessor ta, boolean upper) {
        long year = ta.getLong(ChronoField.YEAR);
        String r;
        if (sub == 'Y') {
            r = padN(year, 4);
        } else if (sub == 'y') {
            r = pad2(year % 100);
        } else if (sub == 'C') {
            r = pad2(year / 100);
        } else if (sub == 'm') {
            r = pad2(ta.getLong(ChronoField.MONTH_OF_YEAR));
        } else if (sub == 'd') {
            r = pad2(ta.getLong(ChronoField.DAY_OF_MONTH));
        } else if (sub == 'e') {
            r = Long.toString(ta.getLong(ChronoField.DAY_OF_MONTH));
        } else if (sub == 'j') {
            r = padN(ta.getLong(ChronoField.DAY_OF_YEAR), 3);
        } else if (sub == 'H') {
            r = pad2(ta.getLong(ChronoField.HOUR_OF_DAY));
        } else if (sub == 'I') {
            r = pad2(hour12(ta.getLong(ChronoField.HOUR_OF_DAY)));
        } else if (sub == 'k') {
            r = Long.toString(ta.getLong(ChronoField.HOUR_OF_DAY));
        } else if (sub == 'l') {
            r = Long.toString(hour12(ta.getLong(ChronoField.HOUR_OF_DAY)));
        } else if (sub == 'M') {
            r = pad2(ta.getLong(ChronoField.MINUTE_OF_HOUR));
        } else if (sub == 'S') {
            r = pad2(ta.getLong(ChronoField.SECOND_OF_MINUTE));
        } else if (sub == 'L') {
            r = padN(ta.getLong(ChronoField.NANO_OF_SECOND) / 1000000, 3);
        } else if (sub == 'N') {
            r = padN(ta.getLong(ChronoField.NANO_OF_SECOND), 9);
        } else if (sub == 'p') {
            if (ta.getLong(ChronoField.HOUR_OF_DAY) < 12) {
                r = "am";
            } else {
                r = "pm";
            }
        } else if (sub == 'B') {
            r = MONTHS[(int) ta.getLong(ChronoField.MONTH_OF_YEAR) - 1];
        } else if (sub == 'b' || sub == 'h') {
            r = MON3[(int) ta.getLong(ChronoField.MONTH_OF_YEAR) - 1];
        } else if (sub == 'A') {
            r = DAYS[(int) ta.getLong(ChronoField.DAY_OF_WEEK) - 1];
        } else if (sub == 'a') {
            r = DAY3[(int) ta.getLong(ChronoField.DAY_OF_WEEK) - 1];
        } else if (sub == 'R') {
            r = pad2(ta.getLong(ChronoField.HOUR_OF_DAY)) + ":" + pad2(ta.getLong(ChronoField.MINUTE_OF_HOUR));
        } else if (sub == 'T') {
            r = pad2(ta.getLong(ChronoField.HOUR_OF_DAY)) + ":" + pad2(ta.getLong(ChronoField.MINUTE_OF_HOUR))
                + ":" + pad2(ta.getLong(ChronoField.SECOND_OF_MINUTE));
        } else if (sub == 'D') {
            r = pad2(ta.getLong(ChronoField.MONTH_OF_YEAR)) + "/" + pad2(ta.getLong(ChronoField.DAY_OF_MONTH))
                + "/" + pad2(year % 100);
        } else if (sub == 'F') {
            r = padN(year, 4) + "-" + pad2(ta.getLong(ChronoField.MONTH_OF_YEAR)) + "-" + pad2(ta.getLong(ChronoField.DAY_OF_MONTH));
        } else if (sub == 'r') {
            long h = ta.getLong(ChronoField.HOUR_OF_DAY);
            String ap;
            if (h < 12) {
                ap = "AM";
            } else {
                ap = "PM";
            }
            r = pad2(hour12(h)) + ":" + pad2(ta.getLong(ChronoField.MINUTE_OF_HOUR)) + ":"
                + pad2(ta.getLong(ChronoField.SECOND_OF_MINUTE)) + " " + ap;
        } else {
            throw new RuntimeException("unsupported date/time conversion: " + sub);
        }
        if (upper) {
            r = upper(r);
        }
        return r;
    }

    private static char groupSep(Locale l) {
        if (l != null && "de".equals(l.getLanguage())) {
            return '.';
        }
        return ',';
    }

    private static char decimalSep(Locale l) {
        if (l != null && "de".equals(l.getLanguage())) {
            return ',';
        }
        return '.';
    }

    // Maps the US-convention separators produced by the number formatters (',' grouping, '.'
    // decimal) to the locale's. A no-op for the US/ROOT convention.
    private static String translate(String s, char grp, char dec) {
        if (grp == ',' && dec == '.') {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i = i + 1) {
            char c = s.charAt(i);
            if (c == ',') {
                sb.append(grp);
            } else if (c == '.') {
                sb.append(dec);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String groupIntPart(String body) {
        int dot = -1;
        for (int i = 0; i < body.length(); i = i + 1) {
            if (body.charAt(i) == '.') {
                dot = i;
                break;
            }
        }
        if (dot < 0) {
            return group(body);
        }
        return group(body.substring(0, dot)) + body.substring(dot, body.length());
    }

    // Non-negative long -> decimal string.
    private static String ustr(long v) {
        if (v == 0) {
            return "0";
        }
        char[] buf = new char[20];
        int pos = 20;
        long x = v;
        while (x != 0) {
            int d = (int) (x % 10);
            pos = pos - 1;
            buf[pos] = (char) ('0' + d);
            x = x / 10;
        }
        return String.valueOf(buf, pos, 20 - pos);
    }

    // Magnitude digits of |v|, accumulated in negative space so Long.MIN_VALUE is safe.
    private static String decimalMag(long v) {
        if (v == 0) {
            return "0";
        }
        long x = v;
        if (x > 0) {
            x = -x;
        }
        char[] buf = new char[20];
        int pos = 20;
        while (x != 0) {
            int d = (int) (-(x % 10));
            pos = pos - 1;
            buf[pos] = (char) ('0' + d);
            x = x / 10;
        }
        return String.valueOf(buf, pos, 20 - pos);
    }

    // Unsigned radix digits (lowercase) via bit masking/shifting (avoids unsigned division).
    private static String radixUnsigned(long uv, int shiftBits) {
        if (uv == 0) {
            return "0";
        }
        int mask = (1 << shiftBits) - 1;
        char[] buf = new char[22];
        int pos = 22;
        long x = uv;
        while (x != 0) {
            int d = (int) (x & mask);
            pos = pos - 1;
            if (d < 10) {
                buf[pos] = (char) ('0' + d);
            } else {
                buf[pos] = (char) ('a' + d - 10);
            }
            x = x >>> shiftBits;
        }
        return String.valueOf(buf, pos, 22 - pos);
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

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i = i + 1) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static String convert(char conv, Object arg, int precision) {
        if (conv == 's' || conv == 'S') {
            String v;
            if (arg == null) {
                v = "null";
            } else {
                v = arg.toString();
            }
            if (precision >= 0 && v.length() > precision) {
                v = v.substring(0, precision);
            }
            if (conv == 'S') {
                v = upper(v);
            }
            return v;
        }
        if (conv == 'b' || conv == 'B') {
            String v;
            if (arg == null) {
                v = "false";
            } else if (arg instanceof Boolean) {
                if (((Boolean) arg).booleanValue()) {
                    v = "true";
                } else {
                    v = "false";
                }
            } else {
                v = "true";
            }
            if (conv == 'B') {
                v = upper(v);
            }
            return v;
        }
        if (conv == 'c' || conv == 'C') {
            char ch;
            if (arg instanceof Character) {
                ch = ((Character) arg).charValue();
            } else {
                ch = (char) ((Integer) arg).intValue();
            }
            String v = "" + ch;
            if (conv == 'C') {
                v = upper(v);
            }
            return v;
        }
        throw new RuntimeException("unsupported conversion: " + conv);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isFlag(char c) {
        return c == '-' || c == '#' || c == '+' || c == ' ' || c == '0' || c == ',' || c == '(';
    }

    private static int parseInt(String s, int from, int to) {
        int v = 0;
        for (int k = from; k < to; k = k + 1) {
            v = v * 10 + (s.charAt(k) - '0');
        }
        return v;
    }

    private static String upper(String s) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < s.length(); k = k + 1) {
            char c = s.charAt(k);
            if (c >= 'a' && c <= 'z') {
                c = (char) (c - 32);
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String pad(String v, int width, boolean left) {
        int n = width - v.length();
        if (n <= 0) {
            return v;
        }
        if (left) {
            return v + repeat(' ', n);
        }
        return repeat(' ', n) + v;
    }
}

// El `Appendable` que hay detras de `new Formatter(OutputStream, ...)`: acumula caracteres y los
// escribe codificados al stream.
//
// Acumula en vez de codificar caracter por caracter porque un caracter no es una unidad de
// codificacion: un par suplente (un emoji, por ejemplo) son dos `char` que juntos dan cuatro bytes en
// UTF-8, y codificar cada mitad por separado daria basura. Se vacia en `flush`/`close`, y tambien
// cuando el buffer crece, cortando **solo** en un limite seguro.
final class SalidaCodificada implements Appendable {

    private final Formatter duenio;
    private final java.io.OutputStream destino;
    private final Charset charset;
    private final StringBuilder pendiente = new StringBuilder();

    SalidaCodificada(Formatter duenio, java.io.OutputStream destino, Charset charset) {
        this.duenio = duenio;
        this.destino = destino;
        this.charset = charset;
    }

    public Appendable append(CharSequence csq) {
        this.pendiente.append(csq == null ? "null" : csq);
        return this;
    }

    public Appendable append(CharSequence csq, int start, int end) {
        this.pendiente.append(csq == null ? "null" : csq, start, end);
        return this;
    }

    public Appendable append(char c) {
        this.pendiente.append(c);
        return this;
    }

    void vaciar() {
        if (this.pendiente.length() == 0) {
            return;
        }
        // Si el ultimo char es la mitad alta de un par suplente, se lo deja para la proxima: su
        // compañero todavia no llego, y codificarlo solo daria el reemplazo.
        int fin = this.pendiente.length();
        if (Character.isHighSurrogate(this.pendiente.charAt(fin - 1))) {
            fin = fin - 1;
        }
        if (fin == 0) {
            return;
        }
        byte[] bytes = this.pendiente.substring(0, fin).getBytes(this.charset);
        this.pendiente.delete(0, fin);
        try {
            this.destino.write(bytes, 0, bytes.length);
        } catch (java.io.IOException e) {
            this.duenio.registrarIo(e);
        }
    }

    void cerrar() {
        try {
            this.destino.close();
        } catch (java.io.IOException e) {
            this.duenio.registrarIo(e);
        }
    }

    public String toString() {
        return this.pendiente.toString();
    }
}
