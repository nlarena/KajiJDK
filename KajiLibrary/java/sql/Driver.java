package java.sql;

/**
 * KajiLibrary's java.sql.Driver -- lo que implementa quien sabe hablar con una base concreta.
 *
 * <p>{@link #acceptsURL} es la pieza que hace funcionar todo el esquema: el {@link DriverManager} le
 * pregunta a cada driver registrado si entiende la URL y usa el primero que dice que si. Por eso una
 * aplicacion puede cambiar de base cambiando una cadena -- nadie nombra al driver.
 *
 * <p>Y por eso mismo {@link #connect} devuelve `null` en vez de fallar cuando la URL no es suya:
 * `null` significa "no es mia, segui preguntando", que es distinto de "es mia y no pude conectar".
 * Confundir las dos haria que un driver ajeno abortara la busqueda.
 */
public interface Driver {

    /**
     * Una conexion a esa URL, o `null` si la URL no es de este driver.
     *
     * @throws SQLException si la URL **si** es suya y la conexion fallo
     */
    Connection connect(String url, java.util.Properties info) throws SQLException;

    /** Si este driver entiende esa URL. */
    boolean acceptsURL(String url) throws SQLException;

    /** Que propiedades hacen falta para conectar a esa URL con lo que ya se sabe. */
    DriverPropertyInfo[] getPropertyInfo(String url, java.util.Properties info) throws SQLException;

    int getMajorVersion();

    int getMinorVersion();

    /**
     * Si el driver cumple el estandar.
     *
     * <p>Solo puede devolver `true` si pasa las pruebas de conformidad, que exigen soportar SQL-92
     * Entry Level entero. Casi ningun driver puede.
     */
    boolean jdbcCompliant();

    /**
     * El logger del que cuelgan los de este driver.
     *
     * @throws SQLFeatureNotSupportedException si el driver no usa `java.util.logging`
     */
    java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException;
}
