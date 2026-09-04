package javax.management;

/** Negacion. De paquete: se fabrica con {@link Query#not}. */
class NotQueryExp extends QueryEval implements QueryExp {

    private static final long serialVersionUID = 5269643775896723397L;

    /**
     * @serial la negada
     */
    private QueryExp exp;

    public NotQueryExp() {
    }

    public NotQueryExp(QueryExp q) {
        exp = q;
    }

    public QueryExp getNegatedExp() {
        return exp;
    }

    public boolean apply(ObjectName name) throws BadStringOperationException,
            BadBinaryOpValueExpException, BadAttributeValueExpException,
            InvalidApplicationException {
        return !exp.apply(name);
    }

    public String toString() {
        return "not (" + exp + ")";
    }
}
