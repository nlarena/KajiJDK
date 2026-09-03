package javax.management.timer;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.Vector;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

/**
 * KajiLibrary's javax.management.timer.Timer -- el reloj que manda notificaciones.
 *
 * <p>Es un MBean que se registra en un agente y al que se le piden avisos para una fecha, con o sin
 * repeticion. Sirve para que un cliente JMX programe algo <b>del lado del agente</b> sin tener que
 * dejar un hilo propio esperando del otro lado de la red.
 *
 * <h2>Las fechas pasadas</h2>
 *
 * <p>Es la parte del comportamiento que no se adivina. Una notificacion inscrita para una fecha que
 * ya paso --porque el reloj estuvo parado, o porque se inscribio con fecha vieja-- se resuelve al
 * arrancar segun {@link #getSendPastNotifications}:
 *
 * <ul>
 *   <li>si es false, la de una sola vez se <b>descarta</b> sin avisar y se borra de la tabla; la
 *       periodica corre su fecha hacia adelante hasta la primera que este en el futuro;
 *   <li>si es true, se manda <b>una</b> vez y despues sigue el mismo camino.
 * </ul>
 *
 * <p>La de una sola vez que se descarta no se llega a programar, asi que <b>no puede</b> salir. Vale
 * decirlo porque el JDK ahi tiene una carrera: programa la atrasada para ya y recien despues decide
 * darla de baja, asi que segun cuan rapido arranque el hilo del reloj el aviso sale o no sale. Lo
 * que hace esta clase es lo que dice la especificacion, y ademas siempre lo mismo.
 *
 * <p>Se manda una sola vez y no una por cada repeticion perdida: un reloj parado dos dias con
 * periodo de un minuto largaria casi tres mil avisos de golpe, y ninguno de ellos serviria para
 * nada. Por la misma razon las repeticiones que se saltearon <b>no</b> se descuentan de
 * {@code nbOccurences}: lo que se pidio fue "avisame tantas veces", no "tantas ranuras de reloj".
 *
 * <h2>El hilo no es demonio</h2>
 *
 * <p>Mientras el reloj esta activo, su hilo mantiene viva a la maquina virtual. Es a proposito y hay
 * que saberlo: un programa que arranca un {@link Timer} y no lo para <b>no termina</b>. La
 * alternativa --un hilo demonio-- perderia avisos justo cuando el programa esta cerrando, que es
 * cuando suelen importar.
 *
 * <h2>Fijo contra retardado</h2>
 *
 * <p>Con {@code fixedRate} en true la proxima fecha se calcula desde la <b>anterior programada</b>,
 * asi que el ritmo promedio se mantiene aunque un disparo salga tarde. Con false se calcula desde el
 * momento en que salio, asi que un atraso se arrastra. Lo primero sirve para muestrear en el tiempo;
 * lo segundo, para dejar un hueco garantizado entre dos tareas pesadas.
 */
public class Timer extends NotificationBroadcasterSupport implements TimerMBean, MBeanRegistration {

    /** Un segundo, en milisegundos. */
    public static final long ONE_SECOND = 1000;

    /** Un minuto. */
    public static final long ONE_MINUTE = 60 * ONE_SECOND;

    /** Una hora. */
    public static final long ONE_HOUR = 60 * ONE_MINUTE;

    /** Un dia. */
    public static final long ONE_DAY = 24 * ONE_HOUR;

    /** Una semana. */
    public static final long ONE_WEEK = 7 * ONE_DAY;

    /** El tipo declarado en {@link #getNotificationInfo}. */
    private static final String NOTIFICATION_CLASS = "javax.management.timer.TimerNotification";

    /** Las inscripciones, en orden de alta. */
    private final Map<Integer, Registration> table = new LinkedHashMap<Integer, Registration>();

    /** El proximo identificador. Vuelve a 1 cuando la tabla queda vacia. */
    private int nextId = 1;

    /** El numero de secuencia, que avanza en cada <b>envio</b>. */
    private long sequence = 1;

    /** Ver la nota de la clase sobre fechas pasadas. */
    private boolean sendPastNotifications = false;

    /** Null mientras el reloj esta parado. */
    private java.util.Timer engine;

    /** El agente donde esta registrado, o null. */
    private MBeanServer server;

