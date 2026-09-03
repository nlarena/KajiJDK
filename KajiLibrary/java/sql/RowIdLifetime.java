package java.sql;

/**
 * KajiLibrary's java.sql.RowIdLifetime -- cuanto vale un {@link RowId} en esta base.
 *
 * <p>Es lo que hace usable a `RowId`: la interfaz no promete nada sobre su duracion, y sin poder
 * preguntarlo no habria manera de saber si guardar uno es razonable. La respuesta va de
 * "no los soporto" a "valen para siempre", y casi todas las bases estan en el medio.
 */
public enum RowIdLifetime {

    /** Esta base no tiene identificadores de fila. */
    ROWID_UNSUPPORTED,

    /** Valen, pero la duracion no es ninguna de las otras. */
    ROWID_VALID_OTHER,

    /** Valen mientras dure la sesion. */
    ROWID_VALID_SESSION,

    /** Valen mientras dure la transaccion. */
    ROWID_VALID_TRANSACTION,

    /** Valen para siempre. */
    ROWID_VALID_FOREVER
}
