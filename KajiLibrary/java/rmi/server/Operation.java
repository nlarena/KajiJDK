package java.rmi.server;

/**
 * Un metodo remoto, descripto por su firma en texto.
 *
 * @deprecated los stubs generados por {@code rmic} identificaban los metodos por posicion en un
 *     arreglo de estos. Los proxies dinamicos los identifican por un hash de la firma, que no se
 *     rompe al reordenar, y esta clase quedo sin uso.
 */
@Deprecated(since = "1.2")
public class Operation {

    private final String operation;

    /** Con esa firma. */
    public Operation(String op) {
        this.operation = op;
    }

    /** La firma. */
    public String getOperation() {
        return this.operation;
    }

    public String toString() {
        return this.operation;
    }
}
