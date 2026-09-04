package javax.management.monitor;

import javax.management.ObjectName;

/**
 * KajiLibrary's javax.management.monitor.GaugeMonitorMBean -- la administracion del monitor de
 * medidores.
 *
 * <p>Un medidor sube y baja --uso de memoria, cantidad de conexiones-- y por eso tiene <b>dos</b>
 * umbrales en vez de uno. Los dos juntos son una banda de histeresis, y esa es toda la idea:
 *
 * <ul>
 *   <li>al pasar el umbral de arriba se avisa, y <b>no se vuelve a avisar</b> hasta que el valor
 *       baje del umbral de abajo;
 *   <li>al bajar del de abajo se avisa, y no se vuelve a avisar hasta que suba del de arriba.
 * </ul>
 *
 * <p>Sin esa banda, un valor oscilando alrededor de un solo umbral produciria un aviso por lectura.
 * Con ella, un valor que tiembla en el borde produce exactamente uno. Es la diferencia entre una
 * alarma util y una que se ignora.
 *
 * <p>Por eso {@link #setThresholds} pone los dos juntos y no hay un setter para cada uno: con
 * setters separados habria un instante en que el de arriba esta por debajo del de abajo, y en ese
 * instante la banda no significa nada.
 */
public interface GaugeMonitorMBean extends MonitorMBean {

    /** El valor calculado para el primer observado. */
    Number getDerivedGauge();

    /** Cuando se calculo. */
    long getDerivedGaugeTimeStamp();

    /** El valor calculado para ese observado. */
    Number getDerivedGauge(ObjectName object);

    /** Cuando se calculo, para ese observado. */
    long getDerivedGaugeTimeStamp(ObjectName object);

    /** El umbral de arriba. */
    Number getHighThreshold();

    /** El de abajo. */
    Number getLowThreshold();

    /**
     * Pone los dos. Ver la nota de la clase sobre por que van juntos.
     *
     * @throws IllegalArgumentException si alguno es null, si son de tipos distintos, o si el de
     *     arriba es menor que el de abajo
     */
    void setThresholds(Number highValue, Number lowValue) throws IllegalArgumentException;

    /** Si se avisa al pasar el de arriba. */
    boolean getNotifyHigh();

    /** Ver {@link #getNotifyHigh}. */
    void setNotifyHigh(boolean value);

    /** Si se avisa al bajar del de abajo. */
    boolean getNotifyLow();

    /** Ver {@link #getNotifyLow}. */
    void setNotifyLow(boolean value);

    /** Si se compara la diferencia con la lectura anterior. */
    boolean getDifferenceMode();

    /** Ver {@link #getDifferenceMode}. */
    void setDifferenceMode(boolean value);
}
