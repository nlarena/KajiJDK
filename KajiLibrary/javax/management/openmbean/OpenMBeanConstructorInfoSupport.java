package javax.management.openmbean;

import java.util.Arrays;
import javax.management.Descriptor;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanParameterInfo;

/**
 * La implementacion de {@link OpenMBeanConstructorInfo}.
 *
 * <p>El constructor toma `OpenMBeanParameterInfo[]` y se lo pasa a `super` como
 * `MBeanParameterInfo[]`. La copia entre los dos arreglos no es ceremonia: los elementos son los
 * mismos objetos --toda implementacion de `OpenMBeanParameterInfo` que sirva acá extiende
 * `MBeanParameterInfo`--, pero los arreglos son de tipos distintos y Java no los convierte solo.
 */
public class OpenMBeanConstructorInfoSupport extends MBeanConstructorInfo
        implements OpenMBeanConstructorInfo {

    private static final long serialVersionUID = -4400441579007477003L;

    /** Un constructor con esos parametros. */
    public OpenMBeanConstructorInfoSupport(String name, String description,
            OpenMBeanParameterInfo[] signature) {
        this(name, description, signature, null);
    }

    /** Un constructor con esos parametros y ese descriptor. */
    public OpenMBeanConstructorInfoSupport(String name, String description,
            OpenMBeanParameterInfo[] signature, Descriptor descriptor) {
        super(Signatures.requireName(name), Signatures.requireDescription(description),
                Signatures.asParameters(signature), descriptor);
    }

    /** Igualdad contra cualquier {@link OpenMBeanConstructorInfo}: nombre y parametros. */
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
