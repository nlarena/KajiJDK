package javax.management;

/**
 * El nombre de la clase del MBean, usado como si fuera un atributo.
 *
 * <p>De paquete: se fabrica con {@link Query#classattr}. Es un atributo virtual --ningun MBean
 * declara uno que se llame {@code Class}-- y por eso reemplaza la lectura entera y no solo el
 * nombre.
 */
class ClassAttributeValueExp extends AttributeValueExp {

    private static final long serialVersionUID = -1081892073854801359L;

    /**
     * @serial siempre "Class"
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

    /** El nombre de la clase segun el agente. */
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
