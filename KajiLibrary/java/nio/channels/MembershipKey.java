package java.nio.channels;

import java.io.IOException;
import java.net.InetAddress;

/**
 * KajiLibrary's java.nio.channels.MembershipKey — the receipt of being in a multicast group.
 *
 * <p>It is what a `join` returns and the only thing with which one can afterwards drop out ({@link
 * #drop()}). That the dropping is asked for by the key and not by the address is not a whim: one
 * same channel can be in the same group through two different cards, and without the key there
 * would be no way of saying which of the two one wants to let go of.
 *
 * <p>{@link #block} and {@link #unblock} filter **senders** inside the group. They serve for the
 * ugly and frequent case: a group where somebody floods, and one wants to go on listening to the
 * others. The filter is the system's, not the program's, so the blocked traffic does not even come
 * up.
 *
 * <p>{@code networkInterface()} was missing because it returns `java.net.NetworkInterface`, which
 * did not exist in this tree. It exists now, and so does the method; it is the datum that completes
 * the key, because the same group address through two different cards are two different
 * memberships.
 */
public abstract class MembershipKey {

    protected MembershipKey() {
    }

    /** Whether the membership is still current. It stops being so on dropping it or on closing the
     * channel. */
    public abstract boolean isValid();

    /**
     * Drops the membership.
     *
     * <p>Over an already invalid key it does nothing, and that idempotence is on purpose: the
     * dropping also happens by itself when the channel is closed, so the explicit `drop()` and the
     * closing step on each other often and neither of the two has to fail because of that.
     */
    public abstract void drop();

    /**
     * Stops receiving whatever `source` sends inside this group.
     *
     * @throws IllegalStateException if the membership was asked for a specific source: filtering
     *         inside a group that is already filtered to a single sender means nothing
     */
    public abstract MembershipKey block(InetAddress source) throws IOException;

    /** Undoes a {@link #block}. */
    public abstract MembershipKey unblock(InetAddress source);

    /** The channel of this membership. */
    public abstract MulticastChannel channel();

    /** The address of the group. */
    public abstract InetAddress group();

    /** The source, if the membership was asked for a single one; `null` if it is for the whole
     * group. */
    public abstract InetAddress sourceAddress();

    /**
     * The card the membership was asked for through.
     *
     * <p>It is the half {@link #group()} is missing in order to identify the membership: the same
     * group through two cards are two keys, and this is the one that says which is which.
     */
    public abstract java.net.NetworkInterface networkInterface();
}
