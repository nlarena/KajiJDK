package javax.management.monitor;

import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanNotificationInfo;
import javax.management.ObjectName;

/**
 * KajiLibrary's javax.management.monitor.StringMonitor -- vigila un atributo de texto.
 *
 * <p>La logica esta en {@link StringMonitorMBean}: se avisa en el <b>cambio</b> de la comparacion, no
 * mientras dura. Aca esta el estado por observado --si la ultima lectura coincidia o no-- que es lo
 * unico que hace falta para detectar el cambio.
 *
 * <p>La comparacion es {@code equals} exacto: mayusculas incluidas, sin recortar espacios. Es lo
 * correcto para lo que se usa --estados como {@code STARTED} o {@code FAILED}-- y hay que saberlo,
 * porque un espacio de mas en el atributo hace que el monitor no dispare nunca.
 *
 * <p>El primer valor leido tambien cuenta como cambio: un monitor que arranca sobre un atributo que
 * ya no coincide avisa en la primera lectura. Es lo que se quiere -- si no, habria que esperar a que
 * el valor cambie dos veces para enterarse de un estado que ya estaba mal.
 */
public class StringMonitor extends Monitor implements StringMonitorMBean {

    /** Lo ultimo que se vio de cada observado. */
    private final Map<ObjectName, Watched> state = new HashMap<ObjectName, Watched>();

    /** Con que se compara. Arranca en la cadena vacia, no en null. */
    private String stringToCompare = "";

    /** Si se avisa cuando pasa a coincidir. */
    private boolean notifyMatch = false;

    /** Si se avisa cuando deja de coincidir. */
    private boolean notifyDiffer = false;

    /** Un monitor parado, comparando contra la cadena vacia. */
    public StringMonitor() {
    }

    /** Arranca la observacion. */
    public synchronized void start() {
        startPolling();
    }

    /** La para. */
    public synchronized void stop() {
        stopPolling();
    }

    /** El valor leido del primer observado. */
    public synchronized String getDerivedGauge() {
        return getDerivedGauge(getObservedObject());
    }

    /** Cuando se leyo. */
    public synchronized long getDerivedGaugeTimeStamp() {
        return getDerivedGaugeTimeStamp(getObservedObject());
    }

    /** El valor leido de ese observado, o null si nunca se leyo. */
    public synchronized String getDerivedGauge(ObjectName object) {
        Watched w = this.state.get(object);
        return (w == null) ? null : w.derivedGauge;
    }

    /** Cuando se leyo; 0 si nunca. */
    public synchronized long getDerivedGaugeTimeStamp(ObjectName object) {
        Watched w = this.state.get(object);
        return (w == null) ? 0 : w.timestamp;
    }

    /** Con que se compara. */
    public synchronized String getStringToCompare() {
        return this.stringToCompare;
    }

    /**
     * Ver {@link #getStringToCompare}.
     *
     * <p>Cambiarla reinicia el estado de todos: con otra cadena, la proxima lectura es un cambio
     * aunque el atributo no se haya movido.
     *
     * @throws IllegalArgumentException si es null
     */
    public synchronized void setStringToCompare(String value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("Null string to compare");
        }
        this.stringToCompare = value;
        for (Watched w : this.state.values()) {
            w.hasCompared = false;
        }
    }

    /** Si se avisa cuando pasa a coincidir. */
    public synchronized boolean getNotifyMatch() {
        return this.notifyMatch;
    }

    /** Ver {@link #getNotifyMatch}. */
    public synchronized void setNotifyMatch(boolean value) {
        this.notifyMatch = value;
    }

    /** Si se avisa cuando deja de coincidir. */
    public synchronized boolean getNotifyDiffer() {
        return this.notifyDiffer;
    }

    /** Ver {@link #getNotifyDiffer}. */
    public synchronized void setNotifyDiffer(boolean value) {
        this.notifyDiffer = value;
    }

    /**
     * Los errores comunes mas los dos disparos de la cadena.
     *
     * <p>Sin {@code THRESHOLD_ERROR}: aca no hay umbral que pueda estar mal.
     */
    public MBeanNotificationInfo[] getNotificationInfo() {
        String[] types = {
            MonitorNotification.RUNTIME_ERROR,
            MonitorNotification.OBSERVED_OBJECT_ERROR,
            MonitorNotification.OBSERVED_ATTRIBUTE_ERROR,
            MonitorNotification.OBSERVED_ATTRIBUTE_TYPE_ERROR,
            MonitorNotification.STRING_TO_COMPARE_VALUE_MATCHED,
            MonitorNotification.STRING_TO_COMPARE_VALUE_DIFFERED,
        };
        return new MBeanNotificationInfo[] {
            new MBeanNotificationInfo(types, "javax.management.monitor.MonitorNotification",
                "Notifications sent by the StringMonitor MBean")
        };
    }

    /**
     * Estado inicial del observado nuevo.
     *
     * <p><b>Divergencia deliberada.</b> El JDK crea aca un valor inicial de tipo {@code Integer} --el
     * mismo para los tres monitores-- y despues {@code getDerivedGauge()} lo castea a
     * {@code String}, asi que preguntarle a un {@code StringMonitor} recien configurado y todavia sin
     * leer <b>tira {@code ClassCastException}</b>. Esta comprobado contra el JDK 25.
     *
     * <p>Aca el valor inicial queda en null, que significa lo que dice --no se leyo nada-- y no
     * rompe. Replicar el error no aportaria nada: es un valor que ningun programa quiere recibir, y
     * la excepcion sale de un lugar que no tiene relacion con lo que el programa pidio.
     */
    synchronized void createObserved(ObjectName name) {
        Watched w = new Watched();
        w.timestamp = System.currentTimeMillis();
        this.state.put(name, w);
    }

    /** Se olvida de el. */
    synchronized void forgetObserved(ObjectName name) {
        this.state.remove(name);
    }

    /** Una lectura: compara y avisa solo si el resultado cambio. */
    synchronized void onValue(ObjectName name, int index, Object value) {
        if (!(value instanceof String)) {
            notifyOnce(index, OBSERVED_ATTRIBUTE_TYPE_ERROR_NOTIFIED,
                MonitorNotification.OBSERVED_ATTRIBUTE_TYPE_ERROR, name,
                "The observed attribute type is not a string");
            return;
        }
        clearFlag(index, OBSERVED_ATTRIBUTE_TYPE_ERROR_NOTIFIED);
        String reading = (String) value;
        Watched w = this.state.get(name);
        if (w == null) {
            w = new Watched();
            this.state.put(name, w);
        }
        w.derivedGauge = reading;
        w.timestamp = System.currentTimeMillis();
        boolean matches = this.stringToCompare.equals(reading);
        // La primera lectura cuenta como cambio; ver la nota de la clase.
        boolean changed = !w.hasCompared || w.matched != matches;
        w.hasCompared = true;
        w.matched = matches;
        if (!changed) {
            return;
        }
        if (matches && this.notifyMatch) {
            send(MonitorNotification.STRING_TO_COMPARE_VALUE_MATCHED, name,
                "The observed attribute has matched the string to compare",
                reading, this.stringToCompare);
        } else if (!matches && this.notifyDiffer) {
            send(MonitorNotification.STRING_TO_COMPARE_VALUE_DIFFERED, name,
                "The observed attribute has differed from the string to compare",
                reading, this.stringToCompare);
        }
    }

    /** Lo ultimo que se vio de un observado. */
    private static final class Watched {
        private String derivedGauge = null;
        private long timestamp = 0;
        private boolean hasCompared = false;
        private boolean matched = false;
    }
}
