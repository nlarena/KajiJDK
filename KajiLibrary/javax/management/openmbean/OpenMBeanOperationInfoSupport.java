package javax.management.openmbean;

import java.util.Arrays;
import javax.management.Descriptor;
import javax.management.MBeanOperationInfo;

/**
 * La implementacion de {@link OpenMBeanOperationInfo}.
 *
 * <p>Guarda el tipo abierto de retorno aparte de lo que hereda: a `super` le pasa el `className` de
 * ese tipo como `type`, que es lo unico que `MBeanOperationInfo` sabe representar. Los dos quedan
 * consistentes por construccion, y por eso `getReturnType` y `getReturnOpenType` no pueden
 * contradecirse.
 */
public class OpenMBeanOperationInfoSupport extends MBeanOperationInfo
        implements OpenMBeanOperationInfo {

    private static final long serialVersionUID = 4996859732565369366L;

    private final OpenType<?> returnOpenType;

    /** Una operacion con esos parametros, ese retorno y ese impacto. */
    public OpenMBeanOperationInfoSupport(String name, String description,
            OpenMBeanParameterInfo[] signature, OpenType<?> returnOpenType, int impact) {
        this(name, description, signature, returnOpenType, impact, null);
    }

    /** Lo mismo, con ese descriptor. */
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
            throw new IllegalArgumentException("el tipo de retorno no puede ser nulo");
        }
        return returnOpenType.getClassName();
    }

    // Los cuatro valores que `MBeanOperationInfo` define. Uno fuera de ese conjunto no describe
    // nada, y aceptarlo dejaria una operacion cuyo impacto nadie puede interpretar.
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

    /** Igualdad contra cualquier {@link OpenMBeanOperationInfo}. */
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
