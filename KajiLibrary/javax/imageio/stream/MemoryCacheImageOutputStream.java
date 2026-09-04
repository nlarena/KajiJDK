package javax.imageio.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * KajiLibrary's javax.imageio.stream.MemoryCacheImageOutputStream -- escribe a un flujo cualquiera,
 * juntando en memoria.
 *
 * <p>El espejo de {@link MemoryCacheImageInputStream}. Un {@link OutputStream} no se puede
 * reposicionar, y escribir un formato de imagen casi siempre necesita volver a corregir el encabezado;
 * la solucion es juntar todo en memoria y soltarlo cuando se puede.
 *
 * <h2>{@link #flushBefore} es lo que escribe de verdad</h2>
 *
 * <p>Es la parte que se malinterpreta. Mientras no se llame, <b>nada</b> llega al flujo de abajo: todo
 * queda en el monton. {@link #close} llama a {@code flush} y con eso sale todo.
 *
 * <p>Y una vez que un tramo salio, no se puede volver sobre el: {@link #seek} a una posicion anterior
 * lanza {@link IndexOutOfBoundsException}. Es el precio de haberlo soltado.
 *
 * <p>El flujo de abajo no se cierra al cerrar este.
 */
public class MemoryCacheImageOutputStream extends ImageOutputStreamImpl {

    /** Cuantos bytes tiene cada bloque. */
    private static final int BLOCK_SIZE = 8192;

    /** A donde va lo que se suelta. */
    private OutputStream stream;

    /** Los bloques juntados; el primero corresponde a {@link #cacheStart}. */
    private final ArrayList<byte[]> cache = new ArrayList<byte[]>();

    /** A que posicion corresponde el primer bloque. */
    private long cacheStart = 0;

    /** Hasta donde se escribio. */
    private long length = 0;

    /**
     * @param stream a donde escribir
     * @throws IllegalArgumentException si es null
     */
    public MemoryCacheImageOutputStream(OutputStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("stream == null!");
        }
        this.stream = stream;
    }

    /** Un byte de lo ya escrito, o -1 si se paso del final. */
    @Override
    public int read() throws IOException {
        checkClosed();
        this.bitOffset = 0;
        if (this.streamPos >= this.length) {
            return -1;
        }
        int value = byteAt(this.streamPos) & 0xFF;
        this.streamPos = this.streamPos + 1;
        return value;
    }

    /** Hasta {@code len} bytes de lo ya escrito. */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkClosed();
        if (b == null) {
            throw new NullPointerException("b == null!");
        }
        if (off < 0 || len < 0 || off + len > b.length || off + len < 0) {
            throw new IndexOutOfBoundsException();
        }
        this.bitOffset = 0;
        if (len == 0) {
            return 0;
        }
        if (this.streamPos >= this.length) {
            return -1;
        }
        int available = (int) Math.min((long) len, this.length - this.streamPos);
        int i = 0;
        while (i < available) {
            b[off + i] = byteAt(this.streamPos + i);
            i = i + 1;
        }
        this.streamPos = this.streamPos + available;
        return available;
    }

    /** Un byte. */
    @Override
    public void write(int b) throws IOException {
        flushBits();
        ensureCapacity(this.streamPos + 1);
        setByteAt(this.streamPos, (byte) b);
        this.streamPos = this.streamPos + 1;
        if (this.streamPos > this.length) {
            this.length = this.streamPos;
        }
    }

    /** Esa parte del arreglo. */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        flushBits();
        if (b == null) {
            throw new NullPointerException("b == null!");
        }
        if (off < 0 || len < 0 || off + len > b.length || off + len < 0) {
            throw new IndexOutOfBoundsException();
        }
        ensureCapacity(this.streamPos + len);
        int i = 0;
        while (i < len) {
            setByteAt(this.streamPos + i, b[off + i]);
            i = i + 1;
        }
        this.streamPos = this.streamPos + len;
        if (this.streamPos > this.length) {
            this.length = this.streamPos;
        }
    }

    /** Cuanto se escribio hasta ahora. */
    @Override
    public long length() {
        return this.length;
    }

    /** Si. */
    @Override
    public boolean isCached() {
        return true;
    }

    /** No. */
    @Override
    public boolean isCachedFile() {
        return false;
    }

    /** Si. */
    @Override
    public boolean isCachedMemory() {
        return true;
    }

    /**
     * Suelta al flujo de abajo todo lo anterior a esa posicion. Ver la nota de la clase.
     *
     * @throws IndexOutOfBoundsException si es anterior al descarte actual o posterior a la posicion
     */
    @Override
    public void flushBefore(long pos) throws IOException {
        long oldFlushed = getFlushedPosition();
        super.flushBefore(pos);
        long i = oldFlushed;
        while (i < pos) {
            this.stream.write(byteAt(i) & 0xFF);
            i = i + 1;
        }
        this.stream.flush();
        // Los bloques que quedaron enteros atras ya no hacen falta.
        long firstNeeded = (pos / BLOCK_SIZE) * BLOCK_SIZE;
        while (this.cacheStart + BLOCK_SIZE <= firstNeeded && !this.cache.isEmpty()) {
            this.cache.remove(0);
            this.cacheStart = this.cacheStart + BLOCK_SIZE;
        }
    }

    /** Suelta todo lo pendiente y cierra. No cierra el flujo de abajo. */
    @Override
    public void close() throws IOException {
        try {
            flushBits();
        } catch (IOException e) {
            // Ya se esta cerrando.
        }
        // Sin esto, lo escrito despues del ultimo flushBefore se perderia en silencio.
        long pos = this.length;
        seek(pos);
        flushBefore(pos);
        super.close();
        this.cache.clear();
        this.stream = null;
    }

    /** Agranda la cache hasta poder escribir en esa posicion. */
    private void ensureCapacity(long pos) {
        while (this.cacheStart + (long) this.cache.size() * BLOCK_SIZE < pos) {
            this.cache.add(new byte[BLOCK_SIZE]);
        }
    }

    /** El byte guardado en esa posicion. */
    private byte byteAt(long pos) {
        long offset = pos - this.cacheStart;
        byte[] block = this.cache.get((int) (offset / BLOCK_SIZE));
        return block[(int) (offset % BLOCK_SIZE)];
    }

    /** Lo escribe. */
    private void setByteAt(long pos, byte value) {
        long offset = pos - this.cacheStart;
        byte[] block = this.cache.get((int) (offset / BLOCK_SIZE));
        block[(int) (offset % BLOCK_SIZE)] = value;
    }
}
