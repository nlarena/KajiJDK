package java.net.spi;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.stream.Stream;

/**
 * KajiLibrary's java.net.spi.InetAddressResolver -- the one that translates names into addresses.
 *
 * <p>It is the socket that lets the system's name resolution be replaced. It serves for rather more
 * than "using another DNS": it allows names to be resolved from a service discovery, or answers to be
 * fixed in a test so that it does not depend on the network.
 *
 * <p>The two methods are <b>not inverses</b>, and that is worth keeping in mind. A name may have
 * several addresses --which is why the outward direction returns a stream-- and an address may have
 * several names or none, but the return direction gives only one. Besides, the return direction is
 * controlled by whoever owns the address, not by whoever owns the name, so a name obtained this way
 * <b>proves</b> nothing: using it to authorize is this API's classic mistake.
 */
public interface InetAddressResolver {

    /**
     * The addresses of that name.
     *
     * @param host the name to resolve
     * @param lookupPolicy which families are asked for and in what order
     * @return a stream, possibly holding several addresses
     * @throws UnknownHostException if the name does not resolve
     */
    Stream<InetAddress> lookupByName(String host, LookupPolicy lookupPolicy)
        throws UnknownHostException;

    /**
     * The name of that address.
     *
     * @param addr the raw bytes, 4 or 16
     * @throws UnknownHostException if there is no name
     */
    String lookupByAddress(byte[] addr) throws UnknownHostException;

    /**
     * What is asked for in a lookup: which address families and in what order.
     *
     * <p>It is a set of bits and not an enum because the two questions are independent: <b>which</b>
     * to bring (IPv4, IPv6 or both) and <b>which first</b>. An enum with every valid combination
     * would have six values and would not say why.
     *
     * <p>{@link #of} rejects the combinations that mean nothing. There are three rules:
     *
     * <ul>
     *   <li>at least one family has to be asked for -- asking for "none, IPv4 first" is nothing;
     *   <li>both orders cannot be asked for at once;
     *   <li>an order can only be asked for over a family that is being asked for:
     *       {@code IPV4_FIRST} with IPv6 alone is a contradiction.
     * </ul>
     *
     * <p>The bits that are none of the four are let through as they stand: they are the road by which
     * this set can grow without invalidating old code.
     */
    final class LookupPolicy {

        /** IPv4 addresses are asked for. */
        public static final int IPV4 = 1 << 0;

        /** IPv6 addresses are asked for. */
        public static final int IPV6 = 1 << 1;

        /** The IPv4 ones come first in the result. */
        public static final int IPV4_FIRST = 1 << 2;

        /** The IPv6 ones come first in the result. */
        public static final int IPV6_FIRST = 1 << 3;

        private final int characteristics;

        private LookupPolicy(int characteristics) {
            this.characteristics = characteristics;
        }

        /**
         * A policy with those bits.
         *
         * @throws IllegalArgumentException if the combination means nothing; see the three rules in
         *     the class's note
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

        /** The bits, exactly as they were passed. */
        public int characteristics() {
            return this.characteristics;
        }
    }
}
