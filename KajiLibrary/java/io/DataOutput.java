package java.io;

// KajiLibrary's java.io.DataOutput — the write side of DataInput's portable primitive
// layout. Every writeX here is the exact inverse of the matching readX, which is the whole
// contract: a stream written by one and read by the other round-trips regardless of the
// machines at either end.
//
// writeBytes and writeChars are the two lossy-looking ones, and deliberately so: writeBytes
// throws away the high byte of every char (for protocols known to be ASCII), writeChars
// keeps both bytes but writes no length. Only writeUTF is self-delimiting.
public interface DataOutput {

    void write(int b);

    void write(byte[] b);

    void write(byte[] b, int off, int len);

    void writeBoolean(boolean v);

    void writeByte(int v);

    void writeShort(int v);

    void writeChar(int v);

    void writeInt(int v);

    void writeLong(long v);

    void writeFloat(float v);

    void writeDouble(double v);

    void writeBytes(String s);

    void writeChars(String s);

    void writeUTF(String s);
}
