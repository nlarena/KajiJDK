package javax.management;

/**
 * Una constante de cadena dentro de una consulta.
 *
 * <p>Su {@link #toString()} la imprime entre comillas simples y **duplica** las que lleve adentro,
 * a la manera de SQL. No es decoracion: la representacion textual de una consulta tiene que poder
 * volver a leerse, y sin duplicarlas una comilla en el dato cerraria la cadena antes de tiempo.
 */
public class StringValueExp implements ValueExp {

    private static final long serialVersionUID = -3256390509806284044L;

    /**
     * @serial el valor
     */
    private String val;

    /** Sin valor; solo para deserializar. */
    public StringValueExp() {
    }

    public StringValueExp(String val) {
        this.val = val;
    }

    /** El valor. */
    public String getValue() {
        return val;
    }

    /** Entre comillas simples, con las internas duplicadas. */
    public String toString() {
        if (val == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder("'");
        for (int i = 0; i < val.length(); i++) {
            char c = val.charAt(i);
            if (c == '\'') {
                b.append("''");
            } else {
                b.append(c);
            }
        }
        return b.append('\'').toString();
    }

    /** No hace nada: una constante no consulta a nadie. */
    @Deprecated
    public void setMBeanServer(MBeanServer s) {
    }

    /** Se devuelve a si misma: una constante ya esta evaluada. */
    public ValueExp apply(ObjectName name) throws BadStringOperationException,
            BadBinaryOpValueExpException, BadAttributeValueExpException,
            InvalidApplicationException {
        return this;
    }
}
