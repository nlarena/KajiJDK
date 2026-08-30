package java.lang;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import java.time.Duration;
import java.time.Instant;

/**
 * KajiLibrary's java.lang.ProcessHandle -- the identity of a native process.
 *
 * <p>The <strong>surface</strong> of the JDK's interface, faithfully. The behaviour behind it is
 * not here: KajiJDK has no OS-process subsystem (no {@code Runtime.exec}/{@code ProcessBuilder}),
 * so the factories that would materialize a handle ({@link #of(long)}, {@link #current()},
 * {@link #allProcesses()}) throw {@link UnsupportedOperationException}. A concrete {@code
 * ProcessHandle} is what a real implementation would return; nothing here produces one.
 */
public interface ProcessHandle extends Comparable<ProcessHandle> {

    /** The native process id. */
    long pid();

    /** The parent process, if any and if visible. */
    Optional<ProcessHandle> parent();

    /** The direct children of this process. */
    Stream<ProcessHandle> children();

    /** Every descendant of this process. */
    Stream<ProcessHandle> descendants();

    /** A snapshot of information about the process. */
    ProcessHandle.Info info();

    /** A future that completes when the process exits. */
    CompletableFuture<ProcessHandle> onExit();

    /** Whether {@link #destroy()} terminates normally rather than forcibly. */
    boolean supportsNormalTermination();

    /** Request normal termination; returns whether the request went out. */
    boolean destroy();

    /** Request forcible termination; returns whether the request went out. */
    boolean destroyForcibly();

    /** Whether the process is still running. */
    boolean isAlive();

    int hashCode();

    boolean equals(Object other);

    int compareTo(ProcessHandle other);

    /**
     * The handle for the process with this id, if it exists and is visible.
     *
     * <p>KajiJDK has no OS-process subsystem, so there is no handle to return.
     */
    static Optional<ProcessHandle> of(long pid) {
        throw new UnsupportedOperationException("los procesos del SO no están soportados");
    }

    /** The handle for the current process (unsupported: no OS-process subsystem). */
    static ProcessHandle current() {
        throw new UnsupportedOperationException("los procesos del SO no están soportados");
    }

    /** Every visible process (unsupported: no OS-process subsystem). */
    static Stream<ProcessHandle> allProcesses() {
        throw new UnsupportedOperationException("los procesos del SO no están soportados");
    }

    /** A snapshot of information about a process. Every field is optional and may be absent. */
    interface Info {
        Optional<String> command();

        Optional<String> commandLine();

        Optional<String[]> arguments();

        Optional<Instant> startInstant();

        Optional<Duration> totalCpuDuration();

        Optional<String> user();
    }
}
