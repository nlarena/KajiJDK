package java.beans.beancontext;

import java.util.Iterator;
import java.util.TooManyListenersException;

/**
 * Un {@link BeanContext} que además reparte **servicios**.
 *
 * <p>Un servicio es un objeto identificado por su clase que el contexto le consigue a quien lo pida.
 * La diferencia con simplemente instanciarlo es la búsqueda: si este contexto no lo tiene, el pedido
 * **sube por la jerarquía**, así que un hijo hondo puede usar algo que registró la raíz sin saber
 * dónde está.
 *
 * <p>Implementa {@link BeanContextServicesListener} porque un contexto anidado es a la vez oyente de
 * su padre: así se entera de los servicios que aparecen más arriba y se los reenvía a sus propios
 * hijos.
 */
public interface BeanContextServices extends BeanContext, BeanContextServicesListener {

    /** Da de alta un proveedor para esa clase de servicio. `false` si ya había uno. */
    boolean addService(Class serviceClass, BeanContextServiceProvider serviceProvider);

    /**
     * Da de baja ese servicio.
     *
     * @param revokeCurrentServicesNow si además hay que invalidar las instancias ya entregadas
     */
    void revokeService(Class serviceClass, BeanContextServiceProvider serviceProvider,
            boolean revokeCurrentServicesNow);

    /** Si el servicio está disponible acá o más arriba. */
    boolean hasService(Class serviceClass);

    /**
     * Una instancia del servicio para ese hijo.
     *
     * @throws TooManyListenersException si el oyente de revocación no se pudo registrar
     */
    Object getService(BeanContextChild child, Object requestor, Class serviceClass,
            Object serviceSelector, BeanContextServiceRevokedListener bcsrl)
            throws TooManyListenersException;

    /** El hijo ya no necesita esa instancia. */
    void releaseService(BeanContextChild child, Object requestor, Object service);

    /** Las clases de servicio disponibles. */
    Iterator getCurrentServiceClasses();

    /** Los selectores que acepta ese servicio, o `null` si no usa selectores. */
    Iterator getCurrentServiceSelectors(Class serviceClass);

    /** Registra un oyente de altas y bajas de servicios. */
    void addBeanContextServicesListener(BeanContextServicesListener bcsl);

    /** Lo quita. */
    void removeBeanContextServicesListener(BeanContextServicesListener bcsl);
}
