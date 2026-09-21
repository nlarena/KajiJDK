package javax.management.modelmbean;

import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.RuntimeOperationsException;

/**
 * KajiLibrary's javax.management.modelmbean.ModelMBeanInfo -- the description of a model MBean.
 *
 * <p>A plain {@code MBeanInfo} says <b>what</b> there is: attributes, operations, notices. This
 * one adds the {@link Descriptor}s, which say <b>how</b> each thing behaves: from which method
 * of the object an attribute comes, for how many seconds its value may be cached, whether it is
 * persisted and how often.
 *
 * <p>That second half is what turns a description into a configuration. Without the descriptors,
 * a model MBean would not know where to take an attribute's value from.
 *
 * <p>The descriptors are asked for by type --{@code "attribute"}, {@code "operation"},
 * {@code "notification"}, {@code "mbean"}, or null for all-- and that is the key to almost
 * every method here.
 */
public interface ModelMBeanInfo {

    /**
     * All the descriptors of that type.
     *
     * @param inDescriptorType {@code "mbean"}, {@code "attribute"}, {@code "operation"},
     *     {@code "constructor"}, {@code "notification"}, or null for all
     */
    Descriptor[] getDescriptors(String inDescriptorType)
        throws MBeanException, RuntimeOperationsException;

    /** Replaces them; each one goes where its {@code descriptorType} field says. */
    void setDescriptors(Descriptor[] inDescriptors)
        throws MBeanException, RuntimeOperationsException;

    /** The descriptor with that name and that type. */
    Descriptor getDescriptor(String inDescriptorName, String inDescriptorType)
        throws MBeanException, RuntimeOperationsException;

    /** Puts it or replaces it. */
    void setDescriptor(Descriptor inDescriptor, String inDescriptorType)
        throws MBeanException, RuntimeOperationsException;

    /** The descriptor of the whole MBean. */
    Descriptor getMBeanDescriptor() throws MBeanException, RuntimeOperationsException;

    /** Ver {@link #getMBeanDescriptor}. */
    void setMBeanDescriptor(Descriptor inDescriptor)
        throws MBeanException, RuntimeOperationsException;

    /** The attribute with that name. */
    ModelMBeanAttributeInfo getAttribute(String inName)
        throws MBeanException, RuntimeOperationsException;

    /** The operation with that name. */
    ModelMBeanOperationInfo getOperation(String inName)
        throws MBeanException, RuntimeOperationsException;

    /** The notice with that name. */
    ModelMBeanNotificationInfo getNotification(String inName)
        throws MBeanException, RuntimeOperationsException;

    /** A copy. */
    Object clone();

    /** The attributes. */
    MBeanAttributeInfo[] getAttributes();

    /** The MBean's class name. */
    String getClassName();

    /** The constructors. */
    MBeanConstructorInfo[] getConstructors();

    /** The description. */
    String getDescription();

    /** The notices. */
    MBeanNotificationInfo[] getNotifications();

    /** The operations. */
    MBeanOperationInfo[] getOperations();
}
