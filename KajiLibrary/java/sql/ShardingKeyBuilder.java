package java.sql;

/**
 * KajiLibrary's java.sql.ShardingKeyBuilder -- builds a {@link ShardingKey} piece by piece.
 *
 * <p>There is a builder and not a constructor because a sharding key can be **composite**: several
 * subkeys with their type, in order. Chaining `subkey(...)` is what expresses that order without
 * inventing a list.
 */
public interface ShardingKeyBuilder {

    /** Adds a subkey with its type. */
    ShardingKeyBuilder subkey(Object subkey, SQLType subkeyType);

    /** The key the added subkeys form. */
    ShardingKey build() throws SQLException;
}
