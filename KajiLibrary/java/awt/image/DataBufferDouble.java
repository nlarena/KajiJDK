package java.awt.image;

/**
 * Coma flotante de 64 bits. Vale la misma nota que
 * {@link DataBufferFloat} sobre que familia de accesores redefine.
 */
public final class DataBufferDouble extends DataBuffer {

    // Los datos de cada banco. `data` es un atajo al banco 0: se usa en cada lectura
    // y bajar por `bankdata[0]` cada vez seria una indireccion de mas en el camino
    // mas caliente de todo el paquete.
    private double[] data;
    private double[][] bankdata;

    /** Un banco de `size` elementos, en cero. */
    public DataBufferDouble(int size) {
        super(DataBuffer.TYPE_DOUBLE, size);
        this.data = new double[size];
        this.bankdata = new double[1][];
        this.bankdata[0] = this.data;
    }

    /** `numBanks` bancos de `size` elementos, en cero. */
    public DataBufferDouble(int size, int numBanks) {
        super(DataBuffer.TYPE_DOUBLE, size, numBanks);
        this.bankdata = new double[numBanks][];
        for (int i = 0; i < numBanks; i++) {
            this.bankdata[i] = new double[size];
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
    public DataBufferDouble(double[] dataArray, int size) {
        super(DataBuffer.TYPE_DOUBLE, size);
        this.data = dataArray;
        this.bankdata = new double[1][];
        this.bankdata[0] = this.data;
    }

    /** Como el anterior, empezando en `offset`. */
    public DataBufferDouble(double[] dataArray, int size, int offset) {
        super(DataBuffer.TYPE_DOUBLE, size, 1, offset);
        this.data = dataArray;
        this.bankdata = new double[1][];
        this.bankdata[0] = this.data;
    }

    /** Varios bancos sobre esos arreglos, sin copiarlos. */
    public DataBufferDouble(double[][] dataArray, int size) {
        super(DataBuffer.TYPE_DOUBLE, size, dataArray.length);
        this.bankdata = dataArray;
        this.data = this.bankdata[0];
    }

    /** Varios bancos, cada uno con su desplazamiento. */
    public DataBufferDouble(double[][] dataArray, int size, int[] offsets) {
        super(DataBuffer.TYPE_DOUBLE, size, dataArray.length, offsets);
        this.bankdata = dataArray;
        this.data = this.bankdata[0];
    }

    /** El arreglo del banco 0, sin copiar. */
    public double[] getData() {
        return this.data;
    }

    /** El arreglo de ese banco, sin copiar. */
    public double[] getData(int bank) {
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
    public double[][] getBankData() {
        double[][] out = new double[this.bankdata.length][];
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
        this.data[i + this.offset] = (double) val;
    }

    public void setElem(int bank, int i, int val) {
        this.bankdata[bank][i + this.offsets[bank]] = (double) val;
    }

    public float getElemFloat(int i) {
        return (float) this.data[i + this.offset];
    }

    public float getElemFloat(int bank, int i) {
        return (float) this.bankdata[bank][i + this.offsets[bank]];
    }

    public void setElemFloat(int i, float val) {
        this.data[i + this.offset] = (double) val;
    }

    public void setElemFloat(int bank, int i, float val) {
        this.bankdata[bank][i + this.offsets[bank]] = (double) val;
    }

    public double getElemDouble(int i) {
        return this.data[i + this.offset];
    }

    public double getElemDouble(int bank, int i) {
        return this.bankdata[bank][i + this.offsets[bank]];
    }

    public void setElemDouble(int i, double val) {
        this.data[i + this.offset] = (double) val;
    }

    public void setElemDouble(int bank, int i, double val) {
        this.bankdata[bank][i + this.offsets[bank]] = (double) val;
    }
}
