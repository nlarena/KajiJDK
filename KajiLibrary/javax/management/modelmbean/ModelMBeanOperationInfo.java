package javax.management.modelmbean;

import java.lang.reflect.Method;
import javax.management.Descriptor;
import javax.management.DescriptorAccess;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.RuntimeOperationsException;

/**
 * KajiLibrary's javax.management.modelmbean.ModelMBeanOperationInfo -- an operation of a model
 * MBean.
 *
 * <p>Its descriptor carries {@code targetObject} and {@code targetType}: <b>on what object</b>
 * it is invoked. In the specification that is what allows a model MBean to expose operations
 * of several different objects as if they were its own, which is what it is used for when a
 * whole subsystem is managed with a single MBean.
 *
 * <p>{@link RequiredModelMBean} in this library does not read those two fields: it always
 * invokes on the managed resource given to {@code setManagedResource}. One MBean per object
 * is the way to get the same thing here.
 *
 * <h2>The descriptor is mutable, and the {@code Info} stops being so</h2>
 *
 * <p>{@code MBeanFeatureInfo} is immutable on purpose: it is what an agent publishes and what the
 * clients keep. This subclass adds {@link #setDescriptor}, which breaks that.
 *
 * <p>It is right that it breaks it --a model MBean is configured at run time and for that the
 * descriptor has to be changeable-- and it has to be known: changing the descriptor of an
 * {@code Info} that was already published changes what the clients who kept it see.
 *
 * <p>That is why {@link #getDescriptor} returns a <b>copy</b>: reading it gives no way of writing
 * it.
 *
 * <p>What {@link #setDescriptor} checks is only {@link Descriptor#isValid}: a {@code name} and a
 * {@code descriptorType}, both with a value. The JDK also demands that the type be the one that
 * matches the feature; here a descriptor of the wrong type goes through.
 */
public class ModelMBeanOperationInfo extends MBeanOperationInfo implements DescriptorAccess {

    private static final long serialVersionUID = 6532732096650090465L;

    /** The descriptor; never null. */
    private Descriptor modelDescriptor;

    /** From the real method, without a descriptor. */
    public ModelMBeanOperationInfo(String description, Method operationMethod) {
        super(description, operationMethod);
    }

    /** The same, with a descriptor. */
    public ModelMBeanOperationInfo(String description, Method operationMethod,
                                   Descriptor descriptor) {
        super(description, operationMethod);
        setDescriptor(descriptor);
    }

    /** Declaring the signature by hand. */
    public ModelMBeanOperationInfo(String name, String description, MBeanParameterInfo[] signature,
                                   String type, int impact) {
        super(name, description, signature, type, impact);
    }

    /** The same, with a descriptor. */
    public ModelMBeanOperationInfo(String name, String description, MBeanParameterInfo[] signature,
                                   String type, int impact, Descriptor descriptor) {
        super(name, description, signature, type, impact);
        setDescriptor(descriptor);
    }

    /** A copy. */
    public ModelMBeanOperationInfo(ModelMBeanOperationInfo inInfo) {
        super(inInfo.getName(), inInfo.getDescription(), inInfo.getSignature(),
            inInfo.getReturnType(), inInfo.getImpact());
        setDescriptor(inInfo.getDescriptor());
    }


    /** A copy of the descriptor. See the class note. */
    public Descriptor getDescriptor() {
        if (this.modelDescriptor == null) {
            this.modelDescriptor = defaultDescriptor();
        }
        return (Descriptor) this.modelDescriptor.clone();
    }

    /**
     * Replaces it.
     *
     * @param inDescriptor {@code null} goes back to the default descriptor
     * @throws RuntimeOperationsException if the descriptor is not {@link Descriptor#isValid
     *     valid}
     */
    public void setDescriptor(Descriptor inDescriptor) {
        if (inDescriptor == null) {
            this.modelDescriptor = defaultDescriptor();
            return;
        }
        if (!inDescriptor.isValid()) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("Invalid descriptor passed to ModelMBeanOperationInfo"));
        }
        this.modelDescriptor = (Descriptor) inDescriptor.clone();
    }

    /** A copy. */
    public Object clone() {
        return new ModelMBeanOperationInfo(this);
    }

    /** The name, the description and the descriptor. */
    public String toString() {
        return getClass().getName() + "(name=" + getName() + ",descriptor=" + getDescriptor() + ")";
    }

    /**
     * The default descriptor: name, type and {@code displayName}.
     *
     * <p>The three fields are the ones {@code isValid} demands plus the one every tool shows.
     * Without them a freshly built {@code Info} would have an invalid descriptor, which is
     * exactly what {@link #setDescriptor} rejects.
     */
    private Descriptor defaultDescriptor() {
        return new DescriptorSupport(new String[] {"name", "descriptorType", "displayName"},
            new Object[] {getName(), "operation", getName()});
    }
}
