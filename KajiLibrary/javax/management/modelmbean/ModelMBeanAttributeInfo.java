package javax.management.modelmbean;

import java.lang.reflect.Method;
import javax.management.Descriptor;
import javax.management.DescriptorAccess;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.RuntimeOperationsException;

/**
 * KajiLibrary's javax.management.modelmbean.ModelMBeanAttributeInfo -- un atributo de un model MBean.
 *
 * <p>Un {@code MBeanAttributeInfo} con descriptor. El descriptor es el que dice de <b>donde</b>
 * sale el valor: los campos {@code getMethod} y {@code setMethod} nombran los metodos del
 * objeto administrado, y {@code currencyTimeLimit} dice cuantos segundos se puede cachear.
 *
 * <p>Ese cacheo es lo que hace util al tipo: un atributo caro --una consulta, una lectura de
 * disco-- se puede exponer sin que cada consola que lo mire lo dispare de nuevo.
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
public class ModelMBeanAttributeInfo extends MBeanAttributeInfo implements DescriptorAccess {

    private static final long serialVersionUID = 6181543027787327345L;

    /** El descriptor; nunca null. */
    private Descriptor modelDescriptor;

    /** Desde los metodos de acceso, sin descriptor. */
    public ModelMBeanAttributeInfo(String name, String description, Method getter, Method setter)
        throws IntrospectionException {
        super(name, description, getter, setter);
    }

    /** Idem, con descriptor. */
    public ModelMBeanAttributeInfo(String name, String description, Method getter, Method setter,
                                   Descriptor descriptor) throws IntrospectionException {
        super(name, description, getter, setter);
        setDescriptor(descriptor);
    }

    /** Declarando el tipo y los permisos a mano. */
    public ModelMBeanAttributeInfo(String name, String type, String description, boolean isReadable,
                                   boolean isWritable, boolean isIs) {
        super(name, type, description, isReadable, isWritable, isIs);
    }

    /** Idem, con descriptor. */
    public ModelMBeanAttributeInfo(String name, String type, String description, boolean isReadable,
                                   boolean isWritable, boolean isIs, Descriptor descriptor) {
        super(name, type, description, isReadable, isWritable, isIs);
        setDescriptor(descriptor);
    }

    /** Una copia. */
    public ModelMBeanAttributeInfo(ModelMBeanAttributeInfo inInfo) {
        super(inInfo.getName(), inInfo.getType(), inInfo.getDescription(), inInfo.isReadable(),
            inInfo.isWritable(), inInfo.isIs());
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
                new IllegalArgumentException("Invalid descriptor passed to ModelMBeanAttributeInfo"));
        }
        this.modelDescriptor = (Descriptor) inDescriptor.clone();
    }

    /** Una copia. */
    public Object clone() {
        return new ModelMBeanAttributeInfo(this);
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
            new Object[] {getName(), "attribute", getName()});
    }
}
