package javax.management.monitor;

import javax.management.ObjectName;

/**
 * KajiLibrary's javax.management.monitor.MonitorMBean -- la interfaz de administracion comun a los
 * tres monitores.
 *
 * <p>Un monitor es un MBean que mira <b>otros</b> MBeans: lee un atributo cada tanto y avisa cuando
 * pasa algo. Esta interfaz es la parte que no depende de que se este mirando: a quien, que atributo,
 * cada cuanto, y arrancar y parar.
 *
 * <h2>Los dos {@code ObservedObject} en singular y en plural</h2>
 *
 * <p>{@link #getObservedObject} y {@link #setObservedObject} son de la version 1, cuando un monitor
 * miraba <b>uno</b>. Cuando se agrego mirar varios, se conservaron: el getter devuelve el primero de
 * la lista y el setter reemplaza la lista entera por uno. Estan obsoletos y siguen andando, que es
 * la unica combinacion que no rompe codigo viejo.
 *
 * <p>El atributo observado es <b>uno solo</b> para todos los objetos observados. Es una limitacion
 * real y se nota al usarlo: para mirar dos atributos distintos hacen falta dos monitores.
 */
public interface MonitorMBean {

    /** Arranca la observacion. */
    void start();

    /** La para. La configuracion queda. */
    void stop();

    /**
     * Agrega un MBean a observar.
     *
     * @throws IllegalArgumentException si es null
     */
    void addObservedObject(ObjectName object) throws IllegalArgumentException;

    /** Lo saca. Si no estaba, no hace nada. */
    void removeObservedObject(ObjectName object);

    /** Si ese esta en la lista. */
    boolean containsObservedObject(ObjectName object);

    /** Todos los observados. */
    ObjectName[] getObservedObjects();

    /**
     * El primero de la lista.
     *
     * @deprecated ver la nota de la clase; usar {@link #getObservedObjects}
     */
    @Deprecated
    ObjectName getObservedObject();

    /**
     * Reemplaza la lista entera por ese.
     *
     * @deprecated ver la nota de la clase; usar {@link #addObservedObject}
     */
    @Deprecated
    void setObservedObject(ObjectName object);

    /** El atributo que se lee. Ver la nota de la clase: es uno solo. */
    String getObservedAttribute();

    /**
     * Ver {@link #getObservedAttribute}.
     *
     * @throws IllegalArgumentException si es null
     */
    void setObservedAttribute(String attribute);

    /** Cada cuantos milisegundos se lee. */
    long getGranularityPeriod();

    /**
     * Ver {@link #getGranularityPeriod}.
     *
     * @throws IllegalArgumentException si no es positivo: un periodo de 0 seria un bucle cerrado
     */
    void setGranularityPeriod(long period) throws IllegalArgumentException;

    /** Si esta observando. */
    boolean isActive();
}
