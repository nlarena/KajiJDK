package javax.sql;

/** KajiLibrary's javax.sql.PooledConnectionBuilder -- {@link java.sql.ConnectionBuilder} para el pool. */
public interface PooledConnectionBuilder {

    PooledConnectionBuilder user(String username);

    PooledConnectionBuilder password(String password);

    PooledConnectionBuilder shardingKey(java.sql.ShardingKey shardingKey);

    PooledConnectionBuilder superShardingKey(java.sql.ShardingKey superShardingKey);

    PooledConnection build() throws java.sql.SQLException;
}
