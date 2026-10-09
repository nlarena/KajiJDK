package javax.management.modelmbean;

import java.lang.reflect.Method;
import javax.management.Descriptor;
import javax.management.DescriptorAccess;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.RuntimeOperationsException;

/**
 * KajiLibrary's javax.management.modelmbean.ModelMBeanAttributeInfo -- an attribute of a model
 * MBean.
 *
 * <p>An {@code MBeanAttributeInfo} with a descriptor. The descriptor is what says <b>where</b>
 * the value comes from: the {@code getMethod} and {@code setMethod} fields name the managed
 * object's methods, and {@code currencyTimeLimit} says for how many seconds the value may be
 * cached.
 *
 * <p>That caching is what makes the type useful in the specification: an expensive attribute
 * --a query, a disk read-- could be exposed without every console that looks at it firing it
 * again. {@link RequiredModelMBean} in this library <b>does not implement it</b>: it carries
 * the field in the descriptor and reads the attribute every time. Whoever exposes an
 * expensive attribute has to cache it on their own side.
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
public class ModelMBeanAttributeInfo extends MBeanAttributeInfo implements DescriptorAccess {

    private static final long serialVersionUID = 6181543027787327345L;

    /** The descriptor; never null. */
    private Descriptor modelDescriptor;

    /** From the accessor methods, without a descriptor. */
    public ModelMBeanAttributeInfo(String name, String description, Method getter, Method setter)
        throws IntrospectionException {
        super(name, description, getter, setter);
    }

    /** The same, with a descriptor. */
    public ModelMBeanAttributeInfo(String name, String description, Method getter, Method setter,
                                   Descriptor descriptor) throws IntrospectionException {
        super(name, description, getter, setter);
        setDescriptor(descriptor);
    }

    /** Declaring the type and the permissions by hand. */
    public ModelMBeanAttributeInfo(String name, String type, String description, boolean isReadable,
                                   boolean isWritable, boolean isIs) {
        super(name, type, description, isReadable, isWritable, isIs);
    }

    /** The same, with a descriptor. */
    public ModelMBeanAttributeInfo(String name, String type, String description, boolean isReadable,
                                   boolean isWritable, boolean isIs, Descriptor descriptor) {
        super(name, type, description, isReadable, isWritable, isIs);
        setDescriptor(descriptor);
    }

    /** A copy. */
    public ModelMBeanAttributeInfo(ModelMBeanAttributeInfo inInfo) {
        super(inInfo.getName(), inInfo.getType(), inInfo.getDescription(), inInfo.isReadable(),
            inInfo.isWritable(), inInfo.isIs());
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
                new IllegalArgumentException("Invalid descriptor passed to ModelMBeanAttributeInfo"));
        }
        this.modelDescriptor = (Descriptor) inDescriptor.clone();
    }

    /** A copy. */
    public Object clone() {
        return new ModelMBeanAttributeInfo(this);
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
            new Object[] {getName(), "attribute", getName()});
    }
}
