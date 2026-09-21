package java.sql;

/**
 * KajiLibrary's java.sql.ShardingKey -- the key that says **which shard** a value lives in.
 *
 * <p>It has no members, and that is what it is: an opaque value the driver builds and understands.
 * Whoever uses it asks for it with {@link ShardingKeyBuilder} and passes it on; they do not inspect
 * it.
 */
public interface ShardingKey {
}
