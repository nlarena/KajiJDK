package jdk.management;

import java.lang.management.PlatformManagedObject;

/**
 * The administration view of the scheduler of virtual threads.
 *
 * <p>A virtual thread has no operating system thread of its own: it runs <em>mounted</em> on one of
 * a pool of carrier threads, and it is unmounted when it blocks. This MXBean exposes the four
 * quantities that describe that pool at a given moment --how many carriers there are, how many
 * virtual threads are mounted, how many wait in the queue-- and the only knob: the parallelism.
 *
 * <p>It is an interface of <em>reading plus one writing</em>, and the asymmetry is deliberate: the
 * size of the pool and the number of mounted ones are consequences, not decisions. The only thing
 * that is chosen is how many carriers the scheduler can use at a time.
 *
 * @since 24
 */
public interface VirtualThreadSchedulerMXBean extends PlatformManagedObject {

    /** How many carrier threads the scheduler can use at a time. */
    int getParallelism();

    /**
     * It changes the parallelism.
     *
     * @throws IllegalArgumentException if the value is not positive, or exceeds the maximum of the
     *     scheduler
     */
    void setParallelism(int size);

    /**
     * How many carrier threads exist now.
     *
     * <p>It does not have to coincide with {@link #getParallelism}: the pool grows on demand and
     * may stay above the parallelism while there are blocked carriers.
     */
    int getPoolSize();

    /** How many virtual threads are mounted on a carrier at this moment. */
    int getMountedVirtualThreadCount();

    /** How many virtual threads are queued waiting for a carrier. */
    long getQueuedVirtualThreadCount();
}
