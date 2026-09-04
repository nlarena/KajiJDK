package javax.management.modelmbean;

import java.util.ArrayList;
import java.util.List;
import javax.management.Descriptor;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.RuntimeOperationsException;

/**
 * KajiLibrary's javax.management.modelmbean.ModelMBeanInfoSupport -- la descripcion concreta de un
 * model MBean.
 *
 * <p>Un {@link MBeanInfo} con descriptores. Los descriptores de cada atributo, operacion y aviso
 * viven en el {@code Info} correspondiente; este agrega el del MBean entero y los metodos para
 * buscarlos por nombre y por tipo.
 *
 * <h2>Buscar por tipo, no por posicion</h2>
 *
 * <p>Casi todos los metodos de aca reciben un <b>tipo de descriptor</b>: {@code "mbean"},
 * {@code "attribute"}, {@code "operation"}, {@code "constructor"}, {@code "notification"}, o null
 * para todos. Es la forma de recorrer la configuracion sin saber cuantos hay de cada cosa.
 *
 * <p>El campo {@code descriptorType} de cada descriptor es lo que lo clasifica, y por eso
 * {@link #setDescriptors} puede recibir una bolsa mezclada y repartirla sola.
 *
 * <h2>Los descriptores por omision</h2>
 *
 * <p>Un {@code Info} sin descriptor no queda sin descriptor: se le arma uno con el nombre, el tipo y
 * el nombre para mostrar. Es lo que hace que un model MBean recien construido sea valido, y por eso
 * {@link #getMBeanDescriptor} nunca devuelve null.
 */
public class ModelMBeanInfoSupport extends MBeanInfo implements ModelMBeanInfo {

    private static final long serialVersionUID = -1935722590756516193L;

    /** El descriptor del MBean entero; nunca null. */
    private Descriptor mbeanDescriptor;

    /** Una copia de otro. */
    public ModelMBeanInfoSupport(ModelMBeanInfo mbi) {
        super(mbi.getClassName(), mbi.getDescription(), mbi.getAttributes(), mbi.getConstructors(),
            mbi.getOperations(), mbi.getNotifications());
        try {
            this.mbeanDescriptor = mbi.getMBeanDescriptor();
        } catch (MBeanException e) {
            this.mbeanDescriptor = null;
        }
        if (this.mbeanDescriptor == null) {
            this.mbeanDescriptor = defaultMBeanDescriptor();
        }
    }

    /** Con los cuatro grupos y sin descriptor propio. */
    public ModelMBeanInfoSupport(String className, String description,
                                 ModelMBeanAttributeInfo[] attributes,
                                 ModelMBeanConstructorInfo[] constructors,
                                 ModelMBeanOperationInfo[] operations,
                                 ModelMBeanNotificationInfo[] notifications) {
        this(className, description, attributes, constructors, operations, notifications, null);
    }

