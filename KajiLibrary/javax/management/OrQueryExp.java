package javax.management;

/** Disyuncion. De paquete: se fabrica con {@link Query#or}. */
class OrQueryExp extends QueryEval implements QueryExp {

    private static final long serialVersionUID = 2962973084421716523L;

    /**
     * @serial la primera
     */
    private QueryExp exp1;

    /**
     * @serial la segunda
     */
    private QueryExp exp2;

    public OrQueryExp() {
    }

    public OrQueryExp(QueryExp q1, QueryExp q2) {
        exp1 = q1;
        exp2 = q2;
    }

    public QueryExp getLeftExp() {
        return exp1;
    }

    public QueryExp getRightExp() {
        return exp2;
    }

    /** En corto: si la primera es verdadera, la segunda no se evalua. */
    public boolean apply(ObjectName name) throws BadStringOperationException,
            BadBinaryOpValueExpException, BadAttributeValueExpException,
            InvalidApplicationException {
        return exp1.apply(name) || exp2.apply(name);
    }

    public String toString() {
        return "(" + exp1 + ") or (" + exp2 + ")";
    }
}
