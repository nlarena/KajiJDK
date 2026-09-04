package javax.management;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 * La implementacion lista para usar de {@link NotificationEmitter}: se hereda o se delega en ella.
 *
 * <p>Tres decisiones de esta clase que no se ven en la firma y que conviene conocer:
 *
 * <ul>
 *   <li><b>La lista es copy-on-write.</b> Es lo que permite que `sendNotification` recorra sin
 *       tomar el candado mientras otro hilo registra o saca oyentes. Con una lista comun habria que
 *       elegir entre sostener el candado durante la entrega --y quedar a merced de un oyente lento
 *       o reentrante-- o copiar en cada envio.
 *   <li><b>El filtro se evalua en el hilo que envia, la entrega puede ir en otro.</b> Es a
 *       proposito: filtrar es barato y descarta; despachar es lo caro. Con el `Executor` del
 *       constructor la entrega sale del hilo del emisor, sin el, va en el mismo.
 *   <li><b>La comparacion de filtro y handback es por identidad</b> (`==`), no por `equals`. Es lo
 *       que hace el JDK y hay que respetarlo: dos handbacks iguales pero distintos son dos
 *       registros distintos.
 * </ul>
 */
public class NotificationBroadcasterSupport implements NotificationEmitter {

    /** Compartido: devolver siempre el mismo arreglo vacio evita una asignacion por consulta. */
    private static final MBeanNotificationInfo[] SIN_INFO = new MBeanNotificationInfo[0];

    /** Corre la tarea en el hilo que llama; es el comportamiento sin `Executor`. */
    private static class EnEsteHilo implements Executor {
        public void execute(Runnable r) {
            r.run();
        }
    }

    private static final Executor ESTE_HILO = new EnEsteHilo();

    /** Un registro: el trio oyente/filtro/handback, que es la unidad que se saca. */
    private static class Registro {
        final NotificationListener oyente;
        final NotificationFilter filtro;
        final Object handback;

        Registro(NotificationListener oyente, NotificationFilter filtro, Object handback) {
            this.oyente = oyente;
            this.filtro = filtro;
            this.handback = handback;
        }
    }

    private final List<Registro> registros = new CopyOnWriteArrayList<Registro>();
    private final Executor ejecutor;
    private final MBeanNotificationInfo[] info;

    /** Entrega en el hilo del emisor y sin declarar que notificaciones emite. */
    public NotificationBroadcasterSupport() {
        this(null, (MBeanNotificationInfo[]) null);
    }

    /** Entrega a traves del `Executor`; si es `null`, en el hilo del emisor. */
    public NotificationBroadcasterSupport(Executor executor) {
        this(executor, (MBeanNotificationInfo[]) null);
    }

    /** Declara que notificaciones emite; entrega en el hilo del emisor. */
    public NotificationBroadcasterSupport(MBeanNotificationInfo... info) {
        this(null, info);
    }

    /**
     * El completo.
     *
     * <p>El arreglo se copia al entrar y al salir de {@link #getNotificationInfo}: es la unica
     * forma de que lo que declara el MBean no cambie a espaldas de quien lo consulto.
     */
    public NotificationBroadcasterSupport(Executor executor, MBeanNotificationInfo... info) {
        this.ejecutor = (executor == null) ? ESTE_HILO : executor;
        if (info == null || info.length == 0) {
            this.info = SIN_INFO;
        } else {
            MBeanNotificationInfo[] copia = new MBeanNotificationInfo[info.length];
            System.arraycopy(info, 0, copia, 0, info.length);
            this.info = copia;
        }
    }

    /**
     * @param listener no puede ser `null`
     * @param filter `null` significa "todas"
     * @throws IllegalArgumentException si el oyente es `null`
     */
    public void addNotificationListener(NotificationListener listener, NotificationFilter filter,
                                        Object handback) {
        if (listener == null) {
            throw new IllegalArgumentException("El oyente no puede ser null");
        }
        registros.add(new Registro(listener, filter, handback));
    }

    /**
     * Saca <b>todos</b> los registros de ese oyente, con cualquier filtro y handback.
     *
     * @throws ListenerNotFoundException si no habia ninguno
     */
    public void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {
        boolean alguno = false;
        // Se recorre una copia porque `registros` es copy-on-write y su iterador no soporta remove.
        for (Registro r : registros.toArray(new Registro[0])) {
            if (r.oyente == listener) {
                registros.remove(r);
                alguno = true;
            }
        }
        if (!alguno) {
            throw new ListenerNotFoundException("El oyente no estaba registrado");
        }
    }

    /**
     * Saca <b>un</b> registro: el que coincide en los tres por identidad.
     *
     * <p>Si el mismo trio se registro dos veces, esta llamada saca uno solo. Es asi en el JDK y es
     * coherente con que `add` no deduplique.
     *
     * @throws ListenerNotFoundException si no hay ninguno que coincida
     */
    public void removeNotificationListener(NotificationListener listener, NotificationFilter filter,
                                           Object handback) throws ListenerNotFoundException {
        for (Registro r : registros) {
            if (r.oyente == listener && r.filtro == filter && r.handback == handback) {
                registros.remove(r);
                return;
            }
        }
        throw new ListenerNotFoundException("No hay un registro con ese oyente, filtro y handback");
    }

    /** Lo que este emisor declara que puede emitir; copia defensiva. */
    public MBeanNotificationInfo[] getNotificationInfo() {
        if (info.length == 0) {
            return SIN_INFO;
        }
        MBeanNotificationInfo[] copia = new MBeanNotificationInfo[info.length];
        System.arraycopy(info, 0, copia, 0, info.length);
        return copia;
    }

    /**
     * Manda la notificacion a los oyentes cuyo filtro la deje pasar.
     *
     * <p>Si un filtro tira, la excepcion sale de aca sin envolver y sin haber entregado a los
     * oyentes que faltaban: es lo que hace el JDK, y esconderla haria que un filtro roto pareciera
     * un filtro que niega.
     */
    public void sendNotification(Notification notification) {
        if (notification == null) {
            return;
        }
        for (final Registro r : registros) {
            NotificationFilter f = r.filtro;
            if (f != null && !f.isNotificationEnabled(notification)) {
                continue;
            }
            final Notification n = notification;
            ejecutor.execute(new Runnable() {
                public void run() {
                    handleNotification(r.oyente, n, r.handback);
                }
            });
        }
    }

    /**
     * El punto de extension: por omision llama al oyente, y se redefine para envolver la entrega
     * --por ejemplo para atajar lo que tire el oyente y que no tumbe al emisor--.
     */
    protected void handleNotification(NotificationListener listener, Notification notif,
                                      Object handback) {
        listener.handleNotification(notif, handback);
    }
}
