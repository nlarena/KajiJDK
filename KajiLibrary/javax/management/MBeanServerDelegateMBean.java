package javax.management;

/**
 * La cara publica del propio agente: es el MBean estandar que describe al servidor de MBeans.
 *
 * <p>Se registra siempre bajo {@code JMImplementation:type=MBeanServerDelegate} y es tambien quien
 * emite los {@link MBeanServerNotification} de alta y baja. O sea: el agente se administra a si
 * mismo por las mismas vias que administra a los demas, sin API aparte.
 */
public interface MBeanServerDelegateMBean {

    /** Identificador unico de esta instancia de agente. */
    String getMBeanServerId();

    /** El nombre de la especificacion que se cumple: "Java Management Extensions". */
    String getSpecificationName();

    /** La version de la especificacion. */
    String getSpecificationVersion();

    /** Quien publico la especificacion. */
    String getSpecificationVendor();

    /** El nombre de esta implementacion. */
    String getImplementationName();

    /** Su version. */
    String getImplementationVersion();

    /** Su fabricante. */
    String getImplementationVendor();
}
