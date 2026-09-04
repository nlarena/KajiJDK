package javax.imageio.stream;

import java.io.DataOutput;
import java.io.IOException;

/**
 * KajiLibrary's javax.imageio.stream.ImageOutputStream -- un flujo de escritura para formatos de
 * imagen.
 *
 * <p>Extiende {@link ImageInputStream} y no solo {@link DataOutput}, y eso llama la atencion: un flujo
 * de <b>escritura</b> que ademas se puede leer.
 *
 * <p>No es un descuido. Escribir un formato de imagen casi siempre necesita volver: se escribe un
 * encabezado con un largo que todavia no se conoce, se escribe la imagen, y se vuelve a corregir el
 * encabezado. Sin poder leer y posicionarse, eso obliga a armar el archivo entero en memoria.
 *
 * <p>Hereda el orden de bytes configurable, y lo aplica al escribir.
 *
 * <h2>Los bits pendientes</h2>
 *
 * <p>{@link #writeBit} y {@link #writeBits} dejan bits a medio byte. Cualquier escritura de byte o
 * mayor <b>cierra</b> ese byte rellenando con ceros, igual que la lectura limpia el desplazamiento.
 *
 * <p>La trampa esta al final: cerrar el flujo con bits pendientes los descarta si no se escribio nada
 * mas. Hay que forzar el cierre del byte escribiendo algo, o llamar {@link #flush}.
 */
public interface ImageOutputStream extends ImageInputStream, DataOutput {

    /**
     * Escribe el byte bajo.
     *
     * @throws IOException si fallo
     */
    void write(int b) throws IOException;

    /**
     * Escribe el arreglo.
     *
     * @throws IOException si fallo
     */
    void write(byte[] b) throws IOException;

    /**
     * Escribe esa parte del arreglo.
     *
     * @throws IOException si fallo
     */
    void write(byte[] b, int off, int len) throws IOException;

    /** Un byte: 1 o 0. */
    void writeBoolean(boolean v) throws IOException;

    /** El byte bajo. */
    void writeByte(int v) throws IOException;

    /** Dos bytes, en el orden configurado. */
    void writeShort(int v) throws IOException;

    /** Dos bytes. */
    void writeChar(int v) throws IOException;

    /** Cuatro bytes. */
    void writeInt(int v) throws IOException;

    /** Ocho bytes. */
    void writeLong(long v) throws IOException;

    /** Cuatro bytes. */
    void writeFloat(float v) throws IOException;

    /** Ocho bytes. */
    void writeDouble(double v) throws IOException;

    /**
     * Un byte por caracter.
     *
     * <p>Se lleva el byte alto de cada uno; solo sirve para ASCII.
     */
    void writeBytes(String s) throws IOException;

    /** Dos bytes por caracter, en el orden configurado. */
    void writeChars(String s) throws IOException;

    /**
     * En UTF modificado.
     *
     * <p>Siempre en orden de red, como {@link ImageInputStream#readUTF}.
     *
     * @throws java.io.UTFDataFormatException si la cadena codificada pasa de 65535 bytes
     */
    void writeUTF(String s) throws IOException;

    /** Esa parte del arreglo, dos bytes por elemento. */
    void writeShorts(short[] s, int off, int len) throws IOException;

    /** Idem, con caracteres. */
    void writeChars(char[] c, int off, int len) throws IOException;

    /** Idem, cuatro bytes por elemento. */
    void writeInts(int[] i, int off, int len) throws IOException;

    /** Idem, ocho bytes. */
    void writeLongs(long[] l, int off, int len) throws IOException;

    /** Idem, coma flotante de cuatro bytes. */
    void writeFloats(float[] f, int off, int len) throws IOException;

    /** Idem, de ocho bytes. */
    void writeDoubles(double[] d, int off, int len) throws IOException;

    /**
     * Un bit; se toma el bit bajo del argumento. Ver la nota de la clase.
     *
     * @throws IOException si fallo
     */
    void writeBit(int bit) throws IOException;

    /**
     * Los {@code numBits} bits bajos del valor.
     *
     * @param numBits de 0 a 64
     * @throws IllegalArgumentException si se piden mas de 64
     */
    void writeBits(long bits, int numBits) throws IOException;

    /**
     * Escribe de verdad todo lo anterior a esa posicion y promete no volver antes.
     *
     * @throws IndexOutOfBoundsException si es anterior a la posicion de descarte, o posterior a la
     *     actual
     */
    void flushBefore(long pos) throws IOException;
}
