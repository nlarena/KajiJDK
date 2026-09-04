package javax.management.monitor;

import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanNotificationInfo;
import javax.management.ObjectName;

/**
 * KajiLibrary's javax.management.monitor.GaugeMonitor -- vigila un valor que sube y baja.
 *
 * <p>La banda de histeresis esta explicada en {@link GaugeMonitorMBean}. Aca esta el estado por
 * observado: en cual de los dos lados de la banda esta cada uno, que es lo unico que hay que
 * recordar para no repetir avisos.
 *
 * <p>A diferencia de {@link CounterMonitor}, este si acepta valores con coma: un medidor de
 * temperatura o de carga es naturalmente fraccionario. La comparacion se hace en {@code double}
 * cuando alguno de los umbrales lo es, y en {@code long} cuando los dos son enteros -- asi un
 * medidor entero no arrastra el error de representacion de los flotantes.
 */
public class GaugeMonitor extends Monitor implements GaugeMonitorMBean {

    /** En que lado de la banda esta cada observado. */
    private final Map<ObjectName, Gauged> state = new HashMap<ObjectName, Gauged>();

    /** El umbral de arriba. */
    private Number highThreshold = Integer.valueOf(0);

    /** El de abajo. */
    private Number lowThreshold = Integer.valueOf(0);

    /** Si se avisa al pasar el de arriba. */
    private boolean notifyHigh = false;

    /** Si se avisa al bajar del de abajo. */
    private boolean notifyLow = false;

    /** Si se compara la diferencia. */
    private boolean differenceMode = false;

    /** Un monitor parado, con los dos umbrales en cero. */
    public GaugeMonitor() {
    }

    /** Arranca la observacion. */
    public synchronized void start() {
        startPolling();
    }

