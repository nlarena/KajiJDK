package javax.management;

/**
 * El valor de un atributo del MBean que se esta evaluando.
 *
 * <p>Es la unica expresion que necesita hablar con el agente, y por eso es donde una consulta pasa
 * de ser texto a ser una pregunta real. {@link #getAttribute} esta separado y es `protected` justo
 * para eso: una subclase puede cambiar de donde sale el valor sin tocar la conversion.
 *
 * <p>La conversion es cerrada: numero, cadena o booleano. Cualquier otra cosa da
 * {@link BadAttributeValueExpException}, porque el algebra de consultas no sabe comparar mas que
 * esos tres.
 */
public class AttributeValueExp implements ValueExp {

    private static final long serialVersionUID = -7768025046539163385L;

    /**
     * @serial el nombre del atributo
     */
    private String attr;

    /** Sin nombre; solo para deserializar. */
    @Deprecated
    public AttributeValueExp() {
    }

    public AttributeValueExp(String attr) {
        this.attr = attr;
    }

    /** El nombre del atributo. */
    public String getAttributeName() {
        return attr;
    }

    /**
     * Lee el atributo y lo envuelve en la constante que corresponda.
     *
     * @throws BadAttributeValueExpException si el valor no es numero, cadena ni booleano
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

    /** El nombre del atributo, pelado. */
    public String toString() {
        return attr;
    }

    /** No hace nada: el servidor lo lleva {@link QueryEval}. */
    @Deprecated
    public void setMBeanServer(MBeanServer s) {
    }

    /**
     * De donde sale el valor.
     *
     * <p>Es `protected` para que una subclase pueda reemplazar la fuente; devuelve `null` ante
     * cualquier falla porque una consulta que no puede leer un atributo tiene que dar "no coincide",
     * no romper el barrido entero.
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
