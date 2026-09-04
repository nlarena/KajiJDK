package javax.sql.rowset;

import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import javax.sql.RowSetEvent;
import javax.sql.RowSetListener;

/**
 * La base de todo {@code RowSet}: guarda las propiedades y los parametros, y avisa a los oyentes.
 *
 * <h2>Que hace y que no</h2>
 *
 * <p>Esta clase <strong>no tiene filas</strong>. Guarda la consulta, la URL, el usuario, el tipo de
 * cursor y los parametros que van a reemplazar los signos de pregunta — todo lo que hace falta
 * <em>antes</em> de traer datos. Los datos son problema de la subclase.
 *
 * <p>Esa division es la que permite que un {@code CachedRowSet} y un {@code JdbcRowSet}, que
 * guardan sus filas de maneras completamente distintas, compartan las cien y pico de propiedades y
 * setters que tienen identicos.
 *
 * <h2>Los parametros, y por que se guardan en vez de aplicarse</h2>
 *
 * <p>Un {@code RowSet} es un componente al estilo JavaBeans: primero se configura, despues se
 * ejecuta. Cuando alguien llama a {@code setInt(1, 42)} todavia puede no haber conexion ni
 * sentencia preparada donde poner ese 42, asi que se guarda en un mapa indexado y la subclase lo
 * recupera con {@link #getParams} en el momento de ejecutar.
 *
 * <p>De ahi salen dos rarezas visibles. Los indices son base 1 de cara afuera y base 0 en el mapa,
 * que es la unica resta que hay en toda la clase. Y los parametros que llevan mas de un dato
 * —{@code setNull} con su tipo SQL, {@code setObject} con escala, los flujos con su largo— se
 * guardan como un {@code Object[]}, porque el mapa tiene un solo lugar por posicion.
 *
 * <h2>Los parametros por nombre no estan</h2>
 *
 * <p>Las tres decenas de metodos {@code setXxx(String, ...)} lanzan
 * {@link SQLFeatureNotSupportedException}. No es un hueco de esta biblioteca: es lo que hace el
 * JDK. Estan declarados porque {@code RowSet} los declara, y no funcionan porque un
 * {@code PreparedStatement} de JDBC no acepta parametros por nombre — no habria adonde mandarlos.
 *
 * <h2>Los cuatro campos {@code protected} de flujos</h2>
 *
 * <p>{@link #binaryStream}, {@link #unicodeStream}, {@link #asciiStream} y {@link #charStream} son
 * un resto de una version anterior, cuando el ultimo flujo fijado se guardaba aparte para que la
 * subclase lo alcanzara. Hoy los flujos van al mapa de parametros como todo lo demas. Se conservan
 * porque son API {@code protected} y alguna subclase de afuera puede estar leyendolos.
 *
 * @since 1.5
 */
public abstract class BaseRowSet implements Serializable, Cloneable {

    private static final long serialVersionUID = 4886719666485113312L;

    /** Marca de que un parametro de flujo es de caracteres Unicode. */
    public static final int UNICODE_STREAM_PARAM = 0;

    /** Marca de que un parametro de flujo es binario. */
    public static final int BINARY_STREAM_PARAM = 1;

    /** Marca de que un parametro de flujo es de caracteres ASCII. */
    public static final int ASCII_STREAM_PARAM = 2;

    /** El ultimo flujo binario fijado; ver la nota de la clase. */
    protected InputStream binaryStream;

    /** El ultimo flujo Unicode fijado; ver la nota de la clase. */
    protected InputStream unicodeStream;

    /** El ultimo flujo ASCII fijado; ver la nota de la clase. */
    protected InputStream asciiStream;

    /** El ultimo flujo de caracteres fijado; ver la nota de la clase. */
    protected Reader charStream;

    private String command;
    private String url;
    private String dataSource;
    private transient String username;
    private transient String password;

    private int rowSetType = ResultSet.TYPE_SCROLL_INSENSITIVE;
    private int concurrency = ResultSet.CONCUR_UPDATABLE;
    private boolean readOnly;
    private boolean escapeProcessing = true;
    private int isolation = Connection.TRANSACTION_READ_COMMITTED;
    private int fetchDir = ResultSet.FETCH_FORWARD;
    private int fetchSize;
    private int maxFieldSize;
    private int maxRows;
    private int queryTimeout;
    private boolean showDeleted;
    private Map<String, Class<?>> map;

    private transient Vector<RowSetListener> listeners = new Vector<RowSetListener>();

