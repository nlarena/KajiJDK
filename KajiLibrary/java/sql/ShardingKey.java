package java.sql;

/**
 * KajiLibrary's java.sql.ShardingKey -- la clave que dice **en que particion** vive un dato.
 *
 * <p>No tiene miembros, y eso es lo que es: un valor opaco que el driver arma y entiende. Quien la
 * usa la pide con {@link ShardingKeyBuilder} y la pasa; no la inspecciona.
 */
public interface ShardingKey {
}
