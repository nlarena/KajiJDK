package javax.sql.rowset;

import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Types;

import javax.sql.RowSetMetaData;

/**
 * Los metadatos de las columnas de un {@code RowSet}, en su version escribible.
 *
 * <h2>Por que hace falta una version escribible</h2>
 *
 * <p>Los metadatos de un {@code ResultSet} son de solo lectura porque los produce el controlador:
 * el que consulta no los inventa, los recibe. Un {@code RowSet} desconectado, en cambio, se puede
 * llenar a mano —sin base de datos de por medio— y ahi <strong>alguien tiene que declarar</strong>
 * cuantas columnas hay, como se llaman y de que tipo son.
 *
 * <p>Esta clase es ese alguien. Se le fijan las columnas y despues se la pasa a
 * {@link CachedRowSet#setMetaData}.
 *
 * <h2>El orden obligatorio</h2>
 *
 * <p>{@link #setColumnCount} tiene que ir primero. Antes de saber cuantas columnas hay no hay donde
 * guardar nada, y cualquier otro {@code set} falla con un indice fuera de rango. Volver a llamarlo
 * descarta lo que se habia fijado.
 *
 * <h2>Las columnas se cuentan desde 1</h2>
 *
 * <p>Es la convencion de todo JDBC y viene de SQL, no de Java. Adentro se guarda en arreglos que
 * empiezan en cero, y esa resta es el unico lugar donde el desfasaje aparece.
 *
 * <h2>Los tres metodos que no se pueden fijar</h2>
 *
 * <p>{@link #isReadOnly}, {@link #isWritable} y {@link #isDefinitelyWritable} no tienen un
 * {@code set} correspondiente en {@link RowSetMetaData}, asi que contestan lo unico razonable para
 * un conjunto que se lleno a mano: escribible. Es lo que hace el JDK y es coherente — quien armo
 * las columnas puede escribirlas.
 *
 * @since 1.5
 */
public class RowSetMetaDataImpl implements RowSetMetaData, Serializable {

    private static final long serialVersionUID = 6893806403181801867L;

    private int colCount;
    private ColInfo[] colInfo;

    /** Lo que se guarda de cada columna. */
    private static class ColInfo implements Serializable {
        private static final long serialVersionUID = 5490834817919311283L;
        boolean autoIncrement;
        boolean caseSensitive;
        boolean currency;
        boolean searchable;
        boolean signed;
        int nullable = columnNullableUnknown;
        int columnDisplaySize;
        String columnLabel;
        String columnName;
        String schemaName = "";
        int colPrecision;
        int colScale;
        String tableName = "";
        String catName = "";
        int colType;
        String colTypeName = "";
    }

    /** Sin columnas; hay que llamar a {@link #setColumnCount} antes de nada. */
    public RowSetMetaDataImpl() {
    }

    /**
     * Valida el indice y devuelve la posicion del arreglo interno.
     *
     * <p>La validacion esta centralizada a proposito: son treinta y pico de metodos que reciben un
     * indice, y repetir el chequeo en cada uno garantiza que alguno quede sin el.
     */
    private ColInfo col(final int columnIndex) throws SQLException {
        if (colInfo == null) {
            throw new SQLException("hay que llamar a setColumnCount antes de usar los metadatos");
        }
        if (columnIndex < 1 || columnIndex > colCount) {
            throw new SQLException("indice de columna fuera de rango: " + columnIndex
                    + " (hay " + colCount + ")");
        }
        return colInfo[columnIndex - 1];
    }

    /**
     * Cuantas columnas va a tener.
     *
     * <p>Descarta lo que se hubiera fijado antes.
     *
     * @param columnCount la cantidad
     * @throws SQLException si es negativa
     */
    public void setColumnCount(final int columnCount) throws SQLException {
        if (columnCount < 0) {
            throw new SQLException("la cantidad de columnas no puede ser negativa");
        }
        this.colCount = columnCount;
        // Uno de mas, como el JDK. La ultima posicion no se usa: `col` valida el rango y despues
        // resta uno, asi que el indice mas alto que se alcanza es columnCount - 1.
        this.colInfo = new ColInfo[columnCount + 1];
        for (int i = 0; i < colInfo.length; i++) {
            colInfo[i] = new ColInfo();
        }
    }

