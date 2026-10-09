package java.lang.management;

/**
 * KajiLibrary's java.lang.management.CompilationMXBean -- the run-time compiler.
 *
 * <p>It exists only if the virtual machine has one: {@code ManagementFactory.getCompilationMXBean()}
 * returns <b>null</b> on one that only interprets. It is one of the few places in the API where null
 * is a right answer and not an error. This virtual machine has a JIT, so here it is not null.
 *
 * <p>{@link #getTotalCompilationTime} is accumulated time across <b>all</b> the compilation threads,
 * not wall clock, so it can be greater than the program has been running. And it is approximate: the
 * documentation itself warns that it may not be monotonic if the virtual machine readjusts its
 * measurement. Here it refuses instead, because nothing publishes that counter -- see
 * {@link #isCompilationTimeMonitoringSupported}.
 */
public interface CompilationMXBean extends PlatformManagedObject {

    /** The compiler's name. */
    String getName();

    /** Whether it knows how to measure how long it spends compiling. */
    boolean isCompilationTimeMonitoringSupported();

    /**
     * Accumulated milliseconds spent compiling. See the class's note.
     *
     * @throws UnsupportedOperationException if it cannot measure it
     */
    long getTotalCompilationTime();
}
