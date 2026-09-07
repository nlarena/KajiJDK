package jdk.jfr.consumer;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

import jdk.jfr.Configuration;
import jdk.jfr.Event;
import jdk.jfr.EventSettings;

/**
 * Una grabacion y su consumo, en el mismo objeto y en el mismo proceso.
 *
 * <h2>Que junta</h2>
 *
 * <p>Una {@link jdk.jfr.Recording} y un {@link EventStream}. Se configura que grabar como en la
 * primera —{@link #enable}, {@link #setMaxAge}— y se consume como en el segundo —{@link #onEvent}—,
 * sin archivo de por medio.
 *
 * <p>Es la forma de que un programa reaccione a sus propios eventos. Un servidor que quiera loguear
 * cada pausa de recoleccion mayor a 100 ms lo escribe asi, en cinco lineas y sin escribir nada a
 * disco.
 *
 * <h2>La trampa de {@code setReuse}</h2>
 *
 * <p>Heredada de {@link EventStream}: por omision el objeto {@link RecordedEvent} se reutiliza en
 * cada entrega, asi que guardarlo en una lista para mirarlo despues no funciona — todos los
 * elementos terminan siendo el mismo objeto con el ultimo valor. Con {@code setReuse(false)} se
 * puede guardar, y cuesta una asignacion por evento.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>El constructor falla con {@link IllegalStateException}, que es exactamente lo que hace el del
 * JDK cuando JFR no esta disponible: por dentro pide el grabador, y pedirlo sin JFR es esa
 * excepcion. No es un agregado de esta biblioteca — es el comportamiento definido para este caso.
 *
 * @since 14
 */
public final class RecordingStream implements AutoCloseable, EventStream {

    private static final String NO_DISPONIBLE = "Flight Recorder no esta disponible en esta VM";

    /**
     * Un flujo con la configuracion por omision.
     *
     * @throws IllegalStateException si JFR no esta disponible, que es el caso en esta VM
     */
    public RecordingStream() {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /**
     * Un flujo con esa configuracion.
     *
     * @param configuration la configuracion
     * @throws IllegalStateException si JFR no esta disponible, que es el caso en esta VM
     */
    public RecordingStream(final Configuration configuration) {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /**
     * Habilita un evento por nombre.
     *
     * @param name el nombre
     * @return los ajustes de ese evento
     */
    public EventSettings enable(final String name) {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /**
     * Reemplaza los ajustes.
     *
     * @param settings los ajustes
     */
    public void setSettings(final Map<String, String> settings) {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /**
     * Habilita un evento por su clase.
     *
     * @param eventClass la clase
     * @return los ajustes de ese evento
     */
    public EventSettings enable(final Class<? extends Event> eventClass) {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /**
     * Deshabilita un evento por nombre.
     *
     * @param name el nombre
     * @return los ajustes de ese evento
     */
    public EventSettings disable(final String name) {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /**
     * Deshabilita un evento por su clase.
     *
     * @param eventClass la clase
     * @return los ajustes de ese evento
     */
    public EventSettings disable(final Class<? extends Event> eventClass) {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /**
     * La antiguedad maxima de los datos.
     *
     * @param maxAge la duracion
     */
    public void setMaxAge(final Duration maxAge) {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /**
     * El tamano maximo.
     *
     * @param maxSize los bytes
     */
    public void setMaxSize(final long maxSize) {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /** {@inheritDoc} */
    public void setReuse(final boolean reuse) {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /** {@inheritDoc} */
    public void setOrdered(final boolean ordered) {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /** {@inheritDoc} */
    public void setStartTime(final Instant startTime) {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /** {@inheritDoc} */
    public void setEndTime(final Instant endTime) {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /** {@inheritDoc} */
    public void onEvent(final String eventName, final Consumer<RecordedEvent> action) {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /** {@inheritDoc} */
    public void onEvent(final Consumer<RecordedEvent> action) {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /** {@inheritDoc} */
    public void onFlush(final Runnable action) {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /** {@inheritDoc} */
    public void onClose(final Runnable action) {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /** {@inheritDoc} */
    public void onError(final Consumer<Throwable> action) {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /**
     * Cierra el flujo.
     *
     * <p>No falla: cerrar algo que nunca arranco es legitimo, y hacerlo fallar romperia cualquier
     * {@code try}-con-recursos.
     */
    public void close() {
    }

    /** {@inheritDoc} */
    public boolean remove(final Object action) {
        return false;
    }

    /** {@inheritDoc} */
    public void start() {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /** {@inheritDoc} */
    public void startAsync() {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /**
     * Detiene la grabacion.
     *
     * @return si estaba grabando
     */
    public boolean stop() {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /**
     * Vuelca los datos a un archivo.
     *
     * @param destination el archivo
     * @throws IOException si no se pudo escribir
     */
    public void dump(final Path destination) throws IOException {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /** {@inheritDoc} */
    public void awaitTermination(final Duration timeout) throws InterruptedException {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /** {@inheritDoc} */
    public void awaitTermination() throws InterruptedException {
        throw new IllegalStateException(NO_DISPONIBLE);
    }

    /** {@inheritDoc} */
    public void onMetadata(final Consumer<MetadataEvent> action) {
        throw new IllegalStateException(NO_DISPONIBLE);
    }
}
