package java.lang.management;

import javax.management.ObjectName;

/**
 * The MXBeans whose data this virtual machine does not keep yet.
 *
 * <p>Package-private: not API. They exist so {@code ManagementFactory} can meet its contract of not
 * returning null from those methods.
 *
 * <p>They follow one rule: <b>what is not known is declared, not invented</b>. Every method that
 * would need a counter from the virtual machine throws {@link UnsupportedOperationException}; the
 * ones asking whether something is supported answer false, which is true; and the ones with a
 * documented value for "not available" --the -1 of the timings-- return that.
 *
 * <p>Returning zeros would be worse than failing: a zero is a statement, and it would be false.
 */
final class UninstrumentedBeans {

    /** The class-loading one. */
    static final ClassLoadingMXBean CLASS_LOADING = new Loading();

    /** The thread one. */
    static final ThreadMXBean THREADS = new Threads();

    private UninstrumentedBeans() {
    }

    /** The message every refusal shares. */
    private static UnsupportedOperationException absent(String what) {
        return new UnsupportedOperationException(what + " is not instrumented in this VM");
    }

    /** Class-loading counters. */
    private static final class Loading implements ClassLoadingMXBean {

        /** The tracking; it is kept even though nothing emits it. */
        private volatile boolean verbose = false;

        public long getTotalLoadedClassCount() {
            throw absent("class loading");
        }

        public int getLoadedClassCount() {
            throw absent("class loading");
        }

        public long getUnloadedClassCount() {
            throw absent("class loading");
        }

        public boolean isVerbose() {
            return this.verbose;
        }

        public void setVerbose(boolean value) {
            this.verbose = value;
        }

        public ObjectName getObjectName() {
            return RuntimeBackedBeans.name(ManagementFactory.CLASS_LOADING_MXBEAN_NAME);
        }
    }

    /** Thread counters. */
    private static final class Threads implements ThreadMXBean {

        public int getThreadCount() {
            throw absent("thread counts");
        }

        public int getPeakThreadCount() {
            throw absent("thread counts");
        }

        public long getTotalStartedThreadCount() {
            throw absent("thread counts");
        }

        public int getDaemonThreadCount() {
            throw absent("thread counts");
        }

        public long[] getAllThreadIds() {
            throw absent("thread enumeration");
        }

        public ThreadInfo getThreadInfo(long id) {
            throw absent("thread information");
        }

        public ThreadInfo[] getThreadInfo(long[] ids) {
            throw absent("thread information");
        }

        public ThreadInfo getThreadInfo(long id, int maxDepth) {
            throw absent("thread information");
        }

        public ThreadInfo[] getThreadInfo(long[] ids, int maxDepth) {
            throw absent("thread information");
        }

        /** No, and saying so is the right answer. */
        public boolean isThreadContentionMonitoringSupported() {
            return false;
        }

        public boolean isThreadContentionMonitoringEnabled() {
            throw new UnsupportedOperationException(
                "Thread contention monitoring is not supported.");
        }

        public void setThreadContentionMonitoringEnabled(boolean enable) {
            throw new UnsupportedOperationException(
                "Thread contention monitoring is not supported.");
        }

        public long getCurrentThreadCpuTime() {
            throw new UnsupportedOperationException("Current thread CPU time is not supported.");
        }

        public long getCurrentThreadUserTime() {
            throw new UnsupportedOperationException("Current thread CPU time is not supported.");
        }

        public long getThreadCpuTime(long id) {
            throw new UnsupportedOperationException("Thread CPU time is not supported.");
        }

        public long getThreadUserTime(long id) {
            throw new UnsupportedOperationException("Thread CPU time is not supported.");
        }

        public boolean isThreadCpuTimeSupported() {
            return false;
        }

        public boolean isCurrentThreadCpuTimeSupported() {
            return false;
        }

        public boolean isThreadCpuTimeEnabled() {
            throw new UnsupportedOperationException("Thread CPU time is not supported.");
        }

        public void setThreadCpuTimeEnabled(boolean enable) {
            throw new UnsupportedOperationException("Thread CPU time is not supported.");
        }

        /**
         * @throws UnsupportedOperationException there is no stating that no deadlock exists without
         *     being able to look at the monitors, and null would mean exactly that
         */
        public long[] findMonitorDeadlockedThreads() {
            throw absent("monitor deadlock detection");
        }

        public void resetPeakThreadCount() {
            throw absent("thread counts");
        }

        /** @throws UnsupportedOperationException for the same reason as the previous one */
        public long[] findDeadlockedThreads() {
            throw absent("deadlock detection");
        }

        public boolean isObjectMonitorUsageSupported() {
            return false;
        }

        public boolean isSynchronizerUsageSupported() {
            return false;
        }

        public ThreadInfo[] getThreadInfo(long[] ids, boolean lockedMonitors,
                                          boolean lockedSynchronizers) {
            throw absent("thread information");
        }

        public ThreadInfo[] dumpAllThreads(boolean lockedMonitors, boolean lockedSynchronizers) {
            throw absent("thread information");
        }

        public ObjectName getObjectName() {
            return RuntimeBackedBeans.name(ManagementFactory.THREAD_MXBEAN_NAME);
        }
    }
}
