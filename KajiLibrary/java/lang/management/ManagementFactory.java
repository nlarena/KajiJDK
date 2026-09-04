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
 * KajiLibrary's java.lang.management.ManagementFactory -- de donde salen las MXBean de la plataforma.
 *
 * <p>Todo el paquete se consigue por aca. Hay tres formas de pedir lo mismo y conviene saber cual usar:
 *
 * <ul>
 *   <li>los {@code getXxxMXBean()} concretos, para la maquina virtual <b>propia</b>. Es lo directo;
 *   <li>{@link #getPlatformMXBean(Class)}, tambien local pero generico, para codigo que no sabe de
 *       antemano que MBean quiere;
 *   <li>las versiones que toman un {@link MBeanServerConnection}, para una maquina virtual
 *       <b>remota</b>. Devuelven un proxy que traduce cada llamada en una consulta por la red.
 * </ul>
 *
 * <p>Ese ultimo punto es lo que hace potente al paquete: el mismo codigo que lee la memoria propia
 * lee la de otro proceso, cambiando solo de donde sale el MBean.
 *
 * <h2>{@link #getCompilationMXBean} puede devolver null</h2>
 *
 * <p>Y no es un error: significa que esta maquina virtual no tiene compilador de tiempo de ejecucion.
 * Es de los pocos lugares de la API donde null es la respuesta correcta.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Aca hay tres grupos, y la diferencia entre ellos es de donde sale el dato:
 *
 * <ul>
 *   <li><b>reales</b>: {@link #getOperatingSystemMXBean}, {@link #getRuntimeMXBean} y
 *       {@link #getMemoryMXBean} contestan con datos de verdad, sacados de {@code System} y de
 *       {@code Runtime}. Lo que dicen es cierto;
 *   <li><b>null legitimo</b>: {@link #getCompilationMXBean} devuelve null, que es exactamente lo que
 *       corresponde -- este es un interprete y no compila nada;
 *   <li><b>no instrumentado</b>: los contadores por area de memoria, los de carga de clases y los de
 *       hilos necesitan que la maquina virtual los lleve, y esta todavia no los expone. Esos metodos
 *       lanzan {@link UnsupportedOperationException} en lugar de devolver ceros, porque un cero seria
 *       una afirmacion falsa y no una ausencia.
 * </ul>
 *
 * <p>{@link #getPlatformMBeanServer} y los proxies remotos tambien faltan: piden un servidor de MBeans
 * de la plataforma con todo esto ya registrado.
 */
public class ManagementFactory {

    /** El nombre del MBean de carga de clases. */
    public static final String CLASS_LOADING_MXBEAN_NAME = "java.lang:type=ClassLoading";

    /** El del compilador. */
    public static final String COMPILATION_MXBEAN_NAME = "java.lang:type=Compilation";

    /** El de memoria. */
    public static final String MEMORY_MXBEAN_NAME = "java.lang:type=Memory";

    /** El del sistema operativo. */
    public static final String OPERATING_SYSTEM_MXBEAN_NAME = "java.lang:type=OperatingSystem";

    /** El de arranque. */
    public static final String RUNTIME_MXBEAN_NAME = "java.lang:type=Runtime";

    /** El de hilos. */
    public static final String THREAD_MXBEAN_NAME = "java.lang:type=Threading";

    /** El prefijo de los recolectores; cada uno agrega {@code ,name=<el suyo>}. */
    public static final String GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE =
        "java.lang:type=GarbageCollector";

    /** El prefijo de los administradores de memoria. */
    public static final String MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE = "java.lang:type=MemoryManager";

    /** El prefijo de las areas de memoria. */
    public static final String MEMORY_POOL_MXBEAN_DOMAIN_TYPE = "java.lang:type=MemoryPool";

    /** No se instancia; el constructor publico es el que el JDK dejo. */
    public ManagementFactory() {
    }

    /**
     * El MBean de carga de clases.
     *
     * <p>Ver la nota de la clase: existe y sus metodos declaran su falta de instrumentacion.
     */
    public static ClassLoadingMXBean getClassLoadingMXBean() {
        return UninstrumentedBeans.CLASS_LOADING;
    }

    /** El MBean de memoria, con el monton medido de verdad. Ver la nota de la clase. */
    public static MemoryMXBean getMemoryMXBean() {
        return RuntimeBackedBeans.MEMORY;
    }

    /** El MBean de hilos. */
    public static ThreadMXBean getThreadMXBean() {
        return UninstrumentedBeans.THREADS;
    }

    /** El MBean de arranque, con datos reales. */
    public static RuntimeMXBean getRuntimeMXBean() {
        return RuntimeBackedBeans.RUNTIME;
    }

    /**
     * El MBean del compilador, o null si no hay.
     *
     * <p>Aca es null: esta maquina virtual interpreta. Ver la nota de la clase.
     */
    public static CompilationMXBean getCompilationMXBean() {
        return null;
    }

    /** El MBean del sistema operativo, con datos reales. */
    public static OperatingSystemMXBean getOperatingSystemMXBean() {
        return RuntimeBackedBeans.OS;
    }

    /**
     * Las areas de memoria.
     *
     * <p>Vacio: esta maquina virtual no publica sus areas por separado. Ver la nota de la clase.
     */
    public static List<MemoryPoolMXBean> getMemoryPoolMXBeans() {
        return Collections.emptyList();
    }

    /** Los administradores de memoria. Vacio, por lo mismo. */
    public static List<MemoryManagerMXBean> getMemoryManagerMXBeans() {
        return Collections.emptyList();
    }

    /** Los recolectores. Vacio, por lo mismo. */
    public static List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
        return Collections.emptyList();
    }

    /**
     * El servidor de MBeans de la plataforma, con todo lo anterior ya registrado.
     *
     * @throws UnsupportedOperationException siempre en esta biblioteca; ver la nota de la clase
     */
    public static synchronized MBeanServer getPlatformMBeanServer() {
        throw new UnsupportedOperationException(
            "no platform MBeanServer in this library");
    }

    /**
     * Un proxy hacia una MXBean de otra maquina virtual.
     *
     * @throws IllegalArgumentException si el nombre no es de una MXBean de la plataforma
     * @throws IOException si fallo la comunicacion
     * @throws UnsupportedOperationException siempre en esta biblioteca
     */
    public static <T> T newPlatformMXBeanProxy(MBeanServerConnection connection, String mxbeanName,
                                               Class<T> mxbeanInterface) throws IOException {
        throw new UnsupportedOperationException(
            "no platform MXBean proxies in this library");
    }

    /**
     * La MXBean de la plataforma de ese tipo, o null si esta maquina virtual no la tiene.
     *
     * <p>Es la forma generica de los {@code getXxxMXBean()}; sirve cuando el tipo se decide en tiempo
     * de ejecucion.
     *
     * @throws IllegalArgumentException si ese tipo no es una MXBean de la plataforma, o si hay mas de
     *     una instancia -- para esas esta {@link #getPlatformMXBeans}
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
     * Todas las MXBean de la plataforma de ese tipo.
     *
     * <p>Devuelve una lista porque hay tipos con mas de una instancia: hay un
     * {@link GarbageCollectorMXBean} por recolector, un {@link MemoryPoolMXBean} por area.
     *
     * @throws IllegalArgumentException si ese tipo no es una MXBean de la plataforma
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
     * Idem, de una maquina virtual remota.
     *
     * @throws UnsupportedOperationException siempre en esta biblioteca
     */
    public static <T extends PlatformManagedObject> T getPlatformMXBean(
        MBeanServerConnection connection, Class<T> mxbeanInterface) throws IOException {
        throw new UnsupportedOperationException(
            "no platform MXBean proxies in this library");
    }

    /**
     * Idem, en lista.
     *
     * @throws UnsupportedOperationException siempre en esta biblioteca
     */
    public static <T extends PlatformManagedObject> List<T> getPlatformMXBeans(
        MBeanServerConnection connection, Class<T> mxbeanInterface) throws IOException {
        throw new UnsupportedOperationException(
            "no platform MXBean proxies in this library");
    }

    /** Todos los tipos de MXBean de la plataforma que esta maquina virtual conoce. */
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
