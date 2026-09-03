package java.awt.image;

/**
 * Como {@link DataBufferByte}, se leen **sin signo**: 0..65535.
 *
 * <p>Usa el mismo `short[]` que {@link DataBufferShort} y se diferencia solo en eso. Confundirlos
 * da una imagen con los tonos claros dados vuelta.
 */
public final class DataBufferUShort extends DataBuffer {

    // Los datos de cada banco. `data` es un atajo al banco 0: se usa en cada lectura
    // y bajar por `bankdata[0]` cada vez seria una indireccion de mas en el camino
    // mas caliente de todo el paquete.
    private short[] data;
    private short[][] bankdata;

    /** Un banco de `size` elementos, en cero. */
    public DataBufferUShort(int size) {
        super(DataBuffer.TYPE_USHORT, size);
        this.data = new short[size];
        this.bankdata = new short[1][];
        this.bankdata[0] = this.data;
    }

    /** `numBanks` bancos de `size` elementos, en cero. */
    public DataBufferUShort(int size, int numBanks) {
        super(DataBuffer.TYPE_USHORT, size, numBanks);
        this.bankdata = new short[numBanks][];
        for (int i = 0; i < numBanks; i++) {
            this.bankdata[i] = new short[size];
        }
        this.data = this.bankdata[0];
    }

    /**
     * Un banco sobre ese arreglo, **sin copiarlo**.
     *
     * <p>El buffer se queda con el arreglo que se le da: escribirle por afuera cambia la
     * imagen. Es a proposito y es lo que permite armar una imagen sobre memoria que ya
     * existe sin duplicarla.
     */
    public DataBufferUShort(short[] dataArray, int size) {
        super(DataBuffer.TYPE_USHORT, size);
        this.data = dataArray;
        this.bankdata = new short[1][];
        this.bankdata[0] = this.data;
    }

    /** Como el anterior, empezando en `offset`. */
    public DataBufferUShort(short[] dataArray, int size, int offset) {
        super(DataBuffer.TYPE_USHORT, size, 1, offset);
        this.data = dataArray;
        this.bankdata = new short[1][];
        this.bankdata[0] = this.data;
    }

    /** Varios bancos sobre esos arreglos, sin copiarlos. */
    public DataBufferUShort(short[][] dataArray, int size) {
        super(DataBuffer.TYPE_USHORT, size, dataArray.length);
        this.bankdata = dataArray;
        this.data = this.bankdata[0];
    }

    /** Varios bancos, cada uno con su desplazamiento. */
    public DataBufferUShort(short[][] dataArray, int size, int[] offsets) {
        super(DataBuffer.TYPE_USHORT, size, dataArray.length, offsets);
        this.bankdata = dataArray;
        this.data = this.bankdata[0];
    }

    /** El arreglo del banco 0, sin copiar. */
    public short[] getData() {
        return this.data;
    }

    /** El arreglo de ese banco, sin copiar. */
    public short[] getData(int bank) {
        return this.bankdata[bank];
    }

    /**
     * Los bancos.
     *
     * <p>El arreglo de afuera es una **copia**; los de adentro no. O sea que agregar o
     * quitar bancos en lo que devuelve no toca al buffer, pero escribir en un banco si.
     * Es asimetrico y es lo que hace el JDK -- comprobado, porque la primera version de
     * esta clase devolvia el arreglo de afuera sin clonar.
     */
    public short[][] getBankData() {
        short[][] out = new short[this.bankdata.length][];
        System.arraycopy(this.bankdata, 0, out, 0, this.bankdata.length);
        return out;
    }

    /** Sin signo: ver la nota de la clase. */
    public int getElem(int i) {
        return this.data[i + this.offset] & 0xFFFF;
    }

    /** Sin signo: ver la nota de la clase. */
    public int getElem(int bank, int i) {
        return this.bankdata[bank][i + this.offsets[bank]] & 0xFFFF;
    }

    public void setElem(int i, int val) {
        this.data[i + this.offset] = (short) val;
    }

    public void setElem(int bank, int i, int val) {
        this.bankdata[bank][i + this.offsets[bank]] = (short) val;
    }
}
