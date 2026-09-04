package javax.management;

/**
 * "El valor es uno de estos."
 *
 * <p>De paquete: se fabrica con {@link Query#in}. Como {@link BetweenQueryExp}, evalua el valor a
 * revisar una sola vez y despues recorre la lista.
 */
class InQueryExp extends QueryEval implements QueryExp {

    private static final long serialVersionUID = -5801329450358952434L;

    /**
     * @serial el valor a revisar
     */
    private ValueExp val;

    /**
     * @serial los valores admitidos
     */
    private ValueExp[] valueList;

    public InQueryExp() {
    }

    public InQueryExp(ValueExp v1, ValueExp[] valueList) {
        val = v1;
        this.valueList = valueList;
    }

    public ValueExp getCheckedValue() {
        return val;
    }

    public ValueExp[] getExplicitValues() {
        return valueList;
    }

    /** Con la lista vacia da `false`: nada pertenece al conjunto vacio. */
    public boolean apply(ObjectName name) throws BadStringOperationException,
            BadBinaryOpValueExpException, BadAttributeValueExpException,
            InvalidApplicationException {
        if (valueList == null) {
            return false;
        }
        ValueExp v = val.apply(name);
        boolean numerico = v instanceof NumericValueExp;
        for (int i = 0; i < valueList.length; i++) {
            ValueExp otro = valueList[i].apply(name);
            if (numerico) {
                if (!(otro instanceof NumericValueExp)) {
                    continue;
                }
                NumericValueExp a = (NumericValueExp) v;
                NumericValueExp b = (NumericValueExp) otro;
                if (a.isLong() && b.isLong()) {
                    if (a.longValue() == b.longValue()) {
                        return true;
                    }
                } else if (a.doubleValue() == b.doubleValue()) {
                    return true;
                }
            } else if (v instanceof StringValueExp && otro instanceof StringValueExp) {
                String a = ((StringValueExp) v).getValue();
                String b = ((StringValueExp) otro).getValue();
                if (a == null ? b == null : a.equals(b)) {
                    return true;
                }
            } else if (v instanceof BooleanValueExp && otro instanceof BooleanValueExp) {
                if (((BooleanValueExp) v).getValue().equals(((BooleanValueExp) otro).getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    public String toString() {
        return val + " in (" + listaTexto() + ")";
    }

    private String listaTexto() {
        StringBuilder b = new StringBuilder();
        if (valueList != null) {
            for (int i = 0; i < valueList.length; i++) {
                if (i > 0) {
                    b.append(", ");
                }
                b.append(valueList[i]);
            }
        }
        return b.toString();
    }
}
