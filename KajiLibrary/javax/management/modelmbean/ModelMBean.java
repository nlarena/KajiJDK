package javax.management.modelmbean;

import javax.management.DynamicMBean;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.PersistentMBean;
import javax.management.RuntimeOperationsException;

/**
 * KajiLibrary's javax.management.modelmbean.ModelMBean -- an MBean whose interface is declared at
 * run time.
 *
 * <p>It is the fourth kind of MBean, and the most flexible. The other three tie the management
 * interface to the code: a standard MBean takes it from a Java interface, a dynamic one declares
 * it in its {@code MBeanInfo}, an open one also restricts the types. A <b>model MBean</b> is
 * configured: it is given any object and a description of which of its methods to expose.
 *
 * <p>With that, a class that knows nothing about JMX and cannot be modified can be managed. It is
 * the case it exists for.
 *
 * <p>It joins three interfaces and none is redundant: {@link DynamicMBean} to answer queries,
 * {@link PersistentMBean} to be able to save and restore itself, and
 * {@link ModelMBeanNotificationBroadcaster} to send notices. The two methods of its own are the
 * ones that configure it: what to expose, and of what object.
 */
public interface ModelMBean
    extends DynamicMBean, PersistentMBean, ModelMBeanNotificationBroadcaster {

    /**
     * What is exposed.
     *
     * <p>It has to be called <b>before</b> registering the MBean in an agent: afterwards the
     * management interface has already been published, and changing it would leave the clients
     * looking at something that does not exist.
     *
     * @throws RuntimeOperationsException if it is null
     */
    void setModelMBeanInfo(ModelMBeanInfo inModelMBeanInfo)
        throws MBeanException, RuntimeOperationsException;

    /**
     * Of what object.
     *
     * @param mr the object to manage
     * @param mr_type the reference type; see {@link InvalidTargetObjectTypeException}
     * @throws InvalidTargetObjectTypeException if that type is not supported
     */
    void setManagedResource(Object mr, String mr_type)
        throws MBeanException, RuntimeOperationsException, InstanceNotFoundException,
               InvalidTargetObjectTypeException;
}
