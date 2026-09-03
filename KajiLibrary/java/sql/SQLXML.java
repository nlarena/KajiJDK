package java.sql;

/**
 * KajiLibrary's java.sql.SQLXML -- un valor XML de la base.
 *
 * <p>Se lee y se escribe **una sola vez**: en cuanto se pidio el contenido de una forma, las otras
 * quedan cerradas. Es lo que permite que la implementacion lo transmita en vez de guardarlo entero.
 */
public interface SQLXML {

    /** El contenido como texto. */
    String getString() throws SQLException;

    /** Fija el contenido desde un texto. */
    void setString(String value) throws SQLException;

    /** El contenido como flujo de bytes. */
    java.io.InputStream getBinaryStream() throws SQLException;

    /** Un flujo para escribirlo. */
    java.io.OutputStream setBinaryStream() throws SQLException;

    /** El contenido como lector. */
    java.io.Reader getCharacterStream() throws SQLException;

    /** Un escritor para escribirlo. */
    java.io.Writer setCharacterStream() throws SQLException;

    /**
     * El contenido como la clase de {@link javax.xml.transform.Source} que se pida.
     *
     * <p>Recibe la clase en vez de tener una sobrecarga por representacion, y devuelve **esa** clase
     * y no la interfaz: es lo que evita el molde en el llamador, que es justo donde un molde seria
     * un error de ejecucion y no de compilacion.
     *
     * @param sourceClass la clase pedida, o `null` para la que el driver prefiera
     */
    <T extends javax.xml.transform.Source> T getSource(Class<T> sourceClass) throws SQLException;

    /** El espejo del anterior para escribir. */
    <T extends javax.xml.transform.Result> T setResult(Class<T> resultClass) throws SQLException;

    /** Suelta los recursos del puntero. */
    void free() throws SQLException;
}
