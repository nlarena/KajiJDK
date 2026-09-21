package javax.management.openmbean;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;

/**
 * The description of a whole open MBean.
 *
 * <p>It is the same content as an ordinary {@code MBeanInfo}, with the difference that its
 * attributes, operations and constructors are the open ones of this package. The return types are
 * still {@code javax.management}'s for the reason {@link OpenMBeanConstructorInfo} explains.
 *
 * <p>Notifications have <b>no</b> open version: a notification carries a {@code userData} of any
 * class, so there is nothing to restrict to open types.
 */
public interface OpenMBeanInfo {

    /** The MBean's class name. */
    String getClassName();

    /** The description, for a person. */
    String getDescription();

    /** The attributes; each is also an {@link OpenMBeanAttributeInfo}. */
    MBeanAttributeInfo[] getAttributes();

    /** The operations; each is also an {@link OpenMBeanOperationInfo}. */
    MBeanOperationInfo[] getOperations();

    /** The constructors; each is also an {@link OpenMBeanConstructorInfo}. */
    MBeanConstructorInfo[] getConstructors();

    /** The notifications. See the class note about why they are not open. */
    MBeanNotificationInfo[] getNotifications();

    boolean equals(Object obj);

    int hashCode();

    String toString();
}
