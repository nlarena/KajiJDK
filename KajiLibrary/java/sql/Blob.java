package java.sql;

/**
 * KajiLibrary's java.sql.Blob -- un dato binario grande, **por referencia**.
 *
 * <p>La razon de que exista en vez de un `byte[]` esta en {@link #getBinaryStream}: un BLOB puede
 * pesar gigabytes, y traerlo entero para leer los primeros mil bytes seria absurdo. Este objeto es un
 * puntero del lado del cliente a un dato que sigue viviendo en la base, y solo se materializa lo que
 * se pide.
 *
 * <p>Eso explica {@link #free}: el puntero ata recursos del otro lado, y esperar al recolector para
 * soltarlos puede ser demasiado tarde.
 *
 * <p>Las posiciones se cuentan **desde uno**, como todo en JDBC.
 */
public interface Blob {

    /** Cuantos bytes tiene. */
    long length() throws SQLException;

    /** `length` bytes a partir de `pos`. */
    byte[] getBytes(long pos, int length) throws SQLException;

    /** Todo el contenido, como flujo. */
    java.io.InputStream getBinaryStream() throws SQLException;

    /** `length` bytes desde `pos`, como flujo. */
    java.io.InputStream getBinaryStream(long pos, long length) throws SQLException;

    /** Donde empieza `pattern` a partir de `start`, o -1. */
    long position(byte[] pattern, long start) throws SQLException;

    /** Igual, buscando el contenido de otro BLOB. */
    long position(Blob pattern, long start) throws SQLException;

    /** Escribe esos bytes en `pos`; devuelve cuantos escribio. */
    int setBytes(long pos, byte[] bytes) throws SQLException;

    /** Igual, tomando una porcion del arreglo. */
    int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException;

    /** Un flujo para escribir desde `pos`. */
    java.io.OutputStream setBinaryStream(long pos) throws SQLException;

    /** Lo recorta a `len` bytes. */
    void truncate(long len) throws SQLException;

    /** Suelta los recursos del puntero. */
    void free() throws SQLException;
}
