package javax.sql.rowset.serial;

import java.io.CharArrayReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.sql.Clob;
import java.sql.SQLException;

/**
 * KajiLibrary's javax.sql.rowset.serial.SerialClob -- una copia en memoria de un CLOB.
 *
 * <p>Lo mismo que {@link SerialBlob} pero con caracteres. Valen las mismas dos advertencias: todo el
 * contenido queda en memoria, y las posiciones empiezan en 1.
 *
 * <p>La diferencia que importa: guarda {@code char[]} y no bytes, asi que la codificacion ya se
 * resolvio al copiarlo. Por eso {@link #getAsciiStream} tiene que volver a codificar, y solo sirve
 * si el contenido es ASCII de verdad -- con un acento adentro, lo que sale no es lo que entro.
 */
public class SerialClob implements Clob, Serializable, Cloneable {

    private static final long serialVersionUID = -1662519690087375313L;

    /** La copia. */
    private char[] buf;

    /** Cuantos caracteres valen. */
    private long len;

    /** Si ya se libero. */
    private boolean freed = false;

    /** Copia esos caracteres. */
    public SerialClob(char[] ch) throws SerialException, SQLException {
        if (ch == null) {
            throw new SQLException("Invalid Clob object. The char array is null");
        }
        this.buf = new char[ch.length];
        System.arraycopy(ch, 0, this.buf, 0, ch.length);
        this.len = ch.length;
    }

    /** Copia el contenido de un CLOB del servidor. */
    public SerialClob(Clob clob) throws SerialException, SQLException {
        if (clob == null) {
            throw new SQLException("Cannot instantiate a SerialClob object with a null Clob object");
        }
        long size = clob.length();
        String text = clob.getSubString(1L, (int) size);
        this.buf = text.toCharArray();
        this.len = size;
    }

    /** Cuantos caracteres tiene. */
    public long length() throws SerialException {
        check();
        return this.len;
    }

    /** Un lector sobre la copia. */
    public Reader getCharacterStream() throws SerialException {
        check();
        return new CharArrayReader(this.buf, 0, (int) this.len);
    }

    /**
     * Un flujo de bytes, tomando cada caracter como un byte.
     *
     * <p>Ver la nota de la clase: solo sirve si el contenido es ASCII.
     */
    public InputStream getAsciiStream() throws SerialException, SQLException {
        check();
        byte[] bytes = new byte[(int) this.len];
        int i = 0;
        while (i < this.len) {
            bytes[i] = (byte) this.buf[i];
            i = i + 1;
        }
        return new java.io.ByteArrayInputStream(bytes);
    }

    /**
     * Una porcion, como cadena.
     *
     * @param pos la primera posicion, empezando en 1
     */
    public String getSubString(long pos, int length) throws SerialException {
        check();
        if (pos < 1 || pos > this.len) {
            throw new SerialException("Invalid position in SerialClob object set");
        }
        if (length < 0 || (pos - 1) + length > this.len) {
            throw new SerialException("Invalid position and substring length");
        }
        return new String(this.buf, (int) (pos - 1), length);
    }

    /**
     * Busca ese texto a partir de esa posicion.
     *
     * @return la posicion donde empieza, empezando en 1, o -1
     */
    public long position(String searchStr, long start) throws SerialException, SQLException {
        check();
        if (start < 1 || start > this.len || searchStr == null) {
            return -1;
        }
        String whole = new String(this.buf, 0, (int) this.len);
        int found = whole.indexOf(searchStr, (int) (start - 1));
        return (found < 0) ? -1 : found + 1;
    }

    /** Idem, con el texto en otro CLOB. */
    public long position(Clob searchStr, long start) throws SerialException, SQLException {
        check();
        if (searchStr == null) {
            return -1;
        }
        return position(searchStr.getSubString(1L, (int) searchStr.length()), start);
    }

    /** Escribe encima, desde esa posicion. */
    public int setString(long pos, String str) throws SerialException {
        return setString(pos, str, 0, str == null ? 0 : str.length());
    }

    /**
     * Escribe encima una porcion del texto.
     *
     * @throws SerialException si no entra en el contenido actual: esta copia no crece
     */
    public int setString(long pos, String str, int offset, int length) throws SerialException {
        check();
        if (str == null) {
            throw new SerialException("Invalid null value for string");
        }
        if (pos < 1 || pos > this.len) {
            throw new SerialException("Invalid position in Clob object set");
        }
        if (offset < 0 || offset + length > str.length()) {
            throw new SerialException("Invalid offset in string set");
        }
        if ((pos - 1) + length > this.len) {
            throw new SerialException(
                "Buffer is not sufficient to hold the value");
        }
        str.getChars(offset, offset + length, this.buf, (int) (pos - 1));
        return length;
    }

    /**
     * No se puede escribir por flujo.
     *
     * @throws SerialException siempre; ver {@link SerialBlob#setBinaryStream}
     */
    public OutputStream setAsciiStream(long pos) throws SerialException, SQLException {
        throw new SerialException("Unsupported operation. SerialClob cannot return a writable "
            + "ascii stream, unless instantiated with a Clob object.");
    }

    /**
     * No se puede escribir por escritor.
     *
     * @throws SerialException siempre
     */
    public Writer setCharacterStream(long pos) throws SerialException, SQLException {
        throw new SerialException("Unsupported operation. SerialClob cannot return a writable "
            + "character stream, unless instantiated with a Clob object.");
    }

    /** Recorta a esa cantidad de caracteres. */
    public void truncate(long length) throws SerialException {
        check();
        if (length > this.len) {
            throw new SerialException("Length more than what can be truncated");
        }
        if (length == 0) {
            this.buf = new char[0];
            this.len = 0;
            return;
        }
        char[] smaller = new char[(int) length];
        System.arraycopy(this.buf, 0, smaller, 0, (int) length);
        this.buf = smaller;
        this.len = length;
    }

    /** Un lector sobre una porcion. */
    public Reader getCharacterStream(long pos, long length) throws SQLException {
        check();
        if (pos < 1 || pos > this.len) {
            throw new SerialException("Invalid position in SerialClob object set");
        }
        if (length < 1 || length > this.len - pos + 1) {
            throw new SerialException("Invalid length or pos + length > total number of characters");
        }
        return new CharArrayReader(this.buf, (int) (pos - 1), (int) length);
    }

    /** Suelta la copia; ver {@link SerialBlob#free}. */
    public void free() throws SQLException {
        this.buf = null;
        this.len = 0;
        this.freed = true;
    }

    /** Iguales si tienen los mismos caracteres. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SerialClob)) {
            return false;
        }
        SerialClob that = (SerialClob) obj;
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

    /** Una copia con sus propios caracteres. */
    public Object clone() {
        try {
            SerialClob copy = new SerialClob(new char[0]);
            if (this.buf != null) {
                copy.buf = new char[this.buf.length];
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
            throw new SerialException("Error: You cannot call a method on a SerialClob instance "
                + "once free() has been called.");
        }
    }
}
