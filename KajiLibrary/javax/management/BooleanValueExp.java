package javax.management;

/** Una constante booleana. De paquete, igual que en el JDK: se fabrica con {@link Query#value}. */
class BooleanValueExp extends QueryEval implements ValueExp {

    private static final long serialVersionUID = 7754922052666594581L;

    /**
     * @serial el valor
     */
    private boolean val;

    BooleanValueExp(boolean val) {
        this.val = val;
    }

    BooleanValueExp(Boolean val) {
        this.val = val.booleanValue();
    }

    /** El valor, envuelto. */
    public Boolean getValue() {
        return Boolean.valueOf(val);
    }

    public String toString() {
        return String.valueOf(val);
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