    /**
     * Si la columna se numera sola.
     *
     * @param columnIndex la columna, desde 1
     * @param property si es autoincremental
     * @throws SQLException si el indice no es valido
     */
    public void setAutoIncrement(final int columnIndex, final boolean property)
            throws SQLException {
        col(columnIndex).autoIncrement = property;
    }

    /**
     * Si al comparar se distinguen mayusculas de minusculas.
     *
     * @param columnIndex la columna, desde 1
     * @param property si distingue
     * @throws SQLException si el indice no es valido
     */
    public void setCaseSensitive(final int columnIndex, final boolean property)
            throws SQLException {
        col(columnIndex).caseSensitive = property;
    }

    /**
     * Si la columna puede aparecer en un {@code WHERE}.
     *
     * @param columnIndex la columna, desde 1
     * @param property si es buscable
     * @throws SQLException si el indice no es valido
     */
    public void setSearchable(final int columnIndex, final boolean property) throws SQLException {
        col(columnIndex).searchable = property;
    }

    /**
     * Si la columna es un valor monetario.
     *
     * @param columnIndex la columna, desde 1
     * @param property si es moneda
     * @throws SQLException si el indice no es valido
     */
    public void setCurrency(final int columnIndex, final boolean property) throws SQLException {
        col(columnIndex).currency = property;
    }

    /**
     * Si la columna admite nulos.
     *
     * @param columnIndex la columna, desde 1
     * @param property una de las constantes {@code columnNo*} de {@code ResultSetMetaData}
     * @throws SQLException si el indice no es valido o la constante no lo es
     */
    public void setNullable(final int columnIndex, final int property) throws SQLException {
        if (property < columnNoNulls || property > columnNullableUnknown) {
            throw new SQLException("valor de nulabilidad invalido: " + property);
        }
        col(columnIndex).nullable = property;
    }

    /**
     * Si el valor tiene signo.
     *
     * @param columnIndex la columna, desde 1
     * @param property si tiene signo
     * @throws SQLException si el indice no es valido
     */
    public void setSigned(final int columnIndex, final boolean property) throws SQLException {
        col(columnIndex).signed = property;
    }

    /**
     * El ancho normal de la columna, en caracteres.
     *
     * @param columnIndex la columna, desde 1
     * @param size el ancho
     * @throws SQLException si el indice no es valido o el ancho es negativo
     */
    public void setColumnDisplaySize(final int columnIndex, final int size) throws SQLException {
        if (size < 0) {
            throw new SQLException("el ancho no puede ser negativo");
        }
        col(columnIndex).columnDisplaySize = size;
    }

    /**
     * El titulo con el que mostrar la columna.
     *
     * @param columnIndex la columna, desde 1
     * @param label el titulo
     * @throws SQLException si el indice no es valido
     */
    public void setColumnLabel(final int columnIndex, final String label) throws SQLException {
        col(columnIndex).columnLabel = label == null ? "" : label;
    }

    /**
     * El nombre de la columna.
     *
     * @param columnIndex la columna, desde 1
     * @param columnName el nombre
     * @throws SQLException si el indice no es valido
     */
    public void setColumnName(final int columnIndex, final String columnName) throws SQLException {
        col(columnIndex).columnName = columnName == null ? "" : columnName;
    }

    /**
     * El esquema de la tabla de la columna.
     *
     * @param columnIndex la columna, desde 1
     * @param schemaName el esquema
     * @throws SQLException si el indice no es valido
     */
    public void setSchemaName(final int columnIndex, final String schemaName) throws SQLException {
        col(columnIndex).schemaName = schemaName == null ? "" : schemaName;
    }

