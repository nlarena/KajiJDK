package java.awt.image;

/**
 * **Signed** 16-bit integers: -32768..32767.
 *
 * <p>Compare with {@link DataBufferUShort}, which uses the same memory and reads it unsigned.
 */
public final class DataBufferShort extends DataBuffer {

    // The data of each bank. `data` is a shortcut to bank 0: it is used on every read
    // and going down through `bankdata[0]` every time would be one indirection too many
    // on the hottest path of the whole package.
    private short[] data;
    private short[][] bankdata;

    /** One bank of `size` elements, at zero. */
    public DataBufferShort(int size) {
        super(DataBuffer.TYPE_SHORT, size);
        this.data = new short[size];
        this.bankdata = new short[1][];
        this.bankdata[0] = this.data;
    }

    /** `numBanks` banks of `size` elements, at zero. */
    public DataBufferShort(int size, int numBanks) {
        super(DataBuffer.TYPE_SHORT, size, numBanks);
        this.bankdata = new short[numBanks][];
        for (int i = 0; i < numBanks; i++) {
            this.bankdata[i] = new short[size];
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
    public DataBufferShort(short[] dataArray, int size) {
        super(DataBuffer.TYPE_SHORT, size);
        this.data = dataArray;
        this.bankdata = new short[1][];
        this.bankdata[0] = this.data;
    }

    /** Like the previous one, starting at `offset`. */
    public DataBufferShort(short[] dataArray, int size, int offset) {
        super(DataBuffer.TYPE_SHORT, size, 1, offset);
        this.data = dataArray;
        this.bankdata = new short[1][];
        this.bankdata[0] = this.data;
    }

    /** Several banks over those arrays, without copying them. */
    public DataBufferShort(short[][] dataArray, int size) {
        super(DataBuffer.TYPE_SHORT, size, dataArray.length);
        this.bankdata = dataArray;
        this.data = this.bankdata[0];
    }

    /** Several banks, each one with its own offset. */
    public DataBufferShort(short[][] dataArray, int size, int[] offsets) {
        super(DataBuffer.TYPE_SHORT, size, dataArray.length, offsets);
        this.bankdata = dataArray;
        this.data = this.bankdata[0];
    }

    /** The array of bank 0, without copying. */
    public short[] getData() {
        return this.data;
    }

    /** The array of that bank, without copying. */
    public short[] getData(int bank) {
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
    public short[][] getBankData() {
        short[][] out = new short[this.bankdata.length][];
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
        this.data[i + this.offset] = (short) val;
    }

    public void setElem(int bank, int i, int val) {
        this.bankdata[bank][i + this.offsets[bank]] = (short) val;
    }
}
