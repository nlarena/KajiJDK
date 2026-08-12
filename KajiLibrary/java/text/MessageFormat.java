package java.text;

// KajiLibrary's java.text.MessageFormat — the static `format` that substitutes `{n[,type[,
// style]]}` elements in a pattern with formatted arguments. A subset: the common static entry
// point, argument reordering, single-quote quoting (`''` -> `'`, `'...'` -> literal), a bare
// `{n}` (Number arguments are number-formatted, others use toString), and `{n,number,pattern}`
// via DecimalFormat. The instance API, choice/date/time formats and non-US symbols are future
// work.
public class MessageFormat {

    // Only the static format() is in this subset; a private constructor suppresses the implicit
    // public no-arg one (the JDK's MessageFormat takes a pattern).
    private MessageFormat() {
    }

    public static String format(String pattern, Object... arguments) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        int n = pattern.length();
        while (i < n) {
            char c = pattern.charAt(i);
            if (c == '\'') {
                if (i + 1 < n && pattern.charAt(i + 1) == '\'') {
                    out.append('\'');
                    i = i + 2;
                } else {
                    i = i + 1;
                    while (i < n && pattern.charAt(i) != '\'') {
                        out.append(pattern.charAt(i));
                        i = i + 1;
                    }
                    i = i + 1;
                }
            } else if (c == '{') {
                i = i + 1;
                int aidx = 0;
                while (i < n && isDigit(pattern.charAt(i))) {
                    aidx = aidx * 10 + (pattern.charAt(i) - '0');
                    i = i + 1;
                }
                String type = null;
                String style = null;
                if (i < n && pattern.charAt(i) == ',') {
                    i = i + 1;
                    StringBuilder tb = new StringBuilder();
                    while (i < n && pattern.charAt(i) != ',' && pattern.charAt(i) != '}') {
                        tb.append(pattern.charAt(i));
                        i = i + 1;
                    }
                    type = tb.toString();
                    if (i < n && pattern.charAt(i) == ',') {
                        i = i + 1;
                        StringBuilder sb2 = new StringBuilder();
                        while (i < n && pattern.charAt(i) != '}') {
                            sb2.append(pattern.charAt(i));
                            i = i + 1;
                        }
                        style = sb2.toString();
                    }
                }
                if (i < n && pattern.charAt(i) == '}') {
                    i = i + 1;
                }
                out.append(formatArg(arguments[aidx], type, style));
            } else {
                out.append(c);
                i = i + 1;
            }
        }
        return out.toString();
    }

    private static String formatArg(Object arg, String type, String style) {
        if (type == null || type.length() == 0) {
            if (arg instanceof Number) {
                return new DecimalFormat("#,##0.###").format(((Number) arg).doubleValue());
            }
            if (arg == null) {
                return "null";
            }
            return arg.toString();
        }
        if (type.equals("number")) {
            double d = ((Number) arg).doubleValue();
            if (style != null && style.length() > 0) {
                return new DecimalFormat(style).format(d);
            }
            return new DecimalFormat("#,##0.###").format(d);
        }
        if (arg == null) {
            return "null";
        }
        return arg.toString();
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
