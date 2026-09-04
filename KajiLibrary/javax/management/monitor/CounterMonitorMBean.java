package javax.management.monitor;

import javax.management.ObjectName;

/**
 * KajiLibrary's javax.management.monitor.CounterMonitorMBean -- la administracion del monitor de
 * contadores.
 *
 * <p>Un contador solo sube. El monitor avisa cuando llega al umbral, y despues tiene que decidir que
 * hacer para no avisar en cada lectura: para eso estan el <b>offset</b> y el <b>modulo</b>, que son
 * las dos piezas que hacen util a esta clase.
 *
 * <ul>
 *   <li>el <b>offset</b> corre el umbral hacia arriba despues de cada disparo. Con umbral 100 y
 *       offset 100 se avisa a las 100, a las 200, a las 300: es la forma de decir "avisame cada
 *       cien" sin reconfigurar nada. Con offset 0 se avisa una sola vez y nunca mas;
 *   <li>el <b>modulo</b> es el valor en el que el contador vuelve a cero. Un contador de 32 bits que
 *       da la vuelta pareceria haber bajado, y sin esto el monitor no distinguiria una vuelta de un
 *       reinicio. Al detectarla, el umbral vuelve a {@link #getInitThreshold}.
 * </ul>
 *
 * <p>De ahi que haya <b>dos</b> umbrales: el inicial, que es el que se configuro, y el actual, que
 * es el que el offset fue corriendo. {@link #getThreshold} devuelve el actual.
 *
 * <p>El modo diferencia cambia que se compara: en vez del valor, la resta con la lectura anterior.
 * Es lo que convierte un contador acumulado en una tasa.
 */
public interface CounterMonitorMBean extends MonitorMBean {

    /** El valor calculado para el primer observado. */
    Number getDerivedGauge();

    /** Cuando se calculo. */
    long getDerivedGaugeTimeStamp();

    /** El umbral actual del primer observado. Ver la nota de la clase. */
    Number getThreshold();

    /**
     * Cambia el umbral, y con el el inicial.
     *
     * @throws IllegalArgumentException si es null o negativo
     */
    void setThreshold(Number value) throws IllegalArgumentException;

    /** El valor calculado para ese observado. */
    Number getDerivedGauge(ObjectName object);

    /** Cuando se calculo, para ese observado. */
    long getDerivedGaugeTimeStamp(ObjectName object);

    /** El umbral actual de ese observado. */
    Number getThreshold(ObjectName object);

    /** El umbral configurado, antes de que el offset lo corriera. */
    Number getInitThreshold();

    /**
     * Ver {@link #getInitThreshold}.
     *
     * @throws IllegalArgumentException si es null o negativo
     */
    void setInitThreshold(Number value) throws IllegalArgumentException;

    /** Cuanto se corre el umbral tras cada disparo; 0 para no correrlo. */
    Number getOffset();

    /**
     * Ver {@link #getOffset}.
     *
     * @throws IllegalArgumentException si es null o negativo
     */
    void setOffset(Number value) throws IllegalArgumentException;

    /** En cuanto da la vuelta el contador; 0 si no da la vuelta. */
    Number getModulus();

    /**
     * Ver {@link #getModulus}.
     *
     * @throws IllegalArgumentException si es null o negativo
     */
    void setModulus(Number value) throws IllegalArgumentException;

    /** Si se avisa al llegar al umbral. */
    boolean getNotify();

    /** Ver {@link #getNotify}. */
    void setNotify(boolean value);

    /** Si se compara la diferencia con la lectura anterior. Ver la nota de la clase. */
    boolean getDifferenceMode();

    /** Ver {@link #getDifferenceMode}. */
    void setDifferenceMode(boolean value);
}
