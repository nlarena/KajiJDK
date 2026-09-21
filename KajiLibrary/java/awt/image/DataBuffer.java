package java.awt.image;

/**
 * The raw numbers of an image, with no interpretation at all.
 *
 * <p>It is the lowest layer of `java.awt.image` and the easiest to misunderstand, so it is worth
 * saying what it does **not** know: it knows nothing about pixels, or width, or colour. It is an
 * array of numbers and nothing more. Who a pixel is, is said by a {@link SampleModel}; what colour
 * it is, by a {@link ColorModel}. Keeping the three things apart is what lets the same memory be
 * read as greyscale or as RGB without copying it.
 *
 * <h2>The banks</h2>
 *
 * <p>A buffer can have several **banks**, which are independent arrays. They serve for keeping each
 * component separately --all the reds together, all the greens together-- instead of interleaved.
 * The methods that do not say a bank work on bank 0.
 *
 * <p>Each bank has an initial offset of its own: {@link #getOffsets} returns them all and
 * {@link #getOffset} the one of bank 0. A buffer of a single bank has a single offset, and that is
 * why the two methods look redundant until there is more than one.
 *
 * <h2>The six types and their ranges</h2>
 *
 * <p>{@link #TYPE_USHORT} and {@link #TYPE_SHORT} both use a `short[]` and differ only in how it is
 * read: the first as 0..65535 and the second as -32768..32767. It is the same memory with two
 * interpretations, and confusing them gives images with the light tones turned round.
 *
 * <h2>The three pairs of accessors</h2>
 *
 * <p>`getElem`, `getElemFloat` and `getElemDouble` read **the same datum** with a different
 * conversion. The integer subclasses override only the first and inherit the other two, which
 * convert; the floating-point ones do it the other way round. That is why `getElemFloat` on a
 * `DataBufferInt` loses nothing --an `int` fits in a `float` with loss only above 2^24-- and
 * `getElem` on a `DataBufferDouble` **does** truncate. It is in the contract and it is not a
 * defect.
 */
public abstract class DataBuffer {

    /** Unsigned 8-bit integers, kept in a `byte[]`. */
    public static final int TYPE_BYTE = 0;
    /** **Unsigned** 16-bit integers, in a `short[]`. */
    public static final int TYPE_USHORT = 1;
    /** **Signed** 16-bit integers, in a `short[]`. */
    public static final int TYPE_SHORT = 2;
    /** Signed 32-bit integers. */
    public static final int TYPE_INT = 3;
    /** 32-bit floating point. */
    public static final int TYPE_FLOAT = 4;
    /** 64-bit floating point. */
    public static final int TYPE_DOUBLE = 5;
    /** None of the above. */
    public static final int TYPE_UNDEFINED = 32;

    /** The type of the data: one of the `TYPE_` constants. */
    protected int dataType;

    /** How many banks it has. */
    protected int banks;

    /** The offset of bank 0. */
    protected int offset;

    /** How many elements each bank uses, starting from its offset. */
    protected int size;

    /** The offset of each bank. */
    protected int[] offsets;

    /**
     * How many bits an element of that type takes.
     *
     * @throws IllegalArgumentException if the type is not one of the six
     */
    public static int getDataTypeSize(int type) {
        if (type < TYPE_BYTE || type > TYPE_DOUBLE) {
            throw new IllegalArgumentException("Unknown data type " + type);
        }
        int[] sizes = { 8, 16, 16, 32, 32, 64 };
        return sizes[type];
    }

    /** One bank, with no offset. */
    protected DataBuffer(int dataType, int size) {
        this(dataType, size, 1, 0);
    }

    /** `numBanks` banks, with no offset. */
    protected DataBuffer(int dataType, int size, int numBanks) {
        this(dataType, size, numBanks, 0);
    }

    /** `numBanks` banks, all with the same offset. */
    protected DataBuffer(int dataType, int size, int numBanks, int offset) {
        this.dataType = dataType;
        this.size = size;
        this.banks = numBanks;
        this.offset = offset;
        this.offsets = new int[numBanks];
        for (int i = 0; i < numBanks; i++) {
            this.offsets[i] = offset;
        }
    }

    /**
     * `numBanks` banks, each one with its own offset.
     *
     * @throws ArrayIndexOutOfBoundsException if there are fewer offsets than banks
     */
    protected DataBuffer(int dataType, int size, int numBanks, int[] offsets) {
        if (offsets.length < numBanks) {
            throw new ArrayIndexOutOfBoundsException("Number of banks does not match number of "
                    + "band offsets");
        }
        this.dataType = dataType;
        this.size = size;
        this.banks = numBanks;
        this.offsets = new int[numBanks];
        for (int i = 0; i < numBanks; i++) {
            this.offsets[i] = offsets[i];
        }
        this.offset = offsets[0];
    }

    /** The type of the data. */
    public int getDataType() {
        return this.dataType;
    }

    /** How many elements each bank uses. */
    public int getSize() {
        return this.size;
    }

    /** The offset of bank 0. */
    public int getOffset() {
        return this.offset;
    }

    /** A copy of the offsets of every bank. */
    public int[] getOffsets() {
        int[] out = new int[this.offsets.length];
        System.arraycopy(this.offsets, 0, out, 0, this.offsets.length);
        return out;
    }

    /** How many banks. */
    public int getNumBanks() {
        return this.banks;
    }

    /** Element `i` of bank 0. */
    public int getElem(int i) {
        return this.getElem(0, i);
    }

    /** Element `i` of the given bank. */
    public abstract int getElem(int bank, int i);

    /** Writes element `i` of bank 0. */
    public void setElem(int i, int val) {
        this.setElem(0, i, val);
    }

    /** Writes element `i` of the given bank. */
    public abstract void setElem(int bank, int i, int val);

    /** Element `i` of bank 0, as a `float`. */
    public float getElemFloat(int i) {
        return this.getElem(i);
    }

    /** Element `i` of the given bank, as a `float`. */
    public float getElemFloat(int bank, int i) {
        return this.getElem(bank, i);
    }

    /** Writes a `float` into bank 0. In an integer buffer it is truncated. */
    public void setElemFloat(int i, float val) {
        this.setElem(i, (int) val);
    }

    /** Writes a `float` into the given bank. In an integer buffer it is truncated. */
    public void setElemFloat(int bank, int i, float val) {
        this.setElem(bank, i, (int) val);
    }

    /** Element `i` of bank 0, as a `double`. */
    public double getElemDouble(int i) {
        return this.getElem(i);
    }

    /** Element `i` of the given bank, as a `double`. */
    public double getElemDouble(int bank, int i) {
        return this.getElem(bank, i);
    }

    /** Writes a `double` into bank 0. In an integer buffer it is truncated. */
    public void setElemDouble(int i, double val) {
        this.setElem(i, (int) val);
    }

    /** Writes a `double` into the given bank. In an integer buffer it is truncated. */
    public void setElemDouble(int bank, int i, double val) {
        this.setElem(bank, i, (int) val);
    }
}
