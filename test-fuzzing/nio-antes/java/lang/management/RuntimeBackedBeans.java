package java.lang.management;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.management.ObjectName;

/**
 * The MXBeans this library can answer with genuine data.
 *
 * <p>Package-private: not API. They come out of {@code System} and {@code Runtime}, which are the
 * two things the virtual machine does expose, plus the fact that it has a JIT.
 *
 * <p>What cannot be known is not invented: {@code getNonHeapMemoryUsage} throws
 * {@link UnsupportedOperationException} instead of returning zeros. See {@link ManagementFactory}'s
 * note.
 */
final class RuntimeBackedBeans {

    /** When this class was loaded; the closest thing to start-up measurable from Java. */
    private static final long START_TIME = System.currentTimeMillis();

    /** A monotonic counter for measuring the time running. */
    private static final long START_NANOS = System.nanoTime();

    /** The operating system's. */
    static final OperatingSystemMXBean OS = new Os();

    /** The runtime's. */
    static final RuntimeMXBean RUNTIME = new Rt();

    /** Memory's. */
    static final MemoryMXBean MEMORY = new Mem();

    /** The run-time compiler's. See {@link Compilation} for what it can and cannot answer. */
    static final CompilationMXBean COMPILATION = new Compilation();

    private RuntimeBackedBeans() {
    }

    /** A system property, or null if it cannot be read. */
    static String property(String name) {
        try {
            return System.getProperty(name);
        } catch (Throwable e) {
            return null;
        }
    }

    /** The MBean's name, or null if it could not be built. */
    static ObjectName name(String s) {
        try {
            return ObjectName.getInstance(s);
        } catch (Throwable e) {
            return null;
        }
    }

    /** Operating system data; all of it real. */
    private static final class Os implements OperatingSystemMXBean {

        public String getName() {
            return property("os.name");
        }

        public String getArch() {
            return property("os.arch");
        }

        public String getVersion() {
            return property("os.version");
        }

        public int getAvailableProcessors() {
            return Runtime.getRuntime().availableProcessors();
        }

        /** Negative: this platform does not publish the load average, which is what that means. */
        public double getSystemLoadAverage() {
            return -1.0;
        }

        public ObjectName getObjectName() {
            return name(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
        }
    }

    /**
     * The run-time compiler.
     *
     * <p>This one exists, and that is the point: `getCompilationMXBean()` used to return null on the
     * grounds that this virtual machine only interprets. It does not -- `src/burst` is a JIT and the
     * interpreter carries its cache (`jit: JitCache`) -- so null was the answer for a machine with no
     * compiler, given by one that has one.
     *
     * <p>What it still cannot answer is <b>how long</b> it has spent compiling: nothing in the VM
     * publishes that counter. So {@link #isCompilationTimeMonitoringSupported} says false and
     * {@link #getTotalCompilationTime} throws, which is exactly the branch the JDK's contract defines
     * for that case -- not a gap invented here.
     */
    private static final class Compilation implements CompilationMXBean {

        public String getName() {
            return "Burst";
        }

        public boolean isCompilationTimeMonitoringSupported() {
            return false;
        }

        public long getTotalCompilationTime() {
            throw new UnsupportedOperationException(
                    "this virtual machine does not measure how long it spends compiling");
        }

        public ObjectName getObjectName() {
            return name(ManagementFactory.COMPILATION_MXBEAN_NAME);
        }
    }

    /** Start-up data; it comes out of the system properties. */
    private static final class Rt implements RuntimeMXBean {

        /**
         * This virtual machine's name.
         *
         * <p>The JDK returns {@code pid@machine}; here there is no pid to ask for, and the method's
         * documentation says explicitly that it can be any string. Returning something shaped like a
         * pid would be inventing one.
         */
        public String getName() {
            return "KajiJDK";
        }

        public String getVmName() {
            return property("java.vm.name");
        }

        public String getVmVendor() {
            return property("java.vm.vendor");
        }

        public String getVmVersion() {
            return property("java.vm.version");
        }

        public String getSpecName() {
            return property("java.vm.specification.name");
        }

        public String getSpecVendor() {
            return property("java.vm.specification.vendor");
        }

        public String getSpecVersion() {
            return property("java.vm.specification.version");
        }

        public String getManagementSpecVersion() {
            return "2.0";
        }

        public String getClassPath() {
            return property("java.class.path");
        }

        public String getLibraryPath() {
            return property("java.library.path");
        }

        /** No: the boot class path went away with modules. */
        public boolean isBootClassPathSupported() {
            return false;
        }

        public String getBootClassPath() {
            throw new UnsupportedOperationException(
                "Boot class path mechanism is not supported");
        }

        /**
         * Empty.
         *
         * <p>It is not a statement that there were no arguments: it is that this virtual machine does
         * not keep them. The empty list is the only thing returnable without inventing, and the
         * method has no way of saying "I do not know".
         */
        public List<String> getInputArguments() {
            return Collections.emptyList();
        }

        /** Since the class was loaded, measured with a monotonic counter. */
        public long getUptime() {
            return (System.nanoTime() - START_NANOS) / 1000000L;
        }

        public long getStartTime() {
            return START_TIME;
        }

        public Map<String, String> getSystemProperties() {
            Map<String, String> out = new HashMap<String, String>();
            Properties props;
            try {
                props = System.getProperties();
            } catch (Throwable e) {
                return out;
            }
            Iterator<Object> it = props.keySet().iterator();
            while (it.hasNext()) {
                Object k = it.next();
                Object v = props.get(k);
                // String-to-string only, as the documentation requires: Properties accepts any
                // object and this map does not.
                if (k instanceof String && v instanceof String) {
                    out.put((String) k, (String) v);
                }
            }
            return out;
        }

        public ObjectName getObjectName() {
            return name(ManagementFactory.RUNTIME_MXBEAN_NAME);
        }
    }

    /** The heap, genuinely measured; the rest, declared absent. */
    private static final class Mem implements MemoryMXBean {

        /** Whether tracking is on; it is kept even though there is nothing to track. */
        private volatile boolean verbose = false;

        /**
         * @throws UnsupportedOperationException this virtual machine does not keep that count
         */
        public int getObjectPendingFinalizationCount() {
            throw new UnsupportedOperationException(
                "finalization is not instrumented in this VM");
        }

        /**
         * The heap, from {@code Runtime}.
         *
         * <p>{@code init} comes out -1 because how much was asked for at start-up is not known;
         * {@code used} is the total minus the free, {@code committed} is the total, and {@code max}
         * is the ceiling.
         */
        public MemoryUsage getHeapMemoryUsage() {
            Runtime r = Runtime.getRuntime();
            long total = r.totalMemory();
            long free = r.freeMemory();
            long max = r.maxMemory();
            long used = total - free;
            if (used < 0) {
                used = 0;
            }
            if (max >= 0 && max < total) {
                max = total;
            }
            return new MemoryUsage(-1L, used, total, max);
        }

        /**
         * @throws UnsupportedOperationException this virtual machine does not separate out what is
         *     not heap
         */
        public MemoryUsage getNonHeapMemoryUsage() {
            throw new UnsupportedOperationException(
                "non-heap memory is not instrumented in this VM");
        }

        public boolean isVerbose() {
            return this.verbose;
        }

        public void setVerbose(boolean value) {
            this.verbose = value;
        }

        /** It suggests collecting; it is exactly {@code System.gc()}. */
        public void gc() {
            System.gc();
        }

        public ObjectName getObjectName() {
            return name(ManagementFactory.MEMORY_MXBEAN_NAME);
        }
    }
}
