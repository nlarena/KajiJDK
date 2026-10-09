package java.io;

import java.io.IOException;

// KajiLibrary's java.io.DataOutput — the write side of DataInput's portable primitive
// layout. Every writeX here is the exact inverse of the matching readX, which is the whole
// contract: a stream written by one and read by the other round-trips regardless of the
// machines at either end.
//
// writeBytes and writeChars are the two lossy-looking ones, and deliberately so: writeBytes
// throws away the high byte of every char (for protocols known to be ASCII), writeChars
// keeps both bytes but writes no length. Only writeUTF is self-delimiting.
public interface DataOutput {

    void write(int b) throws IOException;

    void write(byte[] b) throws IOException;

    void write(byte[] b, int off, int len) throws IOException;

    void writeBoolean(boolean v) throws IOException;

    void writeByte(int v) throws IOException;

    void writeShort(int v) throws IOException;

    void writeChar(int v) throws IOException;

    void writeInt(int v) throws IOException;

    void writeLong(long v) throws IOException;

    void writeFloat(float v) throws IOException;

    void writeDouble(double v) throws IOException;

    void writeBytes(String s) throws IOException;

    void writeChars(String s) throws IOException;

    void writeUTF(String s) throws IOException;
}
