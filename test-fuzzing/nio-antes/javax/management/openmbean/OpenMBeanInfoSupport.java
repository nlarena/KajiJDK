package javax.management.openmbean;

import java.util.Arrays;
import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;

/**
 * The implementation of {@link OpenMBeanInfo}.
 *
 * <p>It converts the three open arrays to {@code javax.management}'s to pass them to
 * {@code super}. The objects are the same: every useful implementation of
 * {@code OpenMBeanAttributeInfo} extends {@code MBeanAttributeInfo}, and the same for the other
 * two. The only thing that changes is the array's type.
 *
 * <p>A {@code null} in any of the arrays is taken as "none", as in {@code MBeanInfo}. It differs
 * from an empty array only in the caller's intention; the result is the same.
 */
public class OpenMBeanInfoSupport extends MBeanInfo implements OpenMBeanInfo {

    private static final long serialVersionUID = 4349395935420511492L;

    private transient int hash;

    /** An open MBean with those members. */
    public OpenMBeanInfoSupport(String className, String description,
            OpenMBeanAttributeInfo[] openAttributes, OpenMBeanConstructorInfo[] openConstructors,
            OpenMBeanOperationInfo[] openOperations, MBeanNotificationInfo[] notifications) {
        this(className, description, openAttributes, openConstructors, openOperations,
                notifications, null);
    }

    /** The same, with that descriptor. */
    public OpenMBeanInfoSupport(String className, String description,
            OpenMBeanAttributeInfo[] openAttributes, OpenMBeanConstructorInfo[] openConstructors,
            OpenMBeanOperationInfo[] openOperations, MBeanNotificationInfo[] notifications,
            Descriptor descriptor) {
        super(className, description, asAttributes(openAttributes),
                asConstructors(openConstructors), asOperations(openOperations), notifications,
                descriptor);
    }

    private static MBeanAttributeInfo[] asAttributes(OpenMBeanAttributeInfo[] src) {
        if (src == null || src.length == 0) {
            return new MBeanAttributeInfo[0];
        }
        MBeanAttributeInfo[] out = new MBeanAttributeInfo[src.length];
        for (int i = 0; i < src.length; i++) {
            out[i] = (MBeanAttributeInfo) src[i];
        }
        return out;
    }

    private static MBeanConstructorInfo[] asConstructors(OpenMBeanConstructorInfo[] src) {
        if (src == null || src.length == 0) {
            return new MBeanConstructorInfo[0];
        }
        MBeanConstructorInfo[] out = new MBeanConstructorInfo[src.length];
        for (int i = 0; i < src.length; i++) {
            out[i] = (MBeanConstructorInfo) src[i];
        }
        return out;
    }

    private static MBeanOperationInfo[] asOperations(OpenMBeanOperationInfo[] src) {
        if (src == null || src.length == 0) {
            return new MBeanOperationInfo[0];
        }
        MBeanOperationInfo[] out = new MBeanOperationInfo[src.length];
        for (int i = 0; i < src.length; i++) {
            out[i] = (MBeanOperationInfo) src[i];
        }
        return out;
    }

    /**
     * Equality against any {@link OpenMBeanInfo}.
     *
     * <p>The arrays are compared <b>without order</b>: two descriptions of the same MBean that list
     * the attributes in a different order describe the same MBean. It is what the contract defines,
     * and it is what makes the comparison survive a serialization that does not preserve order.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OpenMBeanInfo)) {
            return false;
        }
        OpenMBeanInfo other = (OpenMBeanInfo) obj;
        if (!this.getClassName().equals(other.getClassName())) {
            return false;
        }
        return sameSet(this.getAttributes(), other.getAttributes())
                && sameSet(this.getConstructors(), other.getConstructors())
                && sameSet(this.getOperations(), other.getOperations())
                && sameSet(this.getNotifications(), other.getNotifications());
    }

    private static boolean sameSet(Object[] a, Object[] b) {
        if (a.length != b.length) {
            return false;
        }
        // With small arrays --and these are: an MBean's members-- the linear search is cheaper than
        // building two sets, and it does not require the elements to have a `hashCode` consistent
        // with `equals`, which is one more assumption about other people's implementations.
        boolean[] used = new boolean[b.length];
        for (int i = 0; i < a.length; i++) {
            boolean found = false;
            for (int j = 0; j < b.length && !found; j++) {
                if (!used[j] && a[i].equals(b[j])) {
                    used[j] = true;
                    found = true;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /** The sum of the hashes, which is the only order-independent thing. */
    public int hashCode() {
        if (this.hash == 0) {
            int h = this.getClassName().hashCode();
            h = h + sumOf(this.getAttributes());
            h = h + sumOf(this.getConstructors());
            h = h + sumOf(this.getOperations());
            h = h + sumOf(this.getNotifications());
            this.hash = h;
        }
        return this.hash;
    }

    private static int sumOf(Object[] a) {
        int h = 0;
        for (int i = 0; i < a.length; i++) {
            h = h + a[i].hashCode();
        }
        return h;
    }

    public String toString() {
        return OpenMBeanInfoSupport.class.getName()
                + "(class=" + this.getClassName()
                + ",attributes=" + Arrays.asList(this.getAttributes()).toString()
                + ",constructors=" + Arrays.asList(this.getConstructors()).toString()
                + ",operations=" + Arrays.asList(this.getOperations()).toString()
                + ",notifications=" + Arrays.asList(this.getNotifications()).toString() + ")";
    }
}
