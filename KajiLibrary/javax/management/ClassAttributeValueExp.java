package javax.management;

/**
 * The MBean's class name, used as if it were an attribute.
 *
 * <p>Package-private: it is made with {@link Query#classattr}. It is a virtual attribute --no MBean
 * declares one called {@code Class}-- and that is why it replaces the whole read and not just the
 * name.
 */
class ClassAttributeValueExp extends AttributeValueExp {

    private static final long serialVersionUID = -1081892073854801359L;

    /**
     * @serial always "Class"
     */
    private String attr;

    public ClassAttributeValueExp() {
        super("Class");
        attr = "Class";
    }

    public ValueExp apply(ObjectName name) throws BadStringOperationException,
            BadBinaryOpValueExpException, BadAttributeValueExpException,
            InvalidApplicationException {
        Object val = getValue(name);
        if (val == null) {
            return new StringValueExp(null);
        }
        if (val instanceof String) {
            return new StringValueExp((String) val);
        }
        throw new BadAttributeValueExpException(val);
    }

    public String toString() {
        return "Class";
    }

    /** The class name according to the agent. */
    protected Object getValue(ObjectName name) {
        try {
            MBeanServer server = QueryEval.getMBeanServer();
            if (server == null) {
                return null;
            }
            return server.getObjectInstance(name).getClassName();
        } catch (Exception e) {
            return null;
        }
    }
}
