package javax.management;

/**
 * A string constant inside a query.
 *
 * <p>Its {@link #toString()} prints it between single quotes and <b>doubles</b> the ones it carries
 * inside, SQL style. It is not decoration: the textual representation of a query has to be readable
 * back, and without doubling them a quote in the data would close the string too early.
 */
public class StringValueExp implements ValueExp {

    private static final long serialVersionUID = -3256390509806284044L;

    /**
     * @serial the value
     */
    private String val;

    /** Without a value; only for deserialization. */
    public StringValueExp() {
    }

    public StringValueExp(String val) {
        this.val = val;
    }

    /** The value. */
    public String getValue() {
        return val;
    }

    /** Between single quotes, with the inner ones doubled. */
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

    /** Does nothing: a constant asks nobody. */
    @Deprecated
    public void setMBeanServer(MBeanServer s) {
    }

    /** Returns itself: a constant is already evaluated. */
    public ValueExp apply(ObjectName name) throws BadStringOperationException,
            BadBinaryOpValueExpException, BadAttributeValueExpException,
            InvalidApplicationException {
        return this;
    }
}
