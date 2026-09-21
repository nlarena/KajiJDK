package javax.management;

/**
 * "The value is one of these."
 *
 * <p>Package-private: it is made with {@link Query#in}. Like {@link BetweenQueryExp}, it evaluates
 * the value to check only once and then walks the list.
 */
class InQueryExp extends QueryEval implements QueryExp {

    private static final long serialVersionUID = -5801329450358952434L;

    /**
     * @serial the value to check
     */
    private ValueExp val;

    /**
     * @serial the admitted values
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

    /** With the list empty it gives {@code false}: nothing belongs to the empty set. */
    public boolean apply(ObjectName name) throws BadStringOperationException,
            BadBinaryOpValueExpException, BadAttributeValueExpException,
            InvalidApplicationException {
        if (valueList == null) {
            return false;
        }
        ValueExp v = val.apply(name);
        boolean numeric = v instanceof NumericValueExp;
        for (int i = 0; i < valueList.length; i++) {
            ValueExp other = valueList[i].apply(name);
            if (numeric) {
                if (!(other instanceof NumericValueExp)) {
                    continue;
                }
                NumericValueExp a = (NumericValueExp) v;
                NumericValueExp b = (NumericValueExp) other;
                if (a.isLong() && b.isLong()) {
                    if (a.longValue() == b.longValue()) {
                        return true;
                    }
                } else if (a.doubleValue() == b.doubleValue()) {
                    return true;
                }
            } else if (v instanceof StringValueExp && other instanceof StringValueExp) {
                String a = ((StringValueExp) v).getValue();
                String b = ((StringValueExp) other).getValue();
                if (a == null ? b == null : a.equals(b)) {
                    return true;
                }
            } else if (v instanceof BooleanValueExp && other instanceof BooleanValueExp) {
                if (((BooleanValueExp) v).getValue().equals(((BooleanValueExp) other).getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    public String toString() {
        return val + " in (" + listText() + ")";
    }

    private String listText() {
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
