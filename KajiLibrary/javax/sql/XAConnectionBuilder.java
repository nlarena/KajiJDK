package javax.sql;

/** KajiLibrary's javax.sql.XAConnectionBuilder -- {@link java.sql.ConnectionBuilder} para XA. */
public interface XAConnectionBuilder {

    XAConnectionBuilder user(String username);

    XAConnectionBuilder password(String password);

    XAConnectionBuilder shardingKey(java.sql.ShardingKey shardingKey);

    XAConnectionBuilder superShardingKey(java.sql.ShardingKey superShardingKey);

    XAConnection build() throws java.sql.SQLException;
}
