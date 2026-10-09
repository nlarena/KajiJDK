package javax.management;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * An operation the MBean exposes.
 *
 * <p>What sets it apart is {@link #getImpact()}: the model declares whether the operation
 * <b>reads</b> ({@link #INFO}), <b>modifies</b> ({@link #ACTION}) or both ({@link #ACTION_INFO}).
 * It is not decoration -- a console can offer the read-only ones without asking for confirmation
 * and ask for it for the others, and an automatic client can retry without fear an operation that
 * declares itself {@code INFO}. {@link #UNKNOWN} is what is left when nobody bothered to say.
 */
public class MBeanOperationInfo extends MBeanFeatureInfo implements Cloneable {

    static final long serialVersionUID = -6178860474881375330L;

    static final MBeanOperationInfo[] NO_OPERATIONS = new MBeanOperationInfo[0];

    /** The operation only reads: {@value}. */
    public static final int INFO = 0;

    /** The operation modifies the MBean: {@value}. */
    public static final int ACTION = 1;

    /** Modifies and also returns information: {@value}. */
    public static final int ACTION_INFO = 2;

    /** Not declared: {@value}. */
    public static final int UNKNOWN = 3;

    /**
     * @serial the name of the class it returns
     */
    private final String type;

    /**
     * @serial the parameters
     */
    private final MBeanParameterInfo[] signature;

    /**
     * @serial INFO, ACTION, ACTION_INFO or UNKNOWN
     */
    private final int impact;

    /**
     * From a method by reflection. The impact is left at {@link #UNKNOWN}: reflection can read the
     * signature but cannot know whether the method changes anything.
     */
    public MBeanOperationInfo(String description, Method method) {
        this(method.getName(), description, signatureOf(method),
             method.getReturnType().getName(),
             method.getReturnType() == Void.TYPE ? ACTION : UNKNOWN, null);
    }

    public MBeanOperationInfo(String name, String description, MBeanParameterInfo[] signature,
                              String type, int impact) {
        this(name, description, signature, type, impact, null);
    }

    public MBeanOperationInfo(String name, String description, MBeanParameterInfo[] signature,
                              String type, int impact, Descriptor descriptor) {
        super(name, description, descriptor);
        this.type = type;
        this.impact = impact;
        this.signature = signature == null || signature.length == 0
                ? MBeanParameterInfo.NO_PARAMS : copy(signature);
    }

    private static MBeanParameterInfo[] copy(MBeanParameterInfo[] s) {
        MBeanParameterInfo[] r = new MBeanParameterInfo[s.length];
        System.arraycopy(s, 0, r, 0, s.length);
        return r;
    }

    private static MBeanParameterInfo[] signatureOf(Method m) {
        Class<?>[] p = m.getParameterTypes();
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

    /** The name of the class the operation returns. */
    public String getReturnType() {
        return type;
    }

    /** A fresh copy on every call. */
    public MBeanParameterInfo[] getSignature() {
        return copy(signature);
    }

    /** {@link #INFO}, {@link #ACTION}, {@link #ACTION_INFO} or {@link #UNKNOWN}. */
    public int getImpact() {
        return impact;
    }

    public String toString() {
        String i;
        switch (impact) {
            case ACTION:
                i = "action";
                break;
            case ACTION_INFO:
                i = "action/info";
                break;
            case INFO:
                i = "info";
                break;
            case UNKNOWN:
                i = "unknown";
                break;
            default:
                i = "(" + impact + ")";
        }
        return getClass().getName() + "[description=" + getDescription() + ", name=" + getName()
                + ", returnType=" + getReturnType()
                + ", signature=" + MBeanInfo.asText(signature)
                + ", impact=" + i
                + ", descriptor=" + getDescriptor() + "]";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MBeanOperationInfo)) {
            return false;
        }
        MBeanOperationInfo p = (MBeanOperationInfo) o;
        return same(p.getName(), getName())
                && same(p.getReturnType(), getReturnType())
                && same(p.getDescription(), getDescription())
                && p.getImpact() == getImpact()
                && p.getDescriptor().equals(getDescriptor())
                && Arrays.equals(p.signature, signature);
    }

    public int hashCode() {
        return getName().hashCode() ^ getReturnType().hashCode();
    }
}
