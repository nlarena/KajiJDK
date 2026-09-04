package jdk.internal.net;

/**
 * La costura de TCP entre la biblioteca y la VM.
 *
 * <p>El mismo diseno que {@link jdk.internal.proc.Proc}: el nativo hace lo minimo y **no sabe nada
 * de las clases de Java**. Toma y devuelve cadenas, arreglos y enteros; quien sea `Socket` es
 * problema del lado Java, que puede cambiar sin tocar la VM.
 *
 * <p>Un socket es un `handle`, un indice en una tabla de la VM. Hace falta un handle y no una
 * operacion de una sola vez porque un socket **es** estado entre llamadas: su par, sus plazos, si ya
 * se cerro una de sus mitades. Los handles **no se reciclan**, asi que uno viejo nunca apunta a un
 * socket nuevo -- que es el error que seria mas dificil de encontrar.
 *
 * <h2>Los codigos de error, que no son todos -1</h2>
 *
 * <p>{@link #read} devuelve **-1** en fin de flujo y **-2** si vencio el plazo, y esa distincion es
 * el motivo de que no haya un solo centinela: una conexion cerrada y una que sigue viva pero callada
 * son dos cosas distintas, y el contrato de `Socket` las distingue --`SocketTimeoutException` no es
 * un EOF--. Con un solo -1 no habria forma de saber cual paso.
 *
 * <h2>Nada de aca espera: el -3</h2>
 *
 * <p>{@link #read} y {@link #accept} devuelven **-3** para "todavia no hay nada", y **ninguna de las
 * dos bloquea**. Tiene que ser asi: los hilos de Java de esta VM comparten un interprete, asi que un
 * nativo que se quede esperando adentro no deja correr a ningun otro hilo --incluido el que iba a
 * conectar o a contestar--. Un `accept` bloqueante no seria lento, seria un abrazo mortal.
 *
 * <p>La espera va del lado Java: reintentar con un {@link Thread#sleep} corto entre intentos.
 * Dormir si es algo que esta VM sabe manejar --suelta el interprete-- asi que el hilo que espera no
 * le impide a nadie avanzar. Y como el tiempo lo cuenta Java, `ServerSocket.accept` puede respetar
 * `setSoTimeout`, cosa que un accept del sistema no permitia.
 */
public final class Net {

    private Net() {
    }

    /**
     * Conecta a ese host y puerto. Devuelve el handle, o -1 si no se pudo.
     *
     * @param timeoutMs cero significa sin limite
     */
    public static native int connect(String host, int port, int timeoutMs);

    /**
     * Ata y escucha. Devuelve el handle, o -1.
     *
     * <p>Un puerto cero deja que el sistema elija; el que toco se lee con {@link #localPort}.
     */
    public static native int listen(String host, int port, int backlog);

    /**
     * Acepta una conexion **sin esperar**. Devuelve el handle del socket nuevo, **-3** si todavia no
     * hay nadie, o -1 si fallo. Ver la cabecera: quien quiera esperar, reintenta.
     */
    public static native int accept(int handle);

    /**
     * Lee en `buf` **sin esperar**. Devuelve cuantos bytes puso, **-1** en fin de flujo, **-3** si
     * todavia no llego nada. El **-2** queda reservado para el plazo vencido, que lo decide quien
     * llama: este nativo no cuenta tiempo.
     */
    public static native int read(int handle, byte[] buf, int off, int len);

    /** Escribe. `true` solo si se escribio todo. */
    public static native boolean write(int handle, byte[] buf, int off, int len);

    /** Cierra el socket. Un handle que no existe se ignora. */
    public static native void close(int handle);

    /** Cierra la mitad de lectura. */
    public static native boolean shutdownIn(int handle);

    /** Cierra la mitad de escritura. */
    public static native boolean shutdownOut(int handle);

    /** El puerto local, o -1. Sirve para un flujo y para un escucha. */
    public static native int localPort(int handle);

    /** La direccion local, o `null`. */
    public static native String localAddress(int handle);

    /** El puerto del par, o -1. */
    public static native int remotePort(int handle);

    /** La direccion del par, o `null`. */
    public static native String remoteAddress(int handle);

