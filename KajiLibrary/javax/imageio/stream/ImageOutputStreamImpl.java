package javax.imageio.stream;

import java.io.IOException;
import java.nio.ByteOrder;

/**
 * KajiLibrary's javax.imageio.stream.ImageOutputStreamImpl -- todo {@link ImageOutputStream} menos el
 * acceso al dato.
 *
 * <p>El espejo de {@link ImageInputStreamImpl}: una subclase da {@link #write(int)} y
 * {@link #write(byte[], int, int)}, y el resto sale de aca. Y como hereda de aquella, tambien tiene
 * que dar los dos {@code read}.
 *
 * <h2>Los bits pendientes y {@link #flushBits}</h2>
 *
 * <p>Escribir bits sueltos deja un byte a medio llenar. La regla la aplica esta clase: cada escritura
 * de byte o mayor llama a {@link #flushBits}, que cierra el byte rellenando con ceros.
 *
 * <p>{@code flushBits} hace algo que sorprende: <b>lee</b> el byte que esta en el flujo antes de
 * reescribirlo, para no pisar los bits que ya estaban. Es la razon de que
 * {@code ImageOutputStream} tenga que ser tambien de lectura.
 */
public abstract class ImageOutputStreamImpl extends ImageInputStreamImpl
    implements ImageOutputStream {

    /** Para las subclases. */
    public ImageOutputStreamImpl() {
    }

    /** Lo tiene que dar la subclase. */
    public abstract void write(int b) throws IOException;

    /** Escribe el arreglo. */
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /** Lo tiene que dar la subclase. */
    public abstract void write(byte[] b, int off, int len) throws IOException;

    /** Un byte: 1 o 0. */
    public void writeBoolean(boolean v) throws IOException {
        if (v) {
            write(1);
        } else {
            write(0);
        }
    }

    /** El byte bajo. */
    public void writeByte(int v) throws IOException {
        write(v);
    }

    /** Dos bytes, en el orden configurado. */
    public void writeShort(int v) throws IOException {
        if (this.byteOrder == ByteOrder.BIG_ENDIAN) {
            this.byteBuf[0] = (byte) (v >>> 8);
            this.byteBuf[1] = (byte) v;
        } else {
            this.byteBuf[0] = (byte) v;
            this.byteBuf[1] = (byte) (v >>> 8);
        }
        write(this.byteBuf, 0, 2);
    }

    /** Dos bytes. */
    public void writeChar(int v) throws IOException {
        writeShort(v);
    }

    /** Cuatro bytes. */
    public void writeInt(int v) throws IOException {
        if (this.byteOrder == ByteOrder.BIG_ENDIAN) {
            this.byteBuf[0] = (byte) (v >>> 24);
            this.byteBuf[1] = (byte) (v >>> 16);
            this.byteBuf[2] = (byte) (v >>> 8);
            this.byteBuf[3] = (byte) v;
        } else {
            this.byteBuf[0] = (byte) v;
            this.byteBuf[1] = (byte) (v >>> 8);
            this.byteBuf[2] = (byte) (v >>> 16);
            this.byteBuf[3] = (byte) (v >>> 24);
        }
        write(this.byteBuf, 0, 4);
    }

    /** Ocho bytes. */
    public void writeLong(long v) throws IOException {
        if (this.byteOrder == ByteOrder.BIG_ENDIAN) {
            this.byteBuf[0] = (byte) (v >>> 56);
            this.byteBuf[1] = (byte) (v >>> 48);
            this.byteBuf[2] = (byte) (v >>> 40);
            this.byteBuf[3] = (byte) (v >>> 32);
            this.byteBuf[4] = (byte) (v >>> 24);
            this.byteBuf[5] = (byte) (v >>> 16);
            this.byteBuf[6] = (byte) (v >>> 8);
            this.byteBuf[7] = (byte) v;
        } else {
            this.byteBuf[0] = (byte) v;
            this.byteBuf[1] = (byte) (v >>> 8);
            this.byteBuf[2] = (byte) (v >>> 16);
            this.byteBuf[3] = (byte) (v >>> 24);
            this.byteBuf[4] = (byte) (v >>> 32);
            this.byteBuf[5] = (byte) (v >>> 40);
            this.byteBuf[6] = (byte) (v >>> 48);
            this.byteBuf[7] = (byte) (v >>> 56);
        }
        write(this.byteBuf, 0, 8);
    }

    /** Cuatro bytes. */
    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    /** Ocho bytes. */
    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    /** Un byte por caracter; solo sirve para ASCII. */
    public void writeBytes(String s) throws IOException {
        int i = 0;
        while (i < s.length()) {
            write((byte) s.charAt(i));
            i = i + 1;
        }
    }

    /** Dos bytes por caracter. */
    public void writeChars(String s) throws IOException {
        int i = 0;
        while (i < s.length()) {
            writeChar(s.charAt(i));
            i = i + 1;
        }
    }

    /**
     * En UTF modificado, siempre en orden de red.
     *
     * <p>Se restaura el orden aunque falle, igual que en la lectura.
     *
     * @throws java.io.UTFDataFormatException si la cadena codificada pasa de 65535 bytes
     */
    public void writeUTF(String s) throws IOException {
        flushBits();
        int strlen = s.length();
        int utflen = 0;
        int i = 0;
        while (i < strlen) {
            int c = s.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                utflen = utflen + 1;
            } else if (c > 0x07FF) {
                utflen = utflen + 3;
            } else {
                utflen = utflen + 2;
            }
            i = i + 1;
        }
        if (utflen > 65535) {
            throw new java.io.UTFDataFormatException("encoded string too long: "
                + utflen + " bytes");
        }
        byte[] bytearr = new byte[utflen + 2];
        bytearr[0] = (byte) ((utflen >>> 8) & 0xFF);
        bytearr[1] = (byte) (utflen & 0xFF);
        int count = 2;
        i = 0;
        while (i < strlen) {
            int c = s.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                bytearr[count] = (byte) c;
                count = count + 1;
            } else if (c > 0x07FF) {
                bytearr[count] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                bytearr[count + 1] = (byte) (0x80 | ((c >> 6) & 0x3F));
                bytearr[count + 2] = (byte) (0x80 | (c & 0x3F));
                count = count + 3;
            } else {
                bytearr[count] = (byte) (0xC0 | ((c >> 6) & 0x1F));
                bytearr[count + 1] = (byte) (0x80 | (c & 0x3F));
                count = count + 2;
            }
            i = i + 1;
        }
        // El largo va en orden de red pase lo que pase, por eso se arma el arreglo entero a mano en
        // lugar de usar writeShort, que respetaria el orden configurado.
        write(bytearr, 0, utflen + 2);
    }

    /** Esa parte del arreglo, dos bytes por elemento. */
    public void writeShorts(short[] s, int off, int len) throws IOException {
        checkBounds(off, len, s.length);
        int i = 0;
        while (i < len) {
            writeShort(s[off + i]);
            i = i + 1;
        }
    }

    /** Idem, con caracteres. */
    public void writeChars(char[] c, int off, int len) throws IOException {
        checkBounds(off, len, c.length);
        int i = 0;
        while (i < len) {
            writeChar(c[off + i]);
            i = i + 1;
        }
    }

    /** Idem, cuatro bytes. */
    public void writeInts(int[] i, int off, int len) throws IOException {
        checkBounds(off, len, i.length);
        int k = 0;
        while (k < len) {
            writeInt(i[off + k]);
            k = k + 1;
        }
    }

    /** Idem, ocho bytes. */
    public void writeLongs(long[] l, int off, int len) throws IOException {
        checkBounds(off, len, l.length);
        int i = 0;
        while (i < len) {
            writeLong(l[off + i]);
            i = i + 1;
        }
    }

    /** Idem, coma flotante de cuatro bytes. */
    public void writeFloats(float[] f, int off, int len) throws IOException {
        checkBounds(off, len, f.length);
        int i = 0;
        while (i < len) {
            writeFloat(f[off + i]);
            i = i + 1;
        }
    }

    /** Idem, de ocho bytes. */
    public void writeDoubles(double[] d, int off, int len) throws IOException {
        checkBounds(off, len, d.length);
        int i = 0;
        while (i < len) {
            writeDouble(d[off + i]);
            i = i + 1;
        }
    }

    /** Un bit; se toma el bit bajo. */
    public void writeBit(int bit) throws IOException {
        writeBits(bit & 0x1, 1);
    }

    /**
     * Los {@code numBits} bits bajos del valor, del mas significativo al menos.
     *
     * <p>Va de a un bit, con la misma estructura que {@code ImageInputStreamImpl.readBit}: leer el
     * byte, cambiarle el bit que toca, reescribirlo, y volver atras si el byte quedo a medio llenar.
     *
     * <p>Es la version simple. Una que junte bytes enteros seria mas rapida y bastante mas facil de
     * romper en los bordes -- que es donde los formatos comprimidos viven.
     *
     * @throws IllegalArgumentException si se piden mas de 64
     */
    public void writeBits(long bits, int numBits) throws IOException {
        checkClosed();
        if (numBits < 0 || numBits > 64) {
            throw new IllegalArgumentException("Bad value for numBits!");
        }
        int i = numBits - 1;
        while (i >= 0) {
            writeSingleBit((int) ((bits >>> i) & 1L));
            i = i - 1;
        }
    }

    /** Un bit en la posicion actual, conservando los que ya estaban en ese byte. */
    private void writeSingleBit(int bit) throws IOException {
        int offset = this.bitOffset;
        long pos = this.streamPos;
        // El byte se lee antes de reescribirlo: si no, los bits anteriores se perderian. Por eso un
        // ImageOutputStream tiene que ser tambien de lectura.
        seek(pos);
        int existing = read();
        if (existing == -1) {
            existing = 0;
        }
        seek(pos);
        int mask = 1 << (7 - offset);
        int combined;
        if (bit != 0) {
            combined = existing | mask;
        } else {
            combined = existing & ~mask;
        }
        write(combined);
        int next = (offset + 1) & 0x7;
        if (next != 0) {
            // El byte quedo a medio llenar: se vuelve sobre el, y el desplazamiento se pone despues
            // porque `seek` lo limpia.
            seek(pos);
        }
        this.bitOffset = next;
    }

    /**
     * Cierra el byte a medio escribir, rellenando con ceros.
     *
     * <p>Lo llaman todas las escrituras de byte o mayor. Ver la nota de la clase sobre por que lee
     * antes de escribir.
     */
    protected final void flushBits() throws IOException {
        checkClosed();
        if (this.bitOffset != 0) {
            int offset = this.bitOffset;
            long pos = this.streamPos;
            int partial = read();
            if (partial == -1) {
                partial = 0;
            }
            seek(pos);
            int mask = (0xFF >> offset);
            write(partial & ~mask);
            this.bitOffset = 0;
        }
    }

    /** El control de rango que comparten los {@code writeXs}. */
    private static void checkBounds(int off, int len, int length) {
        if (off < 0 || len < 0 || off + len > length || off + len < 0) {
            throw new IndexOutOfBoundsException();
        }
    }
}