    /** La para. */
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
        Gauged g = this.state.get(object);
        return (g == null) ? null : g.derivedGauge;
    }

    /** Cuando se calculo; 0 si nunca. */
    public synchronized long getDerivedGaugeTimeStamp(ObjectName object) {
        Gauged g = this.state.get(object);
        return (g == null) ? 0 : g.timestamp;
    }

    /** El umbral de arriba. */
    public synchronized Number getHighThreshold() {
        return this.highThreshold;
    }

    /** El de abajo. */
    public synchronized Number getLowThreshold() {
        return this.lowThreshold;
    }

    /**
     * Pone los dos. Ver {@link GaugeMonitorMBean#setThresholds} sobre por que van juntos.
     *
     * @throws IllegalArgumentException si alguno es null, si son de tipos distintos, o si el de
     *     arriba es menor que el de abajo
     */
    public synchronized void setThresholds(Number highValue, Number lowValue)
        throws IllegalArgumentException {
        if (highValue == null || lowValue == null) {
            throw new IllegalArgumentException("Null threshold value");
        }
        if (!highValue.getClass().equals(lowValue.getClass())) {
            throw new IllegalArgumentException("Different type threshold values");
        }
        if (highValue.doubleValue() < lowValue.doubleValue()) {
            throw new IllegalArgumentException("High threshold less than low threshold");
        }
        this.highThreshold = highValue;
        this.lowThreshold = lowValue;
        for (Gauged g : this.state.values()) {
            g.aboveHigh = false;
            g.belowLow = false;
        }
    }

    /** Si se avisa al pasar el de arriba. */
    public synchronized boolean getNotifyHigh() {
        return this.notifyHigh;
    }

    /** Ver {@link #getNotifyHigh}. */
    public synchronized void setNotifyHigh(boolean value) {
        this.notifyHigh = value;
    }

    /** Si se avisa al bajar del de abajo. */
    public synchronized boolean getNotifyLow() {
        return this.notifyLow;
    }

    /** Ver {@link #getNotifyLow}. */
    public synchronized void setNotifyLow(boolean value) {
        this.notifyLow = value;
    }

    /** Si se compara la diferencia con la lectura anterior. */
    public synchronized boolean getDifferenceMode() {
        return this.differenceMode;
    }

    /** Ver {@link #getDifferenceMode}. */
    public synchronized void setDifferenceMode(boolean value) {
        this.differenceMode = value;
    }

    /** Los cinco errores comunes mas los dos disparos del medidor. */
    public MBeanNotificationInfo[] getNotificationInfo() {
        String[] types = {
            MonitorNotification.RUNTIME_ERROR,
            MonitorNotification.OBSERVED_OBJECT_ERROR,
            MonitorNotification.OBSERVED_ATTRIBUTE_ERROR,
            MonitorNotification.OBSERVED_ATTRIBUTE_TYPE_ERROR,
            MonitorNotification.THRESHOLD_ERROR,
            MonitorNotification.THRESHOLD_HIGH_VALUE_EXCEEDED,
            MonitorNotification.THRESHOLD_LOW_VALUE_EXCEEDED,
        };
        return new MBeanNotificationInfo[] {
            new MBeanNotificationInfo(types, "javax.management.monitor.MonitorNotification",
                "Notifications sent by the GaugeMonitor MBean")
        };
    }

    /** Estado inicial del observado nuevo; ver {@link Monitor#createObserved}. */
    synchronized void createObserved(ObjectName name) {
        Gauged g = new Gauged();
        g.derivedGauge = Integer.valueOf(0);
        g.timestamp = System.currentTimeMillis();
        this.state.put(name, g);
    }

    /** Se olvida de el. */
    synchronized void forgetObserved(ObjectName name) {
        this.state.remove(name);
    }

    /** Una lectura: calcula el valor derivado y mira de que lado de la banda cayo. */
    synchronized void onValue(ObjectName name, int index, Object value) {
        if (!(value instanceof Number)) {
            notifyOnce(index, OBSERVED_ATTRIBUTE_TYPE_ERROR_NOTIFIED,
                MonitorNotification.OBSERVED_ATTRIBUTE_TYPE_ERROR, name,
                "The observed attribute type is not a number");
            return;
        }
        clearFlag(index, OBSERVED_ATTRIBUTE_TYPE_ERROR_NOTIFIED);
        double reading = ((Number) value).doubleValue();
        Gauged g = this.state.get(name);
        if (g == null) {
            g = new Gauged();
            this.state.put(name, g);
        }
        double derived = reading;
        if (this.differenceMode) {
            derived = g.hasPrevious ? (reading - g.previous) : 0;
        }
        g.previous = reading;
        g.hasPrevious = true;
        g.derivedGauge = Double.valueOf(derived);
        g.timestamp = System.currentTimeMillis();

        double high = this.highThreshold.doubleValue();
        double low = this.lowThreshold.doubleValue();
        if (derived >= high) {
            // Al cruzar hacia arriba se limpia el lado de abajo: la banda queda armada para el
            // proximo descenso.
            g.belowLow = false;
            if (!g.aboveHigh) {
                g.aboveHigh = true;
                if (this.notifyHigh) {
                    send(MonitorNotification.THRESHOLD_HIGH_VALUE_EXCEEDED, name,
                        "The observed attribute has exceeded the high threshold",
                        g.derivedGauge, this.highThreshold);
                }
            }
        } else if (derived <= low) {
            g.aboveHigh = false;
            if (!g.belowLow) {
                g.belowLow = true;
                if (this.notifyLow) {
                    send(MonitorNotification.THRESHOLD_LOW_VALUE_EXCEEDED, name,
                        "The observed attribute has exceeded the low threshold",
                        g.derivedGauge, this.lowThreshold);
                }
            }
        }
        // Adentro de la banda no pasa nada, que es justamente para lo que la banda existe.
    }

    /** Lo que el monitor recuerda de cada observado. */
    private static final class Gauged {
        private Number derivedGauge = null;
        private long timestamp = 0;
        private double previous = 0;
        private boolean hasPrevious = false;
        private boolean aboveHigh = false;
        private boolean belowLow = false;
    }
}
