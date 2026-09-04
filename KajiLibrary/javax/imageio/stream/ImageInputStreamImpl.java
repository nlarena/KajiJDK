package javax.imageio.stream;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;

/**
 * KajiLibrary's javax.imageio.stream.ImageInputStreamImpl -- todo {@link ImageInputStream} menos el
 * acceso al dato.
 *
 * <p>Una subclase solo tiene que dar {@link #read()} y {@link #read(byte[], int, int)}; el resto
 * --los veinte {@code readX}, el orden de bytes, los bits, las marcas-- sale de aca.
 *
 * <h2>La subclase tiene que mantener {@link #streamPos}</h2>
 *
 * <p>Es el contrato que se olvida. Los dos {@code read} abstractos tienen que <b>sumar</b> a
 * {@code streamPos} lo que leyeron: esta clase no lo hace por ellos, porque no sabe cuanto avanzaron.
 *
 * <p>Y tienen que llamar a {@link #checkClosed} antes de tocar nada.
 *
 * <h2>El desplazamiento de bit</h2>
 *
 * <p>{@link #bitOffset} es lo que hace posible leer campos que no caen en limites de byte. La regla la
 * aplica esta clase: cada lectura de un byte o mas lo pone en cero, asi que alternar entre bits y
 * bytes funciona sin llevar la cuenta.
 *
 * <p>{@link #readBits} lee de a un bit por vuelta. Es la version simple y correcta; una que junte
 * bytes enteros seria mas rapida y bastante mas facil de romper en los bordes.
 *
 * <h2>Las marcas y el descarte</h2>
 *
 * <p>{@link #mark} apila posiciones y {@link #reset} las desapila; ver {@link ImageInputStream}. Y
 * {@link #flushedPos} es la barrera: nada anterior se puede volver a leer, y {@link #seek} hacia atras
 * de ahi lanza {@link IndexOutOfBoundsException}.
 *
 * <p>{@link #close} <b>no</b> cierra el flujo de abajo; ver {@link ImageInputStream#close}.
 */
public abstract class ImageInputStreamImpl implements ImageInputStream {

    /**
     * Ocho bytes de andamio para los {@code readX}.
     *
     * <p>De acceso de paquete y compartido entre llamadas: evita alocar un arreglo por cada
     * {@code readInt}, y un lector de imagenes hace millones. No es seguro entre hilos, y el JDK
     * tampoco lo promete.
     */
    byte[] byteBuf = new byte[8];

    /** Con que orden leer lo de mas de un byte. */
    protected ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

    /** En que byte va la lectura. La subclase la mantiene; ver la nota de la clase. */
    protected long streamPos;

    /** En que bit dentro de ese byte. */
    protected int bitOffset;

    /** Hasta donde se descarto. */
    protected long flushedPos;

    /** Si ya se cerro. */
    private boolean isClosed = false;

    /** Las marcas, apiladas. */
    private final ArrayList<Long> markByteStack = new ArrayList<Long>();

    /** Los desplazamientos de bit de cada marca. */
    private final ArrayList<Integer> markBitStack = new ArrayList<Integer>();

    /** Para las subclases. */
    public ImageInputStreamImpl() {
    }

    /**
     * Falla si el flujo esta cerrado.
     *
     * <p>Toda subclase tiene que llamarlo al principio de sus {@code read}.
     *
     * @throws IOException si ya se cerro
     */
    protected final void checkClosed() throws IOException {
        if (this.isClosed) {
            throw new IOException("closed");
        }
    }

    /** Con que orden leer. */
    public void setByteOrder(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }

    /** Cual esta puesto. */
    public ByteOrder getByteOrder() {
        return this.byteOrder;
    }

    /** Lo tiene que dar la subclase. Ver la nota de la clase. */
    public abstract int read() throws IOException;

    /** Hasta llenar el arreglo. */
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /** Lo tiene que dar la subclase. */
    public abstract int read(byte[] b, int off, int len) throws IOException;

