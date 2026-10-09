package javax.management;

/**
 * "The attribute, which is a string, matches this pattern."
 *
 * <p>Package-private: it is made with {@link Query#match} and with the three substring shortcuts.
 *
 * <p>Its pattern language is <b>not</b> that of {@link ObjectName}. Besides {@code *} and {@code ?}
 * it has <b>character classes</b> in brackets, with ranges ({@code [a-z]}) and negation
 * ({@code [!abc]}), and a backslash that escapes the next character. It is more like a shell glob
 * than a JMX wildcard, and confusing them is an easy mistake.
 */
class MatchQueryExp extends QueryEval implements QueryExp {

    private static final long serialVersionUID = -7156603696948215014L;

    /**
     * @serial the attribute
     */
    private AttributeValueExp exp;

    /**
     * @serial the pattern
     */
    private String pattern;

    public MatchQueryExp() {
    }

    public MatchQueryExp(AttributeValueExp a, StringValueExp s) {
        exp = a;
        pattern = s.getValue();
    }

    public AttributeValueExp getAttribute() {
        return exp;
    }

    public String getPattern() {
        return pattern;
    }

    public boolean apply(ObjectName name) throws BadStringOperationException,
            BadBinaryOpValueExpException, BadAttributeValueExpException,
            InvalidApplicationException {
        ValueExp val = exp.apply(name);
        if (!(val instanceof StringValueExp)) {
            return false;
        }
        String s = ((StringValueExp) val).getValue();
        if (s == null) {
            return false;
        }
        return coincide(pattern, s);
    }

    public String toString() {
        return exp + " like " + new StringValueExp(pattern);
    }

    /**
     * Glob-style matching, without recursion.
     *
     * <p>It is resolved by backtracking over the last {@code *} seen, like {@link ObjectName}'s
     * wildcard, so that a hostile pattern cannot overflow the stack.
     */
    private static boolean coincide(String pat, String text) {
        int p = 0;
        int t = 0;
        int star = -1;
        int mark = 0;
        while (t < text.length()) {
            boolean advances = false;
            if (p < pat.length()) {
                char c = pat.charAt(p);
                if (c == '\\') {
                    if (p + 1 < pat.length() && pat.charAt(p + 1) == text.charAt(t)) {
                        p += 2;
                        t++;
                        advances = true;
                    }
                } else if (c == '[') {
                    int end = closing(pat, p);
                    if (end > 0 && inClass(pat, p, end, text.charAt(t))) {
                        p = end + 1;
                        t++;
                        advances = true;
                    }
                } else if (c == '?' || c == text.charAt(t)) {
                    p++;
                    t++;
                    advances = true;
                }
            }
            if (advances) {
                continue;
            }
            if (p < pat.length() && pat.charAt(p) == '*') {
                star = p;
                mark = t;
                p++;
                continue;
            }
            if (star >= 0) {
                p = star + 1;
                mark++;
                t = mark;
                continue;
            }
            return false;
        }
        while (p < pat.length() && pat.charAt(p) == '*') {
            p++;
        }
        return p == pat.length();
    }

    /** Index of the {@code ]} that closes the class opening at {@code start}, or -1. */
    private static int closing(String pat, int start) {
        int i = start + 1;
        if (i < pat.length() && pat.charAt(i) == '!') {
            i++;
        }
        if (i < pat.length() && pat.charAt(i) == ']') {
            i++;
        }
        while (i < pat.length()) {
            if (pat.charAt(i) == ']') {
                return i;
            }
            i++;
        }
        return -1;
    }

    private static boolean inClass(String pat, int start, int end, char c) {
        int i = start + 1;
        boolean negated = false;
        if (i < end && pat.charAt(i) == '!') {
            negated = true;
            i++;
        }
        boolean any = false;
        while (i < end) {
            char a = pat.charAt(i);
            if (i + 2 < end && pat.charAt(i + 1) == '-') {
                char b = pat.charAt(i + 2);
                if (a <= c && c <= b) {
                    any = true;
                }
                i += 3;
            } else {
                if (a == c) {
                    any = true;
                }
                i++;
            }
        }
        return negated ? !any : any;
    }
}
