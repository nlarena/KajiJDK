package javax.management.openmbean;

import java.util.Arrays;
import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;

/**
 * La implementacion de {@link OpenMBeanInfo}.
 *
 * <p>Convierte los tres arreglos abiertos a los de `javax.management` para pasarselos a `super`.
 * Los objetos son los mismos: toda implementacion util de `OpenMBeanAttributeInfo` extiende
 * `MBeanAttributeInfo`, y lo mismo para las otras dos. Lo unico que cambia es el tipo del arreglo.
 *
 * <p>Un `null` en cualquiera de los arreglos se toma como "ninguno", igual que en `MBeanInfo`. Es
 * distinto de un arreglo vacio solo en la intencion de quien llama; el resultado es el mismo.
 */
public class OpenMBeanInfoSupport extends MBeanInfo implements OpenMBeanInfo {

    private static final long serialVersionUID = 4349395935420511492L;

    private transient int hash;

    /** Un MBean abierto con esos miembros. */
    public OpenMBeanInfoSupport(String className, String description,
            OpenMBeanAttributeInfo[] openAttributes, OpenMBeanConstructorInfo[] openConstructors,
            OpenMBeanOperationInfo[] openOperations, MBeanNotificationInfo[] notifications) {
        this(className, description, openAttributes, openConstructors, openOperations,
                notifications, null);
    }

    /** Lo mismo, con ese descriptor. */
    public OpenMBeanInfoSupport(String className, String description,
            OpenMBeanAttributeInfo[] openAttributes, OpenMBeanConstructorInfo[] openConstructors,
            OpenMBeanOperationInfo[] openOperations, MBeanNotificationInfo[] notifications,
            Descriptor descriptor) {
        super(className, description, asAttributes(openAttributes),
                asConstructors(openConstructors), asOperations(openOperations), notifications,
                descriptor);
    }

    private static MBeanAttributeInfo[] asAttributes(OpenMBeanAttributeInfo[] src) {
        if (src == null || src.length == 0) {
            return new MBeanAttributeInfo[0];
        }
        MBeanAttributeInfo[] out = new MBeanAttributeInfo[src.length];
        for (int i = 0; i < src.length; i++) {
            out[i] = (MBeanAttributeInfo) src[i];
        }
        return out;
    }

    private static MBeanConstructorInfo[] asConstructors(OpenMBeanConstructorInfo[] src) {
        if (src == null || src.length == 0) {
            return new MBeanConstructorInfo[0];
        }
        MBeanConstructorInfo[] out = new MBeanConstructorInfo[src.length];
        for (int i = 0; i < src.length; i++) {
            out[i] = (MBeanConstructorInfo) src[i];
        }
        return out;
    }

    private static MBeanOperationInfo[] asOperations(OpenMBeanOperationInfo[] src) {
        if (src == null || src.length == 0) {
            return new MBeanOperationInfo[0];
        }
        MBeanOperationInfo[] out = new MBeanOperationInfo[src.length];
        for (int i = 0; i < src.length; i++) {
            out[i] = (MBeanOperationInfo) src[i];
        }
        return out;
    }

    /**
     * Igualdad contra cualquier {@link OpenMBeanInfo}.
     *
     * <p>Los arreglos se comparan **sin orden**: dos descripciones del mismo MBean que enumeran los
     * atributos en distinto orden describen el mismo MBean. Es lo que define el contrato, y es lo
     * que hace que la comparacion sobreviva a una serializacion que no preserve el orden.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OpenMBeanInfo)) {
            return false;
        }
        OpenMBeanInfo other = (OpenMBeanInfo) obj;
        if (!this.getClassName().equals(other.getClassName())) {
            return false;
        }
        return sameSet(this.getAttributes(), other.getAttributes())
                && sameSet(this.getConstructors(), other.getConstructors())
                && sameSet(this.getOperations(), other.getOperations())
                && sameSet(this.getNotifications(), other.getNotifications());
    }

    private static boolean sameSet(Object[] a, Object[] b) {
        if (a.length != b.length) {
            return false;
        }
        // Con arreglos chicos --y estos lo son: los miembros de un MBean-- la busqueda lineal es
        // mas barata que armar dos conjuntos, y no exige que los elementos tengan un `hashCode`
        // consistente con `equals`, que es una suposicion de mas sobre implementaciones ajenas.
        boolean[] usado = new boolean[b.length];
        for (int i = 0; i < a.length; i++) {
            boolean encontrado = false;
            for (int j = 0; j < b.length && !encontrado; j++) {
                if (!usado[j] && a[i].equals(b[j])) {
                    usado[j] = true;
                    encontrado = true;
                }
            }
            if (!encontrado) {
                return false;
            }
        }
        return true;
    }

    /** La suma de los hashes, que es lo unico independiente del orden. */
    public int hashCode() {
        if (this.hash == 0) {
            int h = this.getClassName().hashCode();
            h = h + sumOf(this.getAttributes());
            h = h + sumOf(this.getConstructors());
            h = h + sumOf(this.getOperations());
            h = h + sumOf(this.getNotifications());
            this.hash = h;
        }
        return this.hash;
    }

    private static int sumOf(Object[] a) {
        int h = 0;
        for (int i = 0; i < a.length; i++) {
            h = h + a[i].hashCode();
        }
        return h;
    }

    public String toString() {
        return OpenMBeanInfoSupport.class.getName()
                + "(class=" + this.getClassName()
                + ",attributes=" + Arrays.asList(this.getAttributes()).toString()
                + ",constructors=" + Arrays.asList(this.getConstructors()).toString()
                + ",operations=" + Arrays.asList(this.getOperations()).toString()
                + ",notifications=" + Arrays.asList(this.getNotifications()).toString() + ")";
    }
}
