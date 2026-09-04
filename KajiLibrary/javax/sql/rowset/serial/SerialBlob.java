package javax.sql.rowset.serial;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.SQLException;

/**
 * KajiLibrary's javax.sql.rowset.serial.SerialBlob -- una copia en memoria de un BLOB.
 *
 * <p>Un {@link Blob} de verdad es un <b>puntero</b> a datos que viven en el servidor y solo vale
 * mientras la transaccion que lo produjo siga abierta. Esta clase copia los bytes en memoria, y con
 * eso el dato sobrevive a la conexion, se puede serializar y se puede mandar por la red.
 *
 * <p>El precio es obvio y hay que decirlo: <b>todo</b> el contenido queda en memoria. Un BLOB de
 * varios gigabytes no entra, y ahi hay que quedarse con el puntero y leerlo de a partes.
 *
 * <h2>Las posiciones empiezan en 1</h2>
 *
 * <p>Es la convencion de SQL y no la de Java, y es la fuente clasica de errores en este paquete:
 * {@code getBytes(1, 10)} devuelve los diez primeros bytes, no del segundo al onceavo. Una posicion
 * 0 o negativa es un error.
 */
public class SerialBlob implements Blob, Serializable, Cloneable {

    private static final long serialVersionUID = -8144641928112860441L;

    /** La copia. */
    private byte[] buf;

    /** Cuantos bytes valen; puede ser menos que {@code buf.length} tras un truncado. */
    private long len;

    /** Null cuando ya se libero con {@link #free}. */
    private boolean freed = false;

    /** Copia esos bytes. */
    public SerialBlob(byte[] b) throws SerialException, SQLException {
        if (b == null) {
            throw new SQLException("Invalid Blob object. The byte array is null");
        }
        this.buf = new byte[b.length];
        System.arraycopy(b, 0, this.buf, 0, b.length);
        this.len = b.length;
    }

    /** Copia el contenido de un BLOB del servidor. */
    public SerialBlob(Blob blob) throws SerialException, SQLException {
        if (blob == null) {
            throw new SQLException("Cannot instantiate a SerialBlob object with a null Blob object");
        }
        long size = blob.length();
        byte[] bytes = blob.getBytes(1L, (int) size);
        this.buf = bytes;
        this.len = size;
    }

    /**
     * Una porcion.
     *
     * @param pos la primera posicion, empezando en 1
     * @throws SerialException si la porcion se sale del contenido
     */
    public byte[] getBytes(long pos, int length) throws SerialException {
        check();
        if (pos < 1 || pos > this.len) {
            throw new SerialException("Invalid position in BLOB object set");
        }
        int available = (int) (this.len - (pos - 1));
        int take = (length > available) ? available : length;
        if (take < 0) {
            throw new SerialException("Invalid length in BLOB object set");
        }
        byte[] out = new byte[take];
        System.arraycopy(this.buf, (int) (pos - 1), out, 0, take);
        return out;
    }

    /** Cuantos bytes tiene. */
    public long length() throws SerialException {
        check();
        return this.len;
    }

    /** Un flujo sobre la copia. */
    public InputStream getBinaryStream() throws SerialException {
        check();
        return new ByteArrayInputStream(this.buf, 0, (int) this.len);
    }

    /**
     * Busca ese patron a partir de esa posicion.
     *
     * @return la posicion donde empieza, empezando en 1, o -1
     */
    public long position(byte[] pattern, long start) throws SerialException, SQLException {
        check();
        if (start < 1 || start > this.len || pattern == null) {
            return -1;
        }
        int from = (int) (start - 1);
        int limit = (int) this.len - pattern.length;
        int i = from;
        while (i <= limit) {
            int j = 0;
            while (j < pattern.length && this.buf[i + j] == pattern[j]) {
                j = j + 1;
            }
            if (j == pattern.length) {
                return i + 1;
            }
            i = i + 1;
        }
        return -1;
    }

    /** Idem, con el patron en otro BLOB. */
    public long position(Blob pattern, long start) throws SerialException, SQLException {
        check();
        if (pattern == null) {
            return -1;
        }
        return position(pattern.getBytes(1L, (int) pattern.length()), start);
    }

