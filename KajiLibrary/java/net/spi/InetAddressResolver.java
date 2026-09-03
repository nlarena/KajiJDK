package java.net.spi;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.stream.Stream;

/**
 * KajiLibrary's java.net.spi.InetAddressResolver -- quien traduce nombres a direcciones.
 *
 * <p>Es el enchufe que deja reemplazar la resolucion de nombres del sistema. Sirve para bastante mas
 * que para "usar otro DNS": permite resolver nombres de un descubrimiento de servicios, o fijar
 * respuestas en una prueba para que no dependa de la red.
 *
 * <p>Los dos metodos <b>no son inversos</b>, y conviene tenerlo presente. Un nombre puede tener
 * varias direcciones --por eso la ida devuelve un flujo-- y una direccion puede tener varios nombres
 * o ninguno, pero la vuelta devuelve uno solo. Ademas la vuelta la controla quien es dueño de la
 * direccion, no quien es dueño del nombre, asi que un nombre obtenido asi <b>no prueba</b> nada:
 * usarlo para autorizar es el error clasico de este API.
 */
public interface InetAddressResolver {

    /**
     * Las direcciones de ese nombre.
     *
     * @param host el nombre a resolver
     * @param lookupPolicy que familias se piden y en que orden
     * @return un flujo, posiblemente con varias direcciones
     * @throws UnknownHostException si el nombre no resuelve
     */
    Stream<InetAddress> lookupByName(String host, LookupPolicy lookupPolicy)
        throws UnknownHostException;

    /**
     * El nombre de esa direccion.
     *
     * @param addr los bytes crudos, 4 o 16
     * @throws UnknownHostException si no hay nombre
     */
    String lookupByAddress(byte[] addr) throws UnknownHostException;

    /**
     * Que se pide en una resolucion: que familias de direcciones y en que orden.
     *
     * <p>Es un juego de bits y no un enum porque las dos preguntas son independientes: <b>cuales</b>
     * traer (IPv4, IPv6 o las dos) y <b>cual primero</b>. Un enum con todas las combinaciones
     * validas tendria seis valores y no diria por que.
     *
     * <p>{@link #of} rechaza las combinaciones que no significan nada. Las reglas son tres:
     *
     * <ul>
     *   <li>tiene que pedirse al menos una familia -- pedir "ninguna, IPv4 primero" no es nada;
     *   <li>no se pueden pedir los dos ordenes a la vez;
     *   <li>un orden solo se puede pedir sobre una familia que se esta pidiendo: {@code IPV4_FIRST}
     *       con solo IPv6 es una contradiccion.
     * </ul>
     *
     * <p>Los bits que no son ninguno de los cuatro se dejan pasar tal cual: son la via por la que
     * este juego puede crecer sin invalidar codigo viejo.
     */
    final class LookupPolicy {

        /** Se piden direcciones IPv4. */
        public static final int IPV4 = 1 << 0;

        /** Se piden direcciones IPv6. */
        public static final int IPV6 = 1 << 1;

        /** Las IPv4 van primero en el resultado. */
        public static final int IPV4_FIRST = 1 << 2;

        /** Las IPv6 van primero en el resultado. */
        public static final int IPV6_FIRST = 1 << 3;

        private final int characteristics;

        private LookupPolicy(int characteristics) {
            this.characteristics = characteristics;
        }

        /**
         * Una politica con esos bits.
         *
         * @throws IllegalArgumentException si la combinacion no significa nada; ver las tres reglas
         *     en la nota de la clase
         */
        public static LookupPolicy of(int characteristics) {
            if ((characteristics & (IPV4 | IPV6)) == 0) {
                throw new IllegalArgumentException("No address family specified");
            }
            if ((characteristics & IPV4_FIRST) != 0 && (characteristics & IPV6_FIRST) != 0) {
                throw new IllegalArgumentException("Both IPV4_FIRST and IPV6_FIRST are specified");
            }
            if ((characteristics & IPV4_FIRST) != 0 && (characteristics & IPV4) == 0) {
                throw new IllegalArgumentException("IPV4_FIRST is specified without IPV4");
            }
            if ((characteristics & IPV6_FIRST) != 0 && (characteristics & IPV6) == 0) {
                throw new IllegalArgumentException("IPV6_FIRST is specified without IPV6");
            }
            return new LookupPolicy(characteristics);
        }

        /** Los bits, tal como se pasaron. */
        public int characteristics() {
            return this.characteristics;
        }
    }
}
