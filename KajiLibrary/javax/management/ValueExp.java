package javax.management;

import java.io.Serializable;

/**
 * A value inside a query: a constant, an MBean attribute, or an operation between two.
 *
 * <p>The signature that defines it is {@link #apply}: it returns <b>another</b> {@code ValueExp},
 * not an {@code Object}. That is what makes an expression resolve in steps --{@code a + b} applies
 * to both sides, each returns a constant, and the sum returns a constant-- without ever leaving the
 * type.
 */
public interface ValueExp extends Serializable {

    /**
     * Resolves the expression for the given MBean and returns the value, already constant.
     */
    ValueExp apply(ObjectName name)
            throws BadStringOperationException, BadBinaryOpValueExpException,
                   BadAttributeValueExpException, InvalidApplicationException;

    /**
     * @deprecated the server is carried by {@link QueryEval}
     */
    @Deprecated
    void setMBeanServer(MBeanServer s);
}
