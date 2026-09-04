package javax.imageio.stream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * KajiLibrary's javax.imageio.stream.FileCacheImageOutputStream -- escribe a un flujo cualquiera,
 * juntando en un archivo temporal.
 *
 * <p>La combinacion de {@link FileCacheImageInputStream} y
 * {@link MemoryCacheImageOutputStream}: se puede reposicionar y corregir libremente porque todo pasa
 * primero por un archivo en disco, y {@link #flushBefore} suelta lo que ya no se va a tocar.
 *
 * <p>Como en la version de memoria, <b>nada llega al flujo de abajo</b> hasta que se llama a
 * {@code flushBefore} o a {@link #close}.
 *
 * <p>El temporal se borra al cerrar; el flujo de abajo no se cierra.
 */
public class FileCacheImageOutputStream extends ImageOutputStreamImpl {

    /** A donde va lo que se suelta. */
    private OutputStream stream;

    /** Donde se junta. */
    private File cacheFile;

    /** El temporal, abierto. */
    private RandomAccessFile cache;

    /** Hasta donde se escribio. */
    private long length = 0;

    /**
     * @param stream a donde escribir
     * @param cacheDir donde poner el temporal, o null para el del sistema
     * @throws IllegalArgumentException si el flujo es null, o si el directorio no lo es
     * @throws IOException si no se pudo crear el temporal
     */
    public FileCacheImageOutputStream(OutputStream stream, File cacheDir) throws IOException {
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

    /** Un byte de lo ya escrito. */
    @Override
    public int read() throws IOException {
        checkClosed();
        this.bitOffset = 0;
        if (this.streamPos >= this.length) {
            return -1;
        }
        this.cache.seek(this.streamPos);
        int value = this.cache.read();
        if (value != -1) {
            this.streamPos = this.streamPos + 1;
        }
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
        this.cache.seek(this.streamPos);
        int toRead = (int) Math.min((long) len, this.length - this.streamPos);
        int nbytes = this.cache.read(b, off, toRead);
        if (nbytes > 0) {
            this.streamPos = this.streamPos + nbytes;
        }
        return nbytes;
    }

    /** Un byte. */
    @Override
    public void write(int b) throws IOException {
        flushBits();
        this.cache.seek(this.streamPos);
        this.cache.write(b);
        this.streamPos = this.streamPos + 1;
        if (this.streamPos > this.length) {
            this.length = this.streamPos;
        }
    }

    /** Esa parte del arreglo. */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        flushBits();
        this.cache.seek(this.streamPos);
        this.cache.write(b, off, len);
        this.streamPos = this.streamPos + len;
        if (this.streamPos > this.length) {
            this.length = this.streamPos;
        }
    }

    /** Cuanto se escribio. */
    @Override
    public long length() {
        return this.length;
    }

    /** Se posiciona; se puede pasar del final. */
    @Override
    public void seek(long pos) throws IOException {
        checkClosed();
        if (pos < this.flushedPos) {
            throw new IndexOutOfBoundsException("pos < flushedPos!");
        }
        this.bitOffset = 0;
        this.streamPos = pos;
    }

    /** Si. */
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

    /**
     * Suelta al flujo de abajo todo lo anterior a esa posicion.
     *
     * @throws IndexOutOfBoundsException si es anterior al descarte actual o posterior a la posicion
     */
    @Override
    public void flushBefore(long pos) throws IOException {
        long oldFlushed = getFlushedPosition();
        super.flushBefore(pos);
        if (pos <= oldFlushed) {
            return;
        }
        byte[] buf = new byte[8192];
        long at = oldFlushed;
        this.cache.seek(at);
        while (at < pos) {
            int toRead = (int) Math.min((long) buf.length, pos - at);
            int read = this.cache.read(buf, 0, toRead);
            if (read <= 0) {
                break;
            }
            this.stream.write(buf, 0, read);
            at = at + read;
        }
        this.stream.flush();
    }

    /** Suelta lo pendiente, cierra y borra el temporal. */
    @Override
    public void close() throws IOException {
        try {
            flushBits();
        } catch (IOException e) {
            // Ya se esta cerrando.
        }
        long pos = this.length;
        seek(pos);
        flushBefore(pos);
        super.close();
        this.cache.close();
        this.cache = null;
        this.cacheFile.delete();
        this.cacheFile = null;
        this.stream = null;
    }
}
