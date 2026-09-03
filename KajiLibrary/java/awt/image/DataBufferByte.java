package java.awt.image;

/**
 * Los elementos se leen **sin signo**: un byte 0xFF vale 255 y no -1.
 *
 * <p>Es la unica diferencia interesante de esta clase y la mas facil de olvidar. Java no tiene byte
 * sin signo, asi que sin el enmascarado de `getElem` la mitad clara de una imagen saldria como
 * valores negativos.
 */
public final class DataBufferByte extends DataBuffer {

    // Los datos de cada banco. `data` es un atajo al banco 0: se usa en cada lectura
    // y bajar por `bankdata[0]` cada vez seria una indireccion de mas en el camino
    // mas caliente de todo el paquete.
    private byte[] data;
    private byte[][] bankdata;

    /** Un banco de `size` elementos, en cero. */
    public DataBufferByte(int size) {
        super(DataBuffer.TYPE_BYTE, size);
        this.data = new byte[size];
        this.bankdata = new byte[1][];
        this.bankdata[0] = this.data;
    }

    /** `numBanks` bancos de `size` elementos, en cero. */
    public DataBufferByte(int size, int numBanks) {
        super(DataBuffer.TYPE_BYTE, size, numBanks);
        this.bankdata = new byte[numBanks][];
        for (int i = 0; i < numBanks; i++) {
            this.bankdata[i] = new byte[size];
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
    public DataBufferByte(byte[] dataArray, int size) {
        super(DataBuffer.TYPE_BYTE, size);
        this.data = dataArray;
        this.bankdata = new byte[1][];
        this.bankdata[0] = this.data;
    }

    /** Como el anterior, empezando en `offset`. */
    public DataBufferByte(byte[] dataArray, int size, int offset) {
        super(DataBuffer.TYPE_BYTE, size, 1, offset);
        this.data = dataArray;
        this.bankdata = new byte[1][];
        this.bankdata[0] = this.data;
    }

    /** Varios bancos sobre esos arreglos, sin copiarlos. */
    public DataBufferByte(byte[][] dataArray, int size) {
        super(DataBuffer.TYPE_BYTE, size, dataArray.length);
        this.bankdata = dataArray;
        this.data = this.bankdata[0];
    }

    /** Varios bancos, cada uno con su desplazamiento. */
    public DataBufferByte(byte[][] dataArray, int size, int[] offsets) {
        super(DataBuffer.TYPE_BYTE, size, dataArray.length, offsets);
        this.bankdata = dataArray;
        this.data = this.bankdata[0];
    }

    /** El arreglo del banco 0, sin copiar. */
    public byte[] getData() {
        return this.data;
    }

    /** El arreglo de ese banco, sin copiar. */
    public byte[] getData(int bank) {
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
    public byte[][] getBankData() {
        byte[][] out = new byte[this.bankdata.length][];
        System.arraycopy(this.bankdata, 0, out, 0, this.bankdata.length);
        return out;
    }

    /** Sin signo: ver la nota de la clase. */
    public int getElem(int i) {
        return this.data[i + this.offset] & 0xFF;
    }

    /** Sin signo: ver la nota de la clase. */
    public int getElem(int bank, int i) {
        return this.bankdata[bank][i + this.offsets[bank]] & 0xFF;
    }

    public void setElem(int i, int val) {
        this.data[i + this.offset] = (byte) val;
    }

    public void setElem(int bank, int i, int val) {
        this.bankdata[bank][i + this.offsets[bank]] = (byte) val;
    }
}
