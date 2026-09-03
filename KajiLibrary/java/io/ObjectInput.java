package java.io;

import java.io.DataInput;

// KajiLibrary's java.io.ObjectInput -- `DataInput` mas la capacidad de leer objetos enteros: el
// lado que lee del contrato de serializacion.
//
// Su implementador es `ObjectInputStream`, que **ya esta**. La interfaz igual se declara aparte y no
// se disuelve en el: `Externalizable.readExternal` recibe un `ObjectInput` y no un flujo concreto,
// que es lo que le permite a una clase definir su forma serializada sin atarse a quien la lee.
public interface ObjectInput extends DataInput, AutoCloseable {

    Object readObject() throws ClassNotFoundException, IOException;

    int read() throws IOException;

    int read(byte[] b) throws IOException;

    int read(byte[] b, int off, int len) throws IOException;

    long skip(long n) throws IOException;

    int available() throws IOException;

    void close() throws IOException;
}
