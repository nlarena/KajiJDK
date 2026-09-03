package java.awt.image;

/**
 * Coma flotante de 32 bits.
 *
 * <p>Redefine la familia `Float`/`Double` en vez de la de `int`, que es al reves de las subclases
 * enteras. `getElem` sigue funcionando y **trunca**, que es lo que dice el contrato.
 */
public final class DataBufferFloat extends DataBuffer {

    // Los datos de cada banco. `data` es un atajo al banco 0: se usa en cada lectura
    // y bajar por `bankdata[0]` cada vez seria una indireccion de mas en el camino
    // mas caliente de todo el paquete.
    private float[] data;
    private float[][] bankdata;

    /** Un banco de `size` elementos, en cero. */
    public DataBufferFloat(int size) {
        super(DataBuffer.TYPE_FLOAT, size);
        this.data = new float[size];
        this.bankdata = new float[1][];
        this.bankdata[0] = this.data;
    }

    /** `numBanks` bancos de `size` elementos, en cero. */
    public DataBufferFloat(int size, int numBanks) {
        super(DataBuffer.TYPE_FLOAT, size, numBanks);
        this.bankdata = new float[numBanks][];
        for (int i = 0; i < numBanks; i++) {
            this.bankdata[i] = new float[size];
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
    public DataBufferFloat(float[] dataArray, int size) {
        super(DataBuffer.TYPE_FLOAT, size);
        this.data = dataArray;
        this.bankdata = new float[1][];
        this.bankdata[0] = this.data;
    }

    /** Como el anterior, empezando en `offset`. */
    public DataBufferFloat(float[] dataArray, int size, int offset) {
        super(DataBuffer.TYPE_FLOAT, size, 1, offset);
        this.data = dataArray;
        this.bankdata = new float[1][];
        this.bankdata[0] = this.data;
    }

    /** Varios bancos sobre esos arreglos, sin copiarlos. */
    public DataBufferFloat(float[][] dataArray, int size) {
        super(DataBuffer.TYPE_FLOAT, size, dataArray.length);
        this.bankdata = dataArray;
        this.data = this.bankdata[0];
    }

    /** Varios bancos, cada uno con su desplazamiento. */
    public DataBufferFloat(float[][] dataArray, int size, int[] offsets) {
        super(DataBuffer.TYPE_FLOAT, size, dataArray.length, offsets);
        this.bankdata = dataArray;
        this.data = this.bankdata[0];
    }

    /** El arreglo del banco 0, sin copiar. */
    public float[] getData() {
        return this.data;
    }

    /** El arreglo de ese banco, sin copiar. */
    public float[] getData(int bank) {
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
    public float[][] getBankData() {
        float[][] out = new float[this.bankdata.length][];
        System.arraycopy(this.bankdata, 0, out, 0, this.bankdata.length);
        return out;
    }

    /** Trunca: ver la nota de la clase. */
    public int getElem(int i) {
        return (int) this.data[i + this.offset];
    }

    /** Trunca: ver la nota de la clase. */
    public int getElem(int bank, int i) {
        return (int) this.bankdata[bank][i + this.offsets[bank]];
    }

    public void setElem(int i, int val) {
        this.data[i + this.offset] = (float) val;
    }

    public void setElem(int bank, int i, int val) {
        this.bankdata[bank][i + this.offsets[bank]] = (float) val;
    }

    public float getElemFloat(int i) {
        return this.data[i + this.offset];
    }

    public float getElemFloat(int bank, int i) {
        return this.bankdata[bank][i + this.offsets[bank]];
    }

    public void setElemFloat(int i, float val) {
        this.data[i + this.offset] = (float) val;
    }

    public void setElemFloat(int bank, int i, float val) {
        this.bankdata[bank][i + this.offsets[bank]] = (float) val;
    }

    public double getElemDouble(int i) {
        return (double) this.data[i + this.offset];
    }

    public double getElemDouble(int bank, int i) {
        return (double) this.bankdata[bank][i + this.offsets[bank]];
    }

    public void setElemDouble(int i, double val) {
        this.data[i + this.offset] = (float) val;
    }

    public void setElemDouble(int bank, int i, double val) {
        this.bankdata[bank][i + this.offsets[bank]] = (float) val;
    }
}
