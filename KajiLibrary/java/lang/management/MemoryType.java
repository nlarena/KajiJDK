package java.lang.management;

/**
 * KajiLibrary's java.lang.management.MemoryType -- monton o no monton.
 *
 * <p>Dos valores, y la division es la que hace la especificacion de la maquina virtual:
 *
 * <ul>
 *   <li>{@link #HEAP} es donde viven los objetos. Es lo que el recolector recorre;
 *   <li>{@link #NON_HEAP} es todo lo demas que la maquina virtual reserva: el area de metodos, los
 *       datos de las clases cargadas, el codigo compilado.
 * </ul>
 *
 * <p>Un programa que se queda sin memoria mira primero cual de las dos se lleno: son problemas
 * distintos con soluciones distintas.
 *
 * <p>{@link #toString} devuelve {@code "Heap memory"} y {@code "Non-heap memory"}, no el nombre de la
 * constante. Para el nombre hay que usar {@code name()}.
 */
public enum MemoryType {

    /** Donde viven los objetos. */
    HEAP("Heap memory"),

    /** Lo que la maquina virtual reserva para si. */
    NON_HEAP("Non-heap memory");

    /** El texto para mostrar. */
    private final String description;

    MemoryType(String s) {
        this.description = s;
    }

    /** El texto para mostrar, no el nombre. Ver la nota de la clase. */
    @Override
    public String toString() {
        return this.description;
    }
}
