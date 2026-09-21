package java.sql;

/**
 * KajiLibrary's java.sql.ConnectionBuilder -- asks for a connection with more data than user and
 * password.
 *
 * <p>It exists because `getConnection(user, password)` fell short: with sharded databases one has
 * to say **which shard** to go to, and adding overloads for each combination would have given a
 * family of methods nobody remembers. A chainable builder adds a new piece of data without touching
 * any existing signature.
 */
public interface ConnectionBuilder {

    /** The user. */
    ConnectionBuilder user(String username);

    /** The password. */
    ConnectionBuilder password(String password);

    /** The shard to go to. */
    ConnectionBuilder shardingKey(ShardingKey shardingKey);

    /** The **top-level** shard, when the scheme has two levels. */
    ConnectionBuilder superShardingKey(ShardingKey superShardingKey);

    /** The connection. */
    Connection build() throws SQLException;
}
