package jdk.management.jfr;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.openmbean.CompositeData;

/**
 * Una {@link jdk.jfr.Recording} vista desde el otro lado de una conexion JMX.
 *
 * <h2>Los tiempos y las duraciones son numeros</h2>
 *
 * <p>{@link #getStartTime} devuelve un {@code long} en milisegundos y no un {@code Instant};
 * {@link #getMaxAge} un {@code long} y no un {@code Duration}. No es descuido: los tipos abiertos
 * de JMX son los primitivos, {@code String} y poco mas, y un {@code Instant} no esta entre ellos.
 *
 * <p>Lo mismo con {@link #getState}, que es el <strong>nombre</strong> de la constante de
 * {@link jdk.jfr.RecordingState} y no la constante: una enumeracion tampoco viaja.
 *
 * <p>Convertirlos del lado del cliente es trabajo de quien consume esto, y es la contrapartida de
 * que el protocolo no dependa de que las dos puntas tengan las mismas clases.
 *
 * @since 9
 */
public final class RecordingInfo {

    private final String name;
    private final long id;
    private final boolean dumpOnExit;
    private final long maxAge;
    private final long maxSize;
    private final String state;
    private final long startTime;
    private final long stopTime;
    private final Map<String, String> settings;
    private final String destination;
    private final long size;
    private final boolean toDisk;
    private final long duration;

    RecordingInfo(final String name, final long id, final boolean dumpOnExit,
            final long maxAge, final long maxSize, final String state, final long startTime,
            final long stopTime, final Map<String, String> settings, final String destination,
            final long size, final boolean toDisk, final long duration) {
        this.name = name;
        this.id = id;
        this.dumpOnExit = dumpOnExit;
        this.maxAge = maxAge;
        this.maxSize = maxSize;
        this.state = state;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.settings = Collections.unmodifiableMap(new LinkedHashMap<String, String>(settings));
        this.destination = destination;
        this.size = size;
        this.toDisk = toDisk;
        this.duration = duration;
    }

    /**
     * El nombre de la grabacion.
     *
     * @return el valor
     */
    public String getName() {
        return name;
    }

    /**
     * El identificador de la grabacion.
     *
     * @return el valor
     */
    public long getId() {
        return id;
    }

    /**
     * Si vuelca al terminar la VM.
     *
     * @return el valor
     */
    public boolean getDumpOnExit() {
        return dumpOnExit;
    }

    /**
     * La antiguedad maxima de los datos, en milisegundos; cero es sin limite.
     *
     * @return el valor
     */
    public long getMaxAge() {
        return maxAge;
    }

    /**
     * El tamano maximo en bytes; cero es sin limite.
     *
     * @return el valor
     */
    public long getMaxSize() {
        return maxSize;
    }

    /**
     * El estado, con el nombre de la constante de {@link jdk.jfr.RecordingState}.
     *
     * @return el valor
     */
    public String getState() {
        return state;
    }

    /**
     * Cuando arranco, en milisegundos desde la epoca; cero si no arranco.
     *
     * @return el valor
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Cuando se detuvo, en milisegundos desde la epoca; cero si no se detuvo.
     *
     * @return el valor
     */
    public long getStopTime() {
        return stopTime;
    }

    /**
     * Los ajustes de la grabacion.
     *
     * @return el valor
     */
    public Map<String, String> getSettings() {
        return settings;
    }

    /**
     * Adonde vuelca, o {@code null}.
     *
     * @return el valor
     */
    public String getDestination() {
        return destination;
    }

    /**
     * Cuanto ocupa, en bytes.
     *
     * @return el valor
     */
    public long getSize() {
        return size;
    }

    /**
     * Si escribe a disco mientras graba.
     *
     * @return el valor
     */
    public boolean isToDisk() {
        return toDisk;
    }

    /**
     * Cuanto va a durar, en milisegundos; cero es sin limite.
     *
     * @return el valor
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Reconstruye el objeto desde su forma abierta.
     *
     * <p>Es el camino por el que este dato llega de una VM remota: lo que viaja por JMX es un
     * {@link CompositeData} generico y esto lo vuelve a convertir.
     *
     * @param cd la forma abierta, o {@code null}
     * @return el objeto, o {@code null} si {@code cd} era {@code null}
     * @throws IllegalArgumentException si no tiene la forma esperada
     */
    public static RecordingInfo from(final CompositeData cd) {
        if (cd == null) {
            return null;
        }
        throw new IllegalArgumentException(
                "reconstruir un RecordingInfo necesita el soporte de tipos abiertos de JFR, que"
                + " esta biblioteca no implementa");
    }

    /** {@inheritDoc} */
    public String toString() {
        final StringBuilder sb = new StringBuilder("RecordingInfo{");
        sb.append("name=").append(String.valueOf(name));
        sb.append(", ");
        sb.append("id=").append(String.valueOf(id));
        sb.append(", ");
        sb.append("dumpOnExit=").append(String.valueOf(dumpOnExit));
        sb.append(", ");
        sb.append("maxAge=").append(String.valueOf(maxAge));
        sb.append(", ");
        sb.append("maxSize=").append(String.valueOf(maxSize));
        sb.append(", ");
        sb.append("state=").append(String.valueOf(state));
        sb.append(", ");
        sb.append("startTime=").append(String.valueOf(startTime));
        sb.append(", ");
        sb.append("stopTime=").append(String.valueOf(stopTime));
        sb.append(", ");
        sb.append("settings=").append(String.valueOf(settings));
        sb.append(", ");
        sb.append("destination=").append(String.valueOf(destination));
        sb.append(", ");
        sb.append("size=").append(String.valueOf(size));
        sb.append(", ");
        sb.append("toDisk=").append(String.valueOf(toDisk));
        sb.append(", ");
        sb.append("duration=").append(String.valueOf(duration));
        return sb.append('}').toString();
    }
}
