package javax.management;

/**
 * The query factory: the whole language of {@code queryNames}/{@code queryMBeans} comes in
 * through here.
 *
 * <p>The classes it builds are <b>package-private</b>, and that is the design decision of the whole
 * class. Nobody writes {@code new AndQueryExp(...)}; you write
 * {@code Query.and(Query.gt(Query.attr("Load"), Query.value(80)), ...)}. That way the expression
 * tree is free to change without breaking anyone, and the only public surfaces are the two
 * interfaces --{@link QueryExp} and {@link ValueExp}-- plus the two value classes the user may
 * indeed need to name.
 *
 * <p>Careful with the constants: {@link #GT}, {@link #LT}... and {@link #PLUS}, {@link #MINUS}...
 * are <b>two different numberings</b> and both start at zero. A {@code Query.EQ} is 4 and a
 * {@code Query.DIV} is 3, and nothing in the type prevents mixing them.
 */
public class Query {

    /** Greater than: {@value}. */
    public static final int GT = 0;

    /** Less than: {@value}. */
    public static final int LT = 1;

    /** Greater than or equal: {@value}. */
    public static final int GE = 2;

    /** Less than or equal: {@value}. */
    public static final int LE = 3;

    /** Equal: {@value}. */
    public static final int EQ = 4;

    /** Sum: {@value}. */
    public static final int PLUS = 0;

    /** Difference: {@value}. */
    public static final int MINUS = 1;

    /** Product: {@value}. */
    public static final int TIMES = 2;

    /** Cociente: {@value}. */
    public static final int DIV = 3;

    /** Public because the JDK left it public; the class is all static. */
    public Query() {
    }

    /** Both at once. */
    public static QueryExp and(QueryExp q1, QueryExp q2) {
        return new AndQueryExp(q1, q2);
    }

    /** Either of the two. */
    public static QueryExp or(QueryExp q1, QueryExp q2) {
        return new OrQueryExp(q1, q2);
    }

    /** Greater than. */
    public static QueryExp gt(ValueExp v1, ValueExp v2) {
        return new BinaryRelQueryExp(GT, v1, v2);
    }

    /** Greater than or equal. */
    public static QueryExp geq(ValueExp v1, ValueExp v2) {
        return new BinaryRelQueryExp(GE, v1, v2);
    }

    /** Less than or equal. */
    public static QueryExp leq(ValueExp v1, ValueExp v2) {
        return new BinaryRelQueryExp(LE, v1, v2);
    }

    /** Less than. */
    public static QueryExp lt(ValueExp v1, ValueExp v2) {
        return new BinaryRelQueryExp(LT, v1, v2);
    }

    /** Equal. */
    public static QueryExp eq(ValueExp v1, ValueExp v2) {
        return new BinaryRelQueryExp(EQ, v1, v2);
    }

    /** Between the two, ends included. */
    public static QueryExp between(ValueExp v1, ValueExp v2, ValueExp v3) {
        return new BetweenQueryExp(v1, v2, v3);
    }

    /**
     * Pattern matching.
     *
     * <p>It only accepts an {@link AttributeValueExp} on the left side, not any {@code ValueExp}:
     * comparing two constants with a pattern would make no sense.
     */
    public static QueryExp match(AttributeValueExp a, StringValueExp s) {
        return new MatchQueryExp(a, s);
    }

    /** The value of an MBean attribute. */
    public static AttributeValueExp attr(String name) {
        return new AttributeValueExp(name);
    }

    /** The value of an attribute, but only if the MBean is of the given class. */
    public static AttributeValueExp attr(String className, String name) {
        return new QualifiedAttributeValueExp(className, name);
    }

    /** The MBean's class name, as if it were an attribute. */
    public static AttributeValueExp classattr() {
        return new ClassAttributeValueExp();
    }

    /** The negation. */
    public static QueryExp not(QueryExp queryExp) {
        return new NotQueryExp(queryExp);
    }

    /** Membership of an explicit set. */
    public static QueryExp in(ValueExp val, ValueExp[] valueList) {
        return new InQueryExp(val, valueList);
    }

    /** String constant. */
    public static StringValueExp value(String val) {
        return new StringValueExp(val);
    }

    /** Numeric constant from a wrapper. */
    public static ValueExp value(Number val) {
        return new NumericValueExp(val);
    }

    /** Integer constant. */
    public static ValueExp value(int val) {
        return new NumericValueExp(Integer.valueOf(val));
    }

    /** Long integer constant. */
    public static ValueExp value(long val) {
        return new NumericValueExp(Long.valueOf(val));
    }

    /** Floating point constant. */
    public static ValueExp value(float val) {
        return new NumericValueExp(Float.valueOf(val));
    }

    /** Double floating point constant. */
    public static ValueExp value(double val) {
        return new NumericValueExp(Double.valueOf(val));
    }

    /** Boolean constant. */
    public static ValueExp value(boolean val) {
        return new BooleanValueExp(val);
    }

    /** Sum; on two strings, it concatenates. */
    public static ValueExp plus(ValueExp value1, ValueExp value2) {
        return new BinaryOpValueExp(PLUS, value1, value2);
    }

    /** Product. */
    public static ValueExp times(ValueExp value1, ValueExp value2) {
        return new BinaryOpValueExp(TIMES, value1, value2);
    }

    /** Difference. */
    public static ValueExp minus(ValueExp value1, ValueExp value2) {
        return new BinaryOpValueExp(MINUS, value1, value2);
    }

    /** Cociente. */
    public static ValueExp div(ValueExp value1, ValueExp value2) {
        return new BinaryOpValueExp(DIV, value1, value2);
    }

    /**
     * "Starts with".
     *
     * <p>The three substring shortcuts <b>escape</b> the text before sticking the star on: a
     * {@code *} inside the prefix is looked for literally. It is what sets them apart from
     * {@link #match}, where the pattern is taken as it is.
     */
    public static QueryExp initialSubString(AttributeValueExp a, StringValueExp s) {
        return new MatchQueryExp(a, new StringValueExp(escape(s.getValue()) + "*"));
    }

    /** "Contains". */
    public static QueryExp anySubString(AttributeValueExp a, StringValueExp s) {
        return new MatchQueryExp(a, new StringValueExp("*" + escape(s.getValue()) + "*"));
    }

    /** "Ends with". */
    public static QueryExp finalSubString(AttributeValueExp a, StringValueExp s) {
        return new MatchQueryExp(a, new StringValueExp("*" + escape(s.getValue())));
    }

    /** The MBean is of that class or of a subclass. */
    public static QueryExp isInstanceOf(StringValueExp classNameValue) {
        return new InstanceOfQueryExp(classNameValue);
    }

    /** Prepends a backslash to the four characters with meaning in the pattern. */
    private static String escape(String s) {
        if (s == null) {
            return null;
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '*' || c == '?' || c == '[' || c == '\\') {
                b.append('\\');
            }
            b.append(c);
        }
        return b.toString();
    }
}
