package javax.imageio.stream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * KajiLibrary's javax.imageio.stream.FileImageOutputStream -- escribe un archivo, con acceso directo.
 *
 * <p>El espejo de {@link FileImageInputStream}, sobre un {@link RandomAccessFile} abierto para lectura
 * y escritura. Es la implementacion que hace facil el patron de "escribo el encabezado con un largo
 * que no se, escribo la imagen, vuelvo y corrijo".
 *
 * <p>El archivo se abre en modo {@code "rw"}: si no existe se crea, y si existe <b>no se trunca</b>.
 * Escribir un archivo mas corto que el anterior deja la cola del viejo pegada al final.
 *
 * <p>Ver {@link FileImageInputStream} sobre quien cierra que.
 */
public class FileImageOutputStream extends ImageOutputStreamImpl {

    /** El archivo. */
    private RandomAccessFile raf;

    /**
     * Abre ese archivo para leer y escribir.
     *
     * @throws IllegalArgumentException si es null
     * @throws FileNotFoundException si no se puede abrir
     * @throws IOException si fallo
     */
    public FileImageOutputStream(File f) throws FileNotFoundException, IOException {
        if (f == null) {
            throw new IllegalArgumentException("f == null!");
        }
        this.raf = new RandomAccessFile(f, "rw");
    }

    /**
     * Usa ese archivo ya abierto.
     *
     * @throws IllegalArgumentException si es null
     */
    public FileImageOutputStream(RandomAccessFile raf) {
        if (raf == null) {
            throw new IllegalArgumentException("raf == null!");
        }
        this.raf = raf;
    }

    /** Un byte. */
    @Override
    public int read() throws IOException {
        checkClosed();
        this.bitOffset = 0;
        int val = this.raf.read();
        if (val != -1) {
            this.streamPos = this.streamPos + 1;
        }
        return val;
    }

    /** Hasta {@code len} bytes. */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkClosed();
        this.bitOffset = 0;
        int nbytes = this.raf.read(b, off, len);
        if (nbytes != -1) {
            this.streamPos = this.streamPos + nbytes;
        }
        return nbytes;
    }

    /** Un byte; cierra antes el byte de bits pendiente. */
    @Override
    public void write(int b) throws IOException {
        flushBits();
        this.raf.write(b);
        this.streamPos = this.streamPos + 1;
    }

    /** Esa parte del arreglo. */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        flushBits();
        this.raf.write(b, off, len);
        this.streamPos = this.streamPos + len;
    }

    /** El tamano del archivo, o -1. */
    @Override
    public long length() {
        try {
            checkClosed();
            return this.raf.length();
        } catch (IOException e) {
            return -1L;
        }
    }

    /**
     * Se posiciona.
     *
     * <p>Se puede pasar del final: el archivo crece con ceros al escribir ahi. Es lo que permite
     * reservar espacio para un encabezado y llenarlo despues.
     */
    @Override
    public void seek(long pos) throws IOException {
        checkClosed();
        if (pos < this.flushedPos) {
            throw new IndexOutOfBoundsException("pos < flushedPos!");
        }
        this.bitOffset = 0;
        this.raf.seek(pos);
        this.streamPos = this.raf.getFilePointer();
    }

    /** Cierra el byte pendiente, cierra el flujo y el archivo. */
    @Override
    public void close() throws IOException {
        try {
            flushBits();
        } catch (IOException e) {
            // Ya se esta cerrando; no hay nada mejor que hacer que seguir cerrando.
        }
        super.close();
        this.raf.close();
        this.raf = null;
    }

    /** Cierra si nadie lo hizo. */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
}
