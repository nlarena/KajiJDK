package java.lang.management;

/**
 * KajiLibrary's java.lang.management.ThreadMXBean -- this virtual machine's threads.
 *
 * <p>The package's largest interface, and the only one that can <b>detect deadlocks</b>. That last
 * point is what makes it valuable: it is information a program cannot work out for itself.
 *
 * <h2>The two deadlock finders</h2>
 *
 * <p>{@link #findMonitorDeadlockedThreads} looks only at {@code synchronized} monitors;
 * {@link #findDeadlockedThreads} looks at {@code java.util.concurrent}'s locks as well. The second
 * is nearly always the one wanted -- the first is from a time when the other kind of lock did not
 * exist--.
 *
 * <p>Both return <b>null</b> when none is blocked, not an empty array. It is this interface's
 * commonest trap.
 *
 * <h2>What has to be switched on first</h2>
 *
 * <p>Two things are off by default because they cost:
 *
 * <ul>
 *   <li><b>contention</b> tracking ({@link #setThreadContentionMonitoringEnabled}), without which
 *       {@code getBlockedTime} and {@code getWaitedTime} return -1;
 *   <li><b>processor time</b> tracking ({@link #setThreadCpuTimeEnabled}), without which
 *       {@code getThreadCpuTime} returns -1.
 * </ul>
 *
 * <p>And not every virtual machine supports them; they have to be asked about with the
 * {@code isXxxSupported} first, because switching them on without support throws
 * {@link UnsupportedOperationException}.
 *
 * <h2>The identifiers get reused</h2>
 *
 * <p>A thread's identifier is good while that thread lives. When it dies, the virtual machine may
 * hand the number out again. Keeping identifiers and asking about them later can return another
 * thread's information, or null.
 */
public interface ThreadMXBean extends PlatformManagedObject {

    /** How many live threads there are, the daemon ones included. */
    int getThreadCount();

    /** The peak since it started, or since the last {@link #resetPeakThreadCount}. */
    int getPeakThreadCount();

    /** How many were created in all. */
    long getTotalStartedThreadCount();

    /** How many of the live ones are daemons. */
    int getDaemonThreadCount();

    /** The live ones' identifiers. See the class's note: they get reused. */
    long[] getAllThreadIds();

    /**
     * That thread's information, with no stack.
     *
     * @return null if there is no live thread with that identifier
     * @throws IllegalArgumentException if the identifier is not positive
     */
    ThreadInfo getThreadInfo(long id);

    /** The same, for several; a position that does not exist is left null. */
    ThreadInfo[] getThreadInfo(long[] ids);

    /**
     * The same, with up to that many stack frames.
     *
     * @param maxDepth how many frames at most; {@link Integer#MAX_VALUE} for all of them
     * @throws IllegalArgumentException if the depth is negative
     */
    ThreadInfo getThreadInfo(long id, int maxDepth);

    /** The same, for several. */
    ThreadInfo[] getThreadInfo(long[] ids, int maxDepth);

    /** Whether this virtual machine knows how to measure contention. */
    boolean isThreadContentionMonitoringSupported();

    /**
     * Whether it is switched on.
     *
     * @throws UnsupportedOperationException if it does not support it
     */
    boolean isThreadContentionMonitoringEnabled();

    /**
     * It switches it on or off. See the class's note.
     *
     * @throws UnsupportedOperationException if it does not support it
     */
    void setThreadContentionMonitoringEnabled(boolean enable);

    /**
     * Processor nanoseconds of the current thread, or -1 if it is switched off.
     *
     * @throws UnsupportedOperationException if it does not support it
     */
    long getCurrentThreadCpuTime();

    /**
     * The same, user-mode time only.
     *
     * @throws UnsupportedOperationException if it does not support it
     */
    long getCurrentThreadUserTime();

    /**
     * Processor nanoseconds of that thread, or -1.
     *
     * @throws UnsupportedOperationException if it does not support it
     */
    long getThreadCpuTime(long id);

    /**
     * The same, in user mode.
     *
     * @throws UnsupportedOperationException if it does not support it
     */
    long getThreadUserTime(long id);

    /** Whether it can measure any thread's processor time. */
    boolean isThreadCpuTimeSupported();

    /** Whether it can measure at least the current thread's. */
    boolean isCurrentThreadCpuTimeSupported();

    /**
     * Whether the measurement is switched on.
     *
     * @throws UnsupportedOperationException if it does not support it
     */
    boolean isThreadCpuTimeEnabled();

    /**
     * It switches it on or off.
     *
     * @throws UnsupportedOperationException if it does not support it
     */
    void setThreadCpuTimeEnabled(boolean enable);

    /**
     * The threads blocked in a cycle of monitors.
     *
     * @return null if there is none. See the class's note.
     */
    long[] findMonitorDeadlockedThreads();

    /** It sets the peak to the current count. */
    void resetPeakThreadCount();

    /**
     * The threads blocked in a cycle, counting {@code java.util.concurrent}'s locks as well.
     *
     * @return null if there is none
     * @throws UnsupportedOperationException if this virtual machine cannot look at those locks
     */
    long[] findDeadlockedThreads();

    /** Whether it can report which monitors a thread holds. */
    boolean isObjectMonitorUsageSupported();

    /** Whether it can report which {@code java.util.concurrent} locks it holds. */
    boolean isSynchronizerUsageSupported();

    /**
     * Those threads' information, with the whole stack and optionally the locks.
     *
     * @param lockedMonitors whether to include the held monitors
     * @param lockedSynchronizers whether to include {@code java.util.concurrent}'s locks
     * @throws UnsupportedOperationException if something this virtual machine does not support is
     *     asked for
     */
    ThreadInfo[] getThreadInfo(long[] ids, boolean lockedMonitors, boolean lockedSynchronizers);

    /**
     * The same, limiting the stack depth.
     *
     * <p>By default it ignores the limit and delegates to the version without it; a virtual machine
     * that knows how to cut the stack overrides this so as not to pay for frames that are going to
     * be discarded.
     */
    default ThreadInfo[] getThreadInfo(long[] ids, boolean lockedMonitors,
                                       boolean lockedSynchronizers, int maxDepth) {
        throw new UnsupportedOperationException();
    }

    /** <b>Every</b> live thread's information. */
    ThreadInfo[] dumpAllThreads(boolean lockedMonitors, boolean lockedSynchronizers);

    /** The same, limiting the depth. */
    default ThreadInfo[] dumpAllThreads(boolean lockedMonitors, boolean lockedSynchronizers,
                                        int maxDepth) {
        throw new UnsupportedOperationException();
    }
}
