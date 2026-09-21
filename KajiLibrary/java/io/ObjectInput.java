package java.io;

import java.io.DataInput;

// KajiLibrary's java.io.ObjectInput -- `DataInput` plus the ability to read whole objects: the
// reading side of the serialization contract.
//
// Its implementor is `ObjectInputStream`, which **is here**. The interface is declared apart all
// the same and not dissolved into it: `Externalizable.readExternal` receives an `ObjectInput` and
// not a concrete stream, which is what lets a class define its serialized form without tying itself
// to whoever reads it.
public interface ObjectInput extends DataInput, AutoCloseable {

    Object readObject() throws ClassNotFoundException, IOException;

    int read() throws IOException;

    int read(byte[] b) throws IOException;

    int read(byte[] b, int off, int len) throws IOException;

    long skip(long n) throws IOException;

    int available() throws IOException;

    void close() throws IOException;
}
