package java.beans.beancontext;

/**
 * Escucha la aparición de servicios nuevos, además de su revocación.
 *
 * <p>Extiende a {@link BeanContextServiceRevokedListener} y no lo repite: quien quiere enterarse de
 * las altas casi siempre quiere enterarse también de las bajas, y separarlos obligaría a registrar
 * dos oyentes para seguir un mismo servicio.
 */
public interface BeanContextServicesListener extends BeanContextServiceRevokedListener {

    /** Hay un servicio nuevo disponible. */
    void serviceAvailable(BeanContextServiceAvailableEvent bcsae);
}
