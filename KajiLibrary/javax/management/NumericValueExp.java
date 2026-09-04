package javax.management;

/**
 * Una constante numerica.
 *
 * <p>Guarda un `Number` y recuerda si era entero o de punto flotante ({@link #isLong()}). La
 * distincion importa al comparar: {@code 1} y {@code 1.0} tienen que dar iguales, y por eso las
 * comparaciones se hacen en `long` cuando **los dos** lados son enteros y en `double` si no.
 */
class NumericValueExp extends QueryEval implements ValueExp {

    private static final long serialVersionUID = -4679739485102359104L;

    /**
     * @serial el valor
     */
    private Number val = Double.valueOf(0.0);

    public NumericValueExp() {
    }

    NumericValueExp(Number val) {
        this.val = val;
    }

    /** El valor como `double`. */
    public double doubleValue() {
        if (val instanceof Long || val instanceof Integer) {
            return (double) val.longValue();
        }
        return val.doubleValue();
    }

    /** El valor como `long`. */
    public long longValue() {
        if (val instanceof Long || val instanceof Integer) {
            return val.longValue();
        }
        return (long) val.doubleValue();
    }

    /** Si el numero es entero. */
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

    /** Se devuelve a si misma. */
    public ValueExp apply(ObjectName name) throws BadStringOperationException,
            BadBinaryOpValueExpException, BadAttributeValueExpException,
            InvalidApplicationException {
        return this;
    }

    public void setMBeanServer(MBeanServer s) {
    }
}
