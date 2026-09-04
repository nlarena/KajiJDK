package javax.naming.event;

import java.util.EventListener;

/**
 * KajiLibrary's javax.naming.event.NamingListener -- la base de los oyentes de un contexto.
 *
 * <p>No declara ningun metodo de cambio: solo {@link #namingExceptionThrown}. Los cambios los
 * declaran sus dos subinterfaces, y esta division no es cosmetica -- un oyente se registra diciendo
 * <b>que</b> implementa, y el proveedor solo pide al servidor las notificaciones que alguien
 * escucha. Escuchar de mas cuesta trafico contra el directorio.
 *
 * <p>{@link #namingExceptionThrown} es lo que hay que implementar siempre y lo que casi nadie mira.
 * Cuando llega, la suscripcion <b>ya se cancelo</b>: el oyente no va a recibir nada mas. Ignorarla es
 * como termina un programa mirando un directorio que dejo de avisarle hace horas.
 */
public interface NamingListener extends EventListener {

    /**
     * La suscripcion fallo y quedo cancelada.
     *
     * <p>Ver la nota de la clase: no hay reintento automatico. Volver a escuchar es decision de
     * quien recibe esto.
     */
    void namingExceptionThrown(NamingExceptionEvent evt);
}
