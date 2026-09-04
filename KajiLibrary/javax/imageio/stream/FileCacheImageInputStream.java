package javax.imageio.stream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * KajiLibrary's javax.imageio.stream.FileCacheImageInputStream -- lee un flujo cualquiera, guardando
 * en un archivo temporal.
 *
 * <p>La alternativa a {@link MemoryCacheImageInputStream} cuando lo que se lee no entra en memoria:
 * en lugar de juntar en el monton, junta en disco.
 *
 * <p>El archivo temporal se crea en el directorio que se pase, o en el del sistema si se pasa null.
 * Se borra al cerrar.
 *
 * <p>El compromiso es el de siempre: mas lento, memoria acotada. Un lector que procesa imagenes de
 * cientos de megabytes desde la red quiere esta; uno que lee miniaturas quiere la otra.
 *
 * <p>El flujo de abajo no se cierra al cerrar este.
 */
public class FileCacheImageInputStream extends ImageInputStreamImpl {

    /** De donde se lee de verdad. */
    private InputStream stream;

    /** Donde se guarda lo leido. */
    private File cacheFile;

    /** El archivo temporal, abierto. */
    private RandomAccessFile cache;

    /** Cuantos bytes se juntaron. */
    private long length = 0;

    /** Si el flujo de abajo se termino. */
    private boolean foundEOF = false;

    /**
     * @param stream de donde leer
     * @param cacheDir donde poner el temporal, o null para el del sistema
     * @throws IllegalArgumentException si el flujo es null, o si el directorio no lo es
     * @throws IOException si no se pudo crear el temporal
     */
    public FileCacheImageInputStream(InputStream stream, File cacheDir) throws IOException {
        if (stream == null) {
            throw new IllegalArgumentException("stream == null!");
        }
        if (cacheDir != null && !cacheDir.isDirectory()) {
            throw new IllegalArgumentException("Not a directory!");
        }
        this.stream = stream;
        this.cacheFile = File.createTempFile("imageio", ".tmp", cacheDir);
        this.cache = new RandomAccessFile(this.cacheFile, "rw");
    }

    /** Un byte. */
    @Override
    public int read() throws IOException {
        checkClosed();
        this.bitOffset = 0;
        if (!ensureAvailable(this.streamPos + 1)) {
            return -1;
        }
        this.cache.seek(this.streamPos);
        int value = this.cache.read();
        if (value != -1) {
            this.streamPos = this.streamPos + 1;
        }
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
        this.cache.seek(this.streamPos);
        int nbytes = this.cache.read(b, off, len);
        if (nbytes > 0) {
            this.streamPos = this.streamPos + nbytes;
        }
        return nbytes;
    }

    /** Si. Guarda en un archivo. */
    @Override
    public boolean isCached() {
        return true;
    }

    /** Si. */
    @Override
    public boolean isCachedFile() {
        return true;
    }

    /** No. */
    @Override
    public boolean isCachedMemory() {
        return false;
    }

    /** Cierra y borra el temporal. No cierra el flujo de abajo. */
    @Override
    public void close() throws IOException {
        super.close();
        this.cache.close();
        this.cache = null;
        // Borrar el temporal es parte del contrato: si no, un proceso que lea muchas imagenes va
        // llenando el directorio temporal sin que nada lo avise.
        this.cacheFile.delete();
        this.cacheFile = null;
        this.stream = null;
    }

    /** Cierra si nadie lo hizo. */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    /**
     * Copia del flujo de abajo al temporal hasta tener esa cantidad.
     *
     * @return si se llego
     */
    private boolean ensureAvailable(long needed) throws IOException {
        if (this.length >= needed || this.foundEOF) {
            return this.length >= needed;
        }
        byte[] buf = new byte[8192];
        this.cache.seek(this.length);
        while (this.length < needed) {
            int toRead = (int) Math.min((long) buf.length, needed - this.length);
            int read = this.stream.read(buf, 0, toRead);
            if (read <= 0) {
                this.foundEOF = true;
                break;
            }
            this.cache.write(buf, 0, read);
            this.length = this.length + read;
        }
        return this.length >= needed;
    }
}
