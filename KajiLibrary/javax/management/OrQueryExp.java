package javax.management;

/** Disjunction. Package-private: it is made with {@link Query#or}. */
class OrQueryExp extends QueryEval implements QueryExp {

    private static final long serialVersionUID = 2962973084421716523L;

    /**
     * @serial the first one
     */
    private QueryExp exp1;

    /**
     * @serial the second one
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

    /** Short-circuit: if the first is true, the second is not evaluated. */
    public boolean apply(ObjectName name) throws BadStringOperationException,
            BadBinaryOpValueExpException, BadAttributeValueExpException,
            InvalidApplicationException {
        return exp1.apply(name) || exp2.apply(name);
    }

    public String toString() {
        return "(" + exp1 + ") or (" + exp2 + ")";
    }
}
