package javax.management.openmbean;

import java.util.Arrays;
import javax.management.Descriptor;
import javax.management.MBeanOperationInfo;

/**
 * The implementation of {@link OpenMBeanOperationInfo}.
 *
 * <p>It keeps the open return type apart from what it inherits: to {@code super} it passes that
 * type's {@code className} as {@code type}, which is the only thing {@code MBeanOperationInfo} can
 * represent. The two stay consistent by construction, which is why {@code getReturnType} and
 * {@code getReturnOpenType} cannot contradict each other.
 */
public class OpenMBeanOperationInfoSupport extends MBeanOperationInfo
        implements OpenMBeanOperationInfo {

    private static final long serialVersionUID = 4996859732565369366L;

    private final OpenType<?> returnOpenType;

    /** An operation with those parameters, that return and that impact. */
    public OpenMBeanOperationInfoSupport(String name, String description,
            OpenMBeanParameterInfo[] signature, OpenType<?> returnOpenType, int impact) {
        this(name, description, signature, returnOpenType, impact, null);
    }

    /** The same, with that descriptor. */
    public OpenMBeanOperationInfoSupport(String name, String description,
            OpenMBeanParameterInfo[] signature, OpenType<?> returnOpenType, int impact,
            Descriptor descriptor) {
        super(Signatures.requireName(name), Signatures.requireDescription(description),
                Signatures.asParameters(signature), requireReturnType(returnOpenType),
                requireImpact(impact), descriptor);
        this.returnOpenType = returnOpenType;
    }

    private static String requireReturnType(OpenType<?> returnOpenType) {
        if (returnOpenType == null) {
            throw new IllegalArgumentException("the return type cannot be null");
        }
        return returnOpenType.getClassName();
    }

    // The four values `MBeanOperationInfo` defines. One outside that set describes nothing, and
    // accepting it would leave an operation whose impact nobody can interpret.
    private static int requireImpact(int impact) {
        if (impact != MBeanOperationInfo.INFO && impact != MBeanOperationInfo.ACTION
                && impact != MBeanOperationInfo.ACTION_INFO
                && impact != MBeanOperationInfo.UNKNOWN) {
            throw new IllegalArgumentException("impacto desconocido: " + impact);
        }
        return impact;
    }

    public OpenType<?> getReturnOpenType() {
        return this.returnOpenType;
    }

    /** Equality against any {@link OpenMBeanOperationInfo}. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OpenMBeanOperationInfo)) {
            return false;
        }
        OpenMBeanOperationInfo other = (OpenMBeanOperationInfo) obj;
        return this.getName().equals(other.getName())
                && this.getImpact() == other.getImpact()
                && this.returnOpenType.equals(other.getReturnOpenType())
                && Arrays.equals(this.getSignature(), other.getSignature());
    }

    public int hashCode() {
        return this.getName().hashCode() + this.returnOpenType.hashCode() + this.getImpact()
                + Arrays.asList(this.getSignature()).hashCode();
    }

    public String toString() {
        return OpenMBeanOperationInfoSupport.class.getName()
                + "(name=" + this.getName()
                + ",signature=" + Arrays.asList(this.getSignature()).toString()
                + ",return=" + this.returnOpenType.toString()
                + ",impact=" + this.getImpact() + ")";
    }
}
