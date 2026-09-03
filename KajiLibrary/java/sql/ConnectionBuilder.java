package java.sql;

/**
 * KajiLibrary's java.sql.ConnectionBuilder -- pide una conexion con mas datos que usuario y clave.
 *
 * <p>Existe porque `getConnection(user, password)` se quedo corto: con bases particionadas hay que
 * decir **a que particion** se quiere ir, y agregar sobrecargas por cada combinacion habria dado una
 * familia de metodos que nadie recuerda. Un constructor encadenable agrega un dato nuevo sin tocar
 * ninguna firma existente.
 */
public interface ConnectionBuilder {

    /** El usuario. */
    ConnectionBuilder user(String username);

    /** La clave. */
    ConnectionBuilder password(String password);

    /** La particion a la que ir. */
    ConnectionBuilder shardingKey(ShardingKey shardingKey);

    /** La particion **de nivel superior**, cuando el esquema tiene dos niveles. */
    ConnectionBuilder superShardingKey(ShardingKey superShardingKey);

    /** La conexion. */
    Connection build() throws SQLException;
}
