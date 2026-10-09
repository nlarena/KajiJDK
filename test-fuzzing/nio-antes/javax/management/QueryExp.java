package javax.management;

import java.io.Serializable;

/**
 * A boolean expression evaluated against an MBean: the filter of
 * {@code queryNames}/{@code queryMBeans}.
 *
 * <p>The four {@code throws} of {@link #apply} are not noise, they are the contract: a query can
 * fail for four different reasons --unknown string operation, badly applied binary operator,
 * incomparable attribute value, MBean of the wrong class-- and whoever evaluates it has to be able
 * to tell them apart to decide whether the MBean simply does not match or the query is badly built.
 *
 * <p>It is {@code Serializable} because a query travels to the remote agent and is evaluated
 * <b>there</b>, not here.
 */
public interface QueryExp extends Serializable {

    /**
     * Whether the MBean called {@code name} satisfies the expression.
     *
     * <p>The expressions that need to read attributes do so against the server {@link
     * #setMBeanServer} gave them; those that only look at the name --{@link ObjectName} itself--
     * ignore it.
     */
    boolean apply(ObjectName name)
            throws BadStringOperationException, BadBinaryOpValueExpException,
                   BadAttributeValueExpException, InvalidApplicationException;

    /**
     * Sets the server the attributes are resolved against.
     *
     * <p>It was marked deprecated in the JDK: the server travels today through a {@code
     * ThreadLocal} of {@link QueryEval}, and calling this is not needed.
     *
     * @deprecated the server is carried by {@link QueryEval}
     */
    @Deprecated
    void setMBeanServer(MBeanServer s);
}