    /**
     * Cuantos digitos significativos tiene.
     *
     * @param columnIndex la columna, desde 1
     * @param precision la precision
     * @throws SQLException si el indice no es valido o la precision es negativa
     */
    public void setPrecision(final int columnIndex, final int precision) throws SQLException {
        if (precision < 0) {
            throw new SQLException("la precision no puede ser negativa");
        }
        col(columnIndex).colPrecision = precision;
    }

    /**
     * Cuantos digitos hay a la derecha del punto.
     *
     * @param columnIndex la columna, desde 1
     * @param scale la escala
     * @throws SQLException si el indice no es valido o la escala es negativa
     */
    public void setScale(final int columnIndex, final int scale) throws SQLException {
        if (scale < 0) {
            throw new SQLException("la escala no puede ser negativa");
        }
        col(columnIndex).colScale = scale;
    }

    /**
     * La tabla de la que sale la columna.
     *
     * @param columnIndex la columna, desde 1
     * @param tableName la tabla
     * @throws SQLException si el indice no es valido
     */
    public void setTableName(final int columnIndex, final String tableName) throws SQLException {
        col(columnIndex).tableName = tableName == null ? "" : tableName;
    }

    /**
     * El catalogo de la tabla.
     *
     * @param columnIndex la columna, desde 1
     * @param catalogName el catalogo
     * @throws SQLException si el indice no es valido
     */
    public void setCatalogName(final int columnIndex, final String catalogName)
            throws SQLException {
        col(columnIndex).catName = catalogName == null ? "" : catalogName;
    }

    /**
     * El tipo SQL de la columna.
     *
     * @param columnIndex la columna, desde 1
     * @param SQLType una constante de {@link Types}
     * @throws SQLException si el indice no es valido
     */
    public void setColumnType(final int columnIndex, final int SQLType) throws SQLException {
        col(columnIndex).colType = SQLType;
    }

    /**
     * El nombre del tipo, tal como lo llama la base.
     *
     * @param columnIndex la columna, desde 1
     * @param typeName el nombre del tipo
     * @throws SQLException si el indice no es valido
     */
    public void setColumnTypeName(final int columnIndex, final String typeName)
            throws SQLException {
        col(columnIndex).colTypeName = typeName == null ? "" : typeName;
    }

    /**
     * Cuantas columnas hay.
     *
     * @return la cantidad
     * @throws SQLException nunca
     */
    public int getColumnCount() throws SQLException {
        return colCount;
    }

    /**
     * Si la columna se numera sola.
     *
     * @param columnIndex la columna, desde 1
     * @return si es autoincremental
     * @throws SQLException si el indice no es valido
     */
    public boolean isAutoIncrement(final int columnIndex) throws SQLException {
        return col(columnIndex).autoIncrement;
    }

    /**
     * Si distingue mayusculas de minusculas.
     *
     * @param columnIndex la columna, desde 1
     * @return si distingue
     * @throws SQLException si el indice no es valido
     */
    public boolean isCaseSensitive(final int columnIndex) throws SQLException {
        return col(columnIndex).caseSensitive;
    }

    /**
     * Si puede aparecer en un {@code WHERE}.
     *
     * @param columnIndex la columna, desde 1
     * @return si es buscable
     * @throws SQLException si el indice no es valido
     */
    public boolean isSearchable(final int columnIndex) throws SQLException {
        return col(columnIndex).searchable;
    }

    /**
     * Si es un valor monetario.
     *
     * @param columnIndex la columna, desde 1
     * @return si es moneda
     * @throws SQLException si el indice no es valido
     */
    public boolean isCurrency(final int columnIndex) throws SQLException {
        return col(columnIndex).currency;
    }

    /**
     * Si admite nulos.
     *
     * @param columnIndex la columna, desde 1
     * @return una de las constantes {@code columnNo*}
     * @throws SQLException si el indice no es valido
     */
    public int isNullable(final int columnIndex) throws SQLException {
        return col(columnIndex).nullable;
    }

