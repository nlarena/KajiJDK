package java.sql;

/**
 * KajiLibrary's java.sql.Clob -- un texto grande, por referencia.
 *
 * <p>Lo mismo que {@link Blob} para caracteres, y la diferencia importa: aca las posiciones y las
 * longitudes se cuentan en **caracteres**, no en bytes, asi que dependen de la codificacion de la
 * base. Confundir las dos unidades es el error clasico con esta interfaz.
 */
public interface Clob {

    /** Cuantos caracteres tiene. */
    long length() throws SQLException;

    /** `length` caracteres a partir de `pos`. */
    String getSubString(long pos, int length) throws SQLException;

    /** Todo el contenido, como lector. */
    java.io.Reader getCharacterStream() throws SQLException;

    /** `length` caracteres desde `pos`. */
    java.io.Reader getCharacterStream(long pos, long length) throws SQLException;

    /** El contenido como flujo ASCII. */
    java.io.InputStream getAsciiStream() throws SQLException;

    /** Donde empieza `searchstr` a partir de `start`, o -1. */
    long position(String searchstr, long start) throws SQLException;

    /** Igual, buscando el contenido de otro CLOB. */
    long position(Clob searchstr, long start) throws SQLException;

    /** Escribe ese texto en `pos`; devuelve cuantos caracteres escribio. */
    int setString(long pos, String str) throws SQLException;

    /** Igual, tomando una porcion. */
    int setString(long pos, String str, int offset, int len) throws SQLException;

    /** Un flujo ASCII para escribir desde `pos`. */
    java.io.OutputStream setAsciiStream(long pos) throws SQLException;

    /** Un escritor para escribir desde `pos`. */
    java.io.Writer setCharacterStream(long pos) throws SQLException;

    /** Lo recorta a `len` caracteres. */
    void truncate(long len) throws SQLException;

    /** Suelta los recursos del puntero. */
    void free() throws SQLException;
}
