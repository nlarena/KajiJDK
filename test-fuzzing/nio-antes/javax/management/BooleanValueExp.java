package javax.management;

/** A boolean constant. Package-private, as in the JDK: it is made with {@link Query#value}. */
class BooleanValueExp extends QueryEval implements ValueExp {

    private static final long serialVersionUID = 7754922052666594581L;

    /**
     * @serial the value
     */
    private boolean val;

    BooleanValueExp(boolean val) {
        this.val = val;
    }

    BooleanValueExp(Boolean val) {
        this.val = val.booleanValue();
    }

    /** The value, wrapped. */
    public Boolean getValue() {
        return Boolean.valueOf(val);
    }

    public String toString() {
        return String.valueOf(val);
    }

    /** Returns itself. */
    public ValueExp apply(ObjectName name) throws BadStringOperationException,
            BadBinaryOpValueExpException, BadAttributeValueExpException,
            InvalidApplicationException {
        return this;
    }

    public void setMBeanServer(MBeanServer s) {
    }
}
