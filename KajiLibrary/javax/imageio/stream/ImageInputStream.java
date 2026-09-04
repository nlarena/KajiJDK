package javax.imageio.stream;

import java.io.Closeable;
import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * KajiLibrary's javax.imageio.stream.ImageInputStream -- un flujo de lectura pensado para formatos de
 * imagen.
 *
 * <p>Es un {@link DataInput} con tres cosas que {@code DataInputStream} no tiene, y cada una responde
 * a un problema real de leer imagenes:
 *
 * <ul>
 *   <li><b>orden de bytes configurable</b>. TIFF viene en los dos ordenes y lo dice en su encabezado;
 *       BMP es chico primero, PNG grande primero. Con {@code DataInput} habria que dar vuelta cada
 *       valor a mano;
 *   <li><b>posicionamiento</b>. {@link #seek} y {@link #getStreamPosition} permiten saltar a un
 *       desplazamiento que el propio archivo indica -- que es como esta hecho TIFF entero;
 *   <li><b>lectura por bits</b>. {@link #readBit} y {@link #readBits} leen campos que no estan
 *       alineados a byte, que es lo normal en los formatos comprimidos.
 * </ul>
 *
 * <h2>La posicion de descarte</h2>
 *
 * <p>{@link #flushBefore} promete que no se va a volver antes de esa posicion, y con eso el flujo
 * puede tirar lo que tenia guardado. Es lo que permite leer una imagen enorme desde un socket sin
 * juntarla entera en memoria.
 *
 * <p>La contracara: despues de eso, {@link #seek} a una posicion anterior lanza
 * {@link IndexOutOfBoundsException}. No es un error del flujo sino de quien prometio que no iba a
 * volver.
 *
 * <h2>{@link #mark} se apila</h2>
 *
 * <p>A diferencia del de {@code InputStream}, este es una <b>pila</b>: dos {@code mark} seguidos y dos
 * {@code reset} vuelven a la segunda marca y despues a la primera. Y no lleva limite de lectura, porque
 * el flujo se puede posicionar.
 *
 * <h2>El desplazamiento de bit se limpia solo</h2>
 *
 * <p>Cualquier lectura de byte o mayor pone {@link #getBitOffset} en cero. Es lo que hace que se pueda
 * alternar entre campos de bits y campos de bytes sin llevar la cuenta a mano.
 */
public interface ImageInputStream extends DataInput, Closeable {

    /**
     * Con que orden de bytes leer los valores de mas de un byte. Ver la nota de la clase.
     *
     * <p>No afecta a {@link #readUTF}, que siempre lee en orden de red.
     */
    void setByteOrder(ByteOrder byteOrder);

    /** Cual esta puesto. */
    ByteOrder getByteOrder();

    /**
     * Un byte, de 0 a 255, o -1 al final.
     *
     * @throws IOException si fallo la lectura
     */
    int read() throws IOException;

    /**
     * Hasta llenar el arreglo.
     *
     * @return cuantos se leyeron, o -1 al final
     * @throws IOException si fallo la lectura
     */
    int read(byte[] b) throws IOException;

    /**
     * Hasta {@code len} bytes.
     *
     * @return cuantos se leyeron, o -1 al final
     * @throws IOException si fallo la lectura
     */
    int read(byte[] b, int off, int len) throws IOException;

    /**
     * Hasta {@code len} bytes, <b>sin copiar</b>. Ver {@link IIOByteBuffer}.
     *
     * @throws IOException si fallo la lectura
     */
    void readBytes(IIOByteBuffer buf, int len) throws IOException;

    /**
     * Un byte como booleano.
     *
     * @throws java.io.EOFException si no hay mas
     */
    boolean readBoolean() throws IOException;

    /**
     * Un byte con signo.
     *
     * @throws java.io.EOFException si no hay mas
     */
    byte readByte() throws IOException;

    /**
     * Un byte sin signo.
     *
     * @throws java.io.EOFException si no hay mas
     */
    int readUnsignedByte() throws IOException;

    /**
     * Dos bytes con signo, en el orden configurado.
     *
     * @throws java.io.EOFException si no hay suficientes
     */
    short readShort() throws IOException;

    /**
     * Dos bytes sin signo.
     *
     * @throws java.io.EOFException si no hay suficientes
     */
    int readUnsignedShort() throws IOException;

    /**
     * Dos bytes como caracter.
     *
     * @throws java.io.EOFException si no hay suficientes
     */
    char readChar() throws IOException;

    /**
     * Cuatro bytes con signo.
     *
     * @throws java.io.EOFException si no hay suficientes
     */
    int readInt() throws IOException;

    /**
     * Cuatro bytes sin signo, como {@code long}.
     *
     * <p>Devuelve {@code long} porque un entero de 32 bits sin signo no entra en un {@code int}. Es un
     * tipo que los formatos de imagen usan todo el tiempo.
     *
     * @throws java.io.EOFException si no hay suficientes
     */
    long readUnsignedInt() throws IOException;

    /**
     * Ocho bytes.
     *
     * @throws java.io.EOFException si no hay suficientes
     */
    long readLong() throws IOException;

    /**
     * Cuatro bytes como coma flotante.
     *
     * @throws java.io.EOFException si no hay suficientes
     */
    float readFloat() throws IOException;

    /**
     * Ocho bytes como coma flotante.
     *
     * @throws java.io.EOFException si no hay suficientes
     */
    double readDouble() throws IOException;

    /**
     * Una linea de texto, un byte por caracter.
     *
     * <p>Hereda el problema de {@code DataInputStream.readLine}: no decodifica nada, asi que cualquier
     * cosa que no sea ASCII sale mal.
     *
     * @return la linea, o null al final del flujo
     */
    String readLine() throws IOException;

    /**
     * Una cadena en UTF modificado.
     *
     * <p>Siempre en orden de red, sin importar {@link #setByteOrder}. Es una correccion vieja del JDK:
     * el formato lo define asi y respetar el orden configurado producia cadenas ilegibles.
     *
     * @throws java.io.UTFDataFormatException si los bytes no son UTF modificado valido
     */
    String readUTF() throws IOException;

    /**
     * Llena esa parte del arreglo.
     *
     * @throws java.io.EOFException si no hay suficientes
     */
    void readFully(byte[] b, int off, int len) throws IOException;

    /**
     * Llena el arreglo.
     *
     * @throws java.io.EOFException si no hay suficientes
     */
    void readFully(byte[] b) throws IOException;

    /**
     * Llena esa parte, dos bytes por elemento y en el orden configurado.
     *
     * @throws java.io.EOFException si no hay suficientes
     */
    void readFully(short[] s, int off, int len) throws IOException;

    /**
     * Idem, con caracteres.
     *
     * @throws java.io.EOFException si no hay suficientes
     */
    void readFully(char[] c, int off, int len) throws IOException;

    /**
     * Idem, cuatro bytes por elemento.
     *
     * @throws java.io.EOFException si no hay suficientes
     */
    void readFully(int[] i, int off, int len) throws IOException;

    /**
     * Idem, ocho bytes por elemento.
     *
     * @throws java.io.EOFException si no hay suficientes
     */
    void readFully(long[] l, int off, int len) throws IOException;

    /**
     * Idem, en coma flotante de cuatro bytes.
     *
     * @throws java.io.EOFException si no hay suficientes
     */
    void readFully(float[] f, int off, int len) throws IOException;

    /**
     * Idem, de ocho bytes.
     *
     * @throws java.io.EOFException si no hay suficientes
     */
    void readFully(double[] d, int off, int len) throws IOException;

    /** En que byte va la lectura. */
    long getStreamPosition() throws IOException;

    /** En que bit dentro de ese byte, de 0 a 7. Ver la nota de la clase. */
    int getBitOffset() throws IOException;

    /**
     * Lo fija.
     *
     * @throws IllegalArgumentException si no esta entre 0 y 7
     */
    void setBitOffset(int bitOffset) throws IOException;

    /**
     * Un bit, 0 o 1. Avanza el desplazamiento de bit.
     *
     * @throws java.io.EOFException si no hay mas
     */
    int readBit() throws IOException;

    /**
     * Hasta 64 bits, alineados a la derecha del resultado.
     *
     * @param numBits de 0 a 64
     * @throws IllegalArgumentException si se piden mas de 64
     * @throws java.io.EOFException si no hay suficientes
     */
    long readBits(int numBits) throws IOException;

    /** Cuantos bytes tiene, o -1 si no se sabe. */
    long length() throws IOException;

    /**
     * Saltea bytes.
     *
     * @return cuantos se saltearon de verdad
     */
    int skipBytes(int n) throws IOException;

    /** Idem, con un salto que puede pasar de dos gigabytes. */
    long skipBytes(long n) throws IOException;

    /**
     * Se posiciona en ese byte.
     *
     * @throws IndexOutOfBoundsException si es anterior a la posicion de descarte; ver la nota de la
     *     clase
     */
    void seek(long pos) throws IOException;

    /** Apila la posicion actual. Ver la nota de la clase: se apila. */
    void mark();

    /**
     * Vuelve a la ultima marca.
     *
     * @throws IOException si no hay marca, o si quedo antes de la posicion de descarte
     */
    void reset() throws IOException;

    /**
     * Promete no volver antes de esa posicion. Ver la nota de la clase.
     *
     * @throws IndexOutOfBoundsException si es anterior a la posicion de descarte actual, o posterior
     *     a la posicion actual
     */
    void flushBefore(long pos) throws IOException;

    /** Descarta todo lo anterior a la posicion actual. */
    void flush() throws IOException;

    /** Hasta donde se descarto. */
    long getFlushedPosition();

    /** Si guarda lo leido en algun lado para poder volver. */
    boolean isCached();

    /** Si lo guarda en memoria. */
    boolean isCachedMemory();

    /** Si lo guarda en un archivo temporal. */
    boolean isCachedFile();

    /**
     * Cierra.
     *
     * <p>No cierra el flujo de abajo: quien lo abrio es quien lo cierra. Es lo contrario de lo que
     * hace casi todo {@code java.io} y hay que tenerlo presente.
     */
    void close() throws IOException;
}
