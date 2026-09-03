package java.awt.image;

/**
 * Enteros de 32 bits. Es el tipo de un buffer de pixeles empaquetados
 * --un ARGB entero por pixel-- y por eso el mas comun en imagenes de pantalla.
 */
public final class DataBufferInt extends DataBuffer {

    // Los datos de cada banco. `data` es un atajo al banco 0: se usa en cada lectura
    // y bajar por `bankdata[0]` cada vez seria una indireccion de mas en el camino
    // mas caliente de todo el paquete.
    private int[] data;
    private int[][] bankdata;

    /** Un banco de `size` elementos, en cero. */
    public DataBufferInt(int size) {
        super(DataBuffer.TYPE_INT, size);
        this.data = new int[size];
        this.bankdata = new int[1][];
        this.bankdata[0] = this.data;
    }

    /** `numBanks` bancos de `size` elementos, en cero. */
    public DataBufferInt(int size, int numBanks) {
        super(DataBuffer.TYPE_INT, size, numBanks);
        this.bankdata = new int[numBanks][];
        for (int i = 0; i < numBanks; i++) {
            this.bankdata[i] = new int[size];
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
    public DataBufferInt(int[] dataArray, int size) {
        super(DataBuffer.TYPE_INT, size);
        this.data = dataArray;
        this.bankdata = new int[1][];
        this.bankdata[0] = this.data;
    }

    /** Como el anterior, empezando en `offset`. */
    public DataBufferInt(int[] dataArray, int size, int offset) {
        super(DataBuffer.TYPE_INT, size, 1, offset);
        this.data = dataArray;
        this.bankdata = new int[1][];
        this.bankdata[0] = this.data;
    }

    /** Varios bancos sobre esos arreglos, sin copiarlos. */
    public DataBufferInt(int[][] dataArray, int size) {
        super(DataBuffer.TYPE_INT, size, dataArray.length);
        this.bankdata = dataArray;
        this.data = this.bankdata[0];
    }

    /** Varios bancos, cada uno con su desplazamiento. */
    public DataBufferInt(int[][] dataArray, int size, int[] offsets) {
        super(DataBuffer.TYPE_INT, size, dataArray.length, offsets);
        this.bankdata = dataArray;
        this.data = this.bankdata[0];
    }

    /** El arreglo del banco 0, sin copiar. */
    public int[] getData() {
        return this.data;
    }

    /** El arreglo de ese banco, sin copiar. */
    public int[] getData(int bank) {
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
    public int[][] getBankData() {
        int[][] out = new int[this.bankdata.length][];
        System.arraycopy(this.bankdata, 0, out, 0, this.bankdata.length);
        return out;
    }

    public int getElem(int i) {
        return this.data[i + this.offset];
    }

    public int getElem(int bank, int i) {
        return this.bankdata[bank][i + this.offsets[bank]];
    }

    public void setElem(int i, int val) {
        this.data[i + this.offset] = (int) val;
    }

    public void setElem(int bank, int i, int val) {
        this.bankdata[bank][i + this.offsets[bank]] = (int) val;
    }
}
