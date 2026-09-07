package jdk.jfr;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Una grabacion: que eventos capturar, con que limites y adonde va el resultado.
 *
 * <h2>Los tres limites, y por que son tres</h2>
 *
 * <p>{@link #setDuration} corta por tiempo desde que arranca. {@link #setMaxAge} descarta lo mas
 * viejo de un buffer circular. {@link #setMaxSize} descarta lo mas viejo al llegar a un tamano.
 *
 * <p>Los dos ultimos no son lo mismo aunque lo parezcan, y la diferencia importa: con
 * {@code maxAge} se sabe cuanto tiempo hacia atras se tiene y no cuanto ocupa; con {@code maxSize}
 * se sabe cuanto ocupa y no cuanto tiempo cubre. En un pico de actividad, el mismo tamano cubre
 * muchos menos minutos.
 *
 * <p>{@code duration} es de otra clase: los otros dos dejan la grabacion andando para siempre y
 * descartan lo viejo, este la termina.
 *
 * <h2>Los dos usos</h2>
 *
 * <p><strong>Grabar y volcar</strong>: arrancar, esperar, parar, {@link #dump}. Es lo que se hace
 * para investigar algo que se puede reproducir.
 *
 * <p><strong>Dejar puesta y sacar una instantanea</strong>: arrancar con {@code maxAge} y sin
 * destino, y cuando algo sale mal, {@link FlightRecorder#takeSnapshot}. Es lo que se hace en
 * produccion para tener los ultimos minutos de lo que ya paso.
 *
 * <h2>Cerrar sin volcar pierde los datos</h2>
 *
 * <p>{@link #close} suelta todo. Una grabacion detenida todavia tiene sus datos y una cerrada no,
 * asi que el {@code try}-con-recursos, que cierra al salir del bloque, borra la grabacion si el
 * {@link #dump} no ocurrio adentro. Es el error mas comun con esta API.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>Toda la <strong>configuracion</strong> es real: nombre, ajustes, limites, destino, estado. Se
 * puede construir una grabacion, configurarla y leerla, y {@link #copy} y {@link #getSettings}
 * hacen lo que dicen.
 *
 * <p>Lo que no puede funcionar es lo que necesita el grabador: {@link #start}, {@link #stop},
 * {@link #dump}, {@link #getStream} y {@link #getSize} fallan con {@link IllegalStateException},
 * que es lo mismo que la API define para una VM sin JFR. El objeto no miente sobre su estado — se
 * queda en {@link RecordingState#NEW} porque nunca arranco.
 *
 * @since 9
 */
public final class Recording implements Closeable {

    private static final String NO_DISPONIBLE = "Flight Recorder no esta disponible en esta VM";

    private static final AtomicLong PROXIMO_ID = new AtomicLong(1);

    private final long id = PROXIMO_ID.getAndIncrement();
    private final Map<String, String> ajustes = new LinkedHashMap<String, String>();

    private String nombre;
    private RecordingState estado = RecordingState.NEW;
    private long maxSize;
    private Duration maxAge;
    private Duration duracion;
    private Path destino;
    private boolean dumpOnExit;
    private boolean toDisk = true;

    /**
     * Una grabacion sin ajustes.
     *
     * <p>El nombre arranca siendo el identificador, como en el JDK: una grabacion sin nombre igual
     * tiene que poder distinguirse de otra en una lista.
     */
    public Recording() {
        this.nombre = String.valueOf(id);
    }

    /**
     * Una grabacion con esos ajustes.
     *
     * @param settings los ajustes, con la clave {@code "evento#ajuste"}
     * @throws NullPointerException si es {@code null}
     */
    public Recording(final Map<String, String> settings) {
        this();
        setSettings(settings);
    }

    /**
     * Una grabacion con los ajustes de una configuracion.
     *
     * @param configuration la configuracion
     * @throws NullPointerException si es {@code null}
     */
    public Recording(final Configuration configuration) {
        this();
        setSettings(Objects.requireNonNull(configuration, "configuration").getSettings());
    }

    /**
     * Arranca la grabacion.
     *
     * @throws IllegalStateException en esta VM, o si la grabacion ya arranco o se cerro
     */
    public void start() {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /**
     * Programa el arranque para dentro de ese tiempo.
     *
     * @param delay cuanto esperar
     * @throws NullPointerException si es {@code null}
     * @throws IllegalStateException en esta VM
     */
    public void scheduleStart(final Duration delay) {
        Objects.requireNonNull(delay, "delay");
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /**
     * Detiene la grabacion, conservando los datos.
     *
     * @return si estaba grabando
     * @throws IllegalStateException en esta VM
     */
    public boolean stop() {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /**
     * Los ajustes.
     *
     * @return una copia de los ajustes
     */
    public Map<String, String> getSettings() {
        return new LinkedHashMap<String, String>(ajustes);
    }

    /**
     * Cuanto ocupa la grabacion.
     *
     * @return los bytes
     * @throws IllegalStateException en esta VM, porque no hay datos que medir
     */
    public long getSize() {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /**
     * Cuando se detuvo.
     *
     * @return el momento, o {@code null} si no se detuvo
     */
    public Instant getStopTime() {
        return null;
    }

    /**
     * Cuando arranco.
     *
     * @return el momento, o {@code null} si no arranco
     */
    public Instant getStartTime() {
        return null;
    }

    /**
     * El tamano maximo.
     *
     * @return los bytes; cero es sin limite
     */
    public long getMaxSize() {
        return maxSize;
    }

    /**
     * La antiguedad maxima de los datos.
     *
     * @return la duracion, o {@code null} si no hay limite
     */
    public Duration getMaxAge() {
        return maxAge;
    }

    /**
     * El nombre.
     *
     * @return el nombre
     */
    public String getName() {
        return nombre;
    }

    /**
     * Reemplaza los ajustes.
     *
     * <p>Reemplaza y no fusiona: los que estaban y no vienen en el mapa nuevo se pierden. Es lo que
     * hace el JDK, y es lo coherente con que una grabacion se configure a partir de una
     * {@link Configuration} completa y no a fuerza de retoques.
     *
     * @param settings los ajustes
     * @throws NullPointerException si es {@code null}
     */
    public void setSettings(final Map<String, String> settings) {
        Objects.requireNonNull(settings, "settings");
        ajustes.clear();
        ajustes.putAll(settings);
    }

    /**
     * En que punto de su vida esta.
     *
     * @return el estado
     */
    public RecordingState getState() {
        return estado;
    }

    /**
     * Suelta los datos y los recursos.
     *
     * <p>No falla aunque JFR no este: cerrar algo que nunca arranco es legitimo, y hacerlo fallar
     * romperia cualquier {@code try}-con-recursos.
     */
    public void close() {
        estado = RecordingState.CLOSED;
    }

    /**
     * Una copia de esta grabacion.
     *
     * @param stop si la copia tiene que quedar detenida
     * @return la copia, con su propio identificador
     */
    public Recording copy(final boolean stop) {
        final Recording r = new Recording(getSettings());
        r.nombre = nombre;
        r.maxSize = maxSize;
        r.maxAge = maxAge;
        r.duracion = duracion;
        r.destino = destino;
        r.dumpOnExit = dumpOnExit;
        r.toDisk = toDisk;
        r.estado = stop ? RecordingState.STOPPED : estado;
        return r;
    }

    /**
     * Escribe los datos en un archivo.
     *
     * @param destination el archivo
     * @throws IOException si no se pudo escribir
     * @throws NullPointerException si es {@code null}
     * @throws IllegalStateException en esta VM, porque no hay datos que escribir
     */
    public void dump(final Path destination) throws IOException {
        Objects.requireNonNull(destination, "destination");
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /**
     * Si los datos se escriben a disco mientras se graba.
     *
     * @return si van a disco
     */
    public boolean isToDisk() {
        return toDisk;
    }

    /**
     * Fija el tamano maximo.
     *
     * @param maxSize los bytes; cero para sin limite
     * @throws IllegalArgumentException si es negativo
     */
    public void setMaxSize(final long maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException("el tamano maximo no puede ser negativo");
        }
        this.maxSize = maxSize;
    }

    /**
     * Fija la antiguedad maxima de los datos.
     *
     * @param maxAge la duracion, o {@code null} para sin limite
     * @throws IllegalArgumentException si es negativa
     */
    public void setMaxAge(final Duration maxAge) {
        if (maxAge != null && maxAge.isNegative()) {
            throw new IllegalArgumentException("la antiguedad maxima no puede ser negativa");
        }
        this.maxAge = maxAge;
    }

    /**
     * Fija adonde volcar los datos al terminar.
     *
     * @param destination el archivo, o {@code null} para no volcar
     * @throws IOException si el destino no sirve
     */
    public void setDestination(final Path destination) throws IOException {
        this.destino = destination;
    }

    /**
     * El destino configurado.
     *
     * @return el archivo, o {@code null}
     */
    public Path getDestination() {
        return destino;
    }

    /**
     * El identificador de la grabacion.
     *
     * @return el identificador
     */
    public long getId() {
        return id;
    }

    /**
     * Fija el nombre.
     *
     * @param name el nombre
     * @throws NullPointerException si es {@code null}
     */
    public void setName(final String name) {
        this.nombre = Objects.requireNonNull(name, "name");
    }

    /**
     * Si volcar los datos cuando la VM termine.
     *
     * <p>Solo tiene efecto con un destino puesto: sin el no hay adonde volcar.
     *
     * @param dumpOnExit si volcar
     */
    public void setDumpOnExit(final boolean dumpOnExit) {
        this.dumpOnExit = dumpOnExit;
    }

    /**
     * Si va a volcar al terminar la VM.
     *
     * @return si vuelca
     */
    public boolean getDumpOnExit() {
        return dumpOnExit;
    }

    /**
     * Si escribir a disco mientras se graba.
     *
     * <p>En memoria es mas rapido y limita cuanto se puede guardar; a disco aguanta grabaciones
     * largas y cuesta entrada y salida.
     *
     * @param toDisk si escribir a disco
     */
    public void setToDisk(final boolean toDisk) {
        this.toDisk = toDisk;
    }

    /**
     * Un flujo con los eventos de ese intervalo.
     *
     * @param start el comienzo del intervalo, o {@code null} para desde el principio
     * @param end el final, o {@code null} para hasta el final
     * @return el flujo
     * @throws IOException si no se pudo leer
     * @throws IllegalStateException en esta VM, porque no hay datos que leer
     */
    public InputStream getStream(final Instant start, final Instant end) throws IOException {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /**
     * Cuanto va a durar la grabacion.
     *
     * @return la duracion, o {@code null} si no tiene limite
     */
    public Duration getDuration() {
        return duracion;
    }

    /**
     * Fija cuanto va a durar.
     *
     * @param duration la duracion, o {@code null} para sin limite
     */
    public void setDuration(final Duration duration) {
        this.duracion = duration;
    }

    /**
     * Habilita un evento por nombre y devuelve sus ajustes para seguir configurandolo.
     *
     * @param name el nombre del evento
     * @return los ajustes de ese evento
     * @throws NullPointerException si es {@code null}
     */
    public EventSettings enable(final String name) {
        return ajustesDe(Objects.requireNonNull(name, "name"), "true");
    }

    /**
     * Deshabilita un evento por nombre.
     *
     * @param name el nombre del evento
     * @return los ajustes de ese evento
     * @throws NullPointerException si es {@code null}
     */
    public EventSettings disable(final String name) {
        return ajustesDe(Objects.requireNonNull(name, "name"), "false");
    }

    /**
     * Habilita un evento por su clase.
     *
     * @param eventClass la clase del evento
     * @return los ajustes de ese evento
     * @throws NullPointerException si es {@code null}
     */
    public EventSettings enable(final Class<? extends Event> eventClass) {
        return enable(nombreDe(eventClass));
    }

    /**
     * Deshabilita un evento por su clase.
     *
     * @param eventClass la clase del evento
     * @return los ajustes de ese evento
     * @throws NullPointerException si es {@code null}
     */
    public EventSettings disable(final Class<? extends Event> eventClass) {
        return disable(nombreDe(eventClass));
    }

    /**
     * El nombre con el que el evento aparece en la grabacion.
     *
     * <p>Respeta el {@link Name} de la clase: habilitar por clase y habilitar por nombre tienen que
     * llegar a la misma clave, o una de las dos formas no funcionaria.
     */
    private static String nombreDe(final Class<? extends Event> eventClass) {
        Objects.requireNonNull(eventClass, "eventClass");
        final Name n = eventClass.getAnnotation(Name.class);
        return n != null ? n.value() : eventClass.getName();
    }

    private EventSettings ajustesDe(final String evento, final String habilitado) {
        ajustes.put(evento + "#" + Enabled.NAME, habilitado);
        return new AjustesDeEvento(evento);
    }

    /**
     * Los ajustes de un evento, escribiendo directo en el mapa de la grabacion.
     *
     * <p>Con nombre y no anonima por #482: el generador de bytecode no emite una clase anonima que
     * este en el inicializador de un campo, y conviene no depender de en que contexto se instancia.
     */
    private final class AjustesDeEvento extends EventSettings {

        private final String evento;

        AjustesDeEvento(final String evento) {
            this.evento = evento;
        }

        public EventSettings with(final String name, final String value) {
            Objects.requireNonNull(name, "name");
            ajustes.put(evento + "#" + name, value);
            return this;
        }
    }
}
