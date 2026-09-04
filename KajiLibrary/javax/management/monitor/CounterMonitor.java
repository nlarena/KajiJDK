package javax.management.monitor;

import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanNotificationInfo;
import javax.management.ObjectName;

/**
 * KajiLibrary's javax.management.monitor.CounterMonitor -- vigila un contador.
 *
 * <p>La logica esta explicada en {@link CounterMonitorMBean}: umbral, offset y modulo. Aca esta el
 * estado por observado, que es lo que hace que dos MBeans vigilados por el mismo monitor no se pisen
 * -- cada uno tiene su umbral corrido y su lectura anterior.
 *
 * <p>Solo trabaja con enteros. Un atributo {@code Double} o {@code Float} produce un
 * {@link MonitorNotification#OBSERVED_ATTRIBUTE_TYPE_ERROR} y no una comparacion aproximada: un
 * contador que avanza de a fracciones no es un contador, y compararlo con un umbral entero daria
 * disparos que dependen del redondeo.
 */
public class CounterMonitor extends Monitor implements CounterMonitorMBean {

    /** Lo que el monitor sabe de cada observado. */
    private final Map<ObjectName, Counted> state = new HashMap<ObjectName, Counted>();

    /** El umbral configurado. */
    private Number initThreshold = Integer.valueOf(0);

    /** Cuanto se corre tras cada disparo. */
    private Number offset = Integer.valueOf(0);

    /** En cuanto da la vuelta el contador. */
    private Number modulus = Integer.valueOf(0);

    /** Si se avisa. */
    private boolean notify = false;

    /** Si se compara la diferencia. */
    private boolean differenceMode = false;

    /** Un monitor parado, con todo en cero. */
    public CounterMonitor() {
    }

    /** Arranca la observacion. */
    public synchronized void start() {
        startPolling();
    }

    /** La para. Los umbrales corridos quedan como estaban. */
    public synchronized void stop() {
        stopPolling();
    }

    /** El valor calculado para el primer observado. */
    public synchronized Number getDerivedGauge() {
        return getDerivedGauge(getObservedObject());
    }

    /** Cuando se calculo. */
    public synchronized long getDerivedGaugeTimeStamp() {
        return getDerivedGaugeTimeStamp(getObservedObject());
    }

    /** El valor calculado para ese observado, o null si nunca se leyo. */
    public synchronized Number getDerivedGauge(ObjectName object) {
        Counted c = this.state.get(object);
        return (c == null) ? null : c.derivedGauge;
    }

    /** Cuando se calculo; 0 si nunca. */
    public synchronized long getDerivedGaugeTimeStamp(ObjectName object) {
        Counted c = this.state.get(object);
        return (c == null) ? 0 : c.timestamp;
    }

    /**
     * El umbral <b>actual</b> de ese observado; el inicial si todavia no se corrio.
     *
     * @return null si ese objeto no esta observado
     */
    public synchronized Number getThreshold(ObjectName object) {
        Counted c = this.state.get(object);
        if (c == null) {
            return null;
        }
        return (c.threshold == null) ? this.initThreshold : c.threshold;
    }

    /** El umbral actual del primer observado. */
    public synchronized Number getThreshold() {
        return getThreshold(getObservedObject());
    }

    /**
     * Cambia el umbral. Tambien reinicia los corridos: un umbral nuevo empieza de cero para todos.
     *
     * @throws IllegalArgumentException si es null o negativo
     */
    public synchronized void setThreshold(Number value) throws IllegalArgumentException {
        setInitThreshold(value);
    }

    /** El umbral configurado. */
    public synchronized Number getInitThreshold() {
        return this.initThreshold;
    }

