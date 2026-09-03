package java.sql;

/**
 * KajiLibrary's java.sql.ShardingKeyBuilder -- arma una {@link ShardingKey} pieza por pieza.
 *
 * <p>Hay constructor y no un constructor de clase porque una clave de particion puede ser
 * **compuesta**: varias subclaves con su tipo, en orden. Encadenar `subkey(...)` es lo que expresa
 * ese orden sin inventar una lista.
 */
public interface ShardingKeyBuilder {

    /** Agrega una subclave con su tipo. */
    ShardingKeyBuilder subkey(Object subkey, SQLType subkeyType);

    /** La clave que forman las subclaves agregadas. */
    ShardingKey build() throws SQLException;
}