    /**
     * Todo explicito.
     *
     * @param mbeandescriptor null arma el por omision; ver la nota de la clase
     * @throws RuntimeOperationsException si el descriptor no es valido
     */
    public ModelMBeanInfoSupport(String className, String description,
                                 ModelMBeanAttributeInfo[] attributes,
                                 ModelMBeanConstructorInfo[] constructors,
                                 ModelMBeanOperationInfo[] operations,
                                 ModelMBeanNotificationInfo[] notifications,
                                 Descriptor mbeandescriptor) {
        super(className, description, attributes, constructors, operations, notifications);
        if (mbeandescriptor == null) {
            this.mbeanDescriptor = defaultMBeanDescriptor();
            return;
        }
        if (!mbeandescriptor.isValid()) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("Invalid MBean descriptor"));
        }
        this.mbeanDescriptor = (Descriptor) mbeandescriptor.clone();
    }

    /** Una copia. */
    public Object clone() {
        return new ModelMBeanInfoSupport(this);
    }

    /**
     * Todos los descriptores de ese tipo.
     *
     * @param inDescriptorType null los devuelve todos, empezando por el del MBean
     */
    public Descriptor[] getDescriptors(String inDescriptorType)
        throws MBeanException, RuntimeOperationsException {
        List<Descriptor> out = new ArrayList<Descriptor>();
        boolean all = (inDescriptorType == null || inDescriptorType.length() == 0);
        if (all || "mbean".equalsIgnoreCase(inDescriptorType)) {
            out.add(getMBeanDescriptor());
        }
        if (all || "attribute".equalsIgnoreCase(inDescriptorType)) {
            collect(out, getAttributes());
        }
        if (all || "constructor".equalsIgnoreCase(inDescriptorType)) {
            collect(out, getConstructors());
        }
        if (all || "operation".equalsIgnoreCase(inDescriptorType)) {
            collect(out, getOperations());
        }
        if (all || "notification".equalsIgnoreCase(inDescriptorType)) {
            collect(out, getNotifications());
        }
        return out.toArray(new Descriptor[out.size()]);
    }

    /**
     * Reparte una bolsa de descriptores segun el {@code descriptorType} de cada uno.
     *
     * @throws RuntimeOperationsException si alguno no tiene ese campo
     */
    public void setDescriptors(Descriptor[] inDescriptors)
        throws MBeanException, RuntimeOperationsException {
        if (inDescriptors == null) {
            return;
        }
        int i = 0;
        while (i < inDescriptors.length) {
            Descriptor d = inDescriptors[i];
            if (d != null) {
                Object type = d.getFieldValue("descriptorType");
                if (type == null) {
                    throw new RuntimeOperationsException(new IllegalArgumentException(
                        "Descriptor without a descriptorType field"));
                }
                setDescriptor(d, type.toString());
            }
            i = i + 1;
        }
    }

    /**
     * El descriptor con ese nombre, de cualquier tipo.
     *
     * @return null si no hay ninguno con ese nombre
     */
    public Descriptor getDescriptor(String inDescriptorName)
        throws MBeanException, RuntimeOperationsException {
        return getDescriptor(inDescriptorName, null);
    }

    /**
     * El descriptor con ese nombre y ese tipo.
     *
     * @return null si no esta
     */
    public Descriptor getDescriptor(String inDescriptorName, String inDescriptorType)
        throws MBeanException, RuntimeOperationsException {
        if (inDescriptorName == null) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("Descriptor name is null"));
        }
        Descriptor[] all = getDescriptors(inDescriptorType);
        int i = 0;
        while (i < all.length) {
            Object name = all[i].getFieldValue("name");
            if (name != null && inDescriptorName.equalsIgnoreCase(name.toString())) {
                return all[i];
            }
            i = i + 1;
        }
        return null;
    }

    /**
     * Lo pone en el {@code Info} que le corresponde por nombre.
     *
     * @throws RuntimeOperationsException si no hay ninguno con ese nombre y ese tipo
     */
    public void setDescriptor(Descriptor inDescriptor, String inDescriptorType)
        throws MBeanException, RuntimeOperationsException {
        if (inDescriptor == null) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("Descriptor is null"));
        }
        if ("mbean".equalsIgnoreCase(inDescriptorType)) {
            setMBeanDescriptor(inDescriptor);
            return;
        }
        Object nameField = inDescriptor.getFieldValue("name");
        if (nameField == null) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("Descriptor without a name field"));
        }
        String name = nameField.toString();
        if ("attribute".equalsIgnoreCase(inDescriptorType)) {
            ModelMBeanAttributeInfo target = getAttribute(name);
            if (target != null) {
                target.setDescriptor(inDescriptor);
                return;
            }
        } else if ("operation".equalsIgnoreCase(inDescriptorType)) {
            ModelMBeanOperationInfo target = getOperation(name);
            if (target != null) {
                target.setDescriptor(inDescriptor);
                return;
            }
        } else if ("constructor".equalsIgnoreCase(inDescriptorType)) {
            ModelMBeanConstructorInfo target = getConstructor(name);
            if (target != null) {
                target.setDescriptor(inDescriptor);
                return;
            }
        } else if ("notification".equalsIgnoreCase(inDescriptorType)) {
            ModelMBeanNotificationInfo target = getNotification(name);
            if (target != null) {
                target.setDescriptor(inDescriptor);
                return;
            }
        }
        throw new RuntimeOperationsException(new IllegalArgumentException(
            "No " + inDescriptorType + " named " + name));
    }

    /** El atributo con ese nombre, o null. */
    public ModelMBeanAttributeInfo getAttribute(String inName)
        throws MBeanException, RuntimeOperationsException {
        javax.management.MBeanAttributeInfo[] all = getAttributes();
        int i = 0;
        while (i < all.length) {
            if (all[i] instanceof ModelMBeanAttributeInfo && all[i].getName().equals(inName)) {
                return (ModelMBeanAttributeInfo) all[i];
            }
            i = i + 1;
        }
        return null;
    }

    /** La operacion con ese nombre, o null. */
    public ModelMBeanOperationInfo getOperation(String inName)
        throws MBeanException, RuntimeOperationsException {
        javax.management.MBeanOperationInfo[] all = getOperations();
        int i = 0;
        while (i < all.length) {
            if (all[i] instanceof ModelMBeanOperationInfo && all[i].getName().equals(inName)) {
                return (ModelMBeanOperationInfo) all[i];
            }
            i = i + 1;
        }
        return null;
    }

    /** El constructor con ese nombre, o null. */
    public ModelMBeanConstructorInfo getConstructor(String inName)
        throws MBeanException, RuntimeOperationsException {
        javax.management.MBeanConstructorInfo[] all = getConstructors();
        int i = 0;
        while (i < all.length) {
            if (all[i] instanceof ModelMBeanConstructorInfo && all[i].getName().equals(inName)) {
                return (ModelMBeanConstructorInfo) all[i];
            }
            i = i + 1;
        }
        return null;
    }

    /** El aviso con ese nombre, o null. */
    public ModelMBeanNotificationInfo getNotification(String inName)
        throws MBeanException, RuntimeOperationsException {
        javax.management.MBeanNotificationInfo[] all = getNotifications();
        int i = 0;
        while (i < all.length) {
            if (all[i] instanceof ModelMBeanNotificationInfo && all[i].getName().equals(inName)) {
                return (ModelMBeanNotificationInfo) all[i];
            }
            i = i + 1;
        }
        return null;
    }

    /** El descriptor del MBean entero. Copia. */
    public Descriptor getDescriptor() {
        return (Descriptor) this.mbeanDescriptor.clone();
    }

    /** Ver {@link #getDescriptor}. */
    public Descriptor getMBeanDescriptor() throws MBeanException {
        return (Descriptor) this.mbeanDescriptor.clone();
    }

    /**
     * Lo reemplaza.
     *
     * @param inMBeanDescriptor null vuelve al por omision
     * @throws RuntimeOperationsException si no es valido
     */
    public void setMBeanDescriptor(Descriptor inMBeanDescriptor)
        throws MBeanException, RuntimeOperationsException {
        if (inMBeanDescriptor == null) {
            this.mbeanDescriptor = defaultMBeanDescriptor();
            return;
        }
        if (!inMBeanDescriptor.isValid()) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("Invalid MBean descriptor"));
        }
        this.mbeanDescriptor = (Descriptor) inMBeanDescriptor.clone();
    }

    /** Agrega los descriptores de esos {@code Info} que sean del modelo. */
    private static void collect(List<Descriptor> out, javax.management.MBeanFeatureInfo[] infos) {
        if (infos == null) {
            return;
        }
        int i = 0;
        while (i < infos.length) {
            if (infos[i] instanceof javax.management.DescriptorAccess) {
                out.add(((javax.management.DescriptorAccess) infos[i]).getDescriptor());
            }
            i = i + 1;
        }
    }

    /** El descriptor por omision del MBean; ver la nota de la clase. */
    private Descriptor defaultMBeanDescriptor() {
        return new DescriptorSupport(new String[] {"name", "descriptorType", "displayName",
                                                   "persistPolicy", "log", "visibility"},
            new Object[] {getClassName(), "mbean", getClassName(), "never", "F", "1"});
    }
}
