package java.sql;

/**
 * KajiLibrary's java.sql.Wrapper -- llegar a la implementacion real que hay debajo de un objeto JDBC.
 *
 * <p>Existe por los envoltorios: un pool de conexiones entrega una `Connection` que **no es** la del
 * driver sino una que la envuelve para devolverla al pool al cerrarla. Quien necesite una funcion
 * propia del driver queda del lado de afuera, y este par de metodos es la puerta -- con la ventaja
 * de que se puede **preguntar antes** ({@link #isWrapperFor}) en vez de intentar y atrapar.
 */
public interface Wrapper {

    /**
     * Este objeto visto como `iface`, atravesando los envoltorios.
     *
     * @throws SQLException si no hay nada que implemente `iface` debajo
     */
    <T> T unwrap(Class<T> iface) throws SQLException;

    /** Si {@link #unwrap} con ese tipo va a funcionar. */
    boolean isWrapperFor(Class<?> iface) throws SQLException;
}