    /**
     * Si el valor tiene signo.
     *
     * @param columnIndex la columna, desde 1
     * @return si tiene signo
     * @throws SQLException si el indice no es valido
     */
    public boolean isSigned(final int columnIndex) throws SQLException {
        return col(columnIndex).signed;
    }

    /**
     * El ancho normal de la columna.
     *
     * @param columnIndex la columna, desde 1
     * @return el ancho en caracteres
     * @throws SQLException si el indice no es valido
     */
    public int getColumnDisplaySize(final int columnIndex) throws SQLException {
        return col(columnIndex).columnDisplaySize;
    }

    /**
     * El titulo con el que mostrarla.
     *
     * <p>Si no se fijo ninguno devuelve el nombre: es lo que un visor va a querer mostrar, y no
     * tener titulo no es razon para mostrar una columna sin encabezado.
     *
     * @param columnIndex la columna, desde 1
     * @return el titulo
     * @throws SQLException si el indice no es valido
     */
    public String getColumnLabel(final int columnIndex) throws SQLException {
        final ColInfo c = col(columnIndex);
        return c.columnLabel != null && c.columnLabel.length() > 0
                ? c.columnLabel : getColumnName(columnIndex);
    }

    /**
     * El nombre de la columna.
     *
     * @param columnIndex la columna, desde 1
     * @return el nombre, o cadena vacia si no se fijo
     * @throws SQLException si el indice no es valido
     */
    public String getColumnName(final int columnIndex) throws SQLException {
        final String n = col(columnIndex).columnName;
        return n == null ? "" : n;
    }

    /**
     * El esquema de la tabla.
     *
     * @param columnIndex la columna, desde 1
     * @return el esquema
     * @throws SQLException si el indice no es valido
     */
    public String getSchemaName(final int columnIndex) throws SQLException {
        return col(columnIndex).schemaName;
    }

    /**
     * Los digitos significativos.
     *
     * @param columnIndex la columna, desde 1
     * @return la precision
     * @throws SQLException si el indice no es valido
     */
    public int getPrecision(final int columnIndex) throws SQLException {
        return col(columnIndex).colPrecision;
    }

    /**
     * Los digitos a la derecha del punto.
     *
     * @param columnIndex la columna, desde 1
     * @return la escala
     * @throws SQLException si el indice no es valido
     */
    public int getScale(final int columnIndex) throws SQLException {
        return col(columnIndex).colScale;
    }

    /**
     * La tabla de la que sale.
     *
     * @param columnIndex la columna, desde 1
     * @return la tabla
     * @throws SQLException si el indice no es valido
     */
    public String getTableName(final int columnIndex) throws SQLException {
        return col(columnIndex).tableName;
    }

    /**
     * El catalogo de la tabla.
     *
     * @param columnIndex la columna, desde 1
     * @return el catalogo
     * @throws SQLException si el indice no es valido
     */
    public String getCatalogName(final int columnIndex) throws SQLException {
        return col(columnIndex).catName;
    }

    /**
     * El tipo SQL.
     *
     * @param columnIndex la columna, desde 1
     * @return una constante de {@link Types}
     * @throws SQLException si el indice no es valido
     */
    public int getColumnType(final int columnIndex) throws SQLException {
        return col(columnIndex).colType;
    }

    /**
     * El nombre del tipo.
     *
     * @param columnIndex la columna, desde 1
     * @return el nombre
     * @throws SQLException si el indice no es valido
     */
    public String getColumnTypeName(final int columnIndex) throws SQLException {
        return col(columnIndex).colTypeName;
    }

    /**
     * Si la columna es de solo lectura.
     *
     * <p>Siempre {@code false}: no hay como declararla de solo lectura, porque
     * {@link RowSetMetaData} no tiene el {@code set} correspondiente.
     *
     * @param columnIndex la columna, desde 1
     * @return {@code false}
     * @throws SQLException si el indice no es valido
     */
    public boolean isReadOnly(final int columnIndex) throws SQLException {
        col(columnIndex);
        return false;
    }

