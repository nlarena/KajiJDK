package javax.management;

/**
 * "The MBean is of this class or of a subclass."
 *
 * <p>Package-private: it is made with {@link Query#isInstanceOf}. It is the only query that asks
 * the agent about the <b>type</b> and not about an attribute.
 */
class InstanceOfQueryExp extends QueryEval implements QueryExp {

    private static final long serialVersionUID = -1081892073854801359L;

    /**
     * @serial the class name
     */
    private StringValueExp classNameValue;

    public InstanceOfQueryExp(StringValueExp classNameValue) {
        if (classNameValue == null) {
            throw new IllegalArgumentException("Null class name.");
        }
        this.classNameValue = classNameValue;
    }

    public StringValueExp getClassNameValue() {
        return classNameValue;
    }

    /** Without a server on the thread there is no type to query, and the answer is "no match". */
    public boolean apply(ObjectName name) throws BadStringOperationException,
            BadBinaryOpValueExpException, BadAttributeValueExpException,
            InvalidApplicationException {
        MBeanServer server = QueryEval.getMBeanServer();
        if (server == null) {
            return false;
        }
        StringValueExp val = (StringValueExp) classNameValue.apply(name);
        try {
            return server.isInstanceOf(name, val.getValue());
        } catch (InstanceNotFoundException e) {
            return false;
        }
    }

    public String toString() {
        return "InstanceOf " + classNameValue.toString();
    }
}
