package javax.naming.event;

import java.util.EventObject;
import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.event.NamingExceptionEvent -- la suscripcion se cayo.
 *
 * <p>Lo que llega a {@link NamingListener#namingExceptionThrown}. Para cuando llega, el proveedor
 * <b>ya cancelo</b> la suscripcion del oyente: no es un aviso de que algo anduvo mal y sigue, es el
 * ultimo evento.
 *
 * <p>{@link #dispatch} existe para que quien reparte eventos no tenga que saber que metodo llamar en
 * cada tipo: el evento se despacha a si mismo. Es el mismo patron que usa {@code java.awt.AWTEvent},
 * y es lo que permite tener una cola de eventos de tipos distintos sin un {@code instanceof} por
 * cada uno.
 */
public class NamingExceptionEvent extends EventObject {

    private static final long serialVersionUID = -4877678086134736336L;

    /** Lo que fallo. */
    private final NamingException exception;

    /**
     * @param source el contexto donde estaba la suscripcion
     * @param exc lo que fallo
     */
    public NamingExceptionEvent(EventContext source, NamingException exc) {
        super(source);
        this.exception = exc;
    }

    /** Lo que fallo. */
    public NamingException getException() {
        return this.exception;
    }

    /** El contexto donde estaba la suscripcion. */
    public EventContext getEventContext() {
        return (EventContext) getSource();
    }

    /** Se despacha al oyente. Ver la nota de la clase. */
    public void dispatch(NamingListener listener) {
        listener.namingExceptionThrown(this);
    }
}
