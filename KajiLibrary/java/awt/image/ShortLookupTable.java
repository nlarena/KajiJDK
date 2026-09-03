package java.awt.image;

/**
 * Una {@link LookupTable} de shorts, leidos **sin signo** (0..65535).
 *
 * <p>Es la que sirve para imagenes de 16 bits por componente. Vale la misma nota de signo que
 * {@link ByteLookupTable}.
 */
public class ShortLookupTable extends LookupTable {

    private final short[][] data;

    /**
     * Una tabla por componente.
     *
     * <p>Los arreglos **no se copian**: la tabla se queda con los que se le dan. Es lo
     * que hace el JDK y lo que permite compartir una tabla grande entre varios filtros
     * sin duplicarla.
     *
     * @throws IllegalArgumentException si el desplazamiento es negativo
     */
    public ShortLookupTable(int offset, short[][] data) {
        super(offset, data.length);
        this.data = new short[data.length][];
        for (int i = 0; i < data.length; i++) {
            this.data[i] = data[i];
        }
    }

    /** Una sola tabla, que se aplica a todos los componentes. */
    public ShortLookupTable(int offset, short[] data) {
        super(offset, 1);
        this.data = new short[1][];
        this.data[0] = data;
    }

    /** Las tablas, sin copiar. */
    public final short[][] getTable() {
        return this.data;
    }

    /**
     * Aplica la tabla a un pixel.
     *
     * <p>Con una sola tabla se usa esa para todos los componentes; con varias, la que
     * corresponde a cada uno.
     *
     * @throws ArrayIndexOutOfBoundsException si un valor cae fuera de la tabla despues
     *     de restarle el desplazamiento
     */
    public int[] lookupPixel(int[] src, int[] dst) {
        int[] out = dst == null ? new int[src.length] : dst;
        if (this.data.length == 1) {
            for (int i = 0; i < src.length; i++) {
                out[i] = this.data[0][src[i] - this.getOffset()] & 0xFFFF;
            }
        } else {
            for (int i = 0; i < src.length; i++) {
                out[i] = this.data[i][src[i] - this.getOffset()] & 0xFFFF;
            }
        }
        return out;
    }

    /** Igual que la otra forma, con el tipo propio de esta tabla. */
    public short[] lookupPixel(short[] src, short[] dst) {
        short[] out = dst == null ? new short[src.length] : dst;
        if (this.data.length == 1) {
            for (int i = 0; i < src.length; i++) {
                out[i] = this.data[0][(src[i] & 0xFFFF) - this.getOffset()];
            }
        } else {
            for (int i = 0; i < src.length; i++) {
                out[i] = this.data[i][(src[i] & 0xFFFF) - this.getOffset()];
            }
        }
        return out;
    }
}
