package java.lang.management;

/**
 * KajiLibrary's java.lang.management.MemoryMXBean -- la memoria de la maquina virtual, en dos numeros.
 *
 * <p>El resumen de todas las areas: monton por un lado, no monton por el otro. Para el detalle por
 * area estan los {@link MemoryPoolMXBean}.
 *
 * <p>Es ademas un emisor de notificaciones --hay que consultarlo como
 * {@code javax.management.NotificationEmitter}--: por ahi llegan los
 * {@link MemoryNotificationInfo} cuando un area cruza un umbral.
 *
 * <h2>{@link #gc} no obliga a nada</h2>
 *
 * <p>Es exactamente {@code System.gc()}: una sugerencia. La maquina virtual puede ignorarla, y las
 * modernas seguido lo hacen. Un programa que dependa de que esto libere memoria esta apoyado en algo
 * que no promete nada.
 *
 * <p>{@link #getObjectPendingFinalizationCount} es una <b>aproximacion</b>, y ademas mide algo que ya
 * casi no existe: la finalizacion quedo obsoleta. Que crezca significa que la cola de finalizacion no
 * da abasto, que es una forma clasica de agotar la memoria sin que haya fuga.
 */
public interface MemoryMXBean extends PlatformManagedObject {

    /** Cuantos objetos esperan finalizacion, aproximadamente. Ver la nota de la clase. */
    int getObjectPendingFinalizationCount();

    /** El monton entero. */
    MemoryUsage getHeapMemoryUsage();

    /** Todo lo demas que la maquina virtual reserva. */
    MemoryUsage getNonHeapMemoryUsage();

    /** Si esta rastreando la memoria. */
    boolean isVerbose();

    /** Prende o apaga el rastreo, como {@code -verbose:gc}. */
    void setVerbose(boolean value);

    /** Sugiere recolectar. Ver la nota de la clase: no obliga. */
    void gc();
}
