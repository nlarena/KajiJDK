package javax.management.modelmbean;

import javax.management.Descriptor;
import javax.management.DescriptorAccess;
import javax.management.MBeanNotificationInfo;
import javax.management.RuntimeOperationsException;

/**
 * KajiLibrary's javax.management.modelmbean.ModelMBeanNotificationInfo -- un aviso de un model MBean.
 *
 * <p>El descriptor de un aviso lleva los campos de <b>registro</b>: {@code log} dice si se
 * guarda, {@code logfile} adonde. Es lo que permite que un aviso quede escrito sin que
 * nadie escuche, que es exactamente lo que se quiere de una traza de auditoria.
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
public class ModelMBeanNotificationInfo extends MBeanNotificationInfo implements DescriptorAccess {

    private static final long serialVersionUID = -7445681389570207141L;

    /** El descriptor; nunca null. */
    private Descriptor modelDescriptor;

    /** Sin descriptor. */
    public ModelMBeanNotificationInfo(String[] notifTypes, String name, String description) {
        super(notifTypes, name, description);
    }

    /** Con descriptor. */
    public ModelMBeanNotificationInfo(String[] notifTypes, String name, String description,
                                      Descriptor descriptor) {
        super(notifTypes, name, description);
        setDescriptor(descriptor);
    }

    /** Una copia. */
    public ModelMBeanNotificationInfo(ModelMBeanNotificationInfo inInfo) {
        super(inInfo.getNotifTypes(), inInfo.getName(), inInfo.getDescription());
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
                new IllegalArgumentException("Invalid descriptor passed to ModelMBeanNotificationInfo"));
        }
        this.modelDescriptor = (Descriptor) inDescriptor.clone();
    }

    /** Una copia. */
    public Object clone() {
        return new ModelMBeanNotificationInfo(this);
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
            new Object[] {getName(), "notification", getName()});
    }
}