    /**
     * Ver {@link #getInitThreshold}.
     *
     * @throws IllegalArgumentException si es null o negativo
     */
    public synchronized void setInitThreshold(Number value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Null threshold");
        }
        if (value.longValue() < 0) {
            throw new IllegalArgumentException("Negative threshold");
        }
        this.initThreshold = value;
        for (Counted c : this.state.values()) {
            c.threshold = null;
            c.notified = false;
        }
    }

    /** Cuanto se corre el umbral tras cada disparo. */
    public synchronized Number getOffset() {
        return this.offset;
    }

    /**
     * Ver {@link #getOffset}.
     *
     * @throws IllegalArgumentException si es null o negativo
     */
    public synchronized void setOffset(Number value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Null offset");
        }
        if (value.longValue() < 0) {
            throw new IllegalArgumentException("Negative offset");
        }
        this.offset = value;
    }

    /** En cuanto da la vuelta el contador. */
    public synchronized Number getModulus() {
        return this.modulus;
    }

    /**
     * Ver {@link #getModulus}.
     *
     * @throws IllegalArgumentException si es null o negativo
     */
    public synchronized void setModulus(Number value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Null modulus");
        }
        if (value.longValue() < 0) {
            throw new IllegalArgumentException("Negative modulus");
        }
        this.modulus = value;
    }

    /** Si se avisa al llegar al umbral. */
    public synchronized boolean getNotify() {
        return this.notify;
    }

    /** Ver {@link #getNotify}. */
    public synchronized void setNotify(boolean value) {
        this.notify = value;
    }

    /** Si se compara la diferencia con la lectura anterior. */
    public synchronized boolean getDifferenceMode() {
        return this.differenceMode;
    }

    /** Ver {@link #getDifferenceMode}. */
    public synchronized void setDifferenceMode(boolean value) {
        this.differenceMode = value;
    }

    /** Los cinco errores comunes mas el disparo propio del contador. */
    public MBeanNotificationInfo[] getNotificationInfo() {
        String[] types = {
            MonitorNotification.RUNTIME_ERROR,
            MonitorNotification.OBSERVED_OBJECT_ERROR,
            MonitorNotification.OBSERVED_ATTRIBUTE_ERROR,
            MonitorNotification.OBSERVED_ATTRIBUTE_TYPE_ERROR,
            MonitorNotification.THRESHOLD_ERROR,
            MonitorNotification.THRESHOLD_VALUE_EXCEEDED,
        };
        return new MBeanNotificationInfo[] {
            new MBeanNotificationInfo(types, "javax.management.monitor.MonitorNotification",
                "Notifications sent by the CounterMonitor MBean")
        };
    }

    /** Estado inicial del observado nuevo; ver {@link Monitor#createObserved}. */
    synchronized void createObserved(ObjectName name) {
        Counted c = new Counted();
        c.derivedGauge = Integer.valueOf(0);
        c.timestamp = System.currentTimeMillis();
        this.state.put(name, c);
    }

    /** Se olvida de el. */
    synchronized void forgetObserved(ObjectName name) {
        this.state.remove(name);
    }

    /** Una lectura: calcula el valor derivado y decide si dispara. */
    synchronized void onValue(ObjectName name, int index, Object value) {
        if (!(value instanceof Number) || value instanceof Double || value instanceof Float) {
            notifyOnce(index, OBSERVED_ATTRIBUTE_TYPE_ERROR_NOTIFIED,
                MonitorNotification.OBSERVED_ATTRIBUTE_TYPE_ERROR, name,
                "The observed attribute type is not an integer");
            return;
        }
        clearFlag(index, OBSERVED_ATTRIBUTE_TYPE_ERROR_NOTIFIED);
        long reading = ((Number) value).longValue();
        Counted c = this.state.get(name);
        if (c == null) {
            c = new Counted();
            this.state.put(name, c);
        }
        long derived = reading;
        if (this.differenceMode) {
            derived = c.hasPrevious ? (reading - c.previous) : 0;
            long mod = this.modulus.longValue();
            // Con modulo, una diferencia negativa es una vuelta del contador y no un retroceso.
            if (derived < 0 && mod > 0) {
                derived = derived + mod;
            }
        } else {
            long mod = this.modulus.longValue();
            // Sin modo diferencia, la vuelta se detecta porque el valor bajo.
            if (mod > 0 && c.hasPrevious && reading < c.previous) {
                c.threshold = null;
                c.notified = false;
            }
        }
        c.previous = reading;
        c.hasPrevious = true;
        c.derivedGauge = Long.valueOf(derived);
        c.timestamp = System.currentTimeMillis();

        long threshold = (c.threshold == null) ? this.initThreshold.longValue()
            : c.threshold.longValue();
        if (threshold <= 0 && this.initThreshold.longValue() == 0) {
            // Umbral 0 configurado: no hay nada que vigilar.
            return;
        }
        if (derived < threshold) {
            return;
        }
        if (this.notify && !c.notified) {
            send(MonitorNotification.THRESHOLD_VALUE_EXCEEDED, name,
                "The observed attribute has reached the threshold",
                c.derivedGauge, Long.valueOf(threshold));
        }
        long step = this.offset.longValue();
        if (step > 0) {
            // Se corre hasta pasar el valor actual: si el contador salto varios offsets de una,
            // no tiene sentido dejar el umbral atras y disparar en cada lectura siguiente.
            long moved = threshold;
            while (moved <= derived) {
                moved = moved + step;
            }
            c.threshold = Long.valueOf(moved);
            c.notified = false;
        } else {
            // Sin offset se avisa una sola vez y no se vuelve a mover el umbral.
            c.notified = true;
        }
    }

    /** Lo que el monitor recuerda de cada observado. */
    private static final class Counted {
        private Number derivedGauge = null;
        private long timestamp = 0;
        private long previous = 0;
        private boolean hasPrevious = false;
        private Number threshold = null;
        private boolean notified = false;
    }
}
