package com.sun.nio.sctp;

import java.net.SocketAddress;

/**
 * Las opciones de socket que define el JDK para SCTP.
 *
 * <p>Cada constante es un objeto con nombre y tipo, no una cadena ni un entero, y eso es lo que
 * hace que {@code setOption(SCTP_NODELAY, 5)} no compile: el tipo del valor viaja en el tipo de la
 * opcion. Es el mismo diseno que {@link java.net.StandardSocketOptions}.
 */
public class SctpStandardSocketOptions {

    private SctpStandardSocketOptions() {
    }

    /**
     * Cuantos flujos pedir en cada sentido al negociar la asociacion.
     *
     * <p>Tiene que fijarse <strong>antes</strong> de conectar: los maximos de una asociacion se
     * negocian al establecerla y despues no se mueven. Es la razon de que sea un objeto de dos
     * numeros y no dos opciones sueltas — las dos van en el mismo mensaje de negociacion.
     */
    public static class InitMaxStreams {

        private final int maxInStreams;
        private final int maxOutStreams;

        private InitMaxStreams(int maxInStreams, int maxOutStreams) {
            this.maxInStreams = maxInStreams;
            this.maxOutStreams = maxOutStreams;
        }

        /**
         * @throws IllegalArgumentException si alguno es negativo o pasa de {@code 65535}
         */
        public static InitMaxStreams create(int maxInStreams, int maxOutStreams) {
            if (maxOutStreams < 0 || maxOutStreams > 65535) {
                throw new IllegalArgumentException("maxOutStreams fuera de rango: "
                        + String.valueOf(maxOutStreams));
            }
            if (maxInStreams < 0 || maxInStreams > 65535) {
                throw new IllegalArgumentException("maxInStreams fuera de rango: "
                        + String.valueOf(maxInStreams));
            }
            return new InitMaxStreams(maxInStreams, maxOutStreams);
        }

        /** Cuantos flujos entrantes pedir. */
        public int maxInStreams() {
            return this.maxInStreams;
        }

        /** Cuantos flujos salientes pedir. */
        public int maxOutStreams() {
            return this.maxOutStreams;
        }

        public String toString() {
            return "[maxInStreams:" + String.valueOf(this.maxInStreams)
                    + ", maxOutStreams:" + String.valueOf(this.maxOutStreams) + "]";
        }

        public boolean equals(Object obj) {
            if (obj instanceof InitMaxStreams) {
                InitMaxStreams otro = (InitMaxStreams) obj;
                return otro.maxInStreams == this.maxInStreams
                        && otro.maxOutStreams == this.maxOutStreams;
            }
            return false;
        }

        public int hashCode() {
            // Los dos campos entran en 16 bits cada uno, asi que concatenarlos es inyectivo: dos
            // pares distintos no pueden colisionar. Un `31 * a + b` si podria.
            return (this.maxInStreams << 16) | this.maxOutStreams;
        }
    }

    /** No fragmentar: un mensaje mas grande que el MTU falla en vez de partirse. */
    public static final SctpSocketOption<Boolean> SCTP_DISABLE_FRAGMENTS =
            new Opcion<Boolean>("SCTP_DISABLE_FRAGMENTS", Boolean.class);

    /** Un mensaje se manda recien cuando quien envia lo marca completo. */
    public static final SctpSocketOption<Boolean> SCTP_EXPLICIT_COMPLETE =
            new Opcion<Boolean>("SCTP_EXPLICIT_COMPLETE", Boolean.class);

    /** Cuanto se intercalan los mensajes de flujos distintos al entregarlos. */
    public static final SctpSocketOption<Integer> SCTP_FRAGMENT_INTERLEAVE =
            new Opcion<Integer>("SCTP_FRAGMENT_INTERLEAVE", Integer.class);

    /** Cuantos flujos pedir al negociar; ver {@link InitMaxStreams}. */
    public static final SctpSocketOption<InitMaxStreams> SCTP_INIT_MAXSTREAMS =
            new Opcion<InitMaxStreams>("SCTP_INIT_MAXSTREAMS", InitMaxStreams.class);

    /** Mandar enseguida en vez de juntar mensajes chicos. El {@code TCP_NODELAY} de SCTP. */
    public static final SctpSocketOption<Boolean> SCTP_NODELAY =
            new Opcion<Boolean>("SCTP_NODELAY", Boolean.class);

    /** Cual de las direcciones del par usar por omision. */
    public static final SctpSocketOption<SocketAddress> SCTP_PRIMARY_ADDR =
            new Opcion<SocketAddress>("SCTP_PRIMARY_ADDR", SocketAddress.class);

    /** Pedirle al par que use esta direccion nuestra como primaria. */
    public static final SctpSocketOption<SocketAddress> SCTP_SET_PEER_PRIMARY_ADDR =
            new Opcion<SocketAddress>("SCTP_SET_PEER_PRIMARY_ADDR", SocketAddress.class);

    /** Tamano del buffer de envio. */
    public static final SctpSocketOption<Integer> SO_SNDBUF =
            new Opcion<Integer>("SO_SNDBUF", Integer.class);

    /** Tamano del buffer de recepcion. */
    public static final SctpSocketOption<Integer> SO_RCVBUF =
            new Opcion<Integer>("SO_RCVBUF", Integer.class);

    /** Cuanto esperar al cerrar a que salga lo que quedo pendiente. */
    public static final SctpSocketOption<Integer> SO_LINGER =
            new Opcion<Integer>("SO_LINGER", Integer.class);

    /**
     * Una opcion: un nombre y un tipo.
     *
     * <p>Privada porque el conjunto de opciones es cerrado — son las diez constantes de arriba— y
     * dejar fabricar mas daria objetos que ninguna implementacion sabe atender.
     */
    private static class Opcion<T> implements SctpSocketOption<T> {

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