    /** El nombre bajo el que se registro, o null. */
    private ObjectName objectName;

    /** Un reloj parado y sin inscripciones. */
    public Timer() {
    }

    // ---- MBeanRegistration -----------------------------------------------------------------

    /** Se queda con el agente y el nombre; no arranca nada. */
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        this.server = server;
        this.objectName = name;
        return name;
    }

    /** Nada que hacer. */
    public void postRegister(Boolean registrationDone) {
    }

    /** Nada que hacer: parar antes de tiempo dejaria avisos sin mandar si la baja falla. */
    public void preDeregister() throws Exception {
    }

    /** Para el reloj: ya no hay a quien avisarle. */
    public void postDeregister() {
        stop();
    }

    /**
     * Que manda este MBean.
     *
     * <p>Los tipos salen de las inscripciones que hay <b>en este momento</b>, ordenados. No pueden
     * ser una lista fija porque los elige quien inscribe, no esta clase.
     */
    public synchronized MBeanNotificationInfo[] getNotificationInfo() {
        TreeSet<String> types = new TreeSet<String>();
        for (Registration r : this.table.values()) {
            types.add(r.type);
        }
        String[] asArray = types.toArray(new String[types.size()]);
        return new MBeanNotificationInfo[] {
            new MBeanNotificationInfo(asArray, NOTIFICATION_CLASS, "Notification sent by Timer MBean")
        };
    }

    // ---- arranque y parada -----------------------------------------------------------------

    /**
     * Arranca el reloj.
     *
     * <p>Aca se resuelven las fechas pasadas; ver la nota de la clase. Si ya estaba activo no hace
     * nada, ni siquiera reprogramar.
     */
    public synchronized void start() {
        if (this.engine != null) {
            return;
        }
        this.engine = new java.util.Timer("timer-mbean", false);
        long now = System.currentTimeMillis();
        List<Registration> all = new ArrayList<Registration>(this.table.values());
        for (Registration r : all) {
            if (r.date.getTime() > now) {
                schedule(r);
            } else {
                catchUp(r, now);
            }
        }
        if (this.table.isEmpty()) {
            this.nextId = 1;
        }
    }

    /**
     * Para el reloj.
     *
     * <p>Las inscripciones quedan: un {@link #start} posterior las vuelve a programar, y las que
     * hayan quedado atrasadas pasan por la regla de fechas pasadas.
     */
    public synchronized void stop() {
        if (this.engine == null) {
            return;
        }
        for (Registration r : this.table.values()) {
            if (r.task != null) {
                r.task.cancel();
                r.task = null;
            }
        }
        this.engine.cancel();
        this.engine = null;
    }

    // ---- altas y bajas ---------------------------------------------------------------------

    /** Ver {@link TimerMBean#addNotification(String, String, Object, Date, long, long, boolean)}. */
    public synchronized Integer addNotification(String type, String message, Object userData,
                                                Date date, long period, long nbOccurences,
                                                boolean fixedRate) throws IllegalArgumentException {
        if (date == null) {
            throw new IllegalArgumentException("Timer notification date cannot be null");
        }
        if (period < 0) {
            throw new IllegalArgumentException("Negative period");
        }
        if (nbOccurences < 0) {
            throw new IllegalArgumentException("Negative number of occurrences");
        }
        Integer id = Integer.valueOf(this.nextId);
        this.nextId = this.nextId + 1;
        Registration r = new Registration();
        r.id = id;
        r.type = type;
        r.message = message;
        r.userData = userData;
        // Copia: quien inscribio se puede quedar con el `Date` y mutarlo.
        r.date = new Date(date.getTime());
        r.period = period;
        r.occurrences = nbOccurences;
        r.fixedRate = fixedRate;
        this.table.put(id, r);
        if (this.engine != null) {
            long now = System.currentTimeMillis();
            if (r.date.getTime() > now) {
                schedule(r);
            } else {
                catchUp(r, now);
            }
        }
        return id;
    }

    /** Ver {@link TimerMBean}. */
    public synchronized Integer addNotification(String type, String message, Object userData,
                                                Date date, long period, long nbOccurences)
        throws IllegalArgumentException {
        return addNotification(type, message, userData, date, period, nbOccurences, false);
    }

    /** Ver {@link TimerMBean}. */
    public synchronized Integer addNotification(String type, String message, Object userData,
                                                Date date, long period)
        throws IllegalArgumentException {
        return addNotification(type, message, userData, date, period, 0, false);
    }

    /** Ver {@link TimerMBean}. */
    public synchronized Integer addNotification(String type, String message, Object userData,
                                                Date date) throws IllegalArgumentException {
        return addNotification(type, message, userData, date, 0, 0, false);
    }

    /** Ver {@link TimerMBean#removeNotification}. */
    public synchronized void removeNotification(Integer id) throws InstanceNotFoundException {
        Registration r = (id == null) ? null : this.table.get(id);
        if (r == null) {
            throw new InstanceNotFoundException("Timer notification " + id + " does not exist");
        }
        drop(r);
    }

    /** Ver {@link TimerMBean#removeNotifications}. */
    public synchronized void removeNotifications(String type) throws InstanceNotFoundException {
        List<Registration> hits = new ArrayList<Registration>();
        for (Registration r : this.table.values()) {
            if (r.type == null ? type == null : r.type.equals(type)) {
                hits.add(r);
            }
        }
        if (hits.isEmpty()) {
            throw new InstanceNotFoundException("No timer notification of type " + type);
        }
        for (Registration r : hits) {
            drop(r);
        }
    }

    /** Ver {@link TimerMBean#removeAllNotifications}. */
    public synchronized void removeAllNotifications() {
        for (Registration r : new ArrayList<Registration>(this.table.values())) {
            drop(r);
        }
    }

    // ---- consultas -------------------------------------------------------------------------

    /** Cuantas inscripciones hay. */
    public synchronized int getNbNotifications() {
        return this.table.size();
    }

    /** Los identificadores de todas, en orden de alta. */
    public synchronized Vector<Integer> getAllNotificationIDs() {
        return new Vector<Integer>(this.table.keySet());
    }

    /** Los de ese tipo; vacio si no hay ninguna. */
    public synchronized Vector<Integer> getNotificationIDs(String type) {
        Vector<Integer> found = new Vector<Integer>();
        for (Registration r : this.table.values()) {
            if (r.type == null ? type == null : r.type.equals(type)) {
                found.add(r.id);
            }
        }
        return found;
    }

    /** El tipo, o null si ese identificador no existe. */
    public synchronized String getNotificationType(Integer id) {
        Registration r = lookup(id);
        return (r == null) ? null : r.type;
    }

    /** El mensaje, o null. */
    public synchronized String getNotificationMessage(Integer id) {
        Registration r = lookup(id);
        return (r == null) ? null : r.message;
    }

    /** El dato adjunto, o null. */
    public synchronized Object getNotificationUserData(Integer id) {
        Registration r = lookup(id);
        return (r == null) ? null : r.userData;
    }

    /** La proxima fecha de disparo, o null. Copia, para que no la muevan desde afuera. */
    public synchronized Date getDate(Integer id) {
        Registration r = lookup(id);
        return (r == null) ? null : new Date(r.date.getTime());
    }

    /** El periodo, o null. */
    public synchronized Long getPeriod(Integer id) {
        Registration r = lookup(id);
        return (r == null) ? null : Long.valueOf(r.period);
    }

    /** Los disparos que quedan, o null. Ver el nombre en {@link TimerMBean#getNbOccurences}. */
    public synchronized Long getNbOccurences(Integer id) {
        Registration r = lookup(id);
        return (r == null) ? null : Long.valueOf(r.occurrences);
    }

    /** Si cuenta desde la fecha original, o null. */
    public synchronized Boolean getFixedRate(Integer id) {
        Registration r = lookup(id);
        return (r == null) ? null : Boolean.valueOf(r.fixedRate);
    }

    /** Ver la nota de la clase sobre fechas pasadas. */
    public boolean getSendPastNotifications() {
        return this.sendPastNotifications;
    }

    /** Ver {@link #getSendPastNotifications}. */
    public void setSendPastNotifications(boolean value) {
        this.sendPastNotifications = value;
    }

    /** Si esta corriendo. */
    public boolean isActive() {
        return this.engine != null;
    }

    /** Si no hay ninguna inscripcion. */
    public synchronized boolean isEmpty() {
        return this.table.isEmpty();
    }

    // ---- adentro ---------------------------------------------------------------------------

    /** La busqueda que usan todos los consultores; null es una respuesta valida. */
    private Registration lookup(Integer id) {
        return (id == null) ? null : this.table.get(id);
    }

    /** Da de baja una inscripcion y cancela lo que tuviera programado. */
    private void drop(Registration r) {
        if (r.task != null) {
            r.task.cancel();
            r.task = null;
        }
        this.table.remove(r.id);
        if (this.table.isEmpty()) {
            this.nextId = 1;
        }
    }

    /** Programa el proximo disparo de una inscripcion cuya fecha ya esta en el futuro. */
    private void schedule(Registration r) {
        if (this.engine == null) {
            return;
        }
        Alarm alarm = new Alarm(r);
        r.task = alarm;
        this.engine.schedule(alarm, new Date(r.date.getTime()));
    }

    /**
     * Resuelve una inscripcion cuya fecha ya paso. Ver la nota de la clase.
     *
     * <p>Cuando hay que mandarla, se la <b>programa para ya</b> en vez de mandarla en el acto: el
     * aviso sale por el hilo del reloj como cualquier otro, y no adentro de {@link #start} en el
     * hilo de quien arranca. La diferencia se nota --un oyente lento colgaria el arranque-- y es lo
     * que hace que un disparo atrasado y uno normal se comporten igual.
     *
     * @param now el instante contra el que se compara, tomado una sola vez por el llamador
     */
    private void catchUp(Registration r, long now) {
        if (this.sendPastNotifications) {
            r.date = new Date(now);
            schedule(r);
            return;
        }
        if (r.period == 0) {
            // De una sola vez y sin mandar: no queda nada que programar.
            drop(r);
            return;
        }
        r.date = new Date(advance(r.date.getTime(), r.period, now));
        schedule(r);
    }

    /**
     * Corre una fecha hacia adelante de a periodos enteros hasta pasar {@code now}.
     *
     * <p>Las repeticiones que se saltearon <b>no</b> se descuentan de las ocurrencias; ver la nota
     * de la clase sobre por que.
     */
    private static long advance(long from, long period, long now) {
        long next = from;
        while (next <= now) {
            next = next + period;
        }
        return next;
    }

    /** Manda el aviso de una inscripcion. */
    private void fire(Registration r) {
        long seq = this.sequence;
        this.sequence = this.sequence + 1;
        TimerNotification n = new TimerNotification(
            r.type, this, seq, System.currentTimeMillis(), r.message, r.id);
        n.setUserData(r.userData);
        sendNotification(n);
    }

    /**
     * Lo que corre en el hilo del reloj cuando llega la fecha.
     *
     * <p>Toma el candado del {@link Timer} igual que los metodos publicos, asi que un disparo no se
     * cruza con un alta o una baja a mitad de camino.
     */
    private final class Alarm extends TimerTask {

        private final Registration registration;

        Alarm(Registration registration) {
            this.registration = registration;
        }

        public void run() {
            synchronized (Timer.this) {
                Registration r = this.registration;
                // Pudo darse de baja entre que se programo y que llego la hora.
                if (Timer.this.table.get(r.id) != r) {
                    return;
                }
                Timer.this.fire(r);
                if (r.occurrences > 0) {
                    r.occurrences = r.occurrences - 1;
                    if (r.occurrences == 0) {
                        Timer.this.drop(r);
                        return;
                    }
                }
                if (r.period == 0) {
                    Timer.this.drop(r);
                    return;
                }
                long now = System.currentTimeMillis();
                long base = r.fixedRate ? r.date.getTime() : now;
                long next = base + r.period;
                if (next <= now) {
                    // El reloj venia atrasado: se salta a la proxima ranura futura.
                    next = advance(next, r.period, now);
                }
                r.date = new Date(next);
                Timer.this.schedule(r);
            }
        }
    }

    /** Una inscripcion: lo que se pidio mas lo que falta por hacer. */
    private static final class Registration {
        private Integer id;
        private String type;
        private String message;
        private Object userData;
        private Date date;
        private long period;
        private long occurrences;
        private boolean fixedRate;
        private TimerTask task;
    }
}
