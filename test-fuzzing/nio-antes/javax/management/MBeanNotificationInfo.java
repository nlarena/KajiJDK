package javax.management;

import java.util.Arrays;

/**
 * A notification class the MBean may emit.
 *
 * <p>Careful with the order of the arguments: {@code name} is the name of the notification's
 * <b>Java class</b> --almost always {@code javax.management.Notification}-- and the <b>types</b>,
 * which are the dotted strings one filters by, go in the first argument. They get mixed up all the
 * time because the useful name for a client is the type, not the class.
 */
public class MBeanNotificationInfo extends MBeanFeatureInfo implements Cloneable {

    static final long serialVersionUID = -3888371564530107064L;

    private static final String[] NO_TYPES = new String[0];

    static final MBeanNotificationInfo[] NO_NOTIFICATIONS = new MBeanNotificationInfo[0];

    /**
     * @serial the types that can be emitted
     */
    private String[] types;

    /**
     * @param notifTypes the types, with the dotted convention
     * @param name the name of the notification's Java class
     */
    public MBeanNotificationInfo(String[] notifTypes, String name, String description) {
        this(notifTypes, name, description, null);
    }

    public MBeanNotificationInfo(String[] notifTypes, String name, String description,
                                 Descriptor descriptor) {
        super(name, description, descriptor);
        this.types = notifTypes == null || notifTypes.length == 0 ? NO_TYPES : copy(notifTypes);
    }

    private static String[] copy(String[] s) {
        String[] r = new String[s.length];
        System.arraycopy(s, 0, r, 0, s.length);
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

    /** The types, copied. */
    public String[] getNotifTypes() {
        return copy(types);
    }

    public String toString() {
        return getClass().getName() + "[description=" + getDescription() + ", name=" + getName()
                + ", notifTypes=" + MBeanInfo.asText(types)
                + ", descriptor=" + getDescriptor() + "]";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MBeanNotificationInfo)) {
            return false;
        }
        MBeanNotificationInfo p = (MBeanNotificationInfo) o;
        return same(p.getName(), getName())
                && same(p.getDescription(), getDescription())
                && p.getDescriptor().equals(getDescriptor())
                && Arrays.equals(p.types, types);
    }

    public int hashCode() {
        int h = getName().hashCode();
        for (int i = 0; i < types.length; i++) {
            h ^= types[i].hashCode();
        }
        return h;
    }
}
