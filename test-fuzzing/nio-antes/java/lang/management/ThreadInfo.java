package java.lang.management;

import javax.management.openmbean.CompositeData;

/**
 * KajiLibrary's java.lang.management.ThreadInfo -- a snapshot of a thread.
 *
 * <p>Everything that can be known about a thread from outside: its state, its stack, which lock it
 * is waiting for, who holds it, and --if it was asked for-- which locks it holds itself.
 *
 * <h2>It has no public constructor</h2>
 *
 * <p>And that is on purpose: only the virtual machine can build a coherent one. It is obtained
 * through {@link ThreadMXBean}, or rebuilt from a {@link CompositeData} with {@link #from} when it
 * came from another virtual machine.
 *
 * <h2>It is a snapshot, not a view</h2>
 *
 * <p>What it says was true at the instant it was taken. A thread showing as {@code BLOCKED} may be
 * running by the time it is read. It is for diagnosing, not for deciding.
 *
 * <p>The useful exception is a deadlock: if {@code findDeadlockedThreads} reported it, that does not
 * change by itself -- by definition.
 *
 * <h2>The missing values</h2>
 *
 * <p>{@link #getStackTrace} returns an <b>empty</b> array if no stack was asked for, not null.
 * {@link #getBlockedTime} and {@link #getWaitedTime} return -1 if contention tracking is off, which
 * is the normal case. {@link #getLockOwnerId} returns -1 if nobody holds it, and
 * {@link #getLockInfo} null if the thread is waiting for nothing.
 */
public class ThreadInfo {

    private final long threadId;

    private final String threadName;

    private final Thread.State threadState;

    private final long blockedTime;

    private final long blockedCount;

    private final long waitedTime;

    private final long waitedCount;

    private final LockInfo lock;

    private final String lockName;

    private final long lockOwnerId;

    private final String lockOwnerName;

    private final boolean inNative;

    private final boolean suspended;

    private final boolean daemon;

    private final int priority;

    private final StackTraceElement[] stackTrace;

    private final MonitorInfo[] lockedMonitors;

    private final LockInfo[] lockedSynchronizers;

    /**
     * The only constructor, package-private.
     *
     * <p>{@link #from} and this library's {@link ThreadMXBean} implementation use it. See the
     * class's note on why it is not public.
     */
    ThreadInfo(long threadId, String threadName, Thread.State threadState, long blockedTime,
               long blockedCount, long waitedTime, long waitedCount, LockInfo lock,
               String lockName, long lockOwnerId, String lockOwnerName, boolean inNative,
               boolean suspended, boolean daemon, int priority, StackTraceElement[] stackTrace,
               MonitorInfo[] lockedMonitors, LockInfo[] lockedSynchronizers) {
        this.threadId = threadId;
        this.threadName = threadName;
        this.threadState = threadState;
        this.blockedTime = blockedTime;
        this.blockedCount = blockedCount;
        this.waitedTime = waitedTime;
        this.waitedCount = waitedCount;
        this.lock = lock;
        this.lockName = lockName;
        this.lockOwnerId = lockOwnerId;
        this.lockOwnerName = lockOwnerName;
        this.inNative = inNative;
        this.suspended = suspended;
        this.daemon = daemon;
        this.priority = priority;
        if (stackTrace == null) {
            this.stackTrace = new StackTraceElement[0];
        } else {
            this.stackTrace = stackTrace;
        }
        if (lockedMonitors == null) {
            this.lockedMonitors = new MonitorInfo[0];
        } else {
            this.lockedMonitors = lockedMonitors;
        }
        if (lockedSynchronizers == null) {
            this.lockedSynchronizers = new LockInfo[0];
        } else {
            this.lockedSynchronizers = lockedSynchronizers;
        }
    }

    /** Its identifier. See {@link ThreadMXBean}: they get reused. */
    public long getThreadId() {
        return this.threadId;
    }

    /** Its name. */
    public String getThreadName() {
        return this.threadName;
    }

    /** What state it was in. */
    public Thread.State getThreadState() {
        return this.threadState;
    }

    /** Milliseconds blocked, or -1. See the class's note. */
    public long getBlockedTime() {
        return this.blockedTime;
    }

