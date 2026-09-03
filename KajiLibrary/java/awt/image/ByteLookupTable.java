package java.awt.image;

/**
 * Una {@link LookupTable} de bytes.
 *
 * <p>Los valores se leen **sin signo**, como en {@link DataBufferByte}: una entrada 0xFF vale 255.
 * Sin eso, la mitad clara de la tabla daria valores negativos.
 */
public class ByteLookupTable extends LookupTable {

    private final byte[][] data;

    /**
     * Una tabla por componente.
     *
     * <p>Los arreglos **no se copian**: la tabla se queda con los que se le dan. Es lo
     * que hace el JDK y lo que permite compartir una tabla grande entre varios filtros
     * sin duplicarla.
     *
     * @throws IllegalArgumentException si el desplazamiento es negativo
     */
    public ByteLookupTable(int offset, byte[][] data) {
        super(offset, data.length);
        this.data = new byte[data.length][];
        for (int i = 0; i < data.length; i++) {
            this.data[i] = data[i];
        }
    }

    /** Una sola tabla, que se aplica a todos los componentes. */
    public ByteLookupTable(int offset, byte[] data) {
        super(offset, 1);
        this.data = new byte[1][];
        this.data[0] = data;
    }

    /** Las tablas, sin copiar. */
    public final byte[][] getTable() {
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
                out[i] = this.data[0][src[i] - this.getOffset()] & 0xFF;
            }
        } else {
            for (int i = 0; i < src.length; i++) {
                out[i] = this.data[i][src[i] - this.getOffset()] & 0xFF;
            }
        }
        return out;
    }

    /** Igual que la otra forma, con el tipo propio de esta tabla. */
    public byte[] lookupPixel(byte[] src, byte[] dst) {
        byte[] out = dst == null ? new byte[src.length] : dst;
        if (this.data.length == 1) {
            for (int i = 0; i < src.length; i++) {
                out[i] = this.data[0][(src[i] & 0xFF) - this.getOffset()];
            }
        } else {
            for (int i = 0; i < src.length; i++) {
                out[i] = this.data[i][(src[i] & 0xFF) - this.getOffset()];
            }
        }
        return out;
    }
}