    /**
     * Si se puede escribir.
     *
     * @param columnIndex la columna, desde 1
     * @return {@code true}, por lo dicho en {@link #isReadOnly}
     * @throws SQLException si el indice no es valido
     */
    public boolean isWritable(final int columnIndex) throws SQLException {
        col(columnIndex);
        return true;
    }

    /**
     * Si la escritura seguro va a andar.
     *
     * @param columnIndex la columna, desde 1
     * @return {@code true}, por lo dicho en {@link #isReadOnly}
     * @throws SQLException si el indice no es valido
     */
    public boolean isDefinitelyWritable(final int columnIndex) throws SQLException {
        col(columnIndex);
        return true;
    }

    /**
     * La clase Java que {@code getObject} va a devolver para esa columna.
     *
     * <p>El mapeo es el de la especificacion de JDBC. Los tipos que no estan en la tabla caen en
     * {@code Object}, que es la respuesta correcta para un tipo que la base define y Java no
     * conoce.
     *
     * @param columnIndex la columna, desde 1
     * @return el nombre completo de la clase
     * @throws SQLException si el indice no es valido
     */
    public String getColumnClassName(final int columnIndex) throws SQLException {
        // Una cadena de if y no un switch: #503 -- el generador de bytecode no pliega una etiqueta
        // `case` cuya constante viene de un .class del classpath, y todas estas lo son. Volver al
        // switch cuando se cierre.
        final int t = col(columnIndex).colType;
        if (t == Types.BIT || t == Types.BOOLEAN) {
            return "java.lang.Boolean";
        }
        if (t == Types.TINYINT || t == Types.SMALLINT || t == Types.INTEGER) {
            return "java.lang.Integer";
        }
        if (t == Types.BIGINT) {
            return "java.lang.Long";
        }
        if (t == Types.REAL) {
            return "java.lang.Float";
        }
        if (t == Types.FLOAT || t == Types.DOUBLE) {
            return "java.lang.Double";
        }
        if (t == Types.NUMERIC || t == Types.DECIMAL) {
            return "java.math.BigDecimal";
        }
        if (t == Types.CHAR || t == Types.VARCHAR || t == Types.LONGVARCHAR
                || t == Types.NCHAR || t == Types.NVARCHAR || t == Types.LONGNVARCHAR) {
            return "java.lang.String";
        }
        if (t == Types.BINARY || t == Types.VARBINARY || t == Types.LONGVARBINARY) {
            return "byte[]";
        }
        if (t == Types.DATE) {
            return "java.sql.Date";
        }
        if (t == Types.TIME) {
            return "java.sql.Time";
        }
        if (t == Types.TIMESTAMP) {
            return "java.sql.Timestamp";
        }
        if (t == Types.BLOB) {
            return "java.sql.Blob";
        }
        if (t == Types.CLOB) {
            return "java.sql.Clob";
        }
        if (t == Types.NCLOB) {
            return "java.sql.NClob";
        }
        if (t == Types.ARRAY) {
            return "java.sql.Array";
        }
        if (t == Types.REF) {
            return "java.sql.Ref";
        }
        if (t == Types.ROWID) {
            return "java.sql.RowId";
        }
        if (t == Types.SQLXML) {
            return "java.sql.SQLXML";
        }
        if (t == Types.STRUCT) {
            return "java.sql.Struct";
        }
        // Un tipo que la base define y Java no conoce: Object es la respuesta correcta.
        return "java.lang.Object";
    }
    /**
     * Esta instancia, si se la pide como una interfaz que implementa.
     *
     * @param <T> el tipo pedido
     * @param iface la interfaz
     * @return esta instancia
     * @throws SQLException si no implementa esa interfaz
     */
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        if (iface != null && iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("esta clase no implementa " + iface);
    }

    /**
     * Si {@link #unwrap} va a poder con esa interfaz.
     *
     * @param iface la interfaz
     * @return si la implementa
     * @throws SQLException nunca
     */
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        return iface != null && iface.isInstance(this);
    }
}
