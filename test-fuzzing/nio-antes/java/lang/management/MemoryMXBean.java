package java.lang.management;

/**
 * KajiLibrary's java.lang.management.MemoryMXBean -- the virtual machine's memory, in two numbers.
 *
 * <p>The summary of every pool: heap on one side, non-heap on the other. The {@link MemoryPoolMXBean}
 * are there for the per-pool detail.
 *
 * <p>It is also a notification emitter --it has to be asked for as a
 * {@code javax.management.NotificationEmitter}--: that is how the {@link MemoryNotificationInfo}
 * arrive when a pool crosses a threshold.
 *
 * <h2>{@link #gc} compels nothing</h2>
 *
 * <p>It is exactly {@code System.gc()}: a suggestion. The virtual machine may ignore it, and modern
 * ones often do. A program depending on this to free memory is leaning on something that promises
 * nothing.
 *
 * <p>{@link #getObjectPendingFinalizationCount} is an <b>approximation</b>, and it measures something
 * that hardly exists any more: finalisation is deprecated. Its growing means the finalisation queue
 * cannot keep up, which is a classic way of exhausting memory with no leak involved.
 */
public interface MemoryMXBean extends PlatformManagedObject {

    /** How many objects await finalisation, approximately. See the class's note. */
    int getObjectPendingFinalizationCount();

    /** The whole heap. */
    MemoryUsage getHeapMemoryUsage();

    /** Everything else the virtual machine reserves. */
    MemoryUsage getNonHeapMemoryUsage();

    /** Whether it is tracking memory. */
    boolean isVerbose();

    /** It switches tracking on or off, like {@code -verbose:gc}. */
    void setVerbose(boolean value);

    /** It suggests collecting. See the class's note: it compels nothing. */
    void gc();
}