    /** Parametros por posicion, en base 0; ver la nota de la clase. */
    private Hashtable<Integer, Object> params;

    /** Para las subclases. */
    public BaseRowSet() {
    }

    /**
     * Prepara el mapa de parametros, vaciandolo si ya existia.
     *
     * <p>Hay que llamarlo antes del primer {@code setXxx}. Los setters no lo hacen solos a
     * proposito: fallan diciendo que falta, que es mas facil de diagnosticar que un mapa que
     * aparece a mitad de camino.
     */
    protected void initParams() {
        params = new Hashtable<Integer, Object>();
    }

    private Hashtable<Integer, Object> mapa(final String quien) throws SQLException {
        if (params == null) {
            throw new SQLException("Set initParams() before " + quien);
        }
        return params;
    }

    /** Los indices de parametro son base 1, como en todo JDBC. */
    private void checkParamIndex(final int idx) throws SQLException {
        if (idx < 1) {
            throw new SQLException("el indice de parametro tiene que ser mayor o igual a 1");
        }
    }

    private void poner(final int idx, final Object v, final String quien) throws SQLException {
        checkParamIndex(idx);
        mapa(quien).put(Integer.valueOf(idx - 1), v);
    }

    private static SQLFeatureNotSupportedException porNombre() {
        return new SQLFeatureNotSupportedException("Feature not supported");
    }

    // ---- oyentes ----

