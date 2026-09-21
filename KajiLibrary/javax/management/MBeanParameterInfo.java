package javax.management;

/**
 * A parameter of an operation or of a constructor.
 *
 * <p>The type is a <b>string</b> with the class name, not a {@code Class}. It is on purpose: a
 * remote client describes MBeans whose classes it has not loaded, and a {@code Class} would force
 * loading them just to read the metadata.
 */
public class MBeanParameterInfo extends MBeanFeatureInfo implements Cloneable {

    static final long serialVersionUID = 7432616882776782338L;

    static final MBeanParameterInfo[] NO_PARAMS = new MBeanParameterInfo[0];

    /**
     * @serial the class name of the parameter
     */
    private final String type;

    public MBeanParameterInfo(String name, String type, String description) {
        this(name, type, description, null);
    }

    public MBeanParameterInfo(String name, String type, String description,
                              Descriptor descriptor) {
        super(name, description, descriptor);
        this.type = type;
    }

    /**
     * Shallow copy.
     *
     * <p>It does not return {@code this} even though the class is immutable: it was checked against
     * the JDK and there the copy is a <b>different</b> object. Equal by {@code equals}, different
     * by identity.
     *
     * <p>It swallows the {@code CloneNotSupportedException} and returns {@code null} instead of
     * propagating it, as the JDK does: the class implements {@code Cloneable}, so it cannot happen,
     * and declaring it would force every caller to catch it.
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    /** The class name of the parameter. */
    public String getType() {
        return type;
    }

    public String toString() {
        return getClass().getName() + "[description=" + getDescription() + ", name=" + getName()
                + ", type=" + getType() + ", descriptor=" + getDescriptor() + "]";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MBeanParameterInfo)) {
            return false;
        }
        MBeanParameterInfo p = (MBeanParameterInfo) o;
        return same(p.getName(), getName())
                && same(p.getType(), getType())
                && same(p.getDescription(), getDescription())
                && p.getDescriptor().equals(getDescriptor());
    }

    public int hashCode() {
        return getName().hashCode() ^ getType().hashCode();
    }
}