    /**
     * Hasta {@code len} bytes, sin copiar.
     *
     * <p>Esta implementacion <b>si</b> copia: aloca un arreglo del tamano pedido y lo entrega. La
     * ganancia de no copiar depende de que la subclase tenga un bufer propio del que prestar, y esta
     * clase no lo tiene. Es lo que hace el JDK en esta misma clase.
     *
     * @throws IndexOutOfBoundsException si el largo es negativo
     * @throws NullPointerException si el bufer es null
     */
    public void readBytes(IIOByteBuffer buf, int len) throws IOException {
        if (buf == null) {
            throw new NullPointerException("buf == null!");
        }
        if (len < 0) {
            throw new IndexOutOfBoundsException("len < 0!");
        }
        byte[] data = new byte[len];
        len = read(data, 0, len);
        buf.setData(data);
        buf.setOffset(0);
        buf.setLength(len);
    }

    /** Un byte como booleano. */
    public boolean readBoolean() throws IOException {
        int ch = read();
        if (ch < 0) {
            throw new EOFException();
        }
        return ch != 0;
    }

    /** Un byte con signo. */
    public byte readByte() throws IOException {
        int ch = read();
        if (ch < 0) {
            throw new EOFException();
        }
        return (byte) ch;
    }

    /** Un byte sin signo. */
    public int readUnsignedByte() throws IOException {
        int ch = read();
        if (ch < 0) {
            throw new EOFException();
        }
        return ch;
    }

    /** Dos bytes con signo, en el orden configurado. */
    public short readShort() throws IOException {
        readFullyInternal(2);
        int hi = this.byteBuf[0] & 0xFF;
        int lo = this.byteBuf[1] & 0xFF;
        if (this.byteOrder == ByteOrder.BIG_ENDIAN) {
            return (short) ((hi << 8) | lo);
        }
        return (short) ((lo << 8) | hi);
    }

    /** Dos bytes sin signo. */
    public int readUnsignedShort() throws IOException {
        return readShort() & 0xFFFF;
    }

    /** Dos bytes como caracter. */
    public char readChar() throws IOException {
        return (char) readShort();
    }

    /** Cuatro bytes con signo. */
    public int readInt() throws IOException {
        readFullyInternal(4);
        int b0 = this.byteBuf[0] & 0xFF;
        int b1 = this.byteBuf[1] & 0xFF;
        int b2 = this.byteBuf[2] & 0xFF;
        int b3 = this.byteBuf[3] & 0xFF;
        if (this.byteOrder == ByteOrder.BIG_ENDIAN) {
            return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
        }
        return (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
    }

    /** Cuatro bytes sin signo. Ver {@link ImageInputStream#readUnsignedInt}. */
    public long readUnsignedInt() throws IOException {
        return readInt() & 0xFFFFFFFFL;
    }

    /** Ocho bytes. */
    public long readLong() throws IOException {
        // Se arma con dos enteros de cuatro y no de a ocho bytes: asi el orden de bytes se aplica una
        // sola vez, en readInt, en lugar de repetir la logica.
        int i1 = readInt();
        int i2 = readInt();
        if (this.byteOrder == ByteOrder.BIG_ENDIAN) {
            return ((long) i1 << 32) + (i2 & 0xFFFFFFFFL);
        }
        return ((long) i2 << 32) + (i1 & 0xFFFFFFFFL);
    }

    /** Cuatro bytes como coma flotante. */
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    /** Ocho bytes como coma flotante. */
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    /** Una linea, un byte por caracter. Ver {@link ImageInputStream#readLine}. */
    public String readLine() throws IOException {
        StringBuilder input = new StringBuilder();
        int c = -1;
        boolean eol = false;
        while (!eol) {
            c = read();
            if (c == -1 || c == '\n') {
                eol = true;
            } else if (c == '\r') {
                eol = true;
                // Un \r\n cuenta como un solo fin de linea, y el \n no se consume si no viene.
                long cur = getStreamPosition();
                if (read() != '\n') {
                    seek(cur);
                }
            } else {
                input.append((char) c);
            }
        }
        if (c == -1 && input.length() == 0) {
            return null;
        }
        return input.toString();
    }

    /**
     * Una cadena en UTF modificado.
     *
     * <p>Siempre en orden de red: se cambia el orden, se lee, y se restaura -- incluso si la lectura
     * falla. Ver {@link ImageInputStream#readUTF}.
     */
    public String readUTF() throws IOException {
        checkClosed();
        this.bitOffset = 0;
        ByteOrder oldByteOrder = getByteOrder();
        setByteOrder(ByteOrder.BIG_ENDIAN);
        String ret;
        try {
            ret = DataInputStream.readUTF(this);
        } catch (IOException e) {
            setByteOrder(oldByteOrder);
            throw e;
        }
        setByteOrder(oldByteOrder);
        return ret;
    }

    /** Llena esa parte del arreglo. */
    public void readFully(byte[] b, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off + len > b.length || off + len < 0) {
            throw new IndexOutOfBoundsException();
        }
        while (len > 0) {
            int nbytes = read(b, off, len);
            if (nbytes == -1) {
                throw new EOFException();
            }
            off = off + nbytes;
            len = len - nbytes;
        }
    }

