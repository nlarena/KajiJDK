package java.beans.beancontext;

import java.beans.BeanInfo;

/**
 * El `BeanInfo` de un proveedor que además describe a los servicios que ofrece.
 *
 * <p>Sirve para una herramienta de diseño: sin esto, lo único que se puede mostrar del proveedor es
 * el proveedor mismo, y lo que al usuario le interesa son los servicios.
 */
public interface BeanContextServiceProviderBeanInfo extends BeanInfo {

    /** Un `BeanInfo` por servicio ofrecido. */
    BeanInfo[] getServicesBeanInfo();
}
