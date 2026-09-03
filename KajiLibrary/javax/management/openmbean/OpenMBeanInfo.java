package javax.management.openmbean;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;

/**
 * La descripción de un MBean abierto entero.
 *
 * <p>Es el mismo contenido que un `MBeanInfo` común, con la diferencia de que sus atributos,
 * operaciones y constructores son los abiertos de este paquete. Los tipos de retorno siguen siendo
 * los de `javax.management` por la razón que explica {@link OpenMBeanConstructorInfo}.
 *
 * <p>Las notificaciones **no** tienen versión abierta: una notificación lleva un `userData` de
 * cualquier clase, así que no hay nada que restringir a tipos abiertos.
 */
public interface OpenMBeanInfo {

    /** El nombre de clase del MBean. */
    String getClassName();

    /** La descripción, para una persona. */
    String getDescription();

    /** Los atributos; cada uno es además un {@link OpenMBeanAttributeInfo}. */
    MBeanAttributeInfo[] getAttributes();

    /** Las operaciones; cada una es además un {@link OpenMBeanOperationInfo}. */
    MBeanOperationInfo[] getOperations();

    /** Los constructores; cada uno es además un {@link OpenMBeanConstructorInfo}. */
    MBeanConstructorInfo[] getConstructors();

    /** Las notificaciones. Ver la nota de la clase sobre por qué no son abiertas. */
    MBeanNotificationInfo[] getNotifications();

    boolean equals(Object obj);

    int hashCode();

    String toString();
}
