package java.lang.management;

import javax.management.ObjectName;

/**
 * KajiLibrary's java.lang.management.PlatformManagedObject -- lo que puede publicarse como MBean de la
 * plataforma.
 *
 * <p>Un solo metodo, y la interfaz existe por lo que <b>permite</b>: como todas las MXBean de la
 * plataforma la extienden, {@code ManagementFactory.getPlatformMXBean(Class)} puede pedir cualquiera
 * de ellas con una sola firma generica.
 *
 * <p>{@link #getObjectName} devuelve el nombre con el que ese objeto figura en el servidor de MBeans
 * de la plataforma -- por ejemplo {@code java.lang:type=Memory}. Es el puente entre la API tipada de
 * este paquete y la API por nombre de {@code javax.management}.
 */
public interface PlatformManagedObject {

    /** Con que nombre figura en el servidor de MBeans de la plataforma. */
    ObjectName getObjectName();
}
