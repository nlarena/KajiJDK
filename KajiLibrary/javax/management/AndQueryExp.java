package javax.management;

/** Conjuncion. De paquete: se fabrica con {@link Query#and}. */
class AndQueryExp extends QueryEval implements QueryExp {

    private static final long serialVersionUID = -1081892073854801359L;

    /**
     * @serial la primera
     */
    private QueryExp exp1;

    /**
     * @serial la segunda
     */
    private QueryExp exp2;

    public AndQueryExp() {
    }

    public AndQueryExp(QueryExp q1, QueryExp q2) {
        exp1 = q1;
        exp2 = q2;
    }

    public QueryExp getLeftExp() {
        return exp1;
    }

    public QueryExp getRightExp() {
        return exp2;
    }

    /** En corto: si la primera es falsa, la segunda no se evalua ni se pregunta al agente. */
    public boolean apply(ObjectName name) throws BadStringOperationException,
            BadBinaryOpValueExpException, BadAttributeValueExpException,
            InvalidApplicationException {
        return exp1.apply(name) && exp2.apply(name);
    }

    public String toString() {
        return "(" + exp1 + ") and (" + exp2 + ")";
    }
}
