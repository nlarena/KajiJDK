package javax.management.monitor;

import javax.management.Notification;
import javax.management.ObjectName;

/**
 * KajiLibrary's javax.management.monitor.MonitorNotification -- lo que manda un monitor.
 *
 * <p>Los diez tipos se dividen en dos grupos que conviene distinguir de entrada:
 *
 * <ul>
 *   <li>cinco de <b>error</b> --{@code jmx.monitor.error.*}--, que dicen que el monitor no pudo
 *       observar: el MBean no esta, el atributo no existe, el atributo es de otro tipo, el umbral no
 *       sirve, o algo tiro;
 *   <li>cinco de <b>disparo</b>, que son para lo que el monitor existe: el contador paso el umbral,
 *       el medidor se fue para arriba o para abajo, la cadena coincidio o dejo de coincidir.
 * </ul>
 *
 * <p>Los de error se mandan <b>una sola vez</b> hasta que la condicion cambia. Es lo correcto: un
 * monitor que observa un MBean que no existe, con un periodo de un segundo, mandaria un aviso por
 * segundo para siempre.
 *
 * <p>{@link #getDerivedGauge} es el valor que el monitor calculo --que no siempre es el atributo:
 * con modo diferencia es la resta con la lectura anterior-- y {@link #getTrigger} es contra que se
 * comparo. Los dos juntos son lo que explica por que salto.
 *
 * <p>No tiene constructor publico: los arma el monitor. Ver la nota equivalente en
 * {@code javax.management.timer.TimerNotification}, que si lo tiene, y por la razon opuesta -- ahi
 * la aplicacion elige el contenido del aviso y aca no.
 */
public class MonitorNotification extends Notification {

    private static final long serialVersionUID = -4608189663661929204L;

    /** El MBean observado no esta registrado. */
    public static final String OBSERVED_OBJECT_ERROR = "jmx.monitor.error.mbean";

    /** El atributo observado no existe. */
    public static final String OBSERVED_ATTRIBUTE_ERROR = "jmx.monitor.error.attribute";

    /** El atributo es de un tipo que este monitor no sabe mirar. */
    public static final String OBSERVED_ATTRIBUTE_TYPE_ERROR = "jmx.monitor.error.type";

    /** El umbral no sirve para el tipo del atributo. */
    public static final String THRESHOLD_ERROR = "jmx.monitor.error.threshold";

    /** Algo tiro mientras se observaba. */
    public static final String RUNTIME_ERROR = "jmx.monitor.error.runtime";

    /** El contador llego al umbral. */
    public static final String THRESHOLD_VALUE_EXCEEDED = "jmx.monitor.counter.threshold";

    /** El medidor paso el umbral de arriba. */
    public static final String THRESHOLD_HIGH_VALUE_EXCEEDED = "jmx.monitor.gauge.high";

    /** El medidor paso el umbral de abajo. */
    public static final String THRESHOLD_LOW_VALUE_EXCEEDED = "jmx.monitor.gauge.low";

    /** La cadena paso a coincidir. */
    public static final String STRING_TO_COMPARE_VALUE_MATCHED = "jmx.monitor.string.matches";

    /** La cadena dejo de coincidir. */
    public static final String STRING_TO_COMPARE_VALUE_DIFFERED = "jmx.monitor.string.differs";

    /** Cual de los observados disparo. */
    private final ObjectName observedObject;

    /** Que atributo suyo. */
    private final String observedAttribute;

    /** El valor que el monitor calculo. */
    private final Object derivedGauge;

    /** Contra que se comparo. */
    private final Object trigger;

    /** Paquete-privado: los arma el monitor. Ver la nota de la clase. */
    MonitorNotification(String type, Object source, long sequenceNumber, long timeStamp, String msg,
                        ObjectName observedObject, String observedAttribute, Object derivedGauge,
                        Object trigger) {
        super(type, source, sequenceNumber, timeStamp, msg);
        this.observedObject = observedObject;
        this.observedAttribute = observedAttribute;
        this.derivedGauge = derivedGauge;
        this.trigger = trigger;
    }

    /** El MBean que disparo el aviso. */
    public ObjectName getObservedObject() {
        return this.observedObject;
    }

    /** El atributo que se estaba mirando. */
    public String getObservedAttribute() {
        return this.observedAttribute;
    }

    /** El valor calculado. Ver la nota de la clase: no siempre es el atributo. */
    public Object getDerivedGauge() {
        return this.derivedGauge;
    }

    /** Contra que se comparo: el umbral, o la cadena. */
    public Object getTrigger() {
        return this.trigger;
    }
}