    /** How many times it blocked. */
    public long getBlockedCount() {
        return this.blockedCount;
    }

    /** Milliseconds waiting, or -1. */
    public long getWaitedTime() {
        return this.waitedTime;
    }

    /** How many times it waited. */
    public long getWaitedCount() {
        return this.waitedCount;
    }

    /** Which lock it was waiting for, or null. */
    public LockInfo getLockInfo() {
        return this.lock;
    }

    /** That lock as text, or null. */
    public String getLockName() {
        return this.lockName;
    }

    /** Who held it, or -1. */
    public long getLockOwnerId() {
        return this.lockOwnerId;
    }

    /** The name of whoever held it, or null. */
    public String getLockOwnerName() {
        return this.lockOwnerName;
    }

    /** The stack, or an empty array if none was asked for. */
    public StackTraceElement[] getStackTrace() {
        return this.stackTrace;
    }

    /**
     * Whether it was suspended.
     *
     * <p>It can only be true through {@code Thread.suspend()}, which is deprecated and no longer
     * does anything.
     */
    public boolean isSuspended() {
        return this.suspended;
    }

    /** Whether it was running native code. */
    public boolean isInNative() {
        return this.inNative;
    }

    /** Whether it is a daemon thread. */
    public boolean isDaemon() {
        return this.daemon;
    }

    /** Its priority. */
    public int getPriority() {
        return this.priority;
    }

    /** The monitors it held, or an empty array. */
    public MonitorInfo[] getLockedMonitors() {
        return this.lockedMonitors;
    }

    /** The {@code java.util.concurrent} locks it held, or an empty array. */
    public LockInfo[] getLockedSynchronizers() {
        return this.lockedSynchronizers;
    }

    /** How many frames {@link #toString} prints at most; the rest come out as an ellipsis. */
    private static final int MAX_FRAMES = 8;

    /**
     * The thread's header and up to eight stack frames.
     *
     * <p>It cuts on purpose: a dump of a hundred threads with each one's whole stack is unreadable.
     * {@link #getStackTrace} is there for the full one.
     *
     * <p>It also notes, on the frame it belongs to, the lock the thread is waiting for and the ones
     * it holds. That correlation between frame and lock is what makes a dump readable.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('"').append(this.threadName).append('"');
        if (this.daemon) {
            sb.append(" daemon");
        }
        sb.append(" prio=").append(this.priority);
        sb.append(" Id=").append(this.threadId);
        sb.append(' ').append(this.threadState);
        if (this.lockName != null) {
            sb.append(" on ").append(this.lockName);
        }
        if (this.lockOwnerName != null) {
            sb.append(" owned by \"").append(this.lockOwnerName)
                .append("\" Id=").append(this.lockOwnerId);
        }
        if (this.suspended) {
            sb.append(" (suspended)");
        }
        if (this.inNative) {
            sb.append(" (in native)");
        }
        sb.append('\n');
        int i = 0;
        while (i < this.stackTrace.length && i < MAX_FRAMES) {
            sb.append("\tat ").append(this.stackTrace[i].toString()).append('\n');
            if (i == 0 && this.lock != null) {
                if (this.threadState == Thread.State.BLOCKED) {
                    sb.append("\t-  blocked on ").append(this.lock).append('\n');
                } else if (this.threadState == Thread.State.WAITING
                    || this.threadState == Thread.State.TIMED_WAITING) {
                    sb.append("\t-  waiting on ").append(this.lock).append('\n');
                }
            }
            int j = 0;
            while (j < this.lockedMonitors.length) {
                if (this.lockedMonitors[j].getLockedStackDepth() == i) {
                    sb.append("\t-  locked ").append(this.lockedMonitors[j]).append('\n');
                }
                j = j + 1;
            }
            i = i + 1;
        }
        if (i < this.stackTrace.length) {
            sb.append("\t...").append('\n');
        }
        if (this.lockedSynchronizers.length > 0) {
            sb.append("\n\tNumber of locked synchronizers = ")
                .append(this.lockedSynchronizers.length).append('\n');
            int k = 0;
            while (k < this.lockedSynchronizers.length) {
                sb.append("\t- ").append(this.lockedSynchronizers[k]).append('\n');
                k = k + 1;
            }
        }
        sb.append('\n');
        return sb.toString();
    }

    /**
     * The same, read out of a {@link CompositeData}.
     *
     * <p>It is how it arrives from another virtual machine. The held-lock items appeared later and
     * are read if they are there.
     *
     * @return the object, or null if the datum is null
     * @throws IllegalArgumentException if the datum does not describe a {@code ThreadInfo}
     */
    public static ThreadInfo from(CompositeData cd) {
        if (cd == null) {
            return null;
        }
        final String type = "ThreadInfo";
        String stateName = CompositeItems.string(cd, "threadState", type);
        Thread.State state = null;
        if (stateName != null) {
            state = Thread.State.valueOf(stateName);
        }
        return new ThreadInfo(
            CompositeItems.longValue(cd, "threadId", type),
            CompositeItems.string(cd, "threadName", type),
            state,
            CompositeItems.longValue(cd, "blockedTime", type),
            CompositeItems.longValue(cd, "blockedCount", type),
            CompositeItems.longValue(cd, "waitedTime", type),
            CompositeItems.longValue(cd, "waitedCount", type),
            lockOf(cd),
            CompositeItems.string(cd, "lockName", type),
            CompositeItems.longValue(cd, "lockOwnerId", type),
            CompositeItems.string(cd, "lockOwnerName", type),
            CompositeItems.bool(cd, "inNative", type),
            CompositeItems.bool(cd, "suspended", type),
            boolOrFalse(cd, "daemon"),
            intOrDefault(cd, "priority", Thread.NORM_PRIORITY),
            framesOf(cd),
            monitorsOf(cd),
            synchronizersOf(cd));
    }

