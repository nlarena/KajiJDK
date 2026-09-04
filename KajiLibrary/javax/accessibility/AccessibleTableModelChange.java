package javax.accessibility;

/**
 * Qué cambió en una tabla accesible.
 *
 * <p>Describe el cambio como un **rectángulo** de filas y columnas más el tipo de cambio, en vez de
 * mandar la tabla entera. Es lo que permite que una ayuda técnica siga una planilla grande sin
 * releerla en cada modificación.
 */
public interface AccessibleTableModelChange {

    /** Se insertaron filas o columnas. */
    int INSERT = 1;

    /** Cambió el contenido. */
    int UPDATE = 0;

    /** Se borraron filas o columnas. */
    int DELETE = -1;

    /** `INSERT`, `UPDATE` o `DELETE`. */
    int getType();

    /** La primera fila afectada. */
    int getFirstRow();

    /** La última fila afectada. */
    int getLastRow();

    /** La primera columna afectada. */
    int getFirstColumn();

    /** La última columna afectada. */
    int getLastColumn();
}
