package javax.management.monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

/**
 * KajiLibrary's javax.management.monitor.Monitor -- la base de los tres monitores.
 *
 * <p>Un monitor lee un atributo de uno o varios MBeans cada tanto y avisa cuando pasa algo. Esta
 * clase tiene la parte que no depende de <b>que</b> se mira: la lista de observados, el periodo, el
 * hilo que despierta, y la maquinaria de avisar los errores una sola vez.
 *
 * <h2>El hilo es demonio</h2>
 *
 * <p>Al reves que {@code javax.management.timer.Timer}: un monitor arrancado <b>no</b> impide que la
 * maquina virtual termine. Tiene sentido en los dos casos y por el mismo criterio -- un reloj existe
 * para que algo pase y perderlo es perder trabajo; un monitor existe para observar, y observar
 * mientras el programa se cierra no le sirve a nadie.
 *
 * <h2>Los errores se avisan una vez</h2>
 *
 * <p>Los campos {@code alreadyNotified*} y las cuatro banderas {@code *_NOTIFIED} son eso: un
 * registro, por objeto observado, de que errores ya se avisaron. Sin ellos, un monitor apuntando a un
 * MBean que no existe mandaria un aviso por periodo para siempre.
 *
 * <p>Las banderas se limpian cuando la condicion se arregla, asi que un MBean que desaparece y
 * vuelve produce exactamente dos avisos y no uno ni mil.
 *
 * <h2>Los campos protegidos</h2>
 *
 * <p>Casi todo el estado interno es {@code protected} y no privado. No es una decision de esta
 * biblioteca: la clase es de 1999 y sus subclases del JDK los tocan directo. Se replican tal cual
 * porque una subclase escrita contra el JDK tiene que poder compilar contra esto.
 */