    /** The awaited lock; the item exists since Java 6 and may be missing. */
    private static LockInfo lockOf(CompositeData cd) {
        Object v = CompositeItems.optional(cd, "lockInfo");
        if (v instanceof CompositeData) {
            return LockInfo.from((CompositeData) v);
        }
        return null;
    }

    /** A boolean that may be missing. */
    private static boolean boolOrFalse(CompositeData cd, String name) {
        Object v = CompositeItems.optional(cd, name);
        return v instanceof Boolean && ((Boolean) v).booleanValue();
    }

    /** An integer that may be missing. */
    private static int intOrDefault(CompositeData cd, String name, int fallback) {
        Object v = CompositeItems.optional(cd, name);
        if (v instanceof Integer) {
            return ((Integer) v).intValue();
        }
        return fallback;
    }

    /** The stack. */
    private static StackTraceElement[] framesOf(CompositeData cd) {
        Object v = CompositeItems.optional(cd, "stackTrace");
        if (!(v instanceof CompositeData[])) {
            return new StackTraceElement[0];
        }
        CompositeData[] raw = (CompositeData[]) v;
        StackTraceElement[] out = new StackTraceElement[raw.length];
        int i = 0;
        while (i < raw.length) {
            out[i] = StackTraceElements.from(raw[i]);
            i = i + 1;
        }
        return out;
    }

    /** The held monitors. */
    private static MonitorInfo[] monitorsOf(CompositeData cd) {
        Object v = CompositeItems.optional(cd, "lockedMonitors");
        if (!(v instanceof CompositeData[])) {
            return new MonitorInfo[0];
        }
        CompositeData[] raw = (CompositeData[]) v;
        MonitorInfo[] out = new MonitorInfo[raw.length];
        int i = 0;
        while (i < raw.length) {
            out[i] = MonitorInfo.from(raw[i]);
            i = i + 1;
        }
        return out;
    }

    /** The held {@code java.util.concurrent} locks. */
    private static LockInfo[] synchronizersOf(CompositeData cd) {
        Object v = CompositeItems.optional(cd, "lockedSynchronizers");
        if (!(v instanceof CompositeData[])) {
            return new LockInfo[0];
        }
        CompositeData[] raw = (CompositeData[]) v;
        LockInfo[] out = new LockInfo[raw.length];
        int i = 0;
        while (i < raw.length) {
            out[i] = LockInfo.from(raw[i]);
            i = i + 1;
        }
        return out;
    }
}
