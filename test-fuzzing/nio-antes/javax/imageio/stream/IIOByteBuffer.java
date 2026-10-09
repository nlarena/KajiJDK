package javax.imageio.stream;

/**
 * KajiLibrary's javax.imageio.stream.IIOByteBuffer -- an array, an offset and a length.
 *
 * <p>It exists for a performance reason and not a design one. {@code ImageInputStream.readBytes}
 * hands the caller <b>the stream's internal buffer</b>, without copying: for an image reader that
 * walks megabytes, that copy is measurable.
 *
 * <p>That brings two things to keep in mind:
 *
 * <ul>
 *   <li>the array <b>does not belong to whoever receives it</b>. The stream's next read may
 *       overwrite it. It has to be consumed before reading on, or copied;
 *   <li>the three fields are mutable and written from outside, because the stream fills them in.
 * </ul>
 *
 * <p>It is the only class of the image API that exposes a borrowed buffer, which is why it is worth
 * handling with care instead of keeping it.
 */
public class IIOByteBuffer {

    /** The borrowed array. See the class note. */
    private byte[] data;

    /** From where the data is valid. */
    private int offset;

    /** How many bytes are valid. */
    private int length;

    /**
     * @param data the array; not copied
     * @param offset from where
     * @param length how many
     */
    public IIOByteBuffer(byte[] data, int offset, int length) {
        this.data = data;
        this.offset = offset;
        this.length = length;
    }

    /** The array. See the class note: it may not be yours. */
    public byte[] getData() {
        return this.data;
    }

    /** Changes it. */
    public void setData(byte[] data) {
        this.data = data;
    }

    /** From where the data is valid. */
    public int getOffset() {
        return this.offset;
    }

    /** Changes it. */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /** How many bytes are valid. */
    public int getLength() {
        return this.length;
    }

    /** Changes it. */
    public void setLength(int length) {
        this.length = length;
    }
}
