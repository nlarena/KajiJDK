package javax.management;

import java.lang.reflect.Constructor;
import java.util.Arrays;

/**
 * A public constructor of the MBean.
 *
 * <p>It is in the model because {@link MBeanServer#createMBean} allows creating an MBean
 * <b>inside</b> the agent passing arguments: the client picks the signature by looking at this
 * list. Without it there would be no way to know which constructors there are.
 */
public class MBeanConstructorInfo extends MBeanFeatureInfo implements Cloneable {

    static final long serialVersionUID = 4433990064191844427L;

    static final MBeanConstructorInfo[] NO_CONSTRUCTORS = new MBeanConstructorInfo[0];

    /**
     * @serial the parameters
     */
    private final MBeanParameterInfo[] signature;

    /**
     * The <b>name</b> comes from reflection --it is the class name-- and the argument passed is the
     * description. The order inverted with respect to the other constructors is confusing, and it
     * is like that in the JDK.
     */
    public MBeanConstructorInfo(String description, Constructor<?> constructor) {
        this(constructor.getName(), description, signatureOf(constructor), null);
    }

    public MBeanConstructorInfo(String name, String description,
                                MBeanParameterInfo[] signature) {
        this(name, description, signature, null);
    }

    public MBeanConstructorInfo(String name, String description,
                                MBeanParameterInfo[] signature, Descriptor descriptor) {
        super(name, description, descriptor);
        this.signature = signature == null || signature.length == 0
                ? MBeanParameterInfo.NO_PARAMS : copy(signature);
    }

    private static MBeanParameterInfo[] copy(MBeanParameterInfo[] s) {
        MBeanParameterInfo[] r = new MBeanParameterInfo[s.length];
        System.arraycopy(s, 0, r, 0, s.length);
        return r;
    }

    private static MBeanParameterInfo[] signatureOf(Constructor<?> c) {
        Class<?>[] p = c.getParameterTypes();
        MBeanParameterInfo[] r = new MBeanParameterInfo[p.length];
        for (int i = 0; i < p.length; i++) {
            r[i] = new MBeanParameterInfo("p" + (i + 1), p[i].getName(), "");
        }
        return r;
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

    /** A fresh copy on every call: the internal array is not lent out. */
    public MBeanParameterInfo[] getSignature() {
        return copy(signature);
    }

    public String toString() {
        return getClass().getName() + "[description=" + getDescription() + ", name=" + getName()
                + ", signature=" + MBeanInfo.asText(signature)
                + ", descriptor=" + getDescriptor() + "]";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MBeanConstructorInfo)) {
            return false;
        }
        MBeanConstructorInfo p = (MBeanConstructorInfo) o;
        return same(p.getName(), getName())
                && same(p.getDescription(), getDescription())
                && p.getDescriptor().equals(getDescriptor())
                && Arrays.equals(p.signature, signature);
    }

    public int hashCode() {
        return getName().hashCode() ^ Arrays.hashCode(signature);
    }
}
