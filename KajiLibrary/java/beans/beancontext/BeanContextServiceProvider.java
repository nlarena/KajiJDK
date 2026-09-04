package java.beans.beancontext;

import java.util.Iterator;

/**
 * Quien fabrica las instancias de un servicio.
 *
 * <p>El contexto no implementa los servicios: los registra y delega. Un proveedor se da de alta con
 * `addService(Class, BeanContextServiceProvider)` y desde ahí atiende los pedidos de esa clase.
 *
 * <p>El `requestor` que reciben los tres métodos **no es decorativo**: el proveedor puede devolver
 * instancias distintas según quién pida, y `releaseService` necesita saber a quién se le había dado
 * qué. Un proveedor que ignore ese parámetro sólo puede ofrecer un singleton.
 */
public interface BeanContextServiceProvider {

    /**
     * Una instancia del servicio para ese solicitante.
     *
     * @param serviceSelector un parámetro del servicio, o `null` si no lleva
     * @return la instancia, o `null` si no se puede dar
     */
    Object getService(BeanContextServices bcs, Object requestor, Class serviceClass,
            Object serviceSelector);

    /** El solicitante ya no necesita esa instancia. */
    void releaseService(BeanContextServices bcs, Object requestor, Object service);

    /** Los selectores que este proveedor acepta para esa clase, o `null` si no usa selectores. */
    Iterator getCurrentServiceSelectors(BeanContextServices bcs, Class serviceClass);
}
