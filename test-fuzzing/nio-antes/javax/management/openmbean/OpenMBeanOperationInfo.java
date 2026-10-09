package javax.management.openmbean;

import javax.management.MBeanParameterInfo;

/**
 * The description of an operation of an open MBean.
 *
 * <p>There are two ways of asking about what it returns and they are not redundant:
 * {@code getReturnType} gives the class name, which is what it inherits from
 * {@code MBeanOperationInfo}, and {@code getReturnOpenType} gives the open type, which is what this
 * package adds. The first can be deduced from the second but not the other way round.
 *
 * <p>About the type of {@code getSignature}, the same note as in
 * {@link OpenMBeanConstructorInfo} applies.
 */
public interface OpenMBeanOperationInfo {

    /** The description, for a person. */
    String getDescription();

    /** The operation's name. */
    String getName();

    /** The parameters. */
    MBeanParameterInfo[] getSignature();

    /** Whether it reads, writes, both, or is unknown. See {@code MBeanOperationInfo}. */
    int getImpact();

    /** The class name of what it returns. */
    String getReturnType();

    /** The open type of what it returns. */
    OpenType<?> getReturnOpenType();

    boolean equals(Object obj);

    int hashCode();

    String toString();
}
