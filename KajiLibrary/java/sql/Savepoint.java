package java.sql;

/**
 * KajiLibrary's java.sql.Savepoint -- una marca dentro de una transaccion.
 *
 * <p>Sirve para deshacer **una parte**: se marca, se sigue, y si algo sale mal se vuelve a la marca
 * sin perder lo anterior. Sin esto la unica granularidad seria la transaccion entera, lo cual obliga
 * a rehacer trabajo que estaba bien.
 *
 * <p>Tiene dos accesores y **solo uno es valido** para cada punto: los sin nombre tienen numero, los
 * con nombre tienen nombre, y pedir el otro lanza. La alternativa habria sido dos interfaces, y se
 * eligio una con dos mitades.
 */
public interface Savepoint {

    /**
     * El numero de este punto.
     *
     * @throws SQLException si este punto tiene nombre
     */
    int getSavepointId() throws SQLException;

    /**
     * El nombre de este punto.
     *
     * @throws SQLException si este punto no tiene nombre
     */
    String getSavepointName() throws SQLException;
}
