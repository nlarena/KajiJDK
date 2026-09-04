package javax.management;

import java.util.List;
import java.util.Vector;

/**
 * Filtro por tipo de notificacion, con la semantica de <b>prefijo</b>.
 *
 * <p>La sutileza esta ahi: `enableType("a.b")` no habilita el tipo `a.b` sino todo el que
 * <b>empiece</b> con `a.b`. Es lo que hace util la convencion de puntos --`jmx.mbean.registered` y
 * `jmx.mbean.unregistered` se habilitan los dos con `jmx.mbean`-- y tambien lo que explica que
 * `enableType("")` habilite todo.
 *
 * <p>Arranca con la lista vacia, o sea <b>bloqueando</b> todo. Es al reves de lo que sugiere
 * "filtro por omision" y es intencional: un filtro recien construido y nunca configurado no deja
 * pasar nada.
 */
public class NotificationFilterSupport implements NotificationFilter {

    private static final long serialVersionUID = 6579080007561786969L;

    /**
     * @serial los prefijos habilitados
     */
    private List<String> enabledTypes = new Vector<String>();

    /** Con la lista vacia: no pasa ninguna notificacion hasta que se habilite algun prefijo. */
    public NotificationFilterSupport() {
    }

    /**
     * Deja pasar si el tipo de la notificacion empieza con alguno de los prefijos habilitados.
     */
    public synchronized boolean isNotificationEnabled(Notification notification) {
        String tipo = notification.getType();
        if (tipo == null) {
            return false;
        }
        for (int i = 0; i < enabledTypes.size(); i++) {
            if (tipo.startsWith(enabledTypes.get(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Habilita un prefijo.
     *
     * @throws IllegalArgumentException si es `null`. No se acepta porque un `null` en la lista
     *         haria fallar cada evaluacion posterior del filtro, lejos de donde estuvo el error.
     */
    public synchronized void enableType(String prefix) throws IllegalArgumentException {
        if (prefix == null) {
            throw new IllegalArgumentException("El prefijo del tipo no puede ser null");
        }
        // Idempotente: repetir el mismo prefijo no cambia lo que el filtro deja pasar, y guardarlo
        // dos veces solo alargaria el recorrido.
        if (!enabledTypes.contains(prefix)) {
            enabledTypes.add(prefix);
        }
    }

    /** Saca ese prefijo exacto; si no estaba, no hace nada. */
    public synchronized void disableType(String prefix) {
        enabledTypes.remove(prefix);
    }

    /** Vuelve al estado inicial: bloquea todo. */
    public synchronized void disableAllTypes() {
        enabledTypes.clear();
    }

    /**
     * Los prefijos habilitados.
     *
     * <p>Devuelve la lista <b>interna</b>, igual que el JDK: modificarla modifica el filtro. La
     * firma historica es `Vector` y no se puede angostar sin romper a quien la asigne.
     */
    public synchronized Vector<String> getEnabledTypes() {
        return (Vector<String>) enabledTypes;
    }
}
