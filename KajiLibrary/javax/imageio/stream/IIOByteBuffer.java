package javax.imageio.stream;

/**
 * KajiLibrary's javax.imageio.stream.IIOByteBuffer -- un arreglo, un desplazamiento y un largo.
 *
 * <p>Existe por una razon de rendimiento y no de diseno. {@code ImageInputStream.readBytes} le
 * entrega al que llama <b>el bufer interno del flujo</b>, sin copiar: para un lector de imagenes que
 * recorre megabytes, esa copia es medible.
 *
 * <p>Eso trae dos cosas que hay que tener presentes:
 *
 * <ul>
 *   <li>el arreglo <b>no es de quien lo recibe</b>. La proxima lectura del flujo lo puede pisar. Hay
 *       que consumirlo antes de seguir leyendo, o copiarlo;
 *   <li>los tres campos son mutables y se escriben desde afuera, porque el flujo los rellena.
 * </ul>
 *
 * <p>Es la unica clase de la API de imagenes que expone un bufer prestado, y por eso conviene tratarla
 * con cuidado en lugar de guardarla.
 */
public class IIOByteBuffer {

    /** El arreglo prestado. Ver la nota de la clase. */
    private byte[] data;

    /** Desde donde valen los datos. */
    private int offset;

    /** Cuantos bytes valen. */
    private int length;

    /**
     * @param data el arreglo; no se copia
     * @param offset desde donde
     * @param length cuantos
     */
    public IIOByteBuffer(byte[] data, int offset, int length) {
        this.data = data;
        this.offset = offset;
        this.length = length;
    }

    /** El arreglo. Ver la nota de la clase: puede no ser tuyo. */
    public byte[] getData() {
        return this.data;
    }

    /** Lo cambia. */
    public void setData(byte[] data) {
        this.data = data;
    }

    /** Desde donde valen los datos. */
    public int getOffset() {
        return this.offset;
    }

    /** Lo cambia. */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /** Cuantos bytes valen. */
    public int getLength() {
        return this.length;
    }

    /** Lo cambia. */
    public void setLength(int length) {
        this.length = length;
    }
}
