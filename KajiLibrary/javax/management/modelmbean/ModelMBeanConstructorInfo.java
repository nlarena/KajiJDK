package javax.management.modelmbean;

import java.lang.reflect.Constructor;
import javax.management.Descriptor;
import javax.management.DescriptorAccess;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanParameterInfo;
import javax.management.RuntimeOperationsException;

/**
 * KajiLibrary's javax.management.modelmbean.ModelMBeanConstructorInfo -- un constructor de un model MBean.
 *
 * <p>Su tipo de descriptor es {@code operation} y no {@code constructor}, que sorprende y es lo
 * que dice la especificacion: para el modelo, construir es una operacion mas. El campo
 * {@code role} del descriptor es el que la distingue, con el valor {@code constructor}.
 *
 * <h2>El descriptor es mutable, y el {@code Info} deja de serlo</h2>
 *
 * <p>{@code MBeanFeatureInfo} es inmutable a proposito: es lo que un agente publica y lo que los
 * clientes se guardan. Esta subclase agrega {@link #setDescriptor}, que lo rompe.
 *
 * <p>Esta bien que lo rompa --un model MBean se configura en tiempo de ejecucion y para eso hace
 * falta poder cambiar el descriptor-- y hay que saberlo: cambiar el descriptor de un
 * {@code Info} que ya se publico cambia lo que ven los clientes que se lo guardaron.
 *
 * <p>Por eso {@link #getDescriptor} devuelve una <b>copia</b>: leerlo no da forma de escribirlo.
 */
public class ModelMBeanConstructorInfo extends MBeanConstructorInfo implements DescriptorAccess {

    private static final long serialVersionUID = 3862947819818064362L;

    /** El descriptor; nunca null. */
    private Descriptor modelDescriptor;

    /** Desde el constructor real, sin descriptor. */
    public ModelMBeanConstructorInfo(String description, Constructor<?> constructorElement) {
        super(description, constructorElement);
    }

    /** Idem, con descriptor. */
    public ModelMBeanConstructorInfo(String description, Constructor<?> constructorElement,
                                     Descriptor descriptor) {
        super(description, constructorElement);
        setDescriptor(descriptor);
    }

    /** Declarando la firma a mano. */
    public ModelMBeanConstructorInfo(String name, String description,
                                     MBeanParameterInfo[] signature) {
        super(name, description, signature);
    }

    /** Idem, con descriptor. */
    public ModelMBeanConstructorInfo(String name, String description,
                                     MBeanParameterInfo[] signature, Descriptor descriptor) {
        super(name, description, signature);
        setDescriptor(descriptor);
    }

    /** Una copia. */
    public ModelMBeanConstructorInfo(ModelMBeanConstructorInfo inInfo) {
        super(inInfo.getName(), inInfo.getDescription(), inInfo.getSignature());
        setDescriptor(inInfo.getDescriptor());
    }


    /** Una copia del descriptor. Ver la nota de la clase. */
    public Descriptor getDescriptor() {
        if (this.modelDescriptor == null) {
            this.modelDescriptor = defaultDescriptor();
        }
        return (Descriptor) this.modelDescriptor.clone();
    }

    /**
     * Lo reemplaza.
     *
     * @param inDescriptor null vuelve al descriptor por omision
     * @throws RuntimeOperationsException si el descriptor no es valido para esta clase
     */
    public void setDescriptor(Descriptor inDescriptor) {
        if (inDescriptor == null) {
            this.modelDescriptor = defaultDescriptor();
            return;
        }
        if (!inDescriptor.isValid()) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("Invalid descriptor passed to ModelMBeanConstructorInfo"));
        }
        this.modelDescriptor = (Descriptor) inDescriptor.clone();
    }

    /** Una copia. */
    public Object clone() {
        return new ModelMBeanConstructorInfo(this);
    }

    /** El nombre, la descripcion y el descriptor. */
    public String toString() {
        return getClass().getName() + "(name=" + getName() + ",descriptor=" + getDescriptor() + ")";
    }

    /**
     * El descriptor por omision: nombre, tipo y {@code displayName}.
     *
     * <p>Los tres campos son los que {@code isValid} exige mas el que toda herramienta muestra. Sin
     * ellos, un {@code Info} recien construido tendria un descriptor invalido, que es justo lo que
     * {@link #setDescriptor} rechaza.
     */
    private Descriptor defaultDescriptor() {
        return new DescriptorSupport(new String[] {"name", "descriptorType", "displayName"},
            new Object[] {getName(), "operation", getName()});
    }
}
