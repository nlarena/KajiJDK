package java.lang.management;

import javax.management.openmbean.CompositeData;

/**
 * KajiLibrary's java.lang.management.MemoryNotificationInfo -- un area de memoria paso un umbral.
 *
 * <p>Es lo que viaja como {@code userData} de la notificacion que emite el MBean de memoria. No se
 * manda tal cual: se manda como {@link CompositeData} y de este lado se rearma con {@link #from}, que
 * es por lo que la clase existe.
 *
 * <h2>Los dos umbrales, que no son lo mismo</h2>
 *
 * <ul>
 *   <li>{@link #MEMORY_THRESHOLD_EXCEEDED} se supero el umbral de <b>uso</b>: hay mas ocupado que lo
 *       que se fijo, en este momento;
 *   <li>{@link #MEMORY_COLLECTION_THRESHOLD_EXCEEDED} se supero el umbral de <b>uso despues de
 *       recolectar</b>: quedo mas ocupado que lo que se fijo <i>una vez que el recolector paso</i>.
 * </ul>
 *
 * <p>La segunda es la que importa para detectar una fuga. La primera se dispara todo el tiempo en un
 * programa sano, porque la memoria sube antes de cada recoleccion; la segunda solo se dispara si algo
 * de verdad no se esta pudiendo liberar.
 *
 * <p>{@link #getCount} dice cuantas veces se cruzo ese umbral desde que se fijo, no cuantas
 * notificaciones hubo: la maquina virtual no manda una por cada cruce.
 */
public class MemoryNotificationInfo {

    /** Se supero el umbral de uso. Ver la nota de la clase. */
    public static final String MEMORY_THRESHOLD_EXCEEDED =
        "java.management.memory.threshold.exceeded";

    /** Se supero el umbral de uso despues de recolectar. Ver la nota de la clase. */
    public static final String MEMORY_COLLECTION_THRESHOLD_EXCEEDED =
        "java.management.memory.collection.threshold.exceeded";

    /** Cual area. */
    private final String poolName;

    /** Como estaba cuando se cruzo. */
    private final MemoryUsage usage;

    /** Cuantas veces se cruzo. */
    private final long count;

    /**
     * @throws NullPointerException si el nombre o el uso son null
     */
    public MemoryNotificationInfo(String poolName, MemoryUsage usage, long count) {
        if (poolName == null) {
            throw new NullPointerException("Null poolName");
        }
        if (usage == null) {
            throw new NullPointerException("Null usage");
        }
        this.poolName = poolName;
        this.usage = usage;
        this.count = count;
    }

    /** Cual area de memoria. */
    public String getPoolName() {
        return this.poolName;
    }

    /** Como estaba en el momento del cruce. */
    public MemoryUsage getUsage() {
        return this.usage;
    }

    /** Cuantas veces se cruzo el umbral. Ver la nota de la clase. */
    public long getCount() {
        return this.count;
    }

    /**
     * Lo mismo, leido de un {@link CompositeData}. Ver la nota de la clase.
     *
     * @return el objeto, o null si el dato es null
     * @throws IllegalArgumentException si el dato no describe un {@code MemoryNotificationInfo}
     */
    public static MemoryNotificationInfo from(CompositeData cd) {
        if (cd == null) {
            return null;
        }
        final String type = "MemoryNotificationInfo";
        Object u = CompositeItems.optional(cd, "usage");
        if (!(u instanceof CompositeData)) {
            throw new IllegalArgumentException(
                "Unexpected composite type for " + type + ": item usage is not a CompositeData");
        }
        return new MemoryNotificationInfo(CompositeItems.string(cd, "poolName", type),
                                          MemoryUsage.from((CompositeData) u),
                                          CompositeItems.longValue(cd, "count", type));
    }
}
