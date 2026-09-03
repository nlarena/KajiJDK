package java.sql;

/**
 * KajiLibrary's java.sql.DataTruncation -- un dato no entro y se corto.
 *
 * <p>Es {@link SQLWarning} al **leer** y {@link SQLException} al **escribir**, y la asimetria tiene
 * sentido: leer de menos deja al programa con un dato incompleto que quizas le alcanza; escribir de
 * menos deja a la **base** con un dato incompleto, que ya no se puede deshacer. Que la misma clase
 * sirva para las dos cosas es posible porque hereda de `SQLWarning`, que a su vez es una
 * `SQLException`.
 *
 * <p>{@link #getDataSize} y {@link #getTransferSize} son los dos numeros que importan: cuanto habia y
 * cuanto paso.
 */
public class DataTruncation extends SQLWarning {

    private final int index;
    private final boolean parameter;
    private final boolean read;
    private final int dataSize;
    private final int transferSize;

    public DataTruncation(int index, boolean parameter, boolean read, int dataSize,
            int transferSize) {
        this(index, parameter, read, dataSize, transferSize, null);
    }

    public DataTruncation(int index, boolean parameter, boolean read, int dataSize,
            int transferSize, Throwable cause) {
        // Los dos `SQLState` del estandar: `01004` avisa, `22001` falla. Se elige por `read`, que es
        // lo que distingue leer de escribir.
        super("Data truncation", read ? "01004" : "22001", 0, cause);
        this.index = index;
        this.parameter = parameter;
        this.read = read;
        this.dataSize = dataSize;
        this.transferSize = transferSize;
    }

    /** El indice de la columna o del parametro; -1 si no se sabe. */
    public int getIndex() {
        return this.index;
    }

    /** Si fue un parametro (`true`) o una columna (`false`). */
    public boolean getParameter() {
        return this.parameter;
    }

    /** Si fue al leer (`true`) o al escribir (`false`). */
    public boolean getRead() {
        return this.read;
    }

    /** Cuantos bytes o caracteres habia; -1 si no se sabe. */
    public int getDataSize() {
        return this.dataSize;
    }

    /** Cuantos pasaron de verdad; -1 si no se sabe. */
    public int getTransferSize() {
        return this.transferSize;
    }
}
