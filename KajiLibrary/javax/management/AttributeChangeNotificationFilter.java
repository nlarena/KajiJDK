package javax.management;

import java.util.List;
import java.util.Vector;

/**
 * Filtro de {@link AttributeChangeNotification} por <b>nombre de atributo</b>.
 *
 * <p>La diferencia con {@link NotificationFilterSupport} es doble: aca la comparacion es por
 * <b>igualdad exacta</b> y no por prefijo --los nombres de atributo no tienen jerarquia--, y ademas
 * el filtro exige que la notificacion sea realmente un `AttributeChangeNotification`. Cualquier
 * otra queda afuera aunque su tipo coincida.
 *
 * <p>Igual que el otro, arranca vacio y por lo tanto bloqueando todo.
 */
public class AttributeChangeNotificationFilter implements NotificationFilter {

    private static final long serialVersionUID = -6347317584796410029L;

    /**
     * @serial los nombres de atributo habilitados
     */
    private List<String> enabledAttributes = new Vector<String>();

    /** Con la lista vacia: no pasa nada hasta que se habilite algun atributo. */
    public AttributeChangeNotificationFilter() {
    }

    /**
     * Deja pasar solo los cambios de atributo cuyo nombre este habilitado.
     */
    public synchronized boolean isNotificationEnabled(Notification notification) {
        String tipo = notification.getType();
        if (tipo == null
                || !tipo.equals(AttributeChangeNotification.ATTRIBUTE_CHANGE)
                || !(notification instanceof AttributeChangeNotification)) {
            return false;
        }
        String nombre = ((AttributeChangeNotification) notification).getAttributeName();
        if (nombre == null) {
            return false;
        }
        return enabledAttributes.contains(nombre);
    }

    /**
     * Habilita un nombre de atributo.
     *
     * @throws IllegalArgumentException si es `null`, por la misma razon que en
     *         {@link NotificationFilterSupport#enableType}: fallar aca y no en cada entrega.
     */
    public synchronized void enableAttribute(String name) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("El nombre del atributo no puede ser null");
        }
        if (!enabledAttributes.contains(name)) {
            enabledAttributes.add(name);
        }
    }

    /** Saca ese nombre; si no estaba, no hace nada. */
    public synchronized void disableAttribute(String name) {
        enabledAttributes.remove(name);
    }

    /** Vuelve al estado inicial: bloquea todo. */
    public synchronized void disableAllAttributes() {
        enabledAttributes.clear();
    }

    /**
     * Los nombres habilitados; es la lista interna, igual que en el JDK.
     */
    public synchronized Vector<String> getEnabledAttributes() {
        return (Vector<String>) enabledAttributes;
    }
}
