package java.io;

import java.io.DataOutput;

// KajiLibrary's java.io.ObjectOutput — DataOutput plus writeObject, the write side of the
// serialization contract and the counterpart of ObjectInput. It redeclares the byte-level
// write methods it already inherits from DataOutput; that is the JDK's shape, and it keeps
// the interface readable as a standalone stream abstraction rather than a mixin.
public interface ObjectOutput extends DataOutput, AutoCloseable {

    void writeObject(Object obj);

    void write(int b);

    void write(byte[] b);

    void write(byte[] b, int off, int len);

    void flush();

    void close();
}
