package java.lang.management;

import javax.management.openmbean.CompositeData;

/**
 * KajiLibrary's java.lang.management.MemoryUsage -- cuanta memoria hay y cuanta se usa.
 *
 * <p>Cuatro numeros, y la diferencia entre ellos es lo que hay que entender:
 *
 * <ul>
 *   <li>{@link #getInit} lo que se pidio al arrancar;
 *   <li>{@link #getUsed} lo que hay ocupado ahora;
 *   <li>{@link #getCommitted} lo que el sistema operativo tiene <b>reservado de verdad</b> para la
 *       maquina virtual. Siempre mayor o igual que lo usado, y puede bajar si la maquina virtual
 *       devuelve memoria;
 *   <li>{@link #getMax} el techo, si lo hay.
 * </ul>
 *
 * <p>El que se malinterpreta es {@code committed}. Un programa que mira {@code used/max} para saber si
 * esta cerca del limite se lleva sorpresas: lo que importa para el rendimiento es cuanto falta para
 * que {@code committed} tenga que crecer.
 *
 * <p>{@code init} y {@code max} pueden valer -1, que significa "no definido". Los otros dos no.
 *
 * <p>Es inmutable: es una foto del momento en que se pidio, no una vista viva.
 */
public class MemoryUsage {

    /** Lo pedido al arrancar, o -1. */
    private final long init;

    /** Lo ocupado ahora. */
    private final long used;

    /** Lo reservado al sistema operativo. */
    private final long committed;

    /** El techo, o -1. */
    private final long max;

    /**
     * @throws IllegalArgumentException si algun valor es negativo sin ser -1 donde se permite, si lo
     *     usado supera lo reservado, o si lo reservado supera el techo
     */
    public MemoryUsage(long init, long used, long committed, long max) {
        if (init < -1) {
            throw new IllegalArgumentException(
                "init parameter = " + init + " is negative but not -1.");
        }
        if (max < -1) {
            throw new IllegalArgumentException(
                "max parameter = " + max + " is negative but not -1.");
        }
        if (used < 0) {
            throw new IllegalArgumentException("used parameter = " + used + " is negative.");
        }
        if (committed < 0) {
            throw new IllegalArgumentException(
                "committed parameter = " + committed + " is negative.");
        }
        if (used > committed) {
            throw new IllegalArgumentException(
                "used = " + used + " should be <= committed = " + committed);
        }
        if (max >= 0 && committed > max) {
            throw new IllegalArgumentException(
                "committed = " + committed + " should be < max = " + max);
        }
        this.init = init;
        this.used = used;
        this.committed = committed;
        this.max = max;
    }

    /** Lo pedido al arrancar, o -1. */
    public long getInit() {
        return this.init;
    }

    /** Lo ocupado ahora. */
    public long getUsed() {
        return this.used;
    }

    /** Lo reservado al sistema operativo. Ver la nota de la clase. */
    public long getCommitted() {
        return this.committed;
    }

    /** El techo, o -1. */
    public long getMax() {
        return this.max;
    }

    /** Los cuatro numeros, cada uno en bytes y en kilobytes. */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("init = ").append(this.init).append('(').append(this.init >> 10).append("K) ");
        buf.append("used = ").append(this.used).append('(').append(this.used >> 10).append("K) ");
        buf.append("committed = ").append(this.committed).append('(')
            .append(this.committed >> 10).append("K) ");
        buf.append("max = ").append(this.max).append('(').append(this.max >> 10).append("K)");
        return buf.toString();
    }

    /**
     * Lo mismo, leido de un {@link CompositeData}.
     *
     * <p>Es como llega de una maquina virtual remota: por la red viaja el dato abierto y de este lado
     * se vuelve a armar el objeto.
     *
     * @return el objeto, o null si el dato es null
     * @throws IllegalArgumentException si el dato no describe un {@code MemoryUsage}
     */
    public static MemoryUsage from(CompositeData cd) {
        if (cd == null) {
            return null;
        }
        final String type = "MemoryUsage";
        return new MemoryUsage(CompositeItems.longValue(cd, "init", type),
                               CompositeItems.longValue(cd, "used", type),
                               CompositeItems.longValue(cd, "committed", type),
                               CompositeItems.longValue(cd, "max", type));
    }
}
