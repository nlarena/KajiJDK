package javax.management.modelmbean;

import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.RuntimeOperationsException;

/**
 * KajiLibrary's javax.management.modelmbean.ModelMBeanInfo -- la descripcion de un model MBean.
 *
 * <p>Un {@code MBeanInfo} comun dice <b>que</b> hay: atributos, operaciones, avisos. Este agrega los
 * {@link Descriptor}, que dicen <b>como</b> se comporta cada cosa: de que metodo del objeto sale un
 * atributo, cuantos milisegundos se puede cachear su valor, si se persiste y cada cuanto.
 *
 * <p>Esa segunda mitad es lo que convierte una descripcion en una configuracion. Sin los
 * descriptores, un model MBean no sabria de donde sacar el valor de un atributo.
 *
 * <p>Los descriptores se piden por tipo --{@code "attribute"}, {@code "operation"},
 * {@code "notification"}, {@code "mbean"}, o null para todos-- y esa es la clave de casi todos los
 * metodos de aca.
 */
public interface ModelMBeanInfo {

    /**
     * Todos los descriptores de ese tipo.
     *
     * @param inDescriptorType {@code "mbean"}, {@code "attribute"}, {@code "operation"},
     *     {@code "constructor"}, {@code "notification"}, o null para todos
     */
    Descriptor[] getDescriptors(String inDescriptorType)
        throws MBeanException, RuntimeOperationsException;

    /** Los reemplaza; cada uno va al lugar que dice su campo {@code descriptorType}. */
    void setDescriptors(Descriptor[] inDescriptors)
        throws MBeanException, RuntimeOperationsException;

    /** El descriptor de ese nombre y ese tipo. */
    Descriptor getDescriptor(String inDescriptorName, String inDescriptorType)
        throws MBeanException, RuntimeOperationsException;

    /** Lo pone o lo reemplaza. */
    void setDescriptor(Descriptor inDescriptor, String inDescriptorType)
        throws MBeanException, RuntimeOperationsException;

    /** El descriptor del MBean entero. */
    Descriptor getMBeanDescriptor() throws MBeanException, RuntimeOperationsException;

    /** Ver {@link #getMBeanDescriptor}. */
    void setMBeanDescriptor(Descriptor inDescriptor)
        throws MBeanException, RuntimeOperationsException;

    /** El atributo con ese nombre. */
    ModelMBeanAttributeInfo getAttribute(String inName)
        throws MBeanException, RuntimeOperationsException;

    /** La operacion con ese nombre. */
    ModelMBeanOperationInfo getOperation(String inName)
        throws MBeanException, RuntimeOperationsException;

    /** El aviso con ese nombre. */
    ModelMBeanNotificationInfo getNotification(String inName)
        throws MBeanException, RuntimeOperationsException;

    /** Una copia. */
    Object clone();

    /** Los atributos. */
    MBeanAttributeInfo[] getAttributes();

    /** El nombre de la clase del MBean. */
    String getClassName();

    /** Los constructores. */
    MBeanConstructorInfo[] getConstructors();

    /** La descripcion. */
    String getDescription();

    /** Los avisos. */
    MBeanNotificationInfo[] getNotifications();

    /** Las operaciones. */
    MBeanOperationInfo[] getOperations();
}
