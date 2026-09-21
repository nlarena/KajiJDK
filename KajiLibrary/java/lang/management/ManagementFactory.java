package java.lang.management;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;

/**
 * KajiLibrary's java.lang.management.ManagementFactory -- where the platform's MXBeans come from.
 *
 * <p>The whole package is reached through here. There are three ways of asking for the same thing
 * and it is worth knowing which to use:
 *
 * <ul>
 *   <li>the concrete {@code getXxxMXBean()}, for this virtual machine's <b>own</b> data. It is the
 *       direct way;
 *   <li>{@link #getPlatformMXBean(Class)}, also local but generic, for code that does not know in
 *       advance which MBean it wants;
 *   <li>the versions taking an {@link MBeanServerConnection}, for a <b>remote</b> virtual machine.
 *       They return a proxy that turns each call into a query over the network.
 * </ul>
 *
 * <p>That last point is what makes the package powerful: the same code that reads its own memory
 * reads another process's, changing only where the MBean comes from.
 *
 * <h2>{@link #getCompilationMXBean} may return null</h2>
 *
 * <p>And that is not an error: it means the virtual machine has no run-time compiler. It is one of
 * the few places in the API where null is the right answer. <b>Here it is not null</b> -- see below.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>There are three groups here, and what tells them apart is where the datum comes from:
 *
 * <ul>
 *   <li><b>real</b>: {@link #getOperatingSystemMXBean}, {@link #getRuntimeMXBean},
 *       {@link #getMemoryMXBean} and {@link #getCompilationMXBean} answer with genuine data, taken
 *       from {@code System}, from {@code Runtime} and from the fact that `src/burst` is a JIT. What
 *       they say is true;
 *   <li><b>partly measurable</b>: the compilation bean names the compiler but refuses
 *       {@code getTotalCompilationTime}, because nothing in the virtual machine publishes that
 *       counter. Refusing is the branch the JDK's own contract defines for a machine that cannot
 *       measure it;
 *   <li><b>not instrumented</b>: the per-memory-area counters, the class-loading ones and the thread
 *       ones need the virtual machine to keep them, and it does not expose them yet. Those methods
 *       throw {@link UnsupportedOperationException} instead of returning zeros, because a zero would
 *       be a false statement and not an absence.
 * </ul>
 *
 * <p>{@link #getPlatformMBeanServer} and the remote proxies are missing too: they ask for a platform
 * MBean server with all of this already registered.
 */
public class ManagementFactory {

    /** The class-loading MBean's name. */
    public static final String CLASS_LOADING_MXBEAN_NAME = "java.lang:type=ClassLoading";

    /** The compiler's. */
    public static final String COMPILATION_MXBEAN_NAME = "java.lang:type=Compilation";

    /** Memory's. */
    public static final String MEMORY_MXBEAN_NAME = "java.lang:type=Memory";

    /** The operating system's. */
    public static final String OPERATING_SYSTEM_MXBEAN_NAME = "java.lang:type=OperatingSystem";

    /** The runtime's. */
    public static final String RUNTIME_MXBEAN_NAME = "java.lang:type=Runtime";

    /** Threads'. */
    public static final String THREAD_MXBEAN_NAME = "java.lang:type=Threading";

    /** The collectors' prefix; each one appends {@code ,name=<its own>}. */
    public static final String GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE =
        "java.lang:type=GarbageCollector";

    /** The memory managers' prefix. */
    public static final String MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE = "java.lang:type=MemoryManager";

    /** The memory pools' prefix. */
    public static final String MEMORY_POOL_MXBEAN_DOMAIN_TYPE = "java.lang:type=MemoryPool";

    /** Not instantiated; the public constructor is the one the JDK left behind. */
    public ManagementFactory() {
    }

    /**
     * The class-loading MBean.
     *
     * <p>See the class's note: it exists, and its methods declare their lack of instrumentation.
     */
    public static ClassLoadingMXBean getClassLoadingMXBean() {
        return UninstrumentedBeans.CLASS_LOADING;
    }

    /** The memory MBean, with the heap genuinely measured. See the class's note. */
    public static MemoryMXBean getMemoryMXBean() {
        return RuntimeBackedBeans.MEMORY;
    }

    /** The thread MBean. */
    public static ThreadMXBean getThreadMXBean() {
        return UninstrumentedBeans.THREADS;
    }

    /** The runtime MBean, with real data. */
    public static RuntimeMXBean getRuntimeMXBean() {
        return RuntimeBackedBeans.RUNTIME;
    }

    /**
     * The compiler's MBean, or null if there is none.
     *
     * <p>Here it is not null: this virtual machine has a JIT. See the class's note.
     */
    public static CompilationMXBean getCompilationMXBean() {
        return RuntimeBackedBeans.COMPILATION;
    }

    /** The operating system MBean, with real data. */
    public static OperatingSystemMXBean getOperatingSystemMXBean() {
        return RuntimeBackedBeans.OS;
    }

    /**
     * The memory pools.
     *
     * <p>Empty: this virtual machine does not publish its pools separately. See the class's note.
     */
    public static List<MemoryPoolMXBean> getMemoryPoolMXBeans() {
        return Collections.emptyList();
    }

    /** The memory managers. Empty, for the same reason. */
    public static List<MemoryManagerMXBean> getMemoryManagerMXBeans() {
        return Collections.emptyList();
    }

