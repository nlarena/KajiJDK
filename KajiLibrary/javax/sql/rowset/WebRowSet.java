package javax.sql.rowset;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Un {@link CachedRowSet} que sabe escribirse y leerse como XML.
 *
 * <h2>Que se serializa, y por que no alcanza con las filas</h2>
 *
 * <p>El documento lleva tres cosas: las <strong>propiedades</strong> del conjunto (la consulta, la
 * fuente de datos, el tipo de cursor), los <strong>metadatos</strong> de cada columna, y los
 * <strong>datos</strong> — que a su vez incluyen, para las filas modificadas, el valor original
 * ademas del actual.
 *
 * <p>Esa ultima parte es la que hace que valga la pena. Un conjunto reconstruido del XML puede
 * sincronizar con la base igual que si nunca se hubiera movido, porque tiene contra que comparar.
 * Serializar solo las filas produciria algo que se puede mostrar y no se puede escribir.
 *
 * <h2>Para que sirve en concreto</h2>
 *
 * <p>Para mover un conjunto entre procesos que no comparten clases: un cliente pide datos, los
 * recibe como XML, los modifica sin conexion y devuelve el documento; el servidor lo reconstruye y
 * lo sincroniza. Es la version en texto de lo que {@code Serializable} hace en binario, con la
 * ventaja de que del otro lado puede no haber Java.
 *
 * <h2>Las dos formas de cada metodo</h2>
 *
 * <p>Hay version con {@code Reader}/{@code Writer} y con {@code InputStream}/{@code OutputStream}.
 * No son intercambiables: la de flujos de bytes es la correcta, porque un documento XML declara su
 * propia codificacion adentro y solo se puede respetar si se leen bytes. La de caracteres obliga a
 * que el que llama haya elegido bien la codificacion antes.
 *
 * @since 1.5
 */
public interface WebRowSet extends CachedRowSet {

    /** El identificador publico del esquema XML de un {@code WebRowSet}. */
    String PUBLIC_XML_SCHEMA = "--//Oracle Corporation//XSD Schema//EN";

    /** Donde vive el esquema. */
    String SCHEMA_SYSTEM_ID = "http://java.sun.com/xml/ns/jdbc/webrowset.xsd";

    /**
     * Llena el conjunto desde un documento XML.
     *
     * @param reader de donde leer
     * @throws SQLException si el documento esta mal formado o no corresponde al esquema
     */
    void readXml(Reader reader) throws SQLException;

    /**
     * Llena el conjunto desde un documento XML.
     *
     * @param iStream de donde leer
     * @throws SQLException si el documento no corresponde al esquema
     * @throws IOException si no se pudo leer
     */
    void readXml(InputStream iStream) throws SQLException, IOException;

    /**
     * Escribe como XML el contenido de un {@code ResultSet}.
     *
     * @param rs el resultado a escribir
     * @param writer adonde escribir
     * @throws SQLException si no se pudo leer el resultado o escribir el documento
     */
    void writeXml(ResultSet rs, Writer writer) throws SQLException;

    /**
     * Escribe como XML el contenido de un {@code ResultSet}.
     *
     * @param rs el resultado a escribir
     * @param oStream adonde escribir
     * @throws SQLException si no se pudo leer el resultado
     * @throws IOException si no se pudo escribir
     */
    void writeXml(ResultSet rs, OutputStream oStream) throws SQLException, IOException;

    /**
     * Escribe este conjunto como XML.
     *
     * @param writer adonde escribir
     * @throws SQLException si no se pudo escribir
     */
    void writeXml(Writer writer) throws SQLException;

    /**
     * Escribe este conjunto como XML.
     *
     * @param oStream adonde escribir
     * @throws SQLException si no se pudo armar el documento
     * @throws IOException si no se pudo escribir
     */
    void writeXml(OutputStream oStream) throws SQLException, IOException;
}
