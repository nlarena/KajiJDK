package jdk.jfr.consumer;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import jdk.jfr.EventType;

/**
 * Lee un archivo de grabacion, evento por evento.
 *
 * <h2>Como se usa</h2>
 *
 * <pre>{@code
 * try (RecordingFile f = new RecordingFile(Path.of("grabacion.jfr"))) {
 *     while (f.hasMoreEvents()) {
 *         RecordedEvent e = f.readEvent();
 *         ...
 *     }
 * }
 * }</pre>
 *
 * <p>De a un evento y no todos de una: una grabacion de produccion tiene millones y no entra en
 * memoria. {@link #readAllEvents} existe para los archivos chicos y es la que hay que evitar con
 * los grandes.
 *
 * <h2>{@link #write} filtra sin descomprimir a memoria</h2>
 *
 * <p>Copia a otro archivo solo los eventos que pasen el predicado. Sirve para recortar una
 * grabacion enorme antes de mandarla a alguien, sin abrirla entera.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p><strong>No implementado.</strong> Leer un {@code .jfr} es implementar su formato binario:
 * bloques con su propia tabla de constantes, tipos autodescriptos, enteros de longitud variable y
 * una tabla de metadatos por bloque. No es API publica ni esta especificado en ningun lado — se
 * saca del codigo del JDK.
 *
 * <p>El constructor falla con {@link IOException} diciendo esto. Es la excepcion que ya declaraba,
 * asi que el que llama no tiene que manejar nada nuevo; simplemente se entera de que el archivo no
 * se puede abrir.
 *
 * @since 9
 */
public final class RecordingFile implements Closeable {

    private static final String NO_HAY =
            "leer un archivo de grabacion necesita el lector del formato binario de JFR, que esta "
            + "biblioteca no implementa";

    /**
     * Abre un archivo de grabacion.
     *
     * @param file el archivo
     * @throws IOException en esta VM siempre; ver la nota de la clase
     * @throws NullPointerException si es {@code null}
     */
    public RecordingFile(final Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        throw new IOException(NO_HAY);
    }

    /**
     * El proximo evento.
     *
     * @return el evento
     * @throws IOException en esta VM siempre
     */
    public RecordedEvent readEvent() throws IOException {
        throw new IOException(NO_HAY);
    }

    /**
     * Si queda algun evento por leer.
     *
     * @return si quedan
     */
    public boolean hasMoreEvents() {
        return false;
    }

    /**
     * Los tipos de evento que el archivo declara.
     *
     * @return los tipos
     * @throws IOException en esta VM siempre
     */
    public List<EventType> readEventTypes() throws IOException {
        throw new IOException(NO_HAY);
    }

    /** Cierra el archivo. */
    public void close() throws IOException {
    }

    /**
     * Copia a otro archivo los eventos que pasen el filtro.
     *
     * @param destination el archivo destino
     * @param filter que eventos conservar
     * @throws IOException en esta VM siempre
     * @throws NullPointerException si alguno es {@code null}
     */
    public void write(final Path destination, final Predicate<RecordedEvent> filter)
            throws IOException {
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(filter, "filter");
        throw new IOException(NO_HAY);
    }

    /**
     * Todos los eventos de un archivo, de una.
     *
     * <p>Solo para archivos chicos: una grabacion de produccion no entra en memoria.
     *
     * @param file el archivo
     * @return los eventos
     * @throws IOException en esta VM siempre
     * @throws NullPointerException si es {@code null}
     */
    public static List<RecordedEvent> readAllEvents(final Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        throw new IOException(NO_HAY);
    }
}
