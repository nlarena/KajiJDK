package javax.management;

/**
 * "El valor esta entre estos dos", con los dos extremos incluidos.
 *
 * <p>De paquete: se fabrica con {@link Query#between}. No es azucar sobre dos comparaciones aunque
 * lo parezca: evalua el valor del medio <b>una sola vez</b>, y eso importa cuando ese valor es la
 * lectura de un atributo del MBean.
 */
class BetweenQueryExp extends QueryEval implements QueryExp {

    private static final long serialVersionUID = -2933597532866307444L;

    /**
     * @serial el valor a revisar
     */
    private ValueExp exp1;

    /**
     * @serial la cota de abajo
     */
    private ValueExp exp2;

    /**
     * @serial la cota de arriba
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
