package javax.management;

/**
 * The value of an attribute of the MBean being evaluated.
 *
 * <p>It is the only expression that needs to talk to the agent, and that is why it is where a query
 * goes from being text to being a real question. {@link #getAttribute} is separate and
 * {@code protected} precisely for that: a subclass can change where the value comes from without
 * touching the conversion.
 *
 * <p>The conversion is closed: number, string or boolean. Anything else gives
 * {@link BadAttributeValueExpException}, because the query algebra only knows how to compare those
 * three.
 */
public class AttributeValueExp implements ValueExp {

    private static final long serialVersionUID = -7768025046539163385L;

    /**
     * @serial the attribute name
     */
    private String attr;

    /** Without a name; only for deserialization. */
    @Deprecated
    public AttributeValueExp() {
    }

    public AttributeValueExp(String attr) {
        this.attr = attr;
    }

    /** The attribute name. */
    public String getAttributeName() {
        return attr;
    }

    /**
     * Reads the attribute and wraps it in the matching constant.
     *
     * @throws BadAttributeValueExpException if the value is not a number, a string or a boolean
     */
    public ValueExp apply(ObjectName name) throws BadStringOperationException,
            BadBinaryOpValueExpException, BadAttributeValueExpException,
            InvalidApplicationException {
        Object result = getAttribute(name);
        if (result instanceof Number) {
            return new NumericValueExp((Number) result);
        }
        if (result instanceof String) {
            return new StringValueExp((String) result);
        }
        if (result instanceof Boolean) {
            return new BooleanValueExp((Boolean) result);
        }
        throw new BadAttributeValueExpException(result);
    }

    /** The attribute name, bare. */
    public String toString() {
        return attr;
    }

    /** Does nothing: the server is carried by {@link QueryEval}. */
    @Deprecated
    public void setMBeanServer(MBeanServer s) {
    }

    /**
     * Where the value comes from.
     *
     * <p>It is {@code protected} so that a subclass can replace the source; it returns {@code null}
     * on any failure because a query that cannot read an attribute has to give "no match", not
     * break the whole sweep.
     */
    protected Object getAttribute(ObjectName name) {
        try {
            MBeanServer server = QueryEval.getMBeanServer();
            if (server == null) {
                return null;
            }
            return server.getAttribute(name, attr);
        } catch (Exception e) {
            return null;
        }
    }
}