    /** Escribe encima, desde esa posicion. */
    public int setBytes(long pos, byte[] bytes) throws SerialException, SQLException {
        return setBytes(pos, bytes, 0, bytes == null ? 0 : bytes.length);
    }

    /**
     * Escribe encima una porcion del arreglo.
     *
     * @throws SerialException si no entra en el contenido actual: esta copia no crece
     */
    public int setBytes(long pos, byte[] bytes, int offset, int length)
        throws SerialException, SQLException {
        check();
        if (bytes == null) {
            throw new SerialException("Invalid null value for bytes");
        }
        if (pos < 1 || pos > this.len) {
            throw new SerialException("Invalid position in BLOB object set");
        }
        if (offset < 0 || offset + length > bytes.length) {
            throw new SerialException("Invalid offset in byte array set");
        }
        if ((pos - 1) + length > this.len) {
            throw new SerialException("Buffer is not sufficient to hold the value");
        }
        System.arraycopy(bytes, offset, this.buf, (int) (pos - 1), length);
        return length;
    }

    /**
     * No se puede escribir por flujo.
     *
     * @throws SerialException siempre: un flujo de salida podria crecer, y esta copia tiene un
     *     tamano fijo desde que se construyo. Es lo que hace el JDK
     */
    public OutputStream setBinaryStream(long pos) throws SerialException, SQLException {
        throw new SerialException("Unsupported operation. SerialBlob cannot return a writable "
            + "binary stream, unless instantiated with a Blob object.");
    }

    /** Recorta a esa cantidad de bytes. */
    public void truncate(long length) throws SerialException {
        check();
        if (length > this.len) {
            throw new SerialException("Length more than what can be truncated");
        }
        if (length == 0) {
            this.buf = new byte[0];
            this.len = 0;
            return;
        }
        byte[] smaller = new byte[(int) length];
        System.arraycopy(this.buf, 0, smaller, 0, (int) length);
        this.buf = smaller;
        this.len = length;
    }

    /** Un flujo sobre una porcion. */
    public InputStream getBinaryStream(long pos, long length) throws SQLException {
        check();
        if (pos < 1 || pos > this.len) {
            throw new SerialException("Invalid position in BLOB object set");
        }
        if (length < 1 || length > this.len - pos + 1) {
            throw new SerialException(
                "length is < 1 or pos + length > total number of bytes");
        }
        return new ByteArrayInputStream(this.buf, (int) (pos - 1), (int) length);
    }

    /**
     * Suelta la copia.
     *
     * <p>Despues de esto cualquier otro metodo lanza. Es lo que hace el contrato de {@link Blob} y
     * tiene sentido aca aunque no haya recursos del servidor que soltar: libera la memoria, que en
     * un BLOB grande es justamente el recurso.
     */
    public void free() throws SQLException {
        this.buf = null;
        this.len = 0;
        this.freed = true;
    }

    /** Iguales si tienen los mismos bytes. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SerialBlob)) {
            return false;
        }
        SerialBlob that = (SerialBlob) obj;
        if (this.len != that.len) {
            return false;
        }
        if (this.buf == null || that.buf == null) {
            return this.buf == that.buf;
        }
        int i = 0;
        while (i < this.len) {
            if (this.buf[i] != that.buf[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /** Coherente con {@link #equals}. */
    public int hashCode() {
        int hash = 31;
        int i = 0;
        while (i < this.len) {
            hash = hash * 31 + this.buf[i];
            i = i + 1;
        }
        return hash;
    }

    /** Una copia con sus propios bytes. */
    public Object clone() {
        try {
            SerialBlob copy = new SerialBlob(new byte[0]);
            if (this.buf != null) {
                copy.buf = new byte[this.buf.length];
                System.arraycopy(this.buf, 0, copy.buf, 0, this.buf.length);
            } else {
                copy.buf = null;
            }
            copy.len = this.len;
            copy.freed = this.freed;
            return copy;
        } catch (SQLException e) {
            return null;
        }
    }

    /** Que no se haya liberado. */
    private void check() throws SerialException {
        if (this.freed || this.buf == null) {
            throw new SerialException("Error: You cannot call a method on a SerialBlob instance "
                + "once free() has been called.");
        }
    }
}
