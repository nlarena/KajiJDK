package javax.management.openmbean;

import javax.management.MBeanParameterInfo;

/**
 * The description of a constructor of an open MBean.
 *
 * <p>{@code getSignature} returns {@code MBeanParameterInfo[]} and not
 * {@code OpenMBeanParameterInfo[]}, which is what one would expect here. It is not a JDK oversight:
 * it is the type inherited from {@code MBeanConstructorInfo}, and changing it would break
 * compatibility. The elements <b>are</b> {@code OpenMBeanParameterInfo}, so the cast is safe; what
 * there is no way to do is say so in the signature.
 */
public interface OpenMBeanConstructorInfo {

    /** The description, for a person. */
    String getDescription();

    /** The constructor's name. */
    String getName();

    /** The parameters. See the class note about their type. */
    MBeanParameterInfo[] getSignature();

    boolean equals(Object obj);

    int hashCode();

    String toString();
}
