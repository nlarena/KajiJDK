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
 * Las tres MXBean que esta biblioteca puede contestar con datos de verdad.
 *
 * <p>De acceso de paquete: no es API. Salen de {@code System} y de {@code Runtime}, que son las dos
 * cosas que la maquina virtual si expone.
 *
 * <p>Lo que no se puede saber no se inventa: {@code getNonHeapMemoryUsage} lanza
 * {@link UnsupportedOperationException} en lugar de devolver ceros. Ver la nota de
 * {@link ManagementFactory}.
 */
final class RuntimeBackedBeans {

    /** Cuando se cargo esta clase; lo mas parecido al arranque que se puede medir desde Java. */
    private static final long START_TIME = System.currentTimeMillis();

    /** Un contador monotono para medir el tiempo corriendo. */
    private static final long START_NANOS = System.nanoTime();

    /** El del sistema operativo. */
    static final OperatingSystemMXBean OS = new Os();

    /** El de arranque. */
    static final RuntimeMXBean RUNTIME = new Rt();

    /** El de memoria. */
    static final MemoryMXBean MEMORY = new Mem();

    private RuntimeBackedBeans() {
    }

    /** Una propiedad del sistema, o null si no se puede leer. */
    static String property(String name) {
        try {
            return System.getProperty(name);
        } catch (Throwable e) {
            return null;
        }
    }

    /** El nombre del MBean, o null si no se pudo armar. */
    static ObjectName name(String s) {
        try {
            return ObjectName.getInstance(s);
        } catch (Throwable e) {
            return null;
        }
    }

    /** Datos del sistema operativo; todos reales. */
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

        /** Negativo: esta plataforma no publica la carga promedio, que es lo que significa. */
        public double getSystemLoadAverage() {
            return -1.0;
        }

        public ObjectName getObjectName() {
            return name(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
        }
    }

    /** Datos de arranque; salen de las propiedades del sistema. */
    private static final class Rt implements RuntimeMXBean {

        /**
         * El nombre de esta maquina virtual.
         *
         * <p>El JDK devuelve {@code pid@maquina}; aca no hay pid que consultar, y la documentacion
         * del metodo dice explicitamente que puede ser cualquier cadena. Devolver algo con forma de
         * pid seria inventarlo.
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

        /** No: la ruta de arranque desaparecio con los modulos. */
        public boolean isBootClassPathSupported() {
            return false;
        }

        public String getBootClassPath() {
            throw new UnsupportedOperationException(
                "Boot class path mechanism is not supported");
        }

        /**
         * Vacio.
         *
         * <p>No es una afirmacion de que no hubo argumentos: es que esta maquina virtual no los
         * conserva. La lista vacia es lo unico que se puede devolver sin inventar, y el metodo no
         * tiene forma de decir "no se".
         */
        public List<String> getInputArguments() {
            return Collections.emptyList();
        }

        /** Desde que se cargo la clase, medido con un contador monotono. */
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
                // Solo las de cadena a cadena, como manda la documentacion: las Properties admiten
                // cualquier objeto y este mapa no.
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

    /** El monton, medido de verdad; lo demas, declarado como ausente. */
    private static final class Mem implements MemoryMXBean {

        /** Si el rastreo esta prendido; se guarda aunque no haya nada que rastrear. */
        private volatile boolean verbose = false;

        /**
         * @throws UnsupportedOperationException esta maquina virtual no lleva esa cuenta
         */
        public int getObjectPendingFinalizationCount() {
            throw new UnsupportedOperationException(
                "finalization is not instrumented in this VM");
        }

        /**
         * El monton, desde {@code Runtime}.
         *
         * <p>{@code init} sale -1 porque no se sabe cuanto se pidio al arrancar; {@code used} es lo
         * total menos lo libre, {@code committed} es lo total, y {@code max} es el techo.
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
         * @throws UnsupportedOperationException esta maquina virtual no separa lo que no es monton
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

        /** Sugiere recolectar; es exactamente {@code System.gc()}. */
        public void gc() {
            System.gc();
        }

        public ObjectName getObjectName() {
            return name(ManagementFactory.MEMORY_MXBEAN_NAME);
        }
    }
}
