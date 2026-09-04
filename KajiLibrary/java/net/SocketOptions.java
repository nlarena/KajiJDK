package java.net;

// El vocabulario viejo de las opciones de socket: un entero por opcion y `Object` por valor.
//
// ===========================================================================================
// POR QUE HAY DOS FORMAS DE NOMBRAR LO MISMO
// ===========================================================================================
//
// KajiJDK tiene `StandardSocketOptions`, donde cada opcion es un par (nombre, tipo) y el compilador
// puede rechazar `setOption(SO_KEEPALIVE, 5)`. **Esta interfaz es la anterior**, y su costo se ve de
// una: la opcion es un `int` cualquiera y el valor un `Object` cualquiera, asi que confundir dos
// opciones o pasar el tipo equivocado no se descubre hasta ejecucion, del otro lado del socket.
//
// Se conserva porque es la que implementan `SocketImpl` y `DatagramSocketImpl` --su firma la fija el
// JDK-- y sacarla dejaria a esas dos sin poder declararse. No es una alternativa: es la capa de
// abajo, y `StandardSocketOptions` es la de arriba.
//
// ===========================================================================================
// QUE ENTRA
// ===========================================================================================
//
// Las quince constantes y los dos metodos, o sea todo. Las constantes son numeros acordados --el
// contrato dice cuales, y son estos-- y los dos metodos son **abstractos**: esta interfaz declara
// que alguien sabra leer y escribir opciones, no lo hace.
//
// Que en esta VM nadie la implemente con un socket de verdad no cambia nada de lo que esta escrito
// aca. Una interfaz sin implementaciones sigue siendo exactamente lo que promete: un contrato.
public interface SocketOptions {

    /** Mandar los datos apenas se escriben, sin juntarlos (algoritmo de Nagle apagado). */
    public static final int TCP_NODELAY = 0x0001;

    /** A que direccion local atarse. Solo de lectura: se fija al atar el socket. */
    public static final int SO_BINDADDR = 0x000F;

    /** Reusar una direccion que quedo en TIME_WAIT. */
    public static final int SO_REUSEADDR = 0x04;

    /** Permitir que varios sockets se aten al mismo puerto. */
    public static final int SO_REUSEPORT = 0x0E;

    /** Permitir mandar a la direccion de broadcast. */
    public static final int SO_BROADCAST = 0x0020;

    /** Por que interfaz salen los multicast (la forma vieja, con una direccion). */
    public static final int IP_MULTICAST_IF = 0x10;

    /** Por que interfaz salen los multicast (la forma nueva, con una interfaz). */
    public static final int IP_MULTICAST_IF2 = 0x1f;

    /** Si el que manda un multicast tambien lo recibe. */
    public static final int IP_MULTICAST_LOOP = 0x12;

    /** El campo "tipo de servicio" de la cabecera IP. */
    public static final int IP_TOS = 0x3;

    /** Cuanto esperar al cerrar a que salgan los datos pendientes. */
    public static final int SO_LINGER = 0x0080;

    /** Cuanto espera una lectura antes de darse por vencida. */
    public static final int SO_TIMEOUT = 0x1006;

    /** Tamano del buffer de salida. */
    public static final int SO_SNDBUF = 0x1001;

    /** Tamano del buffer de entrada. */
    public static final int SO_RCVBUF = 0x1002;

    /** Mandar sondas periodicas para detectar una conexion muerta. */
    public static final int SO_KEEPALIVE = 0x0008;

    /** Entregar los datos urgentes mezclados con los normales. */
    public static final int SO_OOBINLINE = 0x1003;

    /**
     * Fija una opcion.
     *
     * <p>Para las opciones que son un interruptor, {@code val} es un `Boolean`; apagarla se pide con
     * `Boolean.FALSE`, no con null.
     *
     * @throws SocketException si la opcion no se conoce, el valor no corresponde, o el socket la
     *                         rechaza
     */
    public void setOption(int optID, Object value) throws SocketException;

    /**
     * El valor de una opcion.
     *
     * <p>Las opciones que son un interruptor devuelven `Boolean.FALSE` cuando estan apagadas y el
     * valor cuando tienen uno -- de ahi que el tipo de retorno sea `Object` y no algo mas preciso.
     *
     * @throws SocketException si la opcion no se conoce o el socket no la puede leer
     */
    public Object getOption(int optID) throws SocketException;
}