    /**
     * Agrega un oyente.
     *
     * @param listener el oyente
     */
    public void addRowSetListener(final RowSetListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Saca un oyente.
     *
     * @param listener el oyente
     */
    public void removeRowSetListener(final RowSetListener listener) {
        listeners.remove(listener);
    }

    /**
     * Avisa a los oyentes que el cursor se movio.
     *
     * <p>Los tres avisos recorren una copia de la lista: un oyente que se desregistre a si mismo
     * mientras se lo notifica dejaria la iteracion sobre una lista que cambio debajo.
     */
    protected void notifyCursorMoved() {
        final RowSetEvent e = new RowSetEvent((javax.sql.RowSet) this);
        for (final RowSetListener l : copiaDeOyentes()) {
            l.cursorMoved(e);
        }
    }

    /** Avisa a los oyentes que la fila actual cambio. */
    protected void notifyRowChanged() {
        final RowSetEvent e = new RowSetEvent((javax.sql.RowSet) this);
        for (final RowSetListener l : copiaDeOyentes()) {
            l.rowChanged(e);
        }
    }

    /** Avisa a los oyentes que el conjunto entero cambio. */
    protected void notifyRowSetChanged() {
        final RowSetEvent e = new RowSetEvent((javax.sql.RowSet) this);
        for (final RowSetListener l : copiaDeOyentes()) {
            l.rowSetChanged(e);
        }
    }

    private RowSetListener[] copiaDeOyentes() {
        synchronized (listeners) {
            return listeners.toArray(new RowSetListener[listeners.size()]);
        }
    }

    // ---- propiedades ----

    /**
     * La consulta a ejecutar.
     *
     * @return la consulta, o {@code null}
     */
    public String getCommand() {
        return command;
    }

    /**
     * Fija la consulta y descarta los parametros que hubiera.
     *
     * <p>Los descarta porque eran de la consulta anterior: los signos de pregunta de la consulta
     * nueva estan en otro lado y significan otra cosa. Dejarlos seria pasar valores a posiciones
     * que ya no les corresponden.
     *
     * @param cmd la consulta
     * @throws SQLException si la consulta es una cadena vacia
     */
    public void setCommand(final String cmd) throws SQLException {
        if (cmd != null && cmd.trim().length() == 0) {
            throw new SQLException("la consulta no puede ser vacia");
        }
        command = cmd;
        if (params != null) {
            initParams();
        }
    }

    /**
     * La URL de JDBC.
     *
     * @return la URL, o {@code null}
     * @throws SQLException nunca
     */
    public String getUrl() throws SQLException {
        return url;
    }

    /**
     * Fija la URL de JDBC.
     *
     * @param url la URL
     * @throws SQLException si es una cadena vacia
     */
    public void setUrl(final String url) throws SQLException {
        if (url != null && url.trim().length() == 0) {
            throw new SQLException("la URL no puede ser vacia");
        }
        this.url = url;
    }

    /**
     * El nombre JNDI de la fuente de datos.
     *
     * @return el nombre, o {@code null}
     */
    public String getDataSourceName() {
        return dataSource;
    }

    /**
     * Fija el nombre JNDI de la fuente de datos y olvida la URL.
     *
     * <p>Son dos formas excluyentes de llegar a la base, y tener las dos puestas dejaria sin
     * definir cual gana. Fijar una borra la otra.
     *
     * @param name el nombre JNDI
     * @throws SQLException si es una cadena vacia
     */
    public void setDataSourceName(final String name) throws SQLException {
        if (name != null && name.trim().length() == 0) {
            throw new SQLException("el nombre de la fuente de datos no puede ser vacio");
        }
        dataSource = name;
        url = null;
    }

    /**
     * El usuario.
     *
     * @return el usuario, o {@code null}
     */
    public String getUsername() {
        return username;
    }

    /**
     * Fija el usuario.
     *
     * @param name el usuario
     */
    public void setUsername(final String name) {
        username = name;
    }

    /**
     * La contrasena.
     *
     * @return la contrasena, o {@code null}
     */
    public String getPassword() {
        return password;
    }

    /**
     * Fija la contrasena.
     *
     * <p>El campo es {@code transient}, igual que el del usuario: un {@code RowSet} se serializa y
     * viaja, y las credenciales no deberian viajar con el.
     *
     * @param pass la contrasena
     */
    public void setPassword(final String pass) {
        password = pass;
    }

    /**
     * Fija el tipo de cursor.
     *
     * @param type una de las constantes {@code TYPE_} de {@code ResultSet}
     * @throws SQLException si el valor no es una de ellas
     */
    public void setType(final int type) throws SQLException {
        if (type != ResultSet.TYPE_FORWARD_ONLY && type != ResultSet.TYPE_SCROLL_INSENSITIVE
                && type != ResultSet.TYPE_SCROLL_SENSITIVE) {
            throw new SQLException("tipo de cursor invalido: " + type);
        }
        rowSetType = type;
    }

    /**
     * El tipo de cursor.
     *
     * @return una de las constantes {@code TYPE_}
     * @throws SQLException nunca
     */
    public int getType() throws SQLException {
        return rowSetType;
    }

    /**
     * Fija la concurrencia.
     *
     * @param concurrency {@code CONCUR_READ_ONLY} o {@code CONCUR_UPDATABLE}
     * @throws SQLException si el valor no es uno de esos dos
     */
    public void setConcurrency(final int concurrency) throws SQLException {
        if (concurrency != ResultSet.CONCUR_READ_ONLY
                && concurrency != ResultSet.CONCUR_UPDATABLE) {
            throw new SQLException("concurrencia invalida: " + concurrency);
        }
        this.concurrency = concurrency;
    }

    /**
     * La concurrencia.
     *
     * @return {@code CONCUR_READ_ONLY} o {@code CONCUR_UPDATABLE}
     * @throws SQLException nunca
     */
    public int getConcurrency() throws SQLException {
        return concurrency;
    }

    /**
     * Si el conjunto es de solo lectura.
     *
     * @return si lo es
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Marca el conjunto como de solo lectura.
     *
     * @param value si marcarlo
     */
    public void setReadOnly(final boolean value) {
        readOnly = value;
    }

    /**
     * El nivel de aislamiento de la transaccion.
     *
     * @return una de las constantes {@code TRANSACTION_} de {@code Connection}
     */
    public int getTransactionIsolation() {
        return isolation;
    }

    /**
     * Fija el nivel de aislamiento.
     *
     * @param level una de las constantes {@code TRANSACTION_}
     * @throws SQLException si el valor no es una de ellas
     */
    public void setTransactionIsolation(final int level) throws SQLException {
        if (level != Connection.TRANSACTION_NONE
                && level != Connection.TRANSACTION_READ_UNCOMMITTED
                && level != Connection.TRANSACTION_READ_COMMITTED
                && level != Connection.TRANSACTION_REPEATABLE_READ
                && level != Connection.TRANSACTION_SERIALIZABLE) {
            throw new SQLException("nivel de aislamiento invalido: " + level);
        }
        isolation = level;
    }

    /**
     * El mapa de tipos SQL a clases Java.
     *
     * @return el mapa, o {@code null} si no se fijo ninguno
     */
    public Map<String, Class<?>> getTypeMap() {
        return map;
    }

    /**
     * Fija el mapa de tipos.
     *
     * @param map el mapa
     */
    public void setTypeMap(final Map<String, Class<?>> map) {
        this.map = map;
    }

    /**
     * El tope de bytes por columna.
     *
     * @return el tope; cero es sin tope
     * @throws SQLException nunca
     */
    public int getMaxFieldSize() throws SQLException {
        return maxFieldSize;
    }

    /**
     * Fija el tope de bytes por columna.
     *
     * @param max el tope; cero para sin tope
     * @throws SQLException si es negativo
     */
    public void setMaxFieldSize(final int max) throws SQLException {
        if (max < 0) {
            throw new SQLException("el tamano maximo de campo no puede ser negativo");
        }
        maxFieldSize = max;
    }

    /**
     * El tope de filas.
     *
     * @return el tope; cero es sin tope
     * @throws SQLException nunca
     */
    public int getMaxRows() throws SQLException {
        return maxRows;
    }

    /**
     * Fija el tope de filas.
     *
     * @param max el tope; cero para sin tope
     * @throws SQLException si es negativo o menor que el tamano de lote ya fijado
     */
    public void setMaxRows(final int max) throws SQLException {
        if (max < 0) {
            throw new SQLException("la cantidad maxima de filas no puede ser negativa");
        }
        if (max != 0 && max < fetchSize) {
            throw new SQLException(
                    "el maximo de filas no puede ser menor que el tamano de lote " + fetchSize);
        }
        maxRows = max;
    }

    /**
     * Prende o apaga el procesamiento de secuencias de escape de SQL.
     *
     * @param enable si procesarlas
     * @throws SQLException nunca
     */
    public void setEscapeProcessing(final boolean enable) throws SQLException {
        escapeProcessing = enable;
    }

    /**
     * Si se procesan las secuencias de escape.
     *
     * @return si se procesan
     * @throws SQLException nunca
     */
    public boolean getEscapeProcessing() throws SQLException {
        return escapeProcessing;
    }

    /**
     * Cuantos segundos se espera a la consulta.
     *
     * @return los segundos; cero es sin limite
     * @throws SQLException nunca
     */
    public int getQueryTimeout() throws SQLException {
        return queryTimeout;
    }

    /**
     * Fija cuantos segundos esperar.
     *
     * @param seconds los segundos; cero para sin limite
     * @throws SQLException si es negativo
     */
    public void setQueryTimeout(final int seconds) throws SQLException {
        if (seconds < 0) {
            throw new SQLException("el tiempo de espera no puede ser negativo");
        }
        queryTimeout = seconds;
    }

    /**
     * Si las filas borradas se ven al recorrer.
     *
     * @return si se ven
     * @throws SQLException nunca
     */
    public boolean getShowDeleted() throws SQLException {
        return showDeleted;
    }

    /**
     * Muestra o esconde las filas borradas.
     *
     * @param value si mostrarlas
     * @throws SQLException nunca
     */
    public void setShowDeleted(final boolean value) throws SQLException {
        showDeleted = value;
    }

    /**
     * Fija en que direccion se van a leer las filas.
     *
     * @param direction una de las constantes {@code FETCH_} de {@code ResultSet}
     * @throws SQLException si el valor no es una de ellas, o si el cursor es de solo avance y se
     *     pide otra direccion
     */
    public void setFetchDirection(final int direction) throws SQLException {
        if (direction != ResultSet.FETCH_FORWARD && direction != ResultSet.FETCH_REVERSE
                && direction != ResultSet.FETCH_UNKNOWN) {
            throw new SQLException("direccion de lectura invalida: " + direction);
        }
        // Un cursor de solo avance no puede leer al reves ni admitir "no se": la unica direccion
        // coherente con su tipo es hacia adelante.
        if (rowSetType == ResultSet.TYPE_FORWARD_ONLY && direction != ResultSet.FETCH_FORWARD) {
            throw new SQLException("un cursor de solo avance solo admite FETCH_FORWARD");
        }
        fetchDir = direction;
    }

    /**
     * La direccion de lectura.
     *
     * @return una de las constantes {@code FETCH_}
     * @throws SQLException nunca
     */
    public int getFetchDirection() throws SQLException {
        return fetchDir;
    }

    /**
     * Cuantas filas se traen por vez.
     *
     * @param rows el tamano de lote; cero deja decidir al controlador
     * @throws SQLException si es negativo o supera el tope de filas
     */
    public void setFetchSize(final int rows) throws SQLException {
        if (rows < 0) {
            throw new SQLException("el tamano de lote no puede ser negativo");
        }
        if (maxRows != 0 && rows > maxRows) {
            throw new SQLException(
                    "el tamano de lote no puede superar el maximo de filas " + maxRows);
        }
        fetchSize = rows;
    }

    /**
     * El tamano de lote.
     *
     * @return el tamano
     * @throws SQLException nunca
     */
    public int getFetchSize() throws SQLException {
        return fetchSize;
    }

    // ---- parametros por posicion ----

    /**
     * Los parametros fijados, ordenados por posicion.
     *
     * <p>Es lo que la subclase usa al ejecutar. Una posicion que nadie fijo queda en {@code null},
     * que no se distingue de un {@code setNull}; por eso {@code setNull} guarda un arreglo con el
     * tipo SQL adentro en vez de guardar {@code null} pelado.
     *
     * @return los parametros
     * @throws SQLException nunca
     */
    public Object[] getParams() throws SQLException {
        if (params == null) {
            initParams();
            return new Object[0];
        }
        final Object[] out = new Object[params.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = params.get(Integer.valueOf(i));
        }
        return out;
    }

    /**
     * Borra todos los parametros.
     *
     * @throws SQLException nunca
     */
    public void clearParameters() throws SQLException {
        if (params != null) {
            params.clear();
        }
    }

    /**
     * Un parametro nulo, con su tipo SQL.
     *
     * @param parameterIndex la posicion, desde 1
     * @param sqlType el tipo SQL
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setNull(final int parameterIndex, final int sqlType) throws SQLException {
        poner(parameterIndex, new Object[] { null, Integer.valueOf(sqlType) }, "setNull");
    }

    /**
     * Un parametro nulo de un tipo definido por el usuario.
     *
     * @param parameterIndex la posicion, desde 1
     * @param sqlType el tipo SQL
     * @param typeName el nombre del tipo
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setNull(final int parameterIndex, final int sqlType, final String typeName)
            throws SQLException {
        poner(parameterIndex, new Object[] { null, Integer.valueOf(sqlType), typeName }, "setNull");
    }

    /**
     * Un parametro booleano.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setBoolean(final int parameterIndex, final boolean x) throws SQLException {
        poner(parameterIndex, Boolean.valueOf(x), "setBoolean");
    }

    /**
     * Un parametro {@code byte}.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setByte(final int parameterIndex, final byte x) throws SQLException {
        poner(parameterIndex, Byte.valueOf(x), "setByte");
    }

    /**
     * Un parametro {@code short}.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setShort(final int parameterIndex, final short x) throws SQLException {
        poner(parameterIndex, Short.valueOf(x), "setShort");
    }

    /**
     * Un parametro {@code int}.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setInt(final int parameterIndex, final int x) throws SQLException {
        poner(parameterIndex, Integer.valueOf(x), "setInt");
    }

    /**
     * Un parametro {@code long}.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setLong(final int parameterIndex, final long x) throws SQLException {
        poner(parameterIndex, Long.valueOf(x), "setLong");
    }

    /**
     * Un parametro {@code float}.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setFloat(final int parameterIndex, final float x) throws SQLException {
        poner(parameterIndex, Float.valueOf(x), "setFloat");
    }

    /**
     * Un parametro {@code double}.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setDouble(final int parameterIndex, final double x) throws SQLException {
        poner(parameterIndex, Double.valueOf(x), "setDouble");
    }

    /**
     * Un parametro decimal.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setBigDecimal(final int parameterIndex, final BigDecimal x) throws SQLException {
        poner(parameterIndex, x, "setBigDecimal");
    }

    /**
     * Un parametro de texto.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setString(final int parameterIndex, final String x) throws SQLException {
        poner(parameterIndex, x, "setString");
    }

    /**
     * Un parametro binario.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setBytes(final int parameterIndex, final byte[] x) throws SQLException {
        poner(parameterIndex, x, "setBytes");
    }

    /**
     * Un parametro de fecha.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setDate(final int parameterIndex, final Date x) throws SQLException {
        poner(parameterIndex, x, "setDate");
    }

    /**
     * Un parametro de hora.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setTime(final int parameterIndex, final Time x) throws SQLException {
        poner(parameterIndex, x, "setTime");
    }

    /**
     * Un parametro de marca de tiempo.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setTimestamp(final int parameterIndex, final Timestamp x) throws SQLException {
        poner(parameterIndex, x, "setTimestamp");
    }

    /**
     * Una fecha con la zona horaria de un calendario.
     *
     * <p>El calendario hace falta porque una fecha SQL no tiene zona: interpretar sus dias sin
     * decir en que zona daria un dia distinto segun donde corra el programa.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @param cal el calendario con la zona
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setDate(final int parameterIndex, final Date x, final Calendar cal)
            throws SQLException {
        poner(parameterIndex, new Object[] { x, cal }, "setDate");
    }

    /**
     * Una hora con la zona horaria de un calendario.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @param cal el calendario con la zona
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setTime(final int parameterIndex, final Time x, final Calendar cal)
            throws SQLException {
        poner(parameterIndex, new Object[] { x, cal }, "setTime");
    }

    /**
     * Una marca de tiempo con la zona horaria de un calendario.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @param cal el calendario con la zona
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setTimestamp(final int parameterIndex, final Timestamp x, final Calendar cal)
            throws SQLException {
        poner(parameterIndex, new Object[] { x, cal }, "setTimestamp");
    }

    /**
     * Un flujo ASCII de largo conocido.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el flujo
     * @param length cuantos bytes leer
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setAsciiStream(final int parameterIndex, final InputStream x, final int length)
            throws SQLException {
        poner(parameterIndex,
                new Object[] { x, Integer.valueOf(length), Integer.valueOf(ASCII_STREAM_PARAM) },
                "setAsciiStream");
        asciiStream = x;
    }

    /**
     * Un flujo ASCII de largo desconocido.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el flujo
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setAsciiStream(final int parameterIndex, final InputStream x) throws SQLException {
        poner(parameterIndex, new Object[] { x, null, Integer.valueOf(ASCII_STREAM_PARAM) },
                "setAsciiStream");
        asciiStream = x;
    }

    /**
     * Un flujo binario de largo conocido.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el flujo
     * @param length cuantos bytes leer
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setBinaryStream(final int parameterIndex, final InputStream x, final int length)
            throws SQLException {
        poner(parameterIndex,
                new Object[] { x, Integer.valueOf(length), Integer.valueOf(BINARY_STREAM_PARAM) },
                "setBinaryStream");
        binaryStream = x;
    }

    /**
     * Un flujo binario de largo desconocido.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el flujo
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setBinaryStream(final int parameterIndex, final InputStream x)
            throws SQLException {
        poner(parameterIndex, new Object[] { x, null, Integer.valueOf(BINARY_STREAM_PARAM) },
                "setBinaryStream");
        binaryStream = x;
    }

    /**
     * Un flujo de bytes Unicode.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el flujo
     * @param length cuantos bytes leer
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     * @deprecated La codificacion no se declara en ningun lado, asi que el que lee tiene que
     *     adivinarla. Usar {@link #setCharacterStream(int, Reader, int)}.
     */
    @Deprecated
    public void setUnicodeStream(final int parameterIndex, final InputStream x, final int length)
            throws SQLException {
        poner(parameterIndex,
                new Object[] { x, Integer.valueOf(length), Integer.valueOf(UNICODE_STREAM_PARAM) },
                "setUnicodeStream");
        unicodeStream = x;
    }

    /**
     * Un flujo de caracteres de largo conocido.
     *
     * @param parameterIndex la posicion, desde 1
     * @param reader el flujo
     * @param length cuantos caracteres leer
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setCharacterStream(final int parameterIndex, final Reader reader, final int length)
            throws SQLException {
        poner(parameterIndex, new Object[] { reader, Integer.valueOf(length) },
                "setCharacterStream");
        charStream = reader;
    }

    /**
     * Un flujo de caracteres de largo desconocido.
     *
     * @param parameterIndex la posicion, desde 1
     * @param reader el flujo
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setCharacterStream(final int parameterIndex, final Reader reader)
            throws SQLException {
        poner(parameterIndex, new Object[] { reader, null }, "setCharacterStream");
        charStream = reader;
    }

    /**
     * Un objeto, con tipo SQL y escala.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @param targetSqlType el tipo SQL de destino
     * @param scale la escala, para los decimales
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setObject(final int parameterIndex, final Object x, final int targetSqlType,
            final int scale) throws SQLException {
        poner(parameterIndex,
                new Object[] { x, Integer.valueOf(targetSqlType), Integer.valueOf(scale) },
                "setObject");
    }

    /**
     * Un objeto, con tipo SQL.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @param targetSqlType el tipo SQL de destino
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setObject(final int parameterIndex, final Object x, final int targetSqlType)
            throws SQLException {
        poner(parameterIndex, new Object[] { x, Integer.valueOf(targetSqlType) }, "setObject");
    }

    /**
     * Un objeto, dejando que el controlador elija el tipo.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setObject(final int parameterIndex, final Object x) throws SQLException {
        poner(parameterIndex, x, "setObject");
    }

    /**
     * Una referencia SQL.
     *
     * @param parameterIndex la posicion, desde 1
     * @param ref el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setRef(final int parameterIndex, final Ref ref) throws SQLException {
        poner(parameterIndex, ref, "setRef");
    }

    /**
     * Un objeto binario grande.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setBlob(final int parameterIndex, final Blob x) throws SQLException {
        poner(parameterIndex, x, "setBlob");
    }

    /**
     * Un objeto binario grande desde un flujo de largo conocido.
     *
     * @param parameterIndex la posicion, desde 1
     * @param inputStream el flujo
     * @param length cuantos bytes leer
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setBlob(final int parameterIndex, final InputStream inputStream, final long length)
            throws SQLException {
        poner(parameterIndex, new Object[] { inputStream, Long.valueOf(length) }, "setBlob");
    }

    /**
     * Un objeto binario grande desde un flujo de largo desconocido.
     *
     * @param parameterIndex la posicion, desde 1
     * @param inputStream el flujo
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setBlob(final int parameterIndex, final InputStream inputStream)
            throws SQLException {
        poner(parameterIndex, new Object[] { inputStream, null }, "setBlob");
    }

    /**
     * Un objeto de caracteres grande.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setClob(final int parameterIndex, final Clob x) throws SQLException {
        poner(parameterIndex, x, "setClob");
    }

    /**
     * Un objeto de caracteres grande desde un flujo de largo conocido.
     *
     * @param parameterIndex la posicion, desde 1
     * @param reader el flujo
     * @param length cuantos caracteres leer
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setClob(final int parameterIndex, final Reader reader, final long length)
            throws SQLException {
        poner(parameterIndex, new Object[] { reader, Long.valueOf(length) }, "setClob");
    }

    /**
     * Un objeto de caracteres grande desde un flujo de largo desconocido.
     *
     * @param parameterIndex la posicion, desde 1
     * @param reader el flujo
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setClob(final int parameterIndex, final Reader reader) throws SQLException {
        poner(parameterIndex, new Object[] { reader, null }, "setClob");
    }

    /**
     * Un objeto de caracteres nacionales grande.
     *
     * @param parameterIndex la posicion, desde 1
     * @param value el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setNClob(final int parameterIndex, final NClob value) throws SQLException {
        poner(parameterIndex, value, "setNClob");
    }

    /**
     * Un objeto de caracteres nacionales grande desde un flujo de largo conocido.
     *
     * @param parameterIndex la posicion, desde 1
     * @param reader el flujo
     * @param length cuantos caracteres leer
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setNClob(final int parameterIndex, final Reader reader, final long length)
            throws SQLException {
        poner(parameterIndex, new Object[] { reader, Long.valueOf(length) }, "setNClob");
    }

    /**
     * Un objeto de caracteres nacionales grande desde un flujo de largo desconocido.
     *
     * @param parameterIndex la posicion, desde 1
     * @param reader el flujo
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setNClob(final int parameterIndex, final Reader reader) throws SQLException {
        poner(parameterIndex, new Object[] { reader, null }, "setNClob");
    }

    /**
     * Un arreglo SQL.
     *
     * @param parameterIndex la posicion, desde 1
     * @param array el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setArray(final int parameterIndex, final Array array) throws SQLException {
        poner(parameterIndex, array, "setArray");
    }

    /**
     * Un valor XML.
     *
     * @param parameterIndex la posicion, desde 1
     * @param xmlObject el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setSQLXML(final int parameterIndex, final SQLXML xmlObject) throws SQLException {
        poner(parameterIndex, xmlObject, "setSQLXML");
    }

    /**
     * Un identificador de fila.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setRowId(final int parameterIndex, final RowId x) throws SQLException {
        poner(parameterIndex, x, "setRowId");
    }

    /**
     * Una cadena de caracteres nacionales.
     *
     * @param parameterIndex la posicion, desde 1
     * @param value el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setNString(final int parameterIndex, final String value) throws SQLException {
        poner(parameterIndex, value, "setNString");
    }

    /**
     * Un flujo de caracteres nacionales de largo conocido.
     *
     * @param parameterIndex la posicion, desde 1
     * @param value el flujo
     * @param length cuantos caracteres leer
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setNCharacterStream(final int parameterIndex, final Reader value,
            final long length) throws SQLException {
        poner(parameterIndex, new Object[] { value, Long.valueOf(length) },
                "setNCharacterStream");
    }

    /**
     * Un flujo de caracteres nacionales de largo desconocido.
     *
     * @param parameterIndex la posicion, desde 1
     * @param value el flujo
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setNCharacterStream(final int parameterIndex, final Reader value)
            throws SQLException {
        poner(parameterIndex, new Object[] { value, null }, "setNCharacterStream");
    }

    /**
     * Una URL.
     *
     * @param parameterIndex la posicion, desde 1
     * @param x el valor
     * @throws SQLException si el indice es menor que 1 o falta {@link #initParams}
     */
    public void setURL(final int parameterIndex, final URL x) throws SQLException {
        poner(parameterIndex, x, "setURL");
    }

    // ---- parametros por nombre: ninguno esta soportado, ver la nota de la clase ----

    /**
     * @param parameterName el nombre
     * @param sqlType el tipo SQL
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setNull(final String parameterName, final int sqlType) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param sqlType el tipo SQL
     * @param typeName el nombre del tipo
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setNull(final String parameterName, final int sqlType, final String typeName)
            throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el valor
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setBoolean(final String parameterName, final boolean x) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el valor
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setByte(final String parameterName, final byte x) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el valor
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setShort(final String parameterName, final short x) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el valor
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setInt(final String parameterName, final int x) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el valor
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setLong(final String parameterName, final long x) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el valor
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setFloat(final String parameterName, final float x) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el valor
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setDouble(final String parameterName, final double x) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el valor
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setBigDecimal(final String parameterName, final BigDecimal x) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el valor
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setString(final String parameterName, final String x) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el valor
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setBytes(final String parameterName, final byte[] x) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el valor
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setDate(final String parameterName, final Date x) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el valor
     * @param cal el calendario
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setDate(final String parameterName, final Date x, final Calendar cal)
            throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el valor
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setTime(final String parameterName, final Time x) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el valor
     * @param cal el calendario
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setTime(final String parameterName, final Time x, final Calendar cal)
            throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el valor
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setTimestamp(final String parameterName, final Timestamp x) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el valor
     * @param cal el calendario
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setTimestamp(final String parameterName, final Timestamp x, final Calendar cal)
            throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el flujo
     * @param length el largo
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setAsciiStream(final String parameterName, final InputStream x, final int length)
            throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el flujo
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setAsciiStream(final String parameterName, final InputStream x)
            throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el flujo
     * @param length el largo
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setBinaryStream(final String parameterName, final InputStream x, final int length)
            throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el flujo
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setBinaryStream(final String parameterName, final InputStream x)
            throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param reader el flujo
     * @param length el largo
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setCharacterStream(final String parameterName, final Reader reader,
            final int length) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param reader el flujo
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setCharacterStream(final String parameterName, final Reader reader)
            throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param value el flujo
     * @param length el largo
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setNCharacterStream(final String parameterName, final Reader value,
            final long length) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param value el flujo
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setNCharacterStream(final String parameterName, final Reader value)
            throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el valor
     * @param targetSqlType el tipo SQL
     * @param scale la escala
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setObject(final String parameterName, final Object x, final int targetSqlType,
            final int scale) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el valor
     * @param targetSqlType el tipo SQL
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setObject(final String parameterName, final Object x, final int targetSqlType)
            throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el valor
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setObject(final String parameterName, final Object x) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el valor
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setBlob(final String parameterName, final Blob x) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param inputStream el flujo
     * @param length el largo
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setBlob(final String parameterName, final InputStream inputStream,
            final long length) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param inputStream el flujo
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setBlob(final String parameterName, final InputStream inputStream)
            throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el valor
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setClob(final String parameterName, final Clob x) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param reader el flujo
     * @param length el largo
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setClob(final String parameterName, final Reader reader, final long length)
            throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param reader el flujo
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setClob(final String parameterName, final Reader reader) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param value el valor
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setNClob(final String parameterName, final NClob value) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param reader el flujo
     * @param length el largo
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setNClob(final String parameterName, final Reader reader, final long length)
            throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param reader el flujo
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setNClob(final String parameterName, final Reader reader) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param value el valor
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setNString(final String parameterName, final String value) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param xmlObject el valor
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setSQLXML(final String parameterName, final SQLXML xmlObject) throws SQLException {
        throw porNombre();
    }

    /**
     * @param parameterName el nombre
     * @param x el valor
     * @throws SQLFeatureNotSupportedException siempre
     */
    public void setRowId(final String parameterName, final RowId x) throws SQLException {
        throw porNombre();
    }
}