    /** Llena el arreglo. */
    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    /** Llena esa parte, dos bytes por elemento. */
    public void readFully(short[] s, int off, int len) throws IOException {
        checkBounds(off, len, s.length);
        int i = 0;
        while (i < len) {
            s[off + i] = readShort();
            i = i + 1;
        }
    }

    /** Idem, con caracteres. */
    public void readFully(char[] c, int off, int len) throws IOException {
        checkBounds(off, len, c.length);
        int i = 0;
        while (i < len) {
            c[off + i] = readChar();
            i = i + 1;
        }
    }

    /** Idem, cuatro bytes por elemento. */
    public void readFully(int[] i, int off, int len) throws IOException {
        checkBounds(off, len, i.length);
        int k = 0;
        while (k < len) {
            i[off + k] = readInt();
            k = k + 1;
        }
    }

    /** Idem, ocho bytes. */
    public void readFully(long[] l, int off, int len) throws IOException {
        checkBounds(off, len, l.length);
        int i = 0;
        while (i < len) {
            l[off + i] = readLong();
            i = i + 1;
        }
    }

    /** Idem, coma flotante de cuatro bytes. */
    public void readFully(float[] f, int off, int len) throws IOException {
        checkBounds(off, len, f.length);
        int i = 0;
        while (i < len) {
            f[off + i] = readFloat();
            i = i + 1;
        }
    }

    /** Idem, de ocho bytes. */
    public void readFully(double[] d, int off, int len) throws IOException {
        checkBounds(off, len, d.length);
        int i = 0;
        while (i < len) {
            d[off + i] = readDouble();
            i = i + 1;
        }
    }

    /** En que byte va. */
    public long getStreamPosition() throws IOException {
        checkClosed();
        return this.streamPos;
    }

    /** En que bit dentro de ese byte. */
    public int getBitOffset() throws IOException {
        checkClosed();
        return this.bitOffset;
    }

    /**
     * Lo fija.
     *
     * @throws IllegalArgumentException si no esta entre 0 y 7
     */
    public void setBitOffset(int bitOffset) throws IOException {
        checkClosed();
        if (bitOffset < 0 || bitOffset > 7) {
            throw new IllegalArgumentException("bitOffset must be betwwen 0 and 7!");
        }
        this.bitOffset = bitOffset;
    }

    /**
     * Un bit.
     *
     * <p>Lee el byte, saca el bit que toca, y si no era el ultimo del byte <b>vuelve atras</b> para
     * que la proxima lectura encuentre el mismo byte. Es lo que hace que leer ocho bits seguidos
     * consuma un byte y no ocho.
     */
    public int readBit() throws IOException {
        checkClosed();
        int bo = this.bitOffset;
        int value = read();
        if (value == -1) {
            throw new EOFException();
        }
        value = (value >> (7 - bo)) & 0x1;
        bo = bo + 1;
        this.bitOffset = bo & 0x7;
        if (this.bitOffset != 0) {
            seek(getStreamPosition() - 1);
            this.bitOffset = bo & 0x7;
        }
        return value;
    }

    /**
     * Hasta 64 bits.
     *
     * <p>De a un bit: es la version simple, y la que no se equivoca en los bordes.
     *
     * @throws IllegalArgumentException si se piden mas de 64
     */
    public long readBits(int numBits) throws IOException {
        checkClosed();
        if (numBits < 0 || numBits > 64) {
            throw new IllegalArgumentException();
        }
        if (numBits == 0) {
            return 0L;
        }
        long accum = 0L;
        int i = 0;
        while (i < numBits) {
            accum = (accum << 1) | readBit();
            i = i + 1;
        }
        return accum;
    }

    /** No se sabe; una subclase que pueda saberlo lo redefine. */
    public long length() {
        return -1L;
    }

