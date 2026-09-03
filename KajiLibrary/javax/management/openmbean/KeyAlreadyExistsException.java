package javax.management.openmbean;

/**
 * Una fila cuyo índice ya está en la tabla.
 *
 * <p>La tira {@link TabularData#put}, y es la razón por la que `put` **no** es un reemplazo como el
 * de un `Map`: una tabla abierta tiene claves derivadas del contenido de la fila, así que dos filas
 * con el mismo índice son un error del que las armó, no una intención de pisar la primera. Para
 * reemplazar hay que borrar y volver a poner.
 */
public class KeyAlreadyExistsException extends IllegalArgumentException {

    private static final long serialVersionUID = 1845183636745282866L;

    /** Sin mensaje. */
    public KeyAlreadyExistsException() {
        super();
    }

    /** Con ese mensaje. */
    public KeyAlreadyExistsException(String msg) {
        super(msg);
    }
}
