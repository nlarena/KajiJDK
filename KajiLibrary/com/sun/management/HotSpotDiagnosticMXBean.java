package com.sun.management;

import java.io.IOException;
import java.lang.management.PlatformManagedObject;
import java.util.List;

/**
 * HotSpot's diagnostic tools: memory dumps, thread dumps, and the VM's options while hot.
 *
 * <h2>Why the options are read from here and not from the command line</h2>
 *
 * <p>Because the command line says what was <strong>asked for</strong>, not what was left. The
 * VM adjusts a lot of values by itself according to the hardware, and an option may end up
 * being worth something nobody wrote anywhere. {@link #getVMOption} returns the effective value
 * together with {@link VMOption#getOrigin its origin}, which is the only thing that allows an
 * automatic adjustment to be told from an operator's decision.
 *
 * <h2>Writing while hot</h2>
 *
 * <p>{@link #setVMOption} only accepts the options marked manageable -- those for which
 * {@link VMOption#isWriteable} gives true. They are few on purpose: most of them size
 * structures that are built on starting, and changing them afterwards would mean nothing.
 *
 * @since 1.6
 */
public interface HotSpotDiagnosticMXBean extends PlatformManagedObject {

    /**
     * It writes a dump of the heap into a file.
     *
     * <p>The file is written by <strong>the VM</strong>, not by whoever calls, so the path is
     * interpreted on the system where the process runs. Over a remote connection that means that
     * the file is left over there.
     *
     * @param outputFile the path, which has to end in {@code .hprof}
     * @param live whether to dump only the reachable objects; it forces a full collection first
     * @throws IOException if it could not be written
     * @throws NullPointerException if the path is {@code null}
     * @throws IllegalArgumentException if the path does not end in {@code .hprof} or already
     *     exists
     */
    void dumpHeap(String outputFile, boolean live) throws IOException;

    /**
     * All this VM's diagnostic options.
     *
     * @return the options
     */
    List<VMOption> getDiagnosticOptions();

    /**
     * An option by name, with its effective value and its origin.
     *
     * @param name the name
     * @return the option
     * @throws NullPointerException if the name is {@code null}
     * @throws IllegalArgumentException if an option with that name does not exist
     */
    VMOption getVMOption(String name);

    /**
     * It changes a manageable option's value.
     *
     * @param name the name
     * @param value the new value, as text
     * @throws NullPointerException if the name or the value is {@code null}
     * @throws IllegalArgumentException if the option does not exist, is not writeable, or the
     *     value does not belong to it
     */
    void setVMOption(String name, String value);

    /**
     * It writes a dump of the threads into a file.
     *
     * <p>By default it is not supported: it is an operation that was added after this interface,
     * and an old implementation does not have it. The implementations that do have it redefine
     * this.
     *
     * @param outputFile the path, interpreted on the system where the VM runs
     * @param format the format
     * @throws IOException if it could not be written
     * @throws UnsupportedOperationException if this implementation does not support it
     * @since 21
     */
    default void dumpThreads(String outputFile, ThreadDumpFormat format) throws IOException {
        throw new UnsupportedOperationException();
    }

    /** The format of a thread dump. */
    enum ThreadDumpFormat {
        /** Text to read, the usual one. */
        TEXT_PLAIN,
        /** JSON, to process with a tool. */
        JSON
    }
}
