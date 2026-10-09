package javax.management;

/** Conjunction. Package-private: it is made with {@link Query#and}. */
class AndQueryExp extends QueryEval implements QueryExp {

    private static final long serialVersionUID = -1081892073854801359L;

    /**
     * @serial the first one
     */
    private QueryExp exp1;

    /**
     * @serial the second one
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

    /**
     * Short-circuit: if the first is false, the second is neither evaluated nor asked of the agent.
     */
    public boolean apply(ObjectName name) throws BadStringOperationException,
            BadBinaryOpValueExpException, BadAttributeValueExpException,
            InvalidApplicationException {
        return exp1.apply(name) && exp2.apply(name);
    }

    public String toString() {
        return "(" + exp1 + ") and (" + exp2 + ")";
    }
}
