package javax.management.relation;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationFilterSupport;
import javax.management.ObjectName;

/**
 * Un filtro que deja pasar las notificaciones de registro y desregistro <strong>solo de ciertos
 * MBeans</strong>.
 *
 * <h2>Por que hace falta filtrar por nombre y no solo por tipo</h2>
 *
 * <p>{@link NotificationFilterSupport} filtra por el <em>tipo</em> de notificacion, y el servidor de
 * MBeans emite un solo tipo para todos los registros. Suscribirse a el en un sistema con miles de
 * MBeans significa despertarse por cada uno.
 *
 * <p>Este filtro agrega la otra dimension: <em>cual</em> MBean. Es lo que le permite al servicio de
 * relaciones enterarse de que se desregistro justo uno de los que sus relaciones referencian, sin
 * mirar todos los demas.
 *
 * <h2>La lista negra y la blanca conviven</h2>
 *
 * <p>Y el orden entre ellas es lo que hay que entender: <strong>lo deshabilitado gana</strong>.
 * {@link #enableAllObjectNames} seguido de {@link #disableObjectName} es "todos menos ese", que es
 * la forma util de expresar una excepcion sin enumerar el resto.
 */
public class MBeanServerNotificationFilter extends NotificationFilterSupport {

    private static final long serialVersionUID = 2605900539589789736L;

    /** {@code null} significa "todos"; una lista significa "solo estos". */
    private List<ObjectName> selectedNames = new ArrayList<ObjectName>();

    /** {@code null} significa "todos deshabilitados"; una lista, "estos no". */
    private List<ObjectName> deselectedNames = null;

    /**
     * Un filtro que no deja pasar nada todavia.
     *
     * <p>Arranca cerrado a proposito: habilita el tipo de notificacion del servidor pero con la
     * lista de nombres vacia. Un filtro que arrancara abierto entregaria todo hasta que alguien se
     * acuerde de cerrarlo.
     */
    public MBeanServerNotificationFilter() {
        super();
        enableType(MBeanServerNotification.REGISTRATION_NOTIFICATION);
        enableType(MBeanServerNotification.UNREGISTRATION_NOTIFICATION);
    }

    /** Ningun MBean pasa. */
    public synchronized void disableAllObjectNames() {
        this.selectedNames = new ArrayList<ObjectName>();
        this.deselectedNames = null;
    }

    /**
     * Ese MBean no pasa, aunque este habilitado.
     *
     * @throws IllegalArgumentException si es {@code null}
     */
    public synchronized void disableObjectName(ObjectName objectName)
            throws IllegalArgumentException {
        if (objectName == null) {
            throw new IllegalArgumentException("el nombre no puede ser null");
        }
        if (this.selectedNames != null) {
            this.selectedNames.remove(objectName);
        }
        if (this.deselectedNames != null && !this.deselectedNames.contains(objectName)) {
            this.deselectedNames.add(objectName);
        }
    }

    /** Todos los MBeans pasan. */
    public synchronized void enableAllObjectNames() {
        this.selectedNames = null;
        this.deselectedNames = new ArrayList<ObjectName>();
    }

    /**
     * Ese MBean pasa.
     *
     * @throws IllegalArgumentException si es {@code null}
     */
    public synchronized void enableObjectName(ObjectName objectName)
            throws IllegalArgumentException {
        if (objectName == null) {
            throw new IllegalArgumentException("el nombre no puede ser null");
        }
        if (this.deselectedNames != null) {
            this.deselectedNames.remove(objectName);
        }
        if (this.selectedNames != null && !this.selectedNames.contains(objectName)) {
            this.selectedNames.add(objectName);
        }
    }

    /** Los habilitados, o {@code null} si estan todos. */
    public synchronized Vector<ObjectName> getEnabledObjectNames() {
        return this.selectedNames == null ? null : new Vector<ObjectName>(this.selectedNames);
    }

    /** Los deshabilitados, o {@code null} si lo estan todos. */
    public synchronized Vector<ObjectName> getDisabledObjectNames() {
        return this.deselectedNames == null ? null : new Vector<ObjectName>(this.deselectedNames);
    }

    /**
     * Si la notificacion pasa: primero por tipo, despues por nombre.
     *
     * <p>El orden importa por costo: la comprobacion de tipo es una comparacion de cadenas y
     * descarta casi todo antes de tocar las listas.
     */
    public synchronized boolean isNotificationEnabled(Notification notif)
            throws IllegalArgumentException {
        if (notif == null) {
            throw new IllegalArgumentException("la notificacion no puede ser null");
        }
        if (!super.isNotificationEnabled(notif)) {
            return false;
        }
        if (!(notif instanceof MBeanServerNotification)) {
            return false;
        }
        ObjectName name = ((MBeanServerNotification) notif).getMBeanName();
        // Lo deshabilitado gana; ver la nota de la clase.
        if (this.deselectedNames == null) {
            return false;
        }
        if (this.deselectedNames.contains(name)) {
            return false;
        }
        return this.selectedNames == null || this.selectedNames.contains(name);
    }
}
