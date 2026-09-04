package javax.management.modelmbean;

import java.lang.reflect.Method;
import javax.management.Descriptor;
import javax.management.DescriptorAccess;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.RuntimeOperationsException;

/**
 * KajiLibrary's javax.management.modelmbean.ModelMBeanOperationInfo -- una operacion de un model MBean.
 *
 * <p>Su descriptor lleva {@code targetObject} y {@code targetType}: <b>sobre que objeto</b> se
 * invoca. Es lo que permite que un model MBean exponga operaciones de varios objetos
 * distintos como si fueran suyas, que es para lo que se usa cuando se administra un
 * subsistema entero con un solo MBean.
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
public class ModelMBeanOperationInfo extends MBeanOperationInfo implements DescriptorAccess {

    private static final long serialVersionUID = 6532732096650090465L;

    /** El descriptor; nunca null. */
    private Descriptor modelDescriptor;

    /** Desde el metodo real, sin descriptor. */
    public ModelMBeanOperationInfo(String description, Method operationMethod) {
        super(description, operationMethod);
    }

    /** Idem, con descriptor. */
    public ModelMBeanOperationInfo(String description, Method operationMethod,
                                   Descriptor descriptor) {
        super(description, operationMethod);
        setDescriptor(descriptor);
    }

    /** Declarando la firma a mano. */
    public ModelMBeanOperationInfo(String name, String description, MBeanParameterInfo[] signature,
                                   String type, int impact) {
        super(name, description, signature, type, impact);
    }

    /** Idem, con descriptor. */
    public ModelMBeanOperationInfo(String name, String description, MBeanParameterInfo[] signature,
                                   String type, int impact, Descriptor descriptor) {
        super(name, description, signature, type, impact);
        setDescriptor(descriptor);
    }

    /** Una copia. */
    public ModelMBeanOperationInfo(ModelMBeanOperationInfo inInfo) {
        super(inInfo.getName(), inInfo.getDescription(), inInfo.getSignature(),
            inInfo.getReturnType(), inInfo.getImpact());
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
                new IllegalArgumentException("Invalid descriptor passed to ModelMBeanOperationInfo"));
        }
        this.modelDescriptor = (Descriptor) inDescriptor.clone();
    }

    /** Una copia. */
    public Object clone() {
        return new ModelMBeanOperationInfo(this);
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
