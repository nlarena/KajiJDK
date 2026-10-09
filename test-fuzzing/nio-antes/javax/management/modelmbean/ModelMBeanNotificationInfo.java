package javax.management.modelmbean;

import javax.management.Descriptor;
import javax.management.DescriptorAccess;
import javax.management.MBeanNotificationInfo;
import javax.management.RuntimeOperationsException;

/**
 * KajiLibrary's javax.management.modelmbean.ModelMBeanNotificationInfo -- a notice of a model
 * MBean.
 *
 * <p>A notice's descriptor carries the <b>logging</b> fields: {@code log} says whether it is
 * kept, {@code logfile} where. In the specification that is what allows a notice to end up
 * written with nobody listening, which is exactly what is wanted of an audit trail.
 *
 * <p>{@link RequiredModelMBean} in this library carries those fields and <b>writes nothing</b>:
 * sending a notice reaches the registered listeners and no further. Whoever needs the trail
 * registers a listener that writes it.
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
public class ModelMBeanNotificationInfo extends MBeanNotificationInfo implements DescriptorAccess {

    private static final long serialVersionUID = -7445681389570207141L;

    /** The descriptor; never null. */
    private Descriptor modelDescriptor;

    /** Without a descriptor. */
    public ModelMBeanNotificationInfo(String[] notifTypes, String name, String description) {
        super(notifTypes, name, description);
    }

    /** With a descriptor. */
    public ModelMBeanNotificationInfo(String[] notifTypes, String name, String description,
                                      Descriptor descriptor) {
        super(notifTypes, name, description);
        setDescriptor(descriptor);
    }

    /** A copy. */
    public ModelMBeanNotificationInfo(ModelMBeanNotificationInfo inInfo) {
        super(inInfo.getNotifTypes(), inInfo.getName(), inInfo.getDescription());
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
                new IllegalArgumentException("Invalid descriptor passed to ModelMBeanNotificationInfo"));
        }
        this.modelDescriptor = (Descriptor) inDescriptor.clone();
    }

    /** A copy. */
    public Object clone() {
        return new ModelMBeanNotificationInfo(this);
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
            new Object[] {getName(), "notification", getName()});
    }
}
