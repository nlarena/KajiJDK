package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.MulticastChannel — a network channel that can join a multicast
 * group.
 *
 * <p>Joining a group is asking **one concrete network card** to start accepting the packets
 * addressed to a group address. Both data are necessary: on a machine with several cards there is
 * no right answer to "through which one", and the system cannot choose it for one.
 *
 * <h2>The two `join`s, which used not to be there</h2>
 *
 * <p>They were missing because of the **types** and not because of the sockets: both take a
 * `java.net.NetworkInterface`, which did not exist in this tree. It exists now, and with it both
 * `join`s -- and {@link MembershipKey#networkInterface()}, which was missing for the same reason.
 *
 * <p>The difference between the two is not one of comfort. The three-argument one asks **only of
 * that sender**: it is a source-specific membership (SSM), which the system has to sustain apart
 * and which not everyone sustains. That is why the contract allows it to throw {@link
 * UnsupportedOperationException} -- and it is not a way out, it is information: it means "this
 * stack does not filter by sender", which is different from "it could not be done".
 */
public interface MulticastChannel extends NetworkChannel {

    /**
     * Closes the channel.
     *
     * <p>It is redeclared --in the JDK as well-- to document that closing **drops every
     * membership**. Without that, a program that closes the channel and does not call `drop()`
     * would leave the card receiving traffic of a group nobody is interested in any more.
     */
    void close();

    /**
     * Joins the group {@code group} through the card {@code interf}.
     *
     * <p>Both data are necessary: on a machine with several cards there is no right answer to
     * "through which one", and the system cannot choose it for one.
     *
     * @return the receipt, with which it is dropped afterwards
     * @throws IllegalArgumentException if the address is not a multicast one, or if it is not of a
     *     family the card supports
     * @throws IllegalStateException if the channel is in that group through that card already
     * @throws java.nio.channels.ClosedChannelException if the channel is closed
     * @throws IOException if the operation failed
     */
    MembershipKey join(java.net.InetAddress group, java.net.NetworkInterface interf)
            throws IOException;

    /**
     * Joins the group {@code group} through the card {@code interf}, **but only for** the sender
     * {@code source}.
     *
     * <p>It is a source-specific membership. It serves when the group is public and the only thing
     * of interest is a known sender: the filtering is done by the system, so the traffic of the
     * others does not even come up.
     *
     * @throws UnsupportedOperationException if the stack does not sustain memberships by sender.
     *     The contract allows it, and saying so is more useful than failing as if it were a network
     *     error
     * @throws IllegalArgumentException if either of the two addresses does not serve for this
     * @throws IOException if the operation failed
     */
    MembershipKey join(java.net.InetAddress group, java.net.NetworkInterface interf,
            java.net.InetAddress source) throws IOException;
}
