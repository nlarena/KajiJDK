package javax.sql.rowset.spi;

import java.io.Writer;
import java.sql.SQLException;

import javax.sql.RowSetWriter;
import javax.sql.rowset.WebRowSet;

/**
 * El escritor que vuelca un {@link WebRowSet} a XML.
 *
 * <p>Es la contraparte de {@link XmlReader} y escribe lo mismo que aquel espera: propiedades,
 * metadatos, filas actuales y valores originales de las modificadas.
 *
 * <p>A diferencia de un {@link RowSetWriter} comun, esto no sincroniza con nada: no devuelve los
 * cambios al origen, los <strong>serializa</strong>. El destino es un documento, no una base.
 *
 * @since 1.5
 */
public interface XmlWriter extends RowSetWriter {

    /**
     * Escribe el conjunto entero como XML.
     *
     * @param caller el conjunto a escribir
     * @param writer adonde escribirlo
     * @throws SQLException si no se pudo escribir
     */
    void writeXML(WebRowSet caller, Writer writer) throws SQLException;
}
