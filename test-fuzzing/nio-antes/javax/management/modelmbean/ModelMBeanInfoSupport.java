package javax.management.modelmbean;

import java.util.ArrayList;
import java.util.List;
import javax.management.Descriptor;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.RuntimeOperationsException;

/**
 * KajiLibrary's javax.management.modelmbean.ModelMBeanInfoSupport -- the concrete description of
 * a model MBean.
 *
 * <p>An {@link MBeanInfo} with descriptors. The descriptors of each attribute, operation and
 * notice live in the corresponding {@code Info}; this one adds the whole MBean's and the
 * methods to look them up by name and by type.
 *
 * <h2>Searching by type, not by position</h2>
 *
 * <p>Almost every method here takes a <b>descriptor type</b>: {@code "mbean"},
 * {@code "attribute"}, {@code "operation"}, {@code "constructor"},
 * {@code "notification"}, or null for all. It is the way to walk the configuration without
 * knowing how many of each thing there are.
 *
 * <p>Each descriptor's {@code descriptorType} field is what classifies it, and that is why
 * {@link #setDescriptors} can take a mixed bag and sort it out on its own.
 *
 * <h2>The default descriptors</h2>
 *
 * <p>An {@code Info} without a descriptor is not left without one: it gets one built with the
 * name, the type and the display name. It is what makes a freshly built model MBean valid, and
 * that is why {@link #getMBeanDescriptor} never returns null.
 */
public class ModelMBeanInfoSupport extends MBeanInfo implements ModelMBeanInfo {

    private static final long serialVersionUID = -1935722590756516193L;

    /** The whole MBean's descriptor; never null. */
    private Descriptor mbeanDescriptor;

    /** A copy of another one. */
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

    /** With the four groups and without a descriptor of its own. */
    public ModelMBeanInfoSupport(String className, String description,
                                 ModelMBeanAttributeInfo[] attributes,
                                 ModelMBeanConstructorInfo[] constructors,
                                 ModelMBeanOperationInfo[] operations,
                                 ModelMBeanNotificationInfo[] notifications) {
        this(className, description, attributes, constructors, operations, notifications, null);
    }

    /**
     * Everything explicit.
     *
     * @param mbeandescriptor null builds the default one; see the class note
     * @throws RuntimeOperationsException if the descriptor is not valid
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

    /** A copy. */
    public Object clone() {
        return new ModelMBeanInfoSupport(this);
    }

    /**
     * All the descriptors of that type.
     *
     * @param inDescriptorType null returns them all, starting with the MBean's
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
     * Sorts out a bag of descriptors according to each one's {@code descriptorType}.
     *
     * @throws RuntimeOperationsException if one of them lacks that field
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
     * The descriptor with that name, of any type.
     *
     * @return null if there is none with that name
     */
    public Descriptor getDescriptor(String inDescriptorName)
        throws MBeanException, RuntimeOperationsException {
        return getDescriptor(inDescriptorName, null);
    }

    /**
     * The descriptor with that name and that type.
     *
     * @return null if it is not there
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
     * Puts it in the {@code Info} that corresponds to it by name.
     *
     * @throws RuntimeOperationsException if there is none with that name and that type
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

    /** The attribute with that name, or null. */
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

    /** The operation with that name, or null. */
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

    /** The constructor with that name, or null. */
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

    /** The notice with that name, or null. */
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

    /** The whole MBean's descriptor. A copy. */
    public Descriptor getDescriptor() {
        return (Descriptor) this.mbeanDescriptor.clone();
    }

    /** Ver {@link #getDescriptor}. */
    public Descriptor getMBeanDescriptor() throws MBeanException {
        return (Descriptor) this.mbeanDescriptor.clone();
    }

    /**
     * Replaces it.
     *
     * @param inMBeanDescriptor null goes back to the default one
     * @throws RuntimeOperationsException if it is not valid
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

    /** Adds the descriptors of those {@code Info}s that are model ones. */
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

    /** The MBean's default descriptor; see the class note. */
    private Descriptor defaultMBeanDescriptor() {
        return new DescriptorSupport(new String[] {"name", "descriptorType", "displayName",
                                                   "persistPolicy", "log", "visibility"},
            new Object[] {getClassName(), "mbean", getClassName(), "never", "F", "1"});
    }
}
