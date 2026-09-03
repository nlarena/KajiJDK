package java.sql;

/**
 * KajiLibrary's java.sql.BatchUpdateException -- fallo al ejecutar un lote.
 *
 * <p>Lleva **las cuentas** de lo que si se ejecuto, y esa es toda su razon de ser: un lote de mil
 * inserciones que falla en la numero seiscientos no es un exito ni un fracaso, y una excepcion sin
 * ese arreglo dejaria a quien la atrapa sin manera de saber donde quedo. Cada posicion trae la
 * cantidad de filas que esa sentencia toco, o {@link Statement#EXECUTE_FAILED} si esa fallo.
 *
 * <p>El arreglo puede ser mas corto que el lote --hasta donde llego el driver-- y eso tambien es
 * informacion.
 */
public class BatchUpdateException extends SQLException {

    // Se guarda la version `long` y se deriva la `int`: al reves se perderian las cuentas grandes.
    private final long[] largeUpdateCounts;

    public BatchUpdateException() {
        this(null, null, 0, (long[]) null, null);
    }

    public BatchUpdateException(Throwable cause) {
        this(null, null, 0, (long[]) null, cause);
    }

    public BatchUpdateException(int[] updateCounts) {
        this(null, null, 0, aLargo(updateCounts), null);
    }

    public BatchUpdateException(int[] updateCounts, Throwable cause) {
        this(null, null, 0, aLargo(updateCounts), cause);
    }

    public BatchUpdateException(String reason, int[] updateCounts) {
        this(reason, null, 0, aLargo(updateCounts), null);
    }

    public BatchUpdateException(String reason, int[] updateCounts, Throwable cause) {
        this(reason, null, 0, aLargo(updateCounts), cause);
    }

    public BatchUpdateException(String reason, String SQLState, int[] updateCounts) {
        this(reason, SQLState, 0, aLargo(updateCounts), null);
    }

    public BatchUpdateException(String reason, String SQLState, int[] updateCounts,
            Throwable cause) {
        this(reason, SQLState, 0, aLargo(updateCounts), cause);
    }

    public BatchUpdateException(String reason, String SQLState, int vendorCode,
            int[] updateCounts) {
        this(reason, SQLState, vendorCode, aLargo(updateCounts), null);
    }

    public BatchUpdateException(String reason, String SQLState, int vendorCode, int[] updateCounts,
            Throwable cause) {
        this(reason, SQLState, vendorCode, aLargo(updateCounts), cause);
    }

    public BatchUpdateException(String reason, String SQLState, int vendorCode,
            long[] updateCounts, Throwable cause) {
        super(reason, SQLState, vendorCode, cause);
        this.largeUpdateCounts = updateCounts == null ? null : updateCounts.clone();
    }

    /**
     * Las cuentas, truncadas a `int`.
     *
     * <p>Truncadas de verdad: una cuenta que no entra en un `int` sale mal, y es la razon por la que
     * existe {@link #getLargeUpdateCounts}.
     */
    public int[] getUpdateCounts() {
        if (this.largeUpdateCounts == null) {
            return null;
        }
        int[] salida = new int[this.largeUpdateCounts.length];
        int i = 0;
        while (i < salida.length) {
            salida[i] = (int) this.largeUpdateCounts[i];
            i = i + 1;
        }
        return salida;
    }

    /** Las cuentas, sin truncar. */
    public long[] getLargeUpdateCounts() {
        return this.largeUpdateCounts == null ? null : this.largeUpdateCounts.clone();
    }

    private static long[] aLargo(int[] cuentas) {
        if (cuentas == null) {
            return null;
        }
        long[] salida = new long[cuentas.length];
        int i = 0;
        while (i < cuentas.length) {
            salida[i] = cuentas[i];
            i = i + 1;
        }
        return salida;
    }
}
