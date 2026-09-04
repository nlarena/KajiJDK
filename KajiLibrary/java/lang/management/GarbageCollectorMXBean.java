package java.lang.management;

/**
 * KajiLibrary's java.lang.management.GarbageCollectorMXBean -- un recolector de basura.
 *
 * <p>Un {@link MemoryManagerMXBean} que ademas cuenta recolecciones. Hay varios por maquina virtual:
 * los recolectores generacionales tienen uno para la generacion joven y otro para la vieja, y sus
 * numeros se leen muy distinto -- muchas recolecciones jovenes y rapidas es sano, muchas viejas no.
 *
 * <p>Los dos valores son <b>acumulados</b>. Para que digan algo hay que medir la diferencia entre dos
 * lecturas: el tiempo total dividido por el tiempo transcurrido es la fraccion de la maquina que se
 * fue en recolectar, y esa es la cifra que importa.
 *
 * <p>Los dos pueden devolver -1 si la maquina virtual no lleva la cuenta.
 */
public interface GarbageCollectorMXBean extends MemoryManagerMXBean {

    /** Cuantas recolecciones hizo, o -1. */
    long getCollectionCount();

    /** Milisegundos acumulados recolectando, o -1. Ver la nota de la clase. */
    long getCollectionTime();
}