    /** The collectors. Empty, for the same reason. */
    public static List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
        return Collections.emptyList();
    }

    /**
     * The platform MBean server, with everything above already registered.
     *
     * @throws UnsupportedOperationException always in this library; see the class's note
     */
    public static synchronized MBeanServer getPlatformMBeanServer() {
        throw new UnsupportedOperationException(
            "no platform MBeanServer in this library");
    }

    /**
     * A proxy to another virtual machine's MXBean.
     *
     * @throws IllegalArgumentException if the name is not a platform MXBean's
     * @throws IOException if the communication failed
     * @throws UnsupportedOperationException always in this library
     */
    public static <T> T newPlatformMXBeanProxy(MBeanServerConnection connection, String mxbeanName,
                                               Class<T> mxbeanInterface) throws IOException {
        throw new UnsupportedOperationException(
            "no platform MXBean proxies in this library");
    }

    /**
     * The platform MXBean of that type, or null if this virtual machine does not have it.
     *
     * <p>It is the generic form of the {@code getXxxMXBean()}; it is for when the type is decided at
     * run time.
     *
     * @throws IllegalArgumentException if that type is not a platform MXBean's, or if there is more
     *     than one instance -- {@link #getPlatformMXBeans} is there for those
     */
    public static <T extends PlatformManagedObject> T getPlatformMXBean(Class<T> mxbeanInterface) {
        if (mxbeanInterface == null) {
            throw new NullPointerException();
        }
        if (mxbeanInterface == ClassLoadingMXBean.class) {
            return mxbeanInterface.cast(getClassLoadingMXBean());
        }
        if (mxbeanInterface == MemoryMXBean.class) {
            return mxbeanInterface.cast(getMemoryMXBean());
        }
        if (mxbeanInterface == ThreadMXBean.class) {
            return mxbeanInterface.cast(getThreadMXBean());
        }
        if (mxbeanInterface == RuntimeMXBean.class) {
            return mxbeanInterface.cast(getRuntimeMXBean());
        }
        if (mxbeanInterface == OperatingSystemMXBean.class) {
            return mxbeanInterface.cast(getOperatingSystemMXBean());
        }
        if (mxbeanInterface == CompilationMXBean.class) {
            return null;
        }
        if (mxbeanInterface == MemoryPoolMXBean.class
            || mxbeanInterface == MemoryManagerMXBean.class
            || mxbeanInterface == GarbageCollectorMXBean.class
            || mxbeanInterface == BufferPoolMXBean.class) {
            throw new IllegalArgumentException(mxbeanInterface.getName()
                + " can have zero or more than one instances");
        }
        throw new IllegalArgumentException(
            mxbeanInterface.getName() + " is not a platform management interface");
    }

    /**
     * Every platform MXBean of that type.
     *
     * <p>It returns a list because there are types with more than one instance: there is one
     * {@link GarbageCollectorMXBean} per collector, one {@link MemoryPoolMXBean} per pool.
     *
     * @throws IllegalArgumentException if that type is not a platform MXBean's
     */
    public static <T extends PlatformManagedObject> List<T> getPlatformMXBeans(
        Class<T> mxbeanInterface) {
        if (mxbeanInterface == null) {
            throw new NullPointerException();
        }
        if (mxbeanInterface == MemoryPoolMXBean.class
            || mxbeanInterface == MemoryManagerMXBean.class
            || mxbeanInterface == GarbageCollectorMXBean.class
            || mxbeanInterface == BufferPoolMXBean.class
            || mxbeanInterface == PlatformLoggingMXBean.class) {
            return Collections.emptyList();
        }
        T single = getPlatformMXBean(mxbeanInterface);
        if (single == null) {
            return Collections.emptyList();
        }
        List<T> one = new ArrayList<T>(1);
        one.add(single);
        return Collections.unmodifiableList(one);
    }

    /**
     * The same, from a remote virtual machine.
     *
     * @throws UnsupportedOperationException always in this library
     */
    public static <T extends PlatformManagedObject> T getPlatformMXBean(
        MBeanServerConnection connection, Class<T> mxbeanInterface) throws IOException {
        throw new UnsupportedOperationException(
            "no platform MXBean proxies in this library");
    }

    /**
     * The same, as a list.
     *
     * @throws UnsupportedOperationException always in this library
     */
    public static <T extends PlatformManagedObject> List<T> getPlatformMXBeans(
        MBeanServerConnection connection, Class<T> mxbeanInterface) throws IOException {
        throw new UnsupportedOperationException(
            "no platform MXBean proxies in this library");
    }

    /** Every platform MXBean type this virtual machine knows. */
    public static Set<Class<? extends PlatformManagedObject>> getPlatformManagementInterfaces() {
        Set<Class<? extends PlatformManagedObject>> all =
            new HashSet<Class<? extends PlatformManagedObject>>();
        all.add(ClassLoadingMXBean.class);
        all.add(CompilationMXBean.class);
        all.add(MemoryMXBean.class);
        all.add(MemoryManagerMXBean.class);
        all.add(MemoryPoolMXBean.class);
        all.add(GarbageCollectorMXBean.class);
        all.add(OperatingSystemMXBean.class);
        all.add(RuntimeMXBean.class);
        all.add(ThreadMXBean.class);
        all.add(BufferPoolMXBean.class);
        all.add(PlatformLoggingMXBean.class);
        return Collections.unmodifiableSet(all);
    }
}
