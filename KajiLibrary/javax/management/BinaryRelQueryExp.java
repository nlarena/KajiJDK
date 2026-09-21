package javax.management;

/**
 * A comparison between two values.
 *
 * <p>Package-private: it is made with {@link Query#eq}, {@link Query#gt} and friends. The five
 * operators live in the same class with an {@code int} to tell them apart, instead of five classes:
 * it is how it is in the JDK and what makes {@code getOperator()} make sense.
 *
 * <p>The comparison is chosen by the <b>type of the left side</b>, and that asymmetry matters.
 * Integers are compared as {@code long} --if both are--, as {@code double} if either is floating
 * point, strings in lexicographic order and booleans with {@code false &lt; true}.
 */
class BinaryRelQueryExp extends QueryEval implements QueryExp {

    private static final long serialVersionUID = -5690656271650491000L;

    /**
     * @serial the operator
     */
    private int relOp;

    /**
     * @serial the left side
     */
    private ValueExp exp1;

    /**
     * @serial the right side
     */
    private ValueExp exp2;

    public BinaryRelQueryExp() {
    }

    public BinaryRelQueryExp(int op, ValueExp v1, ValueExp v2) {
        relOp = op;
        exp1 = v1;
        exp2 = v2;
    }

    /**
     * One of {@link Query#GT}, {@link Query#LT}, {@link Query#GE}, {@link Query#LE},
     * {@link Query#EQ}.
     */
    public int getOperator() {
        return relOp;
    }

    public ValueExp getLeftValue() {
        return exp1;
    }

    public ValueExp getRightValue() {
        return exp2;
    }

    /**
     * <p>The dispatch goes with chained {@code if}s and not with a {@code switch}, which would be
     * the natural thing: our javac does not yet accept in a {@code case} label a constant that
     * comes from another file, and {@code Query.GT} and friends live in {@code Query}. See finding
     * #461.
     */
    public boolean apply(ObjectName name) throws BadStringOperationException,
            BadBinaryOpValueExpException, BadAttributeValueExpException,
            InvalidApplicationException {
        Object val1 = exp1.apply(name);
        Object val2 = exp2.apply(name);

        if (val1 instanceof NumericValueExp) {
            NumericValueExp n1 = (NumericValueExp) val1;
            NumericValueExp n2 = (NumericValueExp) val2;
            if (n1.isLong() && n2.isLong()) {
                long a = n1.longValue();
                long b = n2.longValue();
                if (relOp == Query.GT) {
                    return a > b;
                }
                if (relOp == Query.LT) {
                    return a < b;
                }
                if (relOp == Query.GE) {
                    return a >= b;
                }
                if (relOp == Query.LE) {
                    return a <= b;
                }
                return relOp == Query.EQ && a == b;
            }
            // With the raw operators and not with `Double.compare`, so that NaN behaves as in the
            // JDK: NaN is not equal to, greater than or less than anything, not even itself.
            double a = n1.doubleValue();
            double b = n2.doubleValue();
            if (relOp == Query.GT) {
                return a > b;
            }
            if (relOp == Query.LT) {
                return a < b;
            }
            if (relOp == Query.GE) {
                return a >= b;
            }
            if (relOp == Query.LE) {
                return a <= b;
            }
            return relOp == Query.EQ && a == b;
        }

        if (val1 instanceof BooleanValueExp) {
            boolean a = ((BooleanValueExp) val1).getValue().booleanValue();
            boolean b = ((BooleanValueExp) val2).getValue().booleanValue();
            // The type's natural order: false < true.
            if (relOp == Query.GT) {
                return a && !b;
            }
            if (relOp == Query.LT) {
                return !a && b;
            }
            if (relOp == Query.GE) {
                return a || !b;
            }
            if (relOp == Query.LE) {
                return !a || b;
            }
            return relOp == Query.EQ && a == b;
        }

        if (val1 instanceof StringValueExp && val2 instanceof StringValueExp) {
            String a = ((StringValueExp) val1).getValue();
            String b = ((StringValueExp) val2).getValue();
            if (a == null || b == null) {
                return relOp == Query.EQ && a == b;
            }
            int c = a.compareTo(b);
            if (relOp == Query.GT) {
                return c > 0;
            }
            if (relOp == Query.LT) {
                return c < 0;
            }
            if (relOp == Query.GE) {
                return c >= 0;
            }
            if (relOp == Query.LE) {
                return c <= 0;
            }
            return relOp == Query.EQ && c == 0;
        }
        return false;
    }

    public String toString() {
        return "(" + exp1 + ") " + relOpString() + " (" + exp2 + ")";
    }

    private String relOpString() {
        if (relOp == Query.GT) {
            return ">";
        }
        if (relOp == Query.LT) {
            return "<";
        }
        if (relOp == Query.GE) {
            return ">=";
        }
        if (relOp == Query.LE) {
            return "<=";
        }
        return "=";
    }
}
