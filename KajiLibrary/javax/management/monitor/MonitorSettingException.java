package javax.management.monitor;

import javax.management.JMRuntimeException;

/**
 * KajiLibrary's javax.management.monitor.MonitorSettingException -- el monitor esta mal configurado.
 *
 * <p>Es una {@link JMRuntimeException}, o sea <b>no comprobada</b>, y eso vale explicarlo: la
 * lanza el hilo del monitor mientras observa, no quien lo configuro. Para cuando salta, el que se
 * equivoco al configurar ya se fue, asi que obligarlo a atajarla no habria servido de nada.
 *
 * <p>En la practica casi no se ve: la mayoria de los errores de configuracion los ataja el propio
 * setter con un {@code IllegalArgumentException} en el momento. Esta queda para lo que solo se puede
 * saber mirando el atributo de verdad --un umbral de un tipo que no compara con el valor observado--
 * y eso recien pasa cuando el monitor esta corriendo.
 */
public class MonitorSettingException extends JMRuntimeException {

    private static final long serialVersionUID = -8807913418190202007L;

    /** Sin detalle. */
    public MonitorSettingException() {
        super();
    }

    /** Con un mensaje que diga que setting esta mal. */
    public MonitorSettingException(String message) {
        super(message);
    }
}
