package javax.sql;

/**
 * KajiLibrary's javax.sql.DataSource -- de donde salen las conexiones.
 *
 * <p>Es **la** manera de obtener una conexion en cualquier aplicacion que no sea un ejemplo: en vez
 * de que el codigo sepa la URL, el usuario y la clave, sabe pedirle una conexion a un objeto que
 * alguien mas configuro. Eso es lo que permite que la misma aplicacion hable con una base distinta
 * sin recompilarse, y que las conexiones vengan de un pool sin que quien las usa se entere.
 *
 * <p>La nota que estaba aca decia que la interfaz quedaba **vacia** porque `java.sql` no existia, y
 * que "el dia que exista, los metodos vienen con el". Existe -- un nucleo acotado, ver
 * {@link java.sql.Connection} -- y los metodos vinieron.
 *
 * <p>Que se declare sin que haya ningun driver no es una promesa vacia: una interfaz es un contrato,
 * y el contrato es exacto. Lo que no habria que hacer es dar una implementacion que finja conectarse.
 */
public interface DataSource extends CommonDataSource, java.sql.Wrapper {

    /** Una conexion, con las credenciales que la fuente tenga configuradas. */
    java.sql.Connection getConnection() throws java.sql.SQLException;

    /** Una conexion con esas credenciales. */
    java.sql.Connection getConnection(String username, String password)
            throws java.sql.SQLException;

    /**
     * Un constructor de conexion, para pedir una con mas datos que usuario y clave.
     *
     * <p>Se niega por defecto en vez de devolver un constructor que despues no sirva: una fuente que
     * no sabe de particiones no puede honrar un `shardingKey`, y descubrirlo al final es peor que al
     * principio.
     */
    default java.sql.ConnectionBuilder createConnectionBuilder() throws java.sql.SQLException {
        throw new UnsupportedOperationException("createConnectionBuilder no esta implementado");
    }
}
