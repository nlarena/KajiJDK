package javax.management.modelmbean;

import java.lang.reflect.Constructor;
import javax.management.Descriptor;
import javax.management.DescriptorAccess;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanParameterInfo;
import javax.management.RuntimeOperationsException;

/**
 * KajiLibrary's javax.management.modelmbean.ModelMBeanConstructorInfo -- a constructor of a
 * model MBean.
 *
 * <p>Its descriptor type is {@code operation} and not {@code constructor}, which is surprising
 * and is what the specification says: for the model, constructing is one more operation. In
 * the JDK what distinguishes it is the descriptor's {@code role} field, with the value
 * {@code constructor}: it puts it in the default descriptor and rejects a descriptor whose
 * {@code role} says something else.
 *
 * <p>Here neither thing happens: the default descriptor built by this class carries
 * {@code name}, {@code descriptorType} and {@code displayName}, and nothing looks at
 * {@code role}.
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
public class ModelMBeanConstructorInfo extends MBeanConstructorInfo implements DescriptorAccess {

    private static final long serialVersionUID = 3862947819818064362L;

    /** The descriptor; never null. */
    private Descriptor modelDescriptor;

    /** From the real constructor, without a descriptor. */
    public ModelMBeanConstructorInfo(String description, Constructor<?> constructorElement) {
        super(description, constructorElement);
    }

    /** The same, with a descriptor. */
    public ModelMBeanConstructorInfo(String description, Constructor<?> constructorElement,
                                     Descriptor descriptor) {
        super(description, constructorElement);
        setDescriptor(descriptor);
    }

    /** Declaring the signature by hand. */
    public ModelMBeanConstructorInfo(String name, String description,
                                     MBeanParameterInfo[] signature) {
        super(name, description, signature);
    }

    /** The same, with a descriptor. */
    public ModelMBeanConstructorInfo(String name, String description,
                                     MBeanParameterInfo[] signature, Descriptor descriptor) {
        super(name, description, signature);
        setDescriptor(descriptor);
    }

    /** A copy. */
    public ModelMBeanConstructorInfo(ModelMBeanConstructorInfo inInfo) {
        super(inInfo.getName(), inInfo.getDescription(), inInfo.getSignature());
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
                new IllegalArgumentException("Invalid descriptor passed to ModelMBeanConstructorInfo"));
        }
        this.modelDescriptor = (Descriptor) inDescriptor.clone();
    }

    /** A copy. */
    public Object clone() {
        return new ModelMBeanConstructorInfo(this);
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
