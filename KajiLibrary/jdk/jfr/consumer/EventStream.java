package jdk.jfr.consumer;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * Un flujo de eventos, para consumirlos <strong>mientras ocurren</strong> en vez de leer un archivo
 * al final.
 *
 * <h2>Que cambia respecto de grabar y volcar</h2>
 *
 * <p>Que no hace falta saber de antemano cuando algo interesante va a pasar. Un programa que
 * reacciona a sus propios eventos —que sube una metrica, que loguea una pausa larga— no puede
 * esperar a que alguien vuelque un archivo.
 *
 * <p>Es tambien lo que permite consumir eventos de <strong>otro</strong> proceso, con
 * {@link #openRepository()}: JFR escribe su repositorio en disco y este flujo lo sigue.
 *
 * <h2>Los dos ajustes que hay que entender antes de usarlo</h2>
 *
 * <p>{@link #setReuse} decide si el mismo objeto {@link RecordedEvent} se reutiliza para cada
 * evento. Con {@code true} —el valor por omision— no se puede guardar el evento para despues: el
 * objeto que se recibio va a estar pisado en la proxima vuelta. Es rapidisimo y es la fuente de
 * error mas comun de esta API.
 *
 * <p>{@link #setOrdered} decide si los eventos llegan en orden de tiempo. Ordenarlos obliga a
 * esperar y a acumular, porque distintos hilos escriben en buffers distintos. Con {@code false}
 * llegan antes y desordenados.
 *
 * <h2>Los dos modos de arranque</h2>
 *
 * <p>{@link #start} bloquea el hilo que llama hasta que el flujo termina; {@link #startAsync}
 * vuelve enseguida y el flujo corre en otro hilo. Con el segundo hace falta
 * {@link #awaitTermination()} para saber cuando termino.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>Las tres fabricas estaticas fallan con {@link IOException} diciendo que no hay repositorio ni
 * lector del formato binario. La interfaz esta entera: una implementacion que lea el formato encaja
 * aca sin tocarla.
 *
 * @since 14
 */
public interface EventStream extends AutoCloseable {

    /**
     * Un flujo sobre el repositorio de la VM actual.
     *
     * @return el flujo
     * @throws IOException si no hay repositorio, que es el caso en esta VM
     */
    static EventStream openRepository() throws IOException {
        throw new IOException(
                "no hay repositorio de JFR en esta VM: el grabador no esta disponible");
    }

    /**
     * Un flujo sobre el repositorio que hay en ese directorio.
     *
     * @param directory el directorio del repositorio
     * @return el flujo
     * @throws IOException si no se puede leer, que es el caso en esta VM
     */
    static EventStream openRepository(final Path directory) throws IOException {
        throw new IOException(
                "leer un repositorio de JFR necesita el lector del formato binario, que esta "
                + "biblioteca no implementa");
    }

    /**
     * Un flujo sobre un archivo de grabacion.
     *
     * @param file el archivo
     * @return el flujo
     * @throws IOException si no se puede leer, que es el caso en esta VM
     */
    static EventStream openFile(final Path file) throws IOException {
        throw new IOException(
                "leer un archivo de grabacion necesita el lector del formato binario, que esta "
                + "biblioteca no implementa");
    }

    /**
     * Que hacer cuando cambian los metadatos.
     *
     * <p>Por omision no hace nada: casi ningun consumidor necesita enterarse, y obligarlo a
     * escribir un metodo vacio seria ruido.
     *
     * @param action la accion
     */
    default void onMetadata(Consumer<MetadataEvent> action) {
    }

    /**
     * Que hacer con cada evento.
     *
     * @param action la accion
     */
    void onEvent(Consumer<RecordedEvent> action);

    /**
     * Que hacer con cada evento de ese tipo.
     *
     * @param eventName el nombre del tipo
     * @param action la accion
     */
    void onEvent(String eventName, Consumer<RecordedEvent> action);

    /**
     * Que hacer cuando el flujo vacia sus buffers.
     *
     * <p>Es el punto donde se sabe que todo lo emitido hasta ese momento ya se entrego, y por lo
     * tanto el unico lugar donde tiene sentido cerrar una ventana de agregacion.
     *
     * @param action la accion
     */
    void onFlush(Runnable action);

    /**
     * Que hacer con un error del flujo.
     *
     * @param action la accion
     */
    void onError(Consumer<Throwable> action);

    /**
     * Que hacer cuando el flujo se cierra.
     *
     * @param action la accion
     */
    void onClose(Runnable action);

    /** Cierra el flujo. */
    void close();

    /**
     * Saca una accion registrada.
     *
     * @param action la accion
     * @return si estaba registrada
     */
    boolean remove(Object action);

    /**
     * Si el mismo objeto de evento se reutiliza para cada entrega.
     *
     * @param reuse si reutilizar
     */
    void setReuse(boolean reuse);

    /**
     * Si los eventos se entregan en orden de tiempo.
     *
     * @param ordered si ordenar
     */
    void setOrdered(boolean ordered);

    /**
     * Desde cuando entregar eventos.
     *
     * @param startTime el momento
     */
    void setStartTime(Instant startTime);

    /**
     * Hasta cuando entregar eventos.
     *
     * @param endTime el momento
     */
    void setEndTime(Instant endTime);

    /** Arranca el flujo en este hilo y no vuelve hasta que termine. */
    void start();

    /** Arranca el flujo en otro hilo y vuelve enseguida. */
    void startAsync();

    /**
     * Espera a que el flujo termine, hasta ese tiempo.
     *
     * @param timeout cuanto esperar
     * @throws InterruptedException si el hilo se interrumpe
     */
    void awaitTermination(Duration timeout) throws InterruptedException;

    /**
     * Espera a que el flujo termine.
     *
     * @throws InterruptedException si el hilo se interrumpe
     */
    void awaitTermination() throws InterruptedException;
}