public abstract class Monitor extends NotificationBroadcasterSupport
    implements MonitorMBean, MBeanRegistration {

    /** De cuanto en cuanto crece {@link #alreadyNotifieds}. */
    protected static final int capacityIncrement = 16;

    /** Cuantos observados hay. */
    protected int elementCount = 0;

    /** Las banderas del <b>primero</b>, por compatibilidad con la version de un solo observado. */
    protected int alreadyNotified = 0;

    /** Las banderas de cada observado, en el mismo orden que la lista. */
    protected int[] alreadyNotifieds = new int[capacityIncrement];

    /** El agente donde se registro, o null. */
    protected MBeanServer server;

    /** Limpia todas las banderas. */
    protected static final int RESET_FLAGS_ALREADY_NOTIFIED = 0;

    /** Ya se aviso que el MBean no esta. */
    protected static final int OBSERVED_OBJECT_ERROR_NOTIFIED = 1;

    /** Ya se aviso que el atributo no existe. */
    protected static final int OBSERVED_ATTRIBUTE_ERROR_NOTIFIED = 2;

    /** Ya se aviso que el atributo es de otro tipo. */
    protected static final int OBSERVED_ATTRIBUTE_TYPE_ERROR_NOTIFIED = 4;

    /** Ya se aviso que algo tiro. */
    protected static final int RUNTIME_ERROR_NOTIFIED = 8;

    /** Una etiqueta para los mensajes de diagnostico. */
    protected String dbgTag = getClass().getName();

    /** Los observados, en orden de alta. */
    private final List<ObjectName> observedObjects = new ArrayList<ObjectName>();

    /** El atributo que se lee de todos. */
    private String observedAttribute = null;

    /** Cada cuanto, en milisegundos. */
    private long granularityPeriod = 10000;

    /** Null mientras esta parado. */
    private Timer engine;

    /** El nombre bajo el que se registro, o null. */
    private ObjectName objectName;

    /** El numero de secuencia de los avisos. */
    private long sequence = 1;

    /** Un monitor parado y sin observados. */
    public Monitor() {
    }

    // ---- MBeanRegistration -----------------------------------------------------------------

    /** Se queda con el agente: es de donde va a leer los atributos. */
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        this.server = server;
        this.objectName = name;
        return name;
    }

    /** Nada que hacer. */
    public void postRegister(Boolean registrationDone) {
    }

    /** Para el monitor: sin agente no puede leer nada. */
    public void preDeregister() throws Exception {
        stop();
    }

    /** Suelta el agente. */
    public void postDeregister() {
        this.server = null;
        this.objectName = null;
    }

    // ---- lo que definen las subclases ------------------------------------------------------

    /** Arranca. Cada monitor concreto valida lo suyo antes de llamar a {@link #startPolling}. */
    public abstract void start();

    /** Para. */
    public abstract void stop();

    // ---- observados ------------------------------------------------------------------------

    /**
     * El primero de la lista.
     *
     * @deprecated ver {@link MonitorMBean#getObservedObject}
     */
    @Deprecated
    public synchronized ObjectName getObservedObject() {
        if (this.observedObjects.isEmpty()) {
            return null;
        }
        return this.observedObjects.get(0);
    }

    /**
     * Reemplaza la lista entera por ese.
     *
     * @throws IllegalArgumentException si es null
     * @deprecated ver {@link MonitorMBean#setObservedObject}
     */
    @Deprecated
    public synchronized void setObservedObject(ObjectName object) throws IllegalArgumentException {
        if (object == null) {
            throw new IllegalArgumentException("Null observed object");
        }
        for (ObjectName old : this.observedObjects) {
            forgetObserved(old);
        }
        this.observedObjects.clear();
        this.observedObjects.add(object);
        createObserved(object);
        resetCounts();
    }

    /**
     * Agrega uno.
     *
     * <p>Agregar el mismo dos veces no hace nada: la lista es un conjunto en la practica, y avisar
     * dos veces por el mismo MBean no le sirve a nadie.
     *
     * @throws IllegalArgumentException si es null
     */
    public synchronized void addObservedObject(ObjectName object) throws IllegalArgumentException {
        if (object == null) {
            throw new IllegalArgumentException("Null observed object");
        }
        if (this.observedObjects.contains(object)) {
            return;
        }
        this.observedObjects.add(object);
        createObserved(object);
        resetCounts();
    }

    /** Lo saca. Si no estaba, no hace nada. */
    public synchronized void removeObservedObject(ObjectName object) {
        if (this.observedObjects.remove(object)) {
            forgetObserved(object);
            resetCounts();
        }
    }

    /** Si ese esta. */
    public synchronized boolean containsObservedObject(ObjectName object) {
        return this.observedObjects.contains(object);
    }

    /** Todos, en orden de alta. Copia. */
    public synchronized ObjectName[] getObservedObjects() {
        return this.observedObjects.toArray(new ObjectName[this.observedObjects.size()]);
    }

    /** El atributo que se lee de todos. */
    public synchronized String getObservedAttribute() {
        return this.observedAttribute;
    }

    /**
     * Ver {@link #getObservedAttribute}.
     *
     * @throws IllegalArgumentException si es null
     */
    public void setObservedAttribute(String attribute) throws IllegalArgumentException {
        if (attribute == null) {
            throw new IllegalArgumentException("Null observed attribute");
        }
        synchronized (this) {
            this.observedAttribute = attribute;
            resetCounts();
        }
    }

    /** Cada cuantos milisegundos se lee. */
    public synchronized long getGranularityPeriod() {
        return this.granularityPeriod;
    }

    /**
     * Ver {@link #getGranularityPeriod}.
     *
     * @throws IllegalArgumentException si no es positivo
     */
    public synchronized void setGranularityPeriod(long period) throws IllegalArgumentException {
        if (period <= 0) {
            throw new IllegalArgumentException("Nonpositive granularity period");
        }
        this.granularityPeriod = period;
        if (this.engine != null) {
            // Reprogramar con el periodo nuevo; si no, el cambio no se nota hasta el proximo
            // arranque, que es justo lo que nadie espera de un setter.
            stopPolling();
            startPolling();
        }
    }

    /** Si esta observando. */
    public synchronized boolean isActive() {
        return this.engine != null;
    }

    // ---- para las subclases ----------------------------------------------------------------

    /**
     * El valor calculado para ese observado, sin comprometerse con un tipo.
     *
     * <p>Paquete-privado y devolviendo {@code Object} a proposito: cada monitor concreto lo redefine
     * con <b>su</b> tipo --{@code Number} en los numericos, {@code String} en el de cadenas-- y esa
     * redefinicion covariante es la que hace que el metodo publico de cada uno sea el tipado.
     *
     * <p>Sin este de aca, la redefinicion no seria una redefinicion sino un metodo nuevo, y el
     * puente que el compilador genera --{@code Object getDerivedGauge(ObjectName)} publico en cada
     * subclase-- no existiria. Ese puente es parte del API que ve la reflexion, asi que la
     * declaracion tiene que estar.
     */
    synchronized Object getDerivedGauge(ObjectName object) {
        return null;
    }

    /**
     * Arranca el hilo que despierta cada periodo.
     *
     * <p>Lo llaman los {@code start()} de las subclases despues de validar lo suyo. Si ya estaba
     * activo no hace nada.
     */
    synchronized void startPolling() {
        if (this.engine != null) {
            return;
        }
        // Demonio: ver la nota de la clase.
        this.engine = new Timer("monitor-mbean", true);
        this.engine.schedule(new Tick(), this.granularityPeriod, this.granularityPeriod);
    }

    /** Para el hilo. Lo llaman los {@code stop()}. */
    synchronized void stopPolling() {
        if (this.engine == null) {
            return;
        }
        this.engine.cancel();
        this.engine = null;
    }

    /**
     * Una lectura de todos los observados.
     *
     * <p>Corre en el hilo del monitor. Cada subclase decide que hacer con el valor.
     */
    synchronized void poll() {
        int i = 0;
        while (i < this.observedObjects.size()) {
            ObjectName name = this.observedObjects.get(i);
            try {
                if (this.server == null) {
                    // Sin agente no hay de donde leer. No es un error del MBean observado.
                    i = i + 1;
                    continue;
                }
                Object value = this.server.getAttribute(name, this.observedAttribute);
                clearFlag(i, OBSERVED_OBJECT_ERROR_NOTIFIED | OBSERVED_ATTRIBUTE_ERROR_NOTIFIED);
                onValue(name, i, value);
            } catch (javax.management.InstanceNotFoundException e) {
                notifyOnce(i, OBSERVED_OBJECT_ERROR_NOTIFIED,
                    MonitorNotification.OBSERVED_OBJECT_ERROR, name,
                    "The observed object is not registered");
            } catch (javax.management.AttributeNotFoundException e) {
                notifyOnce(i, OBSERVED_ATTRIBUTE_ERROR_NOTIFIED,
                    MonitorNotification.OBSERVED_ATTRIBUTE_ERROR, name,
                    "The observed attribute is not contained in the observed object");
            } catch (Exception e) {
                notifyOnce(i, RUNTIME_ERROR_NOTIFIED, MonitorNotification.RUNTIME_ERROR, name,
                    "An error occurred while reading the observed attribute: " + e);
            }
            i = i + 1;
        }
    }

    /** Que hacer con el valor leido. La define cada monitor concreto. */
    abstract void onValue(ObjectName name, int index, Object value);

    /**
     * Le avisa al monitor concreto que hay un observado nuevo.
     *
     * <p>El estado por observado se crea <b>al darlo de alta</b> y no en la primera lectura. Se nota
     * de afuera: recien agregado, {@code getDerivedGauge()} ya contesta un valor inicial en vez de
     * null. Es lo que hace el JDK y tiene sentido -- "todavia no lei nada de este" y "este no
     * existe" son dos respuestas distintas.
     */
    abstract void createObserved(ObjectName name);

    /** Le avisa que se dio de baja: el estado de ese observado se descarta. */
    abstract void forgetObserved(ObjectName name);

    /**
     * Manda un aviso si ese error no se aviso todavia para ese observado.
     *
     * <p>Ver la nota de la clase sobre por que una sola vez.
     */
    void notifyOnce(int index, int flag, String type, ObjectName name, String message) {
        if ((flagsAt(index) & flag) != 0) {
            return;
        }
        setFlag(index, flag);
        send(type, name, message, null, null);
    }

    /** Manda un aviso de disparo, sin banderas de por medio. */
    void send(String type, ObjectName name, String message, Object derivedGauge, Object trigger) {
        long seq = this.sequence;
        this.sequence = this.sequence + 1;
        sendNotification(new MonitorNotification(type, this, seq, System.currentTimeMillis(),
            message, name, this.observedAttribute, derivedGauge, trigger));
    }

    /** Las banderas de ese observado. */
    int flagsAt(int index) {
        if (index < 0 || index >= this.alreadyNotifieds.length) {
            return 0;
        }
        return this.alreadyNotifieds[index];
    }

    /** Prende una bandera. */
    void setFlag(int index, int flag) {
        ensureCapacity(index);
        this.alreadyNotifieds[index] = this.alreadyNotifieds[index] | flag;
        if (index == 0) {
            this.alreadyNotified = this.alreadyNotifieds[0];
        }
    }

    /** Apaga banderas. */
    void clearFlag(int index, int flags) {
        ensureCapacity(index);
        this.alreadyNotifieds[index] = this.alreadyNotifieds[index] & ~flags;
        if (index == 0) {
            this.alreadyNotified = this.alreadyNotifieds[0];
        }
    }

    /** Deja el registro de banderas al tamano de la lista y lo limpia. */
    private void resetCounts() {
        this.elementCount = this.observedObjects.size();
        ensureCapacity(this.elementCount);
        int i = 0;
        while (i < this.alreadyNotifieds.length) {
            this.alreadyNotifieds[i] = RESET_FLAGS_ALREADY_NOTIFIED;
            i = i + 1;
        }
        this.alreadyNotified = RESET_FLAGS_ALREADY_NOTIFIED;
    }

    /** Agranda el registro de a {@link #capacityIncrement}. */
    private void ensureCapacity(int index) {
        if (index < this.alreadyNotifieds.length) {
            return;
        }
        int size = this.alreadyNotifieds.length;
        while (size <= index) {
            size = size + capacityIncrement;
        }
        int[] bigger = new int[size];
        System.arraycopy(this.alreadyNotifieds, 0, bigger, 0, this.alreadyNotifieds.length);
        this.alreadyNotifieds = bigger;
    }

    /** Lo que corre en el hilo del monitor cada periodo. */
    private final class Tick extends TimerTask {

        public void run() {
            poll();
        }
    }
}
