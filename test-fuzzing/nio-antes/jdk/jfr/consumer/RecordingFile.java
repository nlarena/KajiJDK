package jdk.jfr.consumer;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import jdk.jfr.EventType;

/**
 * It reads a recording file, event by event.
 *
 * <h2>How it is used</h2>
 *
 * <pre>{@code
 * try (RecordingFile f = new RecordingFile(Path.of("recording.jfr"))) {
 *     while (f.hasMoreEvents()) {
 *         RecordedEvent e = f.readEvent();
 *         ...
 *     }
 * }
 * }</pre>
 *
 * <p>One event at a time and not all of them at once: a production recording has millions and does
 * not fit in memory. {@link #readAllEvents} exists for the small files and is the one to avoid with
 * the large ones.
 *
 * <h2>{@link #write} filters without decompressing into memory</h2>
 *
 * <p>It copies to another file only the events that pass the predicate. It serves for trimming a
 * huge recording before sending it to somebody, without opening it whole.
 *
 * <h2>State in this VM</h2>
 *
 * <p><strong>Not implemented.</strong> Reading a {@code .jfr} is implementing its binary format:
 * blocks with a constant table of their own, self-described types, integers of variable length and
 * a table of metadata per block. It is not public API nor is it specified anywhere -- it is taken
 * out of the code of the JDK.
 *
 * <p>The constructor fails with {@link IOException} saying this. It is the exception it already
 * declared, so the caller does not have to handle anything new; they simply find out that the file
 * cannot be opened.
 *
 * @since 9
 */
public final class RecordingFile implements Closeable {

    private static final String NOT_THERE =
            "reading a recording file needs the reader of the binary format of JFR, which this "
            + "library does not implement";

    /**
     * It opens a recording file.
     *
     * @param file the file
     * @throws IOException in this VM always; see the note of the class
     * @throws NullPointerException if it is {@code null}
     */
    public RecordingFile(final Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        throw new IOException(NOT_THERE);
    }

    /**
     * The next event.
     *
     * @return the event
     * @throws IOException in this VM always
     */
    public RecordedEvent readEvent() throws IOException {
        throw new IOException(NOT_THERE);
    }

    /**
     * Whether there is any event left to read.
     *
     * @return whether there are any left
     */
    public boolean hasMoreEvents() {
        return false;
    }

    /**
     * The types of event the file declares.
     *
     * @return the types
     * @throws IOException in this VM always
     */
    public List<EventType> readEventTypes() throws IOException {
        throw new IOException(NOT_THERE);
    }

    /** It closes the file. */
    public void close() throws IOException {
    }

    /**
     * It copies to another file the events that pass the filter.
     *
     * @param destination the destination file
     * @param filter which events to keep
     * @throws IOException in this VM always
     * @throws NullPointerException if either is {@code null}
     */
    public void write(final Path destination, final Predicate<RecordedEvent> filter)
            throws IOException {
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(filter, "filter");
        throw new IOException(NOT_THERE);
    }

    /**
     * Every event of a file, at once.
     *
     * <p>Only for small files: a production recording does not fit in memory.
     *
     * @param file the file
     * @return the events
     * @throws IOException in this VM always
     * @throws NullPointerException if it is {@code null}
     */
    public static List<RecordedEvent> readAllEvents(final Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        throw new IOException(NOT_THERE);
    }
}
