package javax.management.openmbean;

import java.util.Arrays;
import javax.management.Descriptor;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanParameterInfo;

/**
 * The implementation of {@link OpenMBeanConstructorInfo}.
 *
 * <p>The constructor takes {@code OpenMBeanParameterInfo[]} and passes it to {@code super} as
 * {@code MBeanParameterInfo[]}. The copy between the two arrays is not ceremony: the elements are
 * the same objects --every implementation of {@code OpenMBeanParameterInfo} usable here extends
 * {@code MBeanParameterInfo}-- but the arrays are of different types and Java does not convert them
 * on its own.
 */
public class OpenMBeanConstructorInfoSupport extends MBeanConstructorInfo
        implements OpenMBeanConstructorInfo {

    private static final long serialVersionUID = -4400441579007477003L;

    /** A constructor with those parameters. */
    public OpenMBeanConstructorInfoSupport(String name, String description,
            OpenMBeanParameterInfo[] signature) {
        this(name, description, signature, null);
    }

    /** A constructor with those parameters and that descriptor. */
    public OpenMBeanConstructorInfoSupport(String name, String description,
            OpenMBeanParameterInfo[] signature, Descriptor descriptor) {
        super(Signatures.requireName(name), Signatures.requireDescription(description),
                Signatures.asParameters(signature), descriptor);
    }

    /** Equality against any {@link OpenMBeanConstructorInfo}: name and parameters. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OpenMBeanConstructorInfo)) {
            return false;
        }
        OpenMBeanConstructorInfo other = (OpenMBeanConstructorInfo) obj;
        return this.getName().equals(other.getName())
                && Arrays.equals(this.getSignature(), other.getSignature());
    }

    public int hashCode() {
        return this.getName().hashCode() + Arrays.asList(this.getSignature()).hashCode();
    }

    public String toString() {
        return OpenMBeanConstructorInfoSupport.class.getName()
                + "(name=" + this.getName()
                + ",signature=" + Arrays.asList(this.getSignature()).toString() + ")";
    }
}
