package javax.imageio.stream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * KajiLibrary's javax.imageio.stream.FileImageInputStream -- lee un archivo, con acceso directo.
 *
 * <p>La implementacion mas simple y la mas eficiente: sobre un {@link RandomAccessFile} no hace falta
 * guardar nada para poder volver atras, porque el archivo ya se puede posicionar.
 *
 * <p>Por eso {@link #isCached} devuelve false. No es una carencia: es que no hay nada que cachear.
 *
 * <h2>Quien cierra que</h2>
 *
 * <p>Los dos constructores se comportan distinto y no esta escrito en ningun lado obvio:
 *
 * <ul>
 *   <li>el que toma un {@link File} abre el archivo y {@link #close} lo cierra;
 *   <li>el que toma un {@link RandomAccessFile} <b>tambien</b> lo cierra, aunque no lo haya abierto
 *       el. Es lo que hace el JDK, y hay que tenerlo presente si el archivo se comparte.
 * </ul>
 */
public class FileImageInputStream extends ImageInputStreamImpl {

    /** El archivo. */
    private RandomAccessFile raf;

    /**
     * Abre ese archivo para leer.
     *
     * @throws IllegalArgumentException si es null
     * @throws FileNotFoundException si no existe o no se puede leer
     * @throws IOException si fallo al abrirlo
     */
    public FileImageInputStream(File f) throws FileNotFoundException, IOException {
        if (f == null) {
            throw new IllegalArgumentException("f == null!");
        }
        this.raf = new RandomAccessFile(f, "r");
    }

    /**
     * Usa ese archivo ya abierto. Ver la nota de la clase sobre quien lo cierra.
     *
     * @throws IllegalArgumentException si es null
     */
    public FileImageInputStream(RandomAccessFile raf) {
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

    /** El tamano del archivo, o -1 si no se puede saber. */
    @Override
    public long length() {
        try {
            checkClosed();
            return this.raf.length();
        } catch (IOException e) {
            return -1L;
        }
    }

    /** Se posiciona; el archivo tambien. */
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

    /** Cierra, y cierra el archivo. Ver la nota de la clase. */
    @Override
    public void close() throws IOException {
        super.close();
        this.raf.close();
        this.raf = null;
    }

    /** Cierra si nadie lo hizo; ver {@link ImageInputStreamImpl#finalize}. */
    @Override
    protected void finalize() throws Throwable {
        // La clase base ya cierra lo suyo; aca no hay nada mas que hacer que dejarla trabajar.
        super.finalize();
    }
}
