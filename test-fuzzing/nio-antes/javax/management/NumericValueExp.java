package javax.management;

/**
 * A numeric constant.
 *
 * <p>It keeps a {@code Number} and remembers whether it was an integer or floating point
 * ({@link #isLong()}). The distinction matters when comparing: {@code 1} and {@code 1.0} have to
 * come out equal, which is why comparisons are done as {@code long} when <b>both</b> sides are
 * integers and as {@code double} otherwise.
 */
class NumericValueExp extends QueryEval implements ValueExp {

    private static final long serialVersionUID = -4679739485102359104L;

    /**
     * @serial the value
     */
    private Number val = Double.valueOf(0.0);

    public NumericValueExp() {
    }

    NumericValueExp(Number val) {
        this.val = val;
    }

    /** The value as a {@code double}. */
    public double doubleValue() {
        if (val instanceof Long || val instanceof Integer) {
            return (double) val.longValue();
        }
        return val.doubleValue();
    }

    /** The value as a {@code long}. */
    public long longValue() {
        if (val instanceof Long || val instanceof Integer) {
            return val.longValue();
        }
        return (long) val.doubleValue();
    }

    /** Whether the number is an integer. */
    public boolean isLong() {
        return val instanceof Long || val instanceof Integer;
    }

    public String toString() {
        if (val == null) {
            return "null";
        }
        if (isLong()) {
            return String.valueOf(val.longValue());
        }
        double d = val.doubleValue();
        if (Double.isInfinite(d)) {
            return d > 0 ? "(1.0 / 0.0)" : "(-1.0 / 0.0)";
        }
        if (Double.isNaN(d)) {
            return "(0.0 / 0.0)";
        }
        return String.valueOf(d);
    }

    /** Returns itself. */
    public ValueExp apply(ObjectName name) throws BadStringOperationException,
            BadBinaryOpValueExpException, BadAttributeValueExpException,
            InvalidApplicationException {
        return this;
    }

    public void setMBeanServer(MBeanServer s) {
    }
}