    /** Saltea bytes. */
    public int skipBytes(int n) throws IOException {
        long pos = getStreamPosition();
        seek(pos + n);
        return (int) (getStreamPosition() - pos);
    }

    /** Idem, con un salto grande. */
    public long skipBytes(long n) throws IOException {
        long pos = getStreamPosition();
        seek(pos + n);
        return getStreamPosition() - pos;
    }

    /**
     * Se posiciona en ese byte.
     *
     * <p>Limpia el desplazamiento de bit, como toda operacion de byte.
     *
     * @throws IndexOutOfBoundsException si es anterior a la posicion de descarte
     */
    public void seek(long pos) throws IOException {
        checkClosed();
        if (pos < this.flushedPos) {
            throw new IndexOutOfBoundsException("pos < flushedPos!");
        }
        this.bitOffset = 0;
        this.streamPos = pos;
    }

    /** Apila la posicion y el desplazamiento de bit. */
    public void mark() {
        try {
            this.markByteStack.add(Long.valueOf(getStreamPosition()));
            this.markBitStack.add(Integer.valueOf(getBitOffset()));
        } catch (IOException e) {
            // El flujo esta cerrado. `mark` no declara IOException, asi que no hay donde avisar; el
            // `reset` correspondiente va a fallar, que es donde el error si se puede reportar.
        }
    }

    /**
     * Desapila la ultima marca.
     *
     * <p>Sin marcas no hace nada: es lo que hace el JDK, y no lanzar aca permite un {@code reset}
     * defensivo.
     */
    public void reset() throws IOException {
        if (this.markByteStack.isEmpty()) {
            return;
        }
        long pos = this.markByteStack.remove(this.markByteStack.size() - 1).longValue();
        if (pos < this.flushedPos) {
            throw new IOException("Previous marked position has been discarded!");
        }
        seek(pos);
        int offset = this.markBitStack.remove(this.markBitStack.size() - 1).intValue();
        setBitOffset(offset);
    }

    /**
     * Promete no volver antes de esa posicion.
     *
     * @throws IndexOutOfBoundsException si es anterior al descarte actual o posterior a la posicion
     */
    public void flushBefore(long pos) throws IOException {
        checkClosed();
        if (pos < this.flushedPos) {
            throw new IndexOutOfBoundsException("pos < flushedPos!");
        }
        if (pos > getStreamPosition()) {
            throw new IndexOutOfBoundsException("pos > getStreamPosition()!");
        }
        this.flushedPos = pos;
    }

    /** Descarta todo lo anterior a la posicion actual. */
    public void flush() throws IOException {
        flushBefore(getStreamPosition());
    }

    /** Hasta donde se descarto. */
    public long getFlushedPosition() {
        return this.flushedPos;
    }

    /** No; una subclase que guarde lo redefine. */
    public boolean isCached() {
        return false;
    }

    /** No. */
    public boolean isCachedMemory() {
        return false;
    }

    /** No. */
    public boolean isCachedFile() {
        return false;
    }

    /**
     * Cierra.
     *
     * <p>No cierra el flujo de abajo; ver {@link ImageInputStream#close}.
     *
     * @throws IOException si ya estaba cerrado
     */
    public void close() throws IOException {
        checkClosed();
        this.isClosed = true;
    }

    /**
     * Cierra si nadie lo hizo.
     *
     * <p>Sobrevive porque la clase base del JDK lo declara y una subclase puede estar llamando a
     * {@code super.finalize()}. La finalizacion quedo obsoleta y no hay que apoyarse en esto: un
     * {@code ImageInputStream} se cierra a mano.
     */
    @Override
    protected void finalize() throws Throwable {
        if (!this.isClosed) {
            try {
                close();
            } catch (IOException e) {
                // Ya se estaba finalizando; no hay a quien reportarle.
            }
        }
        super.finalize();
    }

    /** Llena los primeros {@code n} bytes del andamio. */
    private void readFullyInternal(int n) throws IOException {
        readFully(this.byteBuf, 0, n);
    }

    /** El control de rango que comparten los {@code readFully} de arreglos. */
    private static void checkBounds(int off, int len, int length) {
        if (off < 0 || len < 0 || off + len > length || off + len < 0) {
            throw new IndexOutOfBoundsException();
        }
    }
}
