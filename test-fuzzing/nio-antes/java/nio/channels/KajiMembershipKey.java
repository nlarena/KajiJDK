package java.nio.channels;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;

/**
 * The receipt {@link KajiDatagramChannel#join} returns.
 *
 * <p>It keeps the three things that identify a membership --group, card and sender-- because all
 * three are needed to tell it apart: the same group through two cards are two memberships, and
 * dropping them by address instead of by key could not say which of the two.
 *
 * <h2>{@code block} and {@code unblock} are not sustained, and it is said</h2>
 *
 * <p>Filtering senders inside a group is a filter **of the system**, not of the program: its point
 * is that the blocked traffic does not even come up. The seam of this VM does not know how to ask
 * for it, and doing it in Java --discarding the packets after receiving them-- would fulfil the
 * signature and not the promise: the traffic would go on coming up, the bandwidth would go on being
 * spent, and whoever used `block` to defend themselves from a flood would not be defended. They
 * throw {@link UnsupportedOperationException}, which is what the contract foresees for a stack that
 * does not filter by sender.
 */
final class KajiMembershipKey extends MembershipKey {

    private final KajiDatagramChannel chan;
    private final InetAddress groupAddr;
    private final NetworkInterface card;
    private final InetAddress sender;

    /** How the card was named to the seam; it is needed to drop out by the same road. */
    private final String cardName;

    private boolean valid = true;

    KajiMembershipKey(KajiDatagramChannel chan, InetAddress groupAddr, NetworkInterface card,
            InetAddress sender, String cardName) {
        this.chan = chan;
        this.groupAddr = groupAddr;
        this.card = card;
        this.sender = sender;
        this.cardName = cardName;
    }

    public boolean isValid() {
        return this.valid;
    }

    public void drop() {
        // Over an already invalid key it does nothing, and that idempotence is on purpose: the
        // dropping also happens by itself when the channel is closed, so the explicit `drop()` and the
        // closing step on each other all the time in any program that cleans up properly.
        if (!this.valid) {
            return;
        }
        this.valid = false;
        this.chan.release(this);
    }

    public MembershipKey block(InetAddress source) throws IOException {
        throw new UnsupportedOperationException("source filtering not supported");
    }

    public MembershipKey unblock(InetAddress source) {
        throw new UnsupportedOperationException("source filtering not supported");
    }

    public MulticastChannel channel() {
        return this.chan;
    }

    public InetAddress group() {
        return this.groupAddr;
    }

    public InetAddress sourceAddress() {
        return this.sender;
    }

    public NetworkInterface networkInterface() {
        return this.card;
    }

    // ---- what the channel needs --------------------------------------------------------------

    String card() {
        return this.cardName;
    }

    boolean sameCard(NetworkInterface other) {
        return this.card == null ? other == null : this.card.equals(other);
    }

    // It invalidates it without dropping anything: it is used by the closing of the channel, which
    // closes the socket already and with that the system releases every membership at once.
    void invalidateKey() {
        this.valid = false;
    }
}
