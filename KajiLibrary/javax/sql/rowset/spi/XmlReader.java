package javax.sql.rowset.spi;

import java.io.Reader;
import java.sql.SQLException;

import javax.sql.RowSetReader;
import javax.sql.rowset.WebRowSet;

/**
 * El lector que llena un {@link WebRowSet} desde XML.
 *
 * <p>Lo que lee no son solo los datos: el documento de un {@code WebRowSet} trae ademas las
 * propiedades del conjunto, sus metadatos de columna y —lo que lo hace util— los valores
 * <strong>originales</strong> de las filas modificadas. Sin eso, el conjunto reconstruido no podria
 * detectar conflictos al volver al origen, porque no sabria contra que comparar.
 *
 * <p>Es lo que permite que un {@code RowSet} viaje por la red como texto y llegue del otro lado
 * pudiendo sincronizar igual que si no se hubiera movido.
 *
 * @since 1.5
 */
public interface XmlReader extends RowSetReader {

    /**
     * Llena el conjunto con lo que haya en el flujo.
     *
     * @param caller el conjunto a llenar
     * @param reader de donde leer el XML
     * @throws SQLException si el documento esta mal formado o no corresponde al esquema
     */
    void readXML(WebRowSet caller, Reader reader) throws SQLException;
}
