package javax.management;

/**
 * An attribute that is read only if the MBean is of the given class.
 *
 * <p>Package-private: it is made with {@code Query.attr(class, attribute)}. It serves to query a
 * mixed domain without two different MBeans that happen to have an attribute of the same name
 * getting confused with each other.
 */
class QualifiedAttributeValueExp extends AttributeValueExp {

    private static final long serialVersionUID = 8832517277410933254L;

    /**
     * @serial the qualifying class
     */
    private String className;

    public QualifiedAttributeValueExp() {
    }

    public QualifiedAttributeValueExp(String className, String attr) {
        super(attr);
        this.className = className;
    }

    /** The class that qualifies the attribute. */
    public String getAttrClassName() {
        return className;
    }

    /**
     * If the MBean is not of that class, {@link InvalidApplicationException} is thrown, which is
     * exactly what that exception means: the query does not apply to this MBean.
     */
    public ValueExp apply(ObjectName name) throws BadStringOperationException,
            BadBinaryOpValueExpException, BadAttributeValueExpException,
            InvalidApplicationException {
        MBeanServer server = QueryEval.getMBeanServer();
        if (server != null) {
            try {
                if (!server.isInstanceOf(name, className)) {
                    throw new InvalidApplicationException(className);
                }
            } catch (InstanceNotFoundException e) {
                throw new InvalidApplicationException(className);
            }
        }
        return super.apply(name);
    }

    public String toString() {
        return className + "." + super.toString();
    }
}
