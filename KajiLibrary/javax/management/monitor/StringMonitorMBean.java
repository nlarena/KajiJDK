package javax.management.monitor;

import javax.management.ObjectName;

/**
 * KajiLibrary's javax.management.monitor.StringMonitorMBean -- la administracion del monitor de
 * cadenas.
 *
 * <p>El mas simple de los tres: compara un atributo de texto con una cadena fija y avisa cuando
 * <b>cambia</b> el resultado de la comparacion.
 *
 * <p>La palabra clave es cambia. No avisa mientras coincide, avisa cuando <b>pasa</b> a coincidir; y
 * lo mismo del otro lado. Es la misma idea de histeresis que en {@link GaugeMonitorMBean}, aplicada
 * a algo que solo tiene dos estados: un atributo que dice OK durante una hora produce un aviso, no
 * tres mil seiscientos.
 *
 * <p>De ahi que las dos banderas sean independientes y las dos arranquen apagadas. Lo comun es
 * prender solo una: {@link #setNotifyDiffer} para vigilar que algo deje de estar bien, o
 * {@link #setNotifyMatch} para esperar a que llegue a un estado.
 */
public interface StringMonitorMBean extends MonitorMBean {

    /** El valor leido del primer observado. */
    String getDerivedGauge();

    /** Cuando se leyo. */
    long getDerivedGaugeTimeStamp();

    /** El valor leido de ese observado. */
    String getDerivedGauge(ObjectName object);

    /** Cuando se leyo, para ese observado. */
    long getDerivedGaugeTimeStamp(ObjectName object);

    /** Con que se compara. */
    String getStringToCompare();

    /**
     * Ver {@link #getStringToCompare}.
     *
     * @throws IllegalArgumentException si es null
     */
    void setStringToCompare(String value) throws IllegalArgumentException;

    /** Si se avisa cuando pasa a coincidir. */
    boolean getNotifyMatch();

    /** Ver {@link #getNotifyMatch}. */
    void setNotifyMatch(boolean value);

    /** Si se avisa cuando deja de coincidir. */
    boolean getNotifyDiffer();

    /** Ver {@link #getNotifyDiffer}. */
    void setNotifyDiffer(boolean value);
}
