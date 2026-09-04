package javax.net.ssl;

import java.util.EventListener;

/**
 * Se entera cuando un objeto propio entra o sale de una {@link SSLSession}.
 *
 * <p>Una sesion puede guardar objetos de la aplicacion con {@link SSLSession#putValue}. Si el objeto
 * guardado implementa esta interfaz, la sesion le avisa — y eso le da la oportunidad de soltar lo
 * que tenga tomado cuando lo sacan o cuando la sesion se invalida. Sin este aviso, un objeto
 * guardado en una sesion que muere no tendria forma de saberlo.
 */
public interface SSLSessionBindingListener extends EventListener {

    /** Lo acaban de guardar en una sesion. */
    void valueBound(SSLSessionBindingEvent event);

    /** Lo acaban de sacar, o la sesion se invalido. */
    void valueUnbound(SSLSessionBindingEvent event);
}
