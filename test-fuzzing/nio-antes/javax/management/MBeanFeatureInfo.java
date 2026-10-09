package javax.management;

import java.io.Serializable;

/**
 * What all the pieces of an {@link MBeanInfo} share: a name, a description and a
 * {@link Descriptor}.
 *
 * <p>The description is for reading, not for programming: nothing in JMX interprets it. The name is
 * significant -- it is what the attribute is asked for or the operation invoked by.
 */
public class MBeanFeatureInfo implements Serializable, DescriptorRead {

    static final long serialVersionUID = 3952882688968447265L;

    /**
     * @serial the name
     */
    protected String name;

    /**
     * @serial the text to read
     */
    protected String description;

    private transient Descriptor descriptor;

    public MBeanFeatureInfo(String name, String description) {
        this(name, description, null);
    }

    /**
     * A null {@code descriptor} is kept as the empty one: {@link #getDescriptor()} never gives
     * {@code null}.
     */
    public MBeanFeatureInfo(String name, String description, Descriptor descriptor) {
        this.name = name;
        this.description = description;
        this.descriptor = descriptor == null ? ImmutableDescriptor.EMPTY_DESCRIPTOR : descriptor;
    }

    /** The name. */
    public String getName() {
        return name;
    }

    /** The text to read. */
    public String getDescription() {
        return description;
    }

    /** Never {@code null}. */
    public Descriptor getDescriptor() {
        return descriptor == null ? ImmutableDescriptor.EMPTY_DESCRIPTOR : descriptor;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MBeanFeatureInfo)) {
            return false;
        }
        MBeanFeatureInfo p = (MBeanFeatureInfo) o;
        return same(p.getName(), getName())
                && same(p.getDescription(), getDescription())
                && p.getDescriptor().equals(getDescriptor());
    }

    static boolean same(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    public int hashCode() {
        return getName().hashCode() ^ getDescription().hashCode();
    }
}
