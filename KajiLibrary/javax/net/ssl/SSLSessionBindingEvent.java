package javax.net.ssl;

import java.util.EventObject;

/**
 * Un objeto de la aplicacion entro o salio de una {@link SSLSession}.
 *
 * <p>La fuente del evento es la sesion, asi que {@link #getSession} y {@code getSource} devuelven lo
 * mismo con distinto tipo. Los dos estan porque {@link EventObject} obliga al segundo y nadie
 * quiere castear.
 */
public class SSLSessionBindingEvent extends EventObject {

    private static final long serialVersionUID = 3989172637106345L;

    private final String name;

    /**
     * @throws IllegalArgumentException si la sesion es {@code null}
     */
    public SSLSessionBindingEvent(SSLSession session, String name) {
        super(session);
        this.name = name;
    }

    /** El nombre con el que el valor estaba guardado. */
    public String getName() {
        return this.name;
    }

    /** La sesion donde paso. */
    public SSLSession getSession() {
        return (SSLSession) getSource();
    }
}
