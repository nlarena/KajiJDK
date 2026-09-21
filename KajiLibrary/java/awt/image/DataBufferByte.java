package java.awt.image;

/**
 * The elements are read **unsigned**: a byte 0xFF is 255 and not -1.
 *
 * <p>It is the only interesting difference of this class and the easiest to forget. Java has no
 * unsigned byte, so without the masking of `getElem` the light half of an image would come out as
 * negative values.
 */
public final class DataBufferByte extends DataBuffer {

    // The data of each bank. `data` is a shortcut to bank 0: it is used on every read
    // and going down through `bankdata[0]` every time would be one indirection too many
    // on the hottest path of the whole package.
    private byte[] data;
    private byte[][] bankdata;

    /** One bank of `size` elements, at zero. */
    public DataBufferByte(int size) {
        super(DataBuffer.TYPE_BYTE, size);
        this.data = new byte[size];
        this.bankdata = new byte[1][];
        this.bankdata[0] = this.data;
    }

    /** `numBanks` banks of `size` elements, at zero. */
    public DataBufferByte(int size, int numBanks) {
        super(DataBuffer.TYPE_BYTE, size, numBanks);
        this.bankdata = new byte[numBanks][];
        for (int i = 0; i < numBanks; i++) {
            this.bankdata[i] = new byte[size];
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
    public DataBufferByte(byte[] dataArray, int size) {
        super(DataBuffer.TYPE_BYTE, size);
        this.data = dataArray;
        this.bankdata = new byte[1][];
        this.bankdata[0] = this.data;
    }

    /** Like the previous one, starting at `offset`. */
    public DataBufferByte(byte[] dataArray, int size, int offset) {
        super(DataBuffer.TYPE_BYTE, size, 1, offset);
        this.data = dataArray;
        this.bankdata = new byte[1][];
        this.bankdata[0] = this.data;
    }

    /** Several banks over those arrays, without copying them. */
    public DataBufferByte(byte[][] dataArray, int size) {
        super(DataBuffer.TYPE_BYTE, size, dataArray.length);
        this.bankdata = dataArray;
        this.data = this.bankdata[0];
    }

    /** Several banks, each one with its own offset. */
    public DataBufferByte(byte[][] dataArray, int size, int[] offsets) {
        super(DataBuffer.TYPE_BYTE, size, dataArray.length, offsets);
        this.bankdata = dataArray;
        this.data = this.bankdata[0];
    }

    /** The array of bank 0, without copying. */
    public byte[] getData() {
        return this.data;
    }

    /** The array of that bank, without copying. */
    public byte[] getData(int bank) {
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
    public byte[][] getBankData() {
        byte[][] out = new byte[this.bankdata.length][];
        System.arraycopy(this.bankdata, 0, out, 0, this.bankdata.length);
        return out;
    }

    /** Unsigned: see the note of the class. */
    public int getElem(int i) {
        return this.data[i + this.offset] & 0xFF;
    }

    /** Unsigned: see the note of the class. */
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
