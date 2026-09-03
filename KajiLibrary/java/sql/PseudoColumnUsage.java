package java.sql;

/**
 * KajiLibrary's java.sql.PseudoColumnUsage -- donde se puede usar una pseudocolumna.
 *
 * <p>Una pseudocolumna es una que la base ofrece sin que este en la tabla --el `ROWID` de Oracle es
 * el ejemplo-- y no todas se pueden usar en cualquier lado: algunas solo salen en el `select`, otras
 * solo sirven para filtrar. Sin esto, una herramienta que arma consultas tendria que probar.
 */
public enum PseudoColumnUsage {

    /** Solo en la lista del `select`. */
    SELECT_LIST_ONLY,

    /** Solo en el `where`. */
    WHERE_CLAUSE_ONLY,

    /** En cualquier lado. */
    NO_USAGE_RESTRICTIONS,

    /** No se sabe. */
    USAGE_UNKNOWN
}