    /** El plazo de lectura en milisegundos; cero es sin limite. */
    public static native boolean setSoTimeout(int handle, int ms);

    /** Prende o apaga el algoritmo de Nagle. */
    public static native boolean setTcpNoDelay(int handle, boolean on);

    // ---- UDP -------------------------------------------------------------------------------
    //
    // Mismas reglas que TCP: nada de aca espera, y "todavia no llego nada" es **-3**. Un datagrama
    // no tiene fin de flujo --no hay conexion que cerrar-- asi que el -1 es siempre un error.

    /**
     * Ata un socket de datagramas. Devuelve el handle, o -1.
     *
     * <p>Puerto cero: lo elige el sistema, y se lee con {@link #localPort}.
     */
    public static native int udpBind(String host, int port);

    /** Manda un datagrama. `true` solo si salio entero: un datagrama partido no es un datagrama. */
    public static native boolean udpSend(int handle, String host, int port,
            byte[] buf, int off, int len);

    /**
     * Recibe un datagrama **sin esperar**. Devuelve cuantos bytes puso, **-3** si todavia no llego
     * nada, -1 si fallo.
     *
     * <p>Deja anotado el remitente, que se lee con {@link #udpSenderAddress} y
     * {@link #udpSenderPort}. Los tres son **una sola operacion**: quien reciba tiene que leer el
     * remitente antes de que otro hilo reciba sobre el mismo socket.
     */
    public static native int udpReceive(int handle, byte[] buf, int off, int len);

    /** De quien vino el ultimo datagrama recibido, o `null` si no hubo ninguno. */
    public static native String udpSenderAddress(int handle);

    /** De que puerto vino el ultimo datagrama recibido, o -1. */
    public static native int udpSenderPort(int handle);

    /**
     * Entra a un grupo multicast.
     *
     * @param iface la placa: una direccion IPv4, un indice de placa en IPv6, o la cadena vacia para
     *     dejar que el sistema elija
     */
    public static native boolean udpJoin(int handle, String group, String iface);

    /** Sale de un grupo multicast. Ver {@link #udpJoin}. */
    public static native boolean udpLeave(int handle, String group, String iface);

    /** El limite de saltos de los paquetes multicast que salgan de ese socket. */
    public static native boolean udpSetTtl(int handle, int ttl);

    // ---- las dos que si tienen que bloquear --------------------------------------------------
    //
    // Atar la punta local antes de conectar, y probar si un host contesta, necesitan las dos un
    // `connect` **de verdad** del sistema, que bloquea. Se arrancan aca, corren en un hilo del
    // sistema aparte, y la respuesta se recoge con {@link #answerPoll} sin colgar la VM.

    /**
     * Empieza a probar si ese host contesta. Devuelve el id del casillero.
     *
     * <p>Un rechazo cuenta como respuesta: el RST lo manda el host, asi que prueba que esta vivo
     * igual que una conexion aceptada. Solo el silencio cuenta como no alcanzable.
     *
     * @param local la direccion por la que sale la prueba, o la cadena vacia para dejar que el
     *     sistema elija
     * @param ttl el limite de saltos, o cero para el que venga por omision
     */
    public static native int reachableStart(String host, String local, int ttl);

    /**
     * Empieza a conectar a {@code host}:{@code port} **saliendo por** {@code local}:{@code
     * localPort}. Devuelve el id del casillero; la respuesta es el handle del socket, o -1.
     *
     * @param local la cadena vacia para el comodin, que es lo que pide una direccion local null
     */
    public static native int connectFromStart(String host, int port, String local, int localPort);

    /**
     * La respuesta, o **-3** si todavia no llego.
     *
     * <p>Que significa depende de quien pregunte: la sonda de alcance contesta 1 o 0, el connect
     * contesta el handle o -1.
     */
    public static native int answerPoll(int answer);

    /** Suelta el casillero. Hay que llamarlo siempre, se haya esperado la respuesta o no. */
    public static native void answerFree(int answer);

    /**
     * Manda un byte **fuera de banda**.
     *
     * <p>No es escribir en el flujo: va con una bandera del protocolo, y el que lo recibe lo ve por
     * un camino aparte.
     */
    public static native boolean sendUrgent(int handle, int b);
}
