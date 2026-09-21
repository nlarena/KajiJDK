package javax.management;

import java.lang.reflect.Method;

/**
 * An attribute declared by an MBean: name, type and what can be done with it.
 *
 * <p>Three booleans and not two, which is what surprises. Besides readable and writable there is
 * {@link #isIs()}, which says whether the accessor is called {@code isX} instead of {@code getX}.
 * It only makes sense for {@code boolean}, and it is there because a standard MBean is discovered
 * by reflection over method names: without that bit, the server would not know how to call back.
 *
 * <p>An attribute with readable and writable both {@code false} is legal, if useless: it describes
 * something the MBean declares and does not let be touched.
 */
public class MBeanAttributeInfo extends MBeanFeatureInfo implements Cloneable {

    static final long serialVersionUID = 8644704819898565848L;

    static final MBeanAttributeInfo[] NO_ATTRIBUTES = new MBeanAttributeInfo[0];

    /**
     * @serial the class name of the attribute
     */
    private final String attributeType;

    /**
     * @serial whether it can be written
     */
    private final boolean isWrite;

    /**
     * @serial whether it can be read
     */
    private final boolean isRead;

    /**
     * @serial whether the read accessor is called isX
     */
    private final boolean is;

    public MBeanAttributeInfo(String name, String type, String description,
                              boolean isReadable, boolean isWritable, boolean isIs) {
        this(name, type, description, isReadable, isWritable, isIs, null);
    }

    public MBeanAttributeInfo(String name, String type, String description,
                              boolean isReadable, boolean isWritable, boolean isIs,
                              Descriptor descriptor) {
        super(name, description, descriptor);
        this.attributeType = type;
        this.isRead = isReadable;
        this.isWrite = isWritable;
        this.is = isIs;
    }

    /**
     * Deduces everything from the two accessor methods.
     *
     * <p>Either may be {@code null}: that is what makes an attribute read-only or write-only.
     *
     * @throws IntrospectionException if the two methods do not talk about the same type
     */
    public MBeanAttributeInfo(String name, String description, Method getter, Method setter)
            throws IntrospectionException {
        this(name, typeOf(getter, setter), description,
             getter != null, setter != null, isIsGetter(getter));
    }

    private static boolean isIsGetter(Method getter) {
        return getter != null && getter.getName().startsWith("is");
    }

    private static String typeOf(Method getter, Method setter) throws IntrospectionException {
        String fromGetter = null;
        if (getter != null) {
            if (getter.getParameterTypes().length != 0) {
                throw new IntrospectionException("bad getter arg count");
            }
            Class<?> r = getter.getReturnType();
            if (r == Void.TYPE) {
                throw new IntrospectionException("getter returns void");
            }
            fromGetter = r.getName();
        }
        String fromSetter = null;
        if (setter != null) {
            Class<?>[] p = setter.getParameterTypes();
            if (p.length != 1) {
                throw new IntrospectionException("bad setter arg count");
            }
            fromSetter = p[0].getName();
        }
        if (fromGetter == null && fromSetter == null) {
            throw new IntrospectionException("getter and setter cannot both be null");
        }
        if (fromGetter != null && fromSetter != null && !fromGetter.equals(fromSetter)) {
            throw new IntrospectionException("type mismatch between getter and setter");
        }
        return fromGetter != null ? fromGetter : fromSetter;
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

    /** The class name of the attribute. */
    public String getType() {
        return attributeType;
    }

    /** Whether it can be read. */
    public boolean isReadable() {
        return isRead;
    }

    /** Whether it can be written. */
    public boolean isWritable() {
        return isWrite;
    }

    /** Whether the read accessor is called {@code isX} instead of {@code getX}. */
    public boolean isIs() {
        return is;
    }

    /**
     * The access is printed as {@code read-only}, {@code write-only}, {@code read/write} or
     * {@code no-access}, and the {@code isIs} bit adds one more comma.
     */
    public String toString() {
        String access;
        if (isReadable()) {
            access = isWritable() ? "read/write" : "read-only";
        } else {
            access = isWritable() ? "write-only" : "no-access";
        }
        return getClass().getName() + "[description=" + getDescription() + ", name=" + getName()
                + ", type=" + getType() + ", " + access + (isIs() ? ", isIs" : "")
                + ", descriptor=" + getDescriptor() + "]";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MBeanAttributeInfo)) {
            return false;
        }
        MBeanAttributeInfo p = (MBeanAttributeInfo) o;
        return same(p.getName(), getName())
                && same(p.getType(), getType())
                && same(p.getDescription(), getDescription())
                && p.getDescriptor().equals(getDescriptor())
                && p.isReadable() == isReadable()
                && p.isWritable() == isWritable()
                && p.isIs() == isIs();
    }

    public int hashCode() {
        return getName().hashCode() ^ getType().hashCode();
    }
}
