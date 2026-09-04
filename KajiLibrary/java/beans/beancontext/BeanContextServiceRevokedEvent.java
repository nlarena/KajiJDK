package java.beans.beancontext;

/**
 * Un servicio deja de estar disponible.
 *
 * <p>{@link #isCurrentServiceInvalidNow} es la parte que importa y la que se lee mal: distingue
 * *"no pidas más"* de *"lo que tenés en la mano ya no sirve"*. En el primer caso el usuario puede
 * seguir usando su instancia hasta terminar; en el segundo tiene que soltarla ya. Tratar los dos
 * igual significa o bien perder trabajo a medio hacer, o bien usar un objeto muerto.
 */
public class BeanContextServiceRevokedEvent extends BeanContextEvent {

    /** La clase del servicio revocado. */
    protected Class serviceClass;

    private final boolean invalidateRefs;

    /** El evento de revocación de esa clase de servicio. */
    public BeanContextServiceRevokedEvent(BeanContextServices bcs, Class sc,
            boolean invalidate) {
        super((BeanContext) bcs);
        this.serviceClass = sc;
        this.invalidateRefs = invalidate;
    }

    /** El contexto que lo revoca. */
    public BeanContextServices getSourceAsBeanContextServices() {
        return (BeanContextServices) this.getBeanContext();
    }

    /** La clase del servicio. */
    public Class getServiceClass() {
        return this.serviceClass;
    }

    /** Si esa clase es la del servicio revocado. */
    public boolean isServiceClass(Class service) {
        return this.serviceClass.equals(service);
    }

    /** Si las instancias ya entregadas dejan de valer en este momento. */
    public boolean isCurrentServiceInvalidNow() {
        return this.invalidateRefs;
    }
}
