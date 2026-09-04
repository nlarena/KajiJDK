package javax.management;

/**
 * "El MBean es de esta clase o de una subclase."
 *
 * <p>De paquete: se fabrica con {@link Query#isInstanceOf}. Es la unica consulta que le pregunta al
 * agente por el <b>tipo</b> y no por un atributo.
 */
class InstanceOfQueryExp extends QueryEval implements QueryExp {

    private static final long serialVersionUID = -1081892073854801359L;

    /**
     * @serial el nombre de la clase
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

    /** Sin servidor en el hilo no hay tipo que consultar, y la respuesta es "no coincide". */
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
