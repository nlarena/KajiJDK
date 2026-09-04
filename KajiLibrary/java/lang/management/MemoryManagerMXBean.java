package java.lang.management;

/**
 * KajiLibrary's java.lang.management.MemoryManagerMXBean -- quien administra un area de memoria.
 *
 * <p>Un administrador maneja una o mas areas; un area puede tener mas de un administrador. La relacion
 * es de muchos a muchos, y por eso {@link #getMemoryPoolNames} devuelve un arreglo y no un nombre.
 *
 * <p>{@link #isValid} puede pasar de true a false: la maquina virtual puede dar de baja un
 * administrador mientras corre, y a partir de ahi el MBean sigue existiendo pero no informa mas nada.
 * Hay que consultarlo antes de creerle a los demas metodos.
 *
 * <p>{@link GarbageCollectorMXBean} es la subinterfaz para los administradores que ademas recolectan.
 */
public interface MemoryManagerMXBean extends PlatformManagedObject {

    /** Su nombre. */
    String getName();

    /** Si sigue vigente. Ver la nota de la clase. */
    boolean isValid();

    /** Que areas administra. */
    String[] getMemoryPoolNames();
}
