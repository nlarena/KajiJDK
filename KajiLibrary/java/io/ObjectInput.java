package java.io;

import java.io.DataInput;

// KajiLibrary's java.io.ObjectInput — DataInput plus the ability to read whole objects, and
// the read side of the serialization contract. KajiLibrary implements no ObjectInputStream
// (the engine needs reflective field access and class descriptors we do not have yet), but
// the interface is worth declaring on its own: Externalizable is written against it, so a
// class can define its own serial form here even with no engine to drive it.
public interface ObjectInput extends DataInput, AutoCloseable {

    Object readObject();

    int read();

    int read(byte[] b);

    int read(byte[] b, int off, int len);

    long skip(long n);

    int available();

    void close();
}
