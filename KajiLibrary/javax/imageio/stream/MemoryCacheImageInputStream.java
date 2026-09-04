package javax.imageio.stream;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * KajiLibrary's javax.imageio.stream.MemoryCacheImageInputStream -- lee un flujo cualquiera,
 * guardando en memoria lo que pasa.
 *
 * <p>Un {@link InputStream} no se puede rebobinar, y los formatos de imagen necesitan volver. La
 * solucion de esta clase es guardar todo lo leido en memoria.
 *
 * <p>La consecuencia es la que se espera: <b>la memoria crece con lo que se lee</b>. Para una imagen
 * de cien megabytes desde un socket, son cien megabytes de monton. {@link FileCacheImageInputStream}
 * es la alternativa cuando eso no entra.
 *
 * <p>{@link #flushBefore} es lo que lo hace usable: prometer que no se va a volver antes de cierto
 * punto libera todo lo anterior. Un lector que trabaja en franjas puede leer un archivo enorme con
 * memoria acotada, y por eso conviene llamarlo.
 *
 * <p>El flujo de abajo <b>no</b> se cierra al cerrar este.
 */
public class MemoryCacheImageInputStream extends ImageInputStreamImpl {

    /** Cuantos bytes tiene cada bloque de la cache. */
    private static final int BLOCK_SIZE = 8192;

    /** De donde se lee de verdad. */
    private InputStream stream;

    /** Los bloques guardados; el primero corresponde a {@link #cacheStart}. */
    private final ArrayList<byte[]> cache = new ArrayList<byte[]>();

    /** A que posicion del flujo corresponde el primer bloque guardado. */
    private long cacheStart = 0;

    /** Cuantos bytes se leyeron del flujo de abajo en total. */
    private long length = 0;

    /** Si el flujo de abajo se termino. */
    private boolean foundEOF = false;

    /**
     * @param stream de donde leer
     * @throws IllegalArgumentException si es null
     */
    public MemoryCacheImageInputStream(InputStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("stream == null!");
        }
        this.stream = stream;
    }

    /** Un byte. */
    @Override
    public int read() throws IOException {
        checkClosed();
        this.bitOffset = 0;
        if (!ensureAvailable(this.streamPos + 1)) {
            return -1;
        }
        int value = byteAt(this.streamPos) & 0xFF;
        this.streamPos = this.streamPos + 1;
        return value;
    }

    /** Hasta {@code len} bytes. */
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
        if (!ensureAvailable(this.streamPos + 1)) {
            return -1;
        }
        ensureAvailable(this.streamPos + len);
        int available = (int) Math.min((long) len, this.length - this.streamPos);
        int i = 0;
        while (i < available) {
            b[off + i] = byteAt(this.streamPos + i);
            i = i + 1;
        }
        this.streamPos = this.streamPos + available;
        return available;
    }

    /**
     * Libera todo lo anterior a esa posicion. Ver la nota de la clase.
     *
     * @throws IndexOutOfBoundsException si es anterior al descarte actual o posterior a la posicion
     */
    @Override
    public void flushBefore(long pos) throws IOException {
        super.flushBefore(pos);
        // Se tiran bloques enteros: liberar de a bytes obligaria a mover lo que queda.
        long firstNeeded = (pos / BLOCK_SIZE) * BLOCK_SIZE;
        while (this.cacheStart + BLOCK_SIZE <= firstNeeded && !this.cache.isEmpty()) {
            this.cache.remove(0);
            this.cacheStart = this.cacheStart + BLOCK_SIZE;
        }
    }

    /** Si. Guarda en memoria. */
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

    /** Cierra y suelta la cache. No cierra el flujo de abajo. */
    @Override
    public void close() throws IOException {
        super.close();
        this.cache.clear();
        this.stream = null;
    }

    /** Cierra si nadie lo hizo. */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    /**
     * Lee del flujo de abajo hasta tener esa cantidad de bytes guardados.
     *
     * @return si se llego; false si el flujo se termino antes
     */
    private boolean ensureAvailable(long needed) throws IOException {
        while (this.length < needed && !this.foundEOF) {
            byte[] block;
            int within = (int) ((this.length - this.cacheStart) % BLOCK_SIZE);
            if (within == 0) {
                block = new byte[BLOCK_SIZE];
                this.cache.add(block);
            } else {
                block = this.cache.get(this.cache.size() - 1);
            }
            int read = this.stream.read(block, within, BLOCK_SIZE - within);
            if (read <= 0) {
                this.foundEOF = true;
                // El bloque recien agregado quedo sin nada; se saca para que la cuenta de bloques
                // siga correspondiendo a los bytes guardados.
                if (within == 0 && !this.cache.isEmpty()) {
                    this.cache.remove(this.cache.size() - 1);
                }
                break;
            }
            this.length = this.length + read;
        }
        return this.length >= needed;
    }

    /** El byte guardado en esa posicion del flujo. */
    private byte byteAt(long pos) {
        long offset = pos - this.cacheStart;
        byte[] block = this.cache.get((int) (offset / BLOCK_SIZE));
        return block[(int) (offset % BLOCK_SIZE)];
    }
}
