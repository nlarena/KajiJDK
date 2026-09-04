package java.beans.beancontext;

import java.util.Iterator;

/** Un servicio nuevo está disponible en un {@link BeanContextServices}. */
public class BeanContextServiceAvailableEvent extends BeanContextEvent {

    /** La clase del servicio que apareció. */
    protected Class serviceClass;

    /** El evento del servicio de esa clase. */
    public BeanContextServiceAvailableEvent(BeanContextServices bcs, Class sc) {
        super((BeanContext) bcs);
        this.serviceClass = sc;
    }

    /** El contexto que lo anuncia. */
    public BeanContextServices getSourceAsBeanContextServices() {
        return (BeanContextServices) this.getBeanContext();
    }

    /** La clase del servicio. */
    public Class getServiceClass() {
        return this.serviceClass;
    }

    /**
     * Los selectores que el servicio acepta, o `null` si no usa selectores.
     *
     * <p>Se le pregunta al contexto en el momento de la consulta y no se guarda en el evento: entre
     * que el servicio se anunció y que alguien mira el evento, el proveedor pudo cambiar lo que
     * acepta, y una copia vieja sería peor que ninguna.
     */
    public Iterator getCurrentServiceSelectors() {
        return this.getSourceAsBeanContextServices().getCurrentServiceSelectors(this.serviceClass);
    }
}
