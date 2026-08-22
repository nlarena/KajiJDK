package java.io;

// KajiLibrary's java.io.DataInput — reading Java's primitive types out of a byte stream in a
// fixed, portable layout: big-endian, two's complement, IEEE 754, and "modified UTF-8" for
// strings. The point of pinning the layout down in an interface is that the same bytes mean
// the same thing on every machine, which is why the class file format itself is defined in
// these terms.
//
// Note the end-of-input contract differs from InputStream's: these methods have no spare
// value to return for "no more data" (every int is a legal int), so they throw EOFException.
public interface DataInput {

    void readFully(byte[] b);

    void readFully(byte[] b, int off, int len);

    int skipBytes(int n);

    boolean readBoolean();

    byte readByte();

    int readUnsignedByte();

    short readShort();

    int readUnsignedShort();

    char readChar();

    int readInt();

    long readLong();

    float readFloat();

    double readDouble();

    String readLine();

    String readUTF();
}
