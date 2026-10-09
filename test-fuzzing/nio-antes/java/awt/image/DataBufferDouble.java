package java.awt.image;

/**
 * 64-bit floating point. The same note as {@link DataBufferFloat} applies
 * about which family of accessors it overrides.
 */
public final class DataBufferDouble extends DataBuffer {

    // The data of each bank. `data` is a shortcut to bank 0: it is used on every read
    // and going down through `bankdata[0]` every time would be one indirection too many
    // on the hottest path of the whole package.
    private double[] data;
    private double[][] bankdata;

    /** One bank of `size` elements, at zero. */
    public DataBufferDouble(int size) {
        super(DataBuffer.TYPE_DOUBLE, size);
        this.data = new double[size];
        this.bankdata = new double[1][];
        this.bankdata[0] = this.data;
    }

    /** `numBanks` banks of `size` elements, at zero. */
    public DataBufferDouble(int size, int numBanks) {
        super(DataBuffer.TYPE_DOUBLE, size, numBanks);
        this.bankdata = new double[numBanks][];
        for (int i = 0; i < numBanks; i++) {
            this.bankdata[i] = new double[size];
        }
        this.data = this.bankdata[0];
    }

    /**
     * One bank over that array, **without copying it**.
     *
     * <p>The buffer keeps the array it is given: writing to it from outside changes the
     * image. It is on purpose and it is what makes it possible to build an image over
     * memory that already exists without duplicating it.
     */
    public DataBufferDouble(double[] dataArray, int size) {
        super(DataBuffer.TYPE_DOUBLE, size);
        this.data = dataArray;
        this.bankdata = new double[1][];
        this.bankdata[0] = this.data;
    }

    /** Like the previous one, starting at `offset`. */
    public DataBufferDouble(double[] dataArray, int size, int offset) {
        super(DataBuffer.TYPE_DOUBLE, size, 1, offset);
        this.data = dataArray;
        this.bankdata = new double[1][];
        this.bankdata[0] = this.data;
    }

    /** Several banks over those arrays, without copying them. */
    public DataBufferDouble(double[][] dataArray, int size) {
        super(DataBuffer.TYPE_DOUBLE, size, dataArray.length);
        this.bankdata = dataArray;
        this.data = this.bankdata[0];
    }

    /** Several banks, each one with its own offset. */
    public DataBufferDouble(double[][] dataArray, int size, int[] offsets) {
        super(DataBuffer.TYPE_DOUBLE, size, dataArray.length, offsets);
        this.bankdata = dataArray;
        this.data = this.bankdata[0];
    }

    /** The array of bank 0, without copying. */
    public double[] getData() {
        return this.data;
    }

    /** The array of that bank, without copying. */
    public double[] getData(int bank) {
        return this.bankdata[bank];
    }

    /**
     * The banks.
     *
     * <p>The outer array is a **copy**; the inner ones are not. That is, adding or
     * removing banks in what it returns does not touch the buffer, but writing into a bank
     * does. It is asymmetric and it is what the JDK does -- checked, because the first
     * version of this class returned the outer array without cloning it.
     */
    public double[][] getBankData() {
        double[][] out = new double[this.bankdata.length][];
        System.arraycopy(this.bankdata, 0, out, 0, this.bankdata.length);
        return out;
    }

    /** It truncates: see the note of the class. */
    public int getElem(int i) {
        return (int) this.data[i + this.offset];
    }

    /** It truncates: see the note of the class. */
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
