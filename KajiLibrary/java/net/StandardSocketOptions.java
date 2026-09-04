package java.net;

// Los nombres de las opciones de socket que la plataforma define.
//
// Cada constante es un par (nombre, tipo del valor) y nada mas: describe **que se puede pedir**, no
// lo pide. Por eso la clase entra completa en KajiJDK aunque no haya sockets -- es un vocabulario,
// y un vocabulario no promete que haya con quien hablarlo. El dia que exista un socket, estas
// mismas constantes son las que va a aceptar.
//
// Que el tipo viaje en la constante (`SocketOption<Boolean>` y no `SocketOption`) es lo que hace que
// `setOption(SO_KEEPALIVE, 5)` no compile. Sin eso habria que pasar `Object` y descubrir el error
// en ejecucion, del otro lado del socket.
//
// Estan las once. `IP_MULTICAST_IF` --cuyo tipo es `SocketOption<NetworkInterface>`-- estuvo un
// rato afuera porque `NetworkInterface` no existia en este arbol; ahora existe, con sus busquedas
// tirando `SocketException` en vez de inventar placas (ver la cabecera de esa clase), y eso alcanza
// y sobra para nombrar el tipo de una constante. La constante no promete nada de por si: dice que
// **existe** una opcion de socket cuyo valor es una placa, y eso es cierto.
public final class StandardSocketOptions {

    private StandardSocketOptions() {
    }

    /** Permitir mandar datagramas a la direccion de broadcast. */
    public static final SocketOption<Boolean> SO_BROADCAST =
            new StdSocketOption<Boolean>("SO_BROADCAST", Boolean.class);

    /** Mandar sondas periodicas para detectar conexiones muertas. */
    public static final SocketOption<Boolean> SO_KEEPALIVE =
            new StdSocketOption<Boolean>("SO_KEEPALIVE", Boolean.class);

    /** Tamano del buffer de salida, en bytes. */
    public static final SocketOption<Integer> SO_SNDBUF =
            new StdSocketOption<Integer>("SO_SNDBUF", Integer.class);

    /** Tamano del buffer de entrada, en bytes. */
    public static final SocketOption<Integer> SO_RCVBUF =
            new StdSocketOption<Integer>("SO_RCVBUF", Integer.class);

    /** Reusar una direccion que quedo en TIME_WAIT. */
    public static final SocketOption<Boolean> SO_REUSEADDR =
            new StdSocketOption<Boolean>("SO_REUSEADDR", Boolean.class);

    /** Permitir que varios sockets escuchen el mismo puerto y repartirse las conexiones. */
    public static final SocketOption<Boolean> SO_REUSEPORT =
            new StdSocketOption<Boolean>("SO_REUSEPORT", Boolean.class);

    /** Segundos que `close` espera a que se vacie el buffer de salida; negativo lo desactiva. */
    public static final SocketOption<Integer> SO_LINGER =
            new StdSocketOption<Integer>("SO_LINGER", Integer.class);

    /** El campo "type of service" de la cabecera IP. */
    public static final SocketOption<Integer> IP_TOS =
            new StdSocketOption<Integer>("IP_TOS", Integer.class);

    /** Por que placa salen los datagramas multicast. */
    public static final SocketOption<NetworkInterface> IP_MULTICAST_IF =
            new StdSocketOption<NetworkInterface>("IP_MULTICAST_IF", NetworkInterface.class);

    /** Cuantos saltos vive un datagrama multicast. */
    public static final SocketOption<Integer> IP_MULTICAST_TTL =
            new StdSocketOption<Integer>("IP_MULTICAST_TTL", Integer.class);

    /** Si los datagramas multicast que se mandan tambien se reciben localmente. */
    public static final SocketOption<Boolean> IP_MULTICAST_LOOP =
            new StdSocketOption<Boolean>("IP_MULTICAST_LOOP", Boolean.class);

    /** Mandar los datos apenas se escriben, sin esperar a juntar un paquete lleno (algoritmo de Nagle). */
    public static final SocketOption<Boolean> TCP_NODELAY =
            new StdSocketOption<Boolean>("TCP_NODELAY", Boolean.class);

    // Un par (nombre, tipo) inmutable. No define `equals`: las opciones se comparan por identidad,
    // que es lo que corresponde para constantes, y es lo que hace el JDK.
    private static class StdSocketOption<T> implements SocketOption<T> {

        private final String name;
        private final Class<T> type;

        StdSocketOption(String name, Class<T> type) {
            this.name = name;
            this.type = type;
        }

        public String name() {
            return this.name;
        }

        public Class<T> type() {
            return this.type;
        }

        public String toString() {
            return this.name;
        }
    }
}
