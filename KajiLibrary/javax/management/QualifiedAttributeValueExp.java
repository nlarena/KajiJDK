package javax.management;

/**
 * Un atributo que solo se lee si el MBean es de la clase indicada.
 *
 * <p>De paquete: se fabrica con {@code Query.attr(clase, atributo)}. Sirve para consultar un
 * dominio mezclado sin que dos MBeans distintos que casualmente tienen un atributo del mismo nombre
 * se confundan entre si.
 */
class QualifiedAttributeValueExp extends AttributeValueExp {

    private static final long serialVersionUID = 8832517277410933254L;

    /**
     * @serial la clase que califica
     */
    private String className;

    public QualifiedAttributeValueExp() {
    }

    public QualifiedAttributeValueExp(String className, String attr) {
        super(attr);
        this.className = className;
    }

    /** La clase que califica al atributo. */
    public String getAttrClassName() {
        return className;
    }

    /**
     * Si el MBean no es de esa clase, se tira {@link InvalidApplicationException}, que es
     * exactamente lo que esa excepcion significa: la consulta no aplica a este MBean.
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
