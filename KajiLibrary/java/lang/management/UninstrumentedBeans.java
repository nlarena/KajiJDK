package java.lang.management;

import javax.management.ObjectName;

/**
 * Las MXBean cuyos datos esta maquina virtual todavia no lleva.
 *
 * <p>De acceso de paquete: no es API. Existen para que {@code ManagementFactory} pueda cumplir su
 * contrato de no devolver null en esos metodos.
 *
 * <p>La regla que siguen es una sola: <b>lo que no se sabe se declara, no se inventa</b>. Cada metodo
 * que necesitaria un contador de la maquina virtual lanza {@link UnsupportedOperationException}; los
 * que preguntan si algo esta soportado contestan false, que es cierto; y los que tienen un valor
 * documentado para "no disponible" --el -1 de los tiempos-- devuelven ese.
 *
 * <p>Devolver ceros seria peor que fallar: un cero es una afirmacion, y seria falsa.
 */
final class UninstrumentedBeans {

    /** El de carga de clases. */
    static final ClassLoadingMXBean CLASS_LOADING = new Loading();

    /** El de hilos. */
    static final ThreadMXBean THREADS = new Threads();

    private UninstrumentedBeans() {
    }

    /** El mensaje que comparten todos los rechazos. */
    private static UnsupportedOperationException absent(String what) {
        return new UnsupportedOperationException(what + " is not instrumented in this VM");
    }

    /** Contadores de carga de clases. */
    private static final class Loading implements ClassLoadingMXBean {

        /** El rastreo; se guarda aunque no haya quien lo emita. */
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

    /** Contadores de hilos. */
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

        /** No, y decirlo es la respuesta correcta. */
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
         * @throws UnsupportedOperationException no se puede afirmar que no hay interbloqueo sin
         *     poder mirar los monitores, y null significaria exactamente eso
         */
        public long[] findMonitorDeadlockedThreads() {
            throw absent("monitor deadlock detection");
        }

        public void resetPeakThreadCount() {
            throw absent("thread counts");
        }

        /** @throws UnsupportedOperationException por lo mismo que la anterior */
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
