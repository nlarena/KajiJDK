package javax.management.modelmbean;

import javax.management.DynamicMBean;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.PersistentMBean;
import javax.management.RuntimeOperationsException;

/**
 * KajiLibrary's javax.management.modelmbean.ModelMBean -- un MBean cuya interfaz se declara en
 * tiempo de ejecucion.
 *
 * <p>Es la cuarta clase de MBean, y la mas flexible. Los otros tres atan la interfaz de
 * administracion al codigo: un MBean estandar la saca de una interfaz Java, uno dinamico la declara
 * en su {@code MBeanInfo}, uno abierto ademas restringe los tipos. Un <b>model MBean</b> se
 * configura: se le da un objeto cualquiera y una descripcion de que metodos suyos exponer.
 *
 * <p>Con eso se puede administrar una clase que no sabe nada de JMX y que no se puede modificar. Es
 * el caso para el que existe.
 *
 * <p>Junta tres interfaces y ninguna sobra: {@link DynamicMBean} para responder consultas,
 * {@link PersistentMBean} para poder guardarse y recuperarse, y
 * {@link ModelMBeanNotificationBroadcaster} para mandar avisos. Los dos metodos propios son los que
 * lo configuran: que exponer, y de que objeto.
 */
public interface ModelMBean
    extends DynamicMBean, PersistentMBean, ModelMBeanNotificationBroadcaster {

    /**
     * Que se expone.
     *
     * <p>Tiene que llamarse <b>antes</b> de registrar el MBean en un agente: despues, la interfaz de
     * administracion ya se publico y cambiarla dejaria a los clientes mirando algo que no existe.
     *
     * @throws RuntimeOperationsException si es null
     */
    void setModelMBeanInfo(ModelMBeanInfo inModelMBeanInfo)
        throws MBeanException, RuntimeOperationsException;

    /**
     * De que objeto.
     *
     * @param mr el objeto a administrar
     * @param mr_type el tipo de referencia; ver {@link InvalidTargetObjectTypeException}
     * @throws InvalidTargetObjectTypeException si ese tipo no esta soportado
     */
    void setManagedResource(Object mr, String mr_type)
        throws MBeanException, RuntimeOperationsException, InstanceNotFoundException,
               InvalidTargetObjectTypeException;
}
