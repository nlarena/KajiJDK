package jdk.net;

import java.net.SocketOption;

/**
 * Opciones de socket que el JDK ofrece fuera del conjunto estandar.
 *
 * <h2>Que las separa de {@link java.net.StandardSocketOptions}</h2>
 *
 * <p>Que <strong>ninguna esta garantizada</strong>. Cada una depende de que el sistema operativo la
 * tenga: {@link #TCP_QUICKACK} es de Linux, {@link #SO_PEERCRED} solo tiene sentido en un socket de
 * dominio Unix, y las tres {@code TCP_KEEP*} existen en casi todos lados pero no en todos. Pedir una
 * que la plataforma no soporta tira {@link UnsupportedOperationException}.
 *
 * <p>Por eso son un conjunto aparte y no constantes mas en la clase estandar: el conjunto estandar
 * es un contrato que toda implementacion de Java cumple, y estas no lo son. Antes de usarlas
 * conviene consultar el {@code supportedOptions()} del socket.
 */
public final class ExtendedSocketOptions {

    private ExtendedSocketOptions() {
    }

    /**
     * Confirmar enseguida en vez de esperar a ver si hay datos que mandar de vuelta.
     *
     * <p>TCP retrasa los ACK a proposito, para poder viajar pegados a la respuesta y ahorrar un
     * paquete. En un protocolo de pedido y respuesta esa espera es latencia pura, y esto la apaga.
     * Solo Linux.
     */
    public static final SocketOption<Boolean> TCP_QUICKACK =
            new Opcion<Boolean>("TCP_QUICKACK", Boolean.class);

    /** Cuantos segundos de silencio antes de mandar la primera sonda de keep-alive. */
    public static final SocketOption<Integer> TCP_KEEPIDLE =
            new Opcion<Integer>("TCP_KEEPIDLE", Integer.class);

    /** Cuantos segundos entre sondas. */
    public static final SocketOption<Integer> TCP_KEEPINTERVAL =
            new Opcion<Integer>("TCP_KEEPINTERVAL", Integer.class);

    /** Cuantas sondas sin respuesta antes de dar la conexion por muerta. */
    public static final SocketOption<Integer> TCP_KEEPCOUNT =
            new Opcion<Integer>("TCP_KEEPCOUNT", Integer.class);

    /**
     * El identificador NAPI de la interfaz por la que entran los paquetes.
     *
     * <p>Es de solo lectura y sirve para una cosa: acomodar los hilos que atienden una conexion
     * cerca de la cola de red que la recibe. Fuera de ese uso no dice nada.
     */
    public static final SocketOption<Integer> SO_INCOMING_NAPI_ID =
            new Opcion<Integer>("SO_INCOMING_NAPI_ID", Integer.class);

    /**
     * Quien esta del otro lado, en un socket de dominio Unix.
     *
     * <p>Solo lectura, y la unica opcion de esta clase que devuelve una identidad en vez de un
     * numero. Ver {@link UnixDomainPrincipal} para por que esto es posible aca y no sobre TCP.
     */
    public static final SocketOption<UnixDomainPrincipal> SO_PEERCRED =
            new Opcion<UnixDomainPrincipal>("SO_PEERCRED", UnixDomainPrincipal.class);

    /**
     * No fragmentar: un datagrama mas grande que el MTU falla en vez de partirse.
     *
     * <p>Es como se descubre el MTU del camino, y como un protocolo que ya trae su propia
     * fragmentacion evita que IP le agregue otra encima.
     */
    public static final SocketOption<Boolean> IP_DONTFRAGMENT =
            new Opcion<Boolean>("IP_DONTFRAGMENT", Boolean.class);

    /**
     * Una opcion: un nombre y un tipo.
     *
     * <p>Privada porque el conjunto es cerrado — son las siete constantes de arriba. Fabricar una
     * mas daria un objeto que ningun socket sabe atender.
     */
    private static class Opcion<T> implements SocketOption<T> {

        private final String nombre;
        private final Class<T> tipo;

        Opcion(String nombre, Class<T> tipo) {
            this.nombre = nombre;
            this.tipo = tipo;
        }

        public String name() {
            return this.nombre;
        }

        public Class<T> type() {
            return this.tipo;
        }

        public String toString() {
            return this.nombre;
        }
    }
}
