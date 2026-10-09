package javax.management;

/**
 * "The value is between these two", with both ends included.
 *
 * <p>Package-private: it is made with {@link Query#between}. It is not sugar over two comparisons
 * although it looks like it: it evaluates the middle value <b>only once</b>, and that matters when
 * that value is the reading of an MBean attribute.
 */
class BetweenQueryExp extends QueryEval implements QueryExp {

    private static final long serialVersionUID = -2933597532866307444L;

    /**
     * @serial the value to check
     */
    private ValueExp exp1;

    /**
     * @serial the lower bound
     */
    private ValueExp exp2;

    /**
     * @serial the upper bound
     */
    private ValueExp exp3;

    public BetweenQueryExp() {
    }

    public BetweenQueryExp(ValueExp v1, ValueExp v2, ValueExp v3) {
        exp1 = v1;
        exp2 = v2;
        exp3 = v3;
    }

    public ValueExp getCheckedValue() {
        return exp1;
    }

    public ValueExp getLowerBound() {
        return exp2;
    }

    public ValueExp getUpperBound() {
        return exp3;
    }

    public boolean apply(ObjectName name) throws BadStringOperationException,
            BadBinaryOpValueExpException, BadAttributeValueExpException,
            InvalidApplicationException {
        ValueExp val1 = exp1.apply(name);
        ValueExp val2 = exp2.apply(name);
        ValueExp val3 = exp3.apply(name);

        if (val1 instanceof NumericValueExp) {
            NumericValueExp n1 = (NumericValueExp) val1;
            NumericValueExp n2 = (NumericValueExp) val2;
            NumericValueExp n3 = (NumericValueExp) val3;
            if (n1.isLong() && n2.isLong() && n3.isLong()) {
                long a = n1.longValue();
                return n2.longValue() <= a && a <= n3.longValue();
            }
            double a = n1.doubleValue();
            return n2.doubleValue() <= a && a <= n3.doubleValue();
        }

        if (val1 instanceof BooleanValueExp) {
            boolean a = ((BooleanValueExp) val1).getValue().booleanValue();
            boolean b = ((BooleanValueExp) val2).getValue().booleanValue();
            boolean c = ((BooleanValueExp) val3).getValue().booleanValue();
            return (!b || a) && (a || !c) && (!a || c);
        }

        if (val1 instanceof StringValueExp && val2 instanceof StringValueExp
                && val3 instanceof StringValueExp) {
            String a = ((StringValueExp) val1).getValue();
            String b = ((StringValueExp) val2).getValue();
            String c = ((StringValueExp) val3).getValue();
            if (a == null || b == null || c == null) {
                return false;
            }
            return b.compareTo(a) <= 0 && a.compareTo(c) <= 0;
        }
        return false;
    }

    public String toString() {
        return "(" + exp1 + ") between (" + exp2 + ") and (" + exp3 + ")";
    }
}
