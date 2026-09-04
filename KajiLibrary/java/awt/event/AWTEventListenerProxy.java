package java.awt.event;

import java.awt.AWTEvent;
import java.util.EventListenerProxy;

/**
 * Un {@link AWTEventListener} con la máscara de qué familias le interesan pegada.
 *
 * <p>El `Toolkit` guarda todos los oyentes globales en una sola lista, y sin esto no habría forma de
 * preguntarle **con qué máscara** se registró cada uno: el oyente solo no lo dice. Envolverlo
 * conserva ese dato para que {@code getAWTEventListeners} pueda devolverlo.
 */
public class AWTEventListenerProxy extends EventListenerProxy<AWTEventListener>
        implements AWTEventListener {

    private final long eventMask;

    /**
     * Con la máscara y el oyente.
     *
     * @throws NullPointerException si el oyente es `null`
     */
    public AWTEventListenerProxy(long eventMask, AWTEventListener listener) {
        super(listener);
        this.eventMask = eventMask;
    }

    /** Le pasa el evento al oyente envuelto. */
    public void eventDispatched(AWTEvent event) {
        this.getListener().eventDispatched(event);
    }

    /** Con qué máscara se registró. */
    public long getEventMask() {
        return this.eventMask;
    }
}
