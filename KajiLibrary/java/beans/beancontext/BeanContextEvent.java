package java.beans.beancontext;

import java.util.EventObject;

/**
 * La raíz de los eventos de esta API.
 *
 * <p>Lo que agrega sobre {@link EventObject} es la **propagación**: un contexto anidado reenvía a
 * sus hijos los eventos que le llegan de su padre, y cuando lo hace marca de dónde venían. Sin esa
 * marca, un oyente no podría distinguir un evento propio del contexto que escucha de uno que sólo
 * pasó por ahí, y volvería a reenviarlo — un ciclo en una jerarquía con más de un camino.
 *
 * <p>`setPropagatedFrom` se puede llamar una vez y por el contexto que reenvía, no por cualquiera.
 * Es lo que el JDK deja hacer y no se restringe más acá: la API no tiene por dónde comprobarlo.
 */
public abstract class BeanContextEvent extends EventObject {

    /** El contexto del que se propagó este evento, o `null` si es de primera mano. */
    protected BeanContext propagatedFrom;

    /** El evento originado en ese contexto. */
    protected BeanContextEvent(BeanContext bc) {
        super(bc);
    }

    /** El contexto que originó el evento. */
    public BeanContext getBeanContext() {
        return (BeanContext) this.getSource();
    }

    /** Marca de dónde se propagó. `null` lo vuelve a dejar como de primera mano. */
    public synchronized void setPropagatedFrom(BeanContext bc) {
        this.propagatedFrom = bc;
    }

    /** De dónde se propagó, o `null`. */
    public synchronized BeanContext getPropagatedFrom() {
        return this.propagatedFrom;
    }

    /** Si este evento viene reenviado de otro contexto. */
    public synchronized boolean isPropagated() {
        return this.propagatedFrom != null;
    }
}
